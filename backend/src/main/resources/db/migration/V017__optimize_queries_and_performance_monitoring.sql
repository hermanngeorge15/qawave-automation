-- V017__optimize_queries_and_performance_monitoring.sql
-- Description: Additional query optimizations and performance monitoring setup
-- Author: Database Agent
-- Date: 2026-01-30
-- Issue: #143 - Optimize database queries and indexes

-- ============================================================================
-- INDEX AUDIT SUMMARY
-- ============================================================================
--
-- This migration completes the index optimization work for issue #143.
--
-- Already covered indexes (from previous migrations):
--   qa_packages:
--     - idx_qa_packages_status (status)
--     - idx_qa_packages_mode (mode)
--     - idx_qa_packages_created_at (created_at DESC)
--     - idx_qa_packages_status_created (status, created_at DESC)
--     - idx_qa_packages_triggered_by (triggered_by) - partial
--     - idx_qa_packages_name_search (GIN on name)
--
--   test_runs:
--     - idx_test_runs_scenario_id (scenario_id)
--     - idx_test_runs_qa_package_id (qa_package_id) - partial
--     - idx_test_runs_status (status)
--     - idx_test_runs_scenario_status (scenario_id, status)
--     - idx_test_runs_scenario_latest (scenario_id, created_at DESC)
--     - idx_test_runs_qa_package_status (qa_package_id, status) - partial
--     - idx_test_runs_qa_package_created (qa_package_id, created_at DESC) - partial
--
--   test_scenarios:
--     - idx_test_scenarios_suite_id (suite_id)
--     - idx_test_scenarios_status (status)
--     - idx_test_scenarios_qa_package_id (qa_package_id) - partial
--     - idx_test_scenarios_qa_package_created (qa_package_id, created_at DESC) - partial
--
--   qa_package_events:
--     - idx_qa_package_events_package_id (qa_package_id)
--     - idx_qa_package_events_type (event_type)
--     - idx_qa_package_events_timeline (qa_package_id, created_at)
--
--   test_step_results:
--     - idx_test_step_results_run_id (run_id)
--     - idx_test_step_results_run_order (run_id, step_index)
--     - idx_test_step_results_run_passed (run_id, passed) - partial
--     - idx_test_step_results_run_errors (run_id) WHERE failure_reason IS NOT NULL
--
-- ============================================================================

-- ============================================================================
-- PART 1: Additional composite indexes for common query patterns
-- ============================================================================

-- Index for finding slowest steps within a run (used by findSlowestStepByRunId)
-- The existing idx_test_step_results_slow only covers steps > 1000ms globally
CREATE INDEX IF NOT EXISTS idx_test_step_results_run_duration
ON test_step_results(run_id, duration_ms DESC NULLS LAST)
WHERE duration_ms IS NOT NULL;

COMMENT ON INDEX idx_test_step_results_run_duration IS
'Composite index for finding slowest steps within a specific run';

-- Index for test_runs filtered by scenario and ordered by status+time
-- Useful for dashboard queries showing run history with status filtering
CREATE INDEX IF NOT EXISTS idx_test_runs_scenario_status_created
ON test_runs(scenario_id, status, created_at DESC);

COMMENT ON INDEX idx_test_runs_scenario_status_created IS
'Composite index for scenario run history with status filtering';

-- Index for qa_packages filtered by mode and status (common dashboard query)
CREATE INDEX IF NOT EXISTS idx_qa_packages_mode_status
ON qa_packages(mode, status);

COMMENT ON INDEX idx_qa_packages_mode_status IS
'Composite index for filtering packages by both mode and status';

-- Index for ai_logs correlation lookups with time ordering
CREATE INDEX IF NOT EXISTS idx_ai_logs_correlation_created
ON ai_logs(correlation_id, created_at DESC)
WHERE correlation_id IS NOT NULL;

COMMENT ON INDEX idx_ai_logs_correlation_created IS
'Composite index for fetching AI logs by correlation in chronological order';

-- ============================================================================
-- PART 2: Performance monitoring views
-- ============================================================================

-- View to check index usage statistics
CREATE OR REPLACE VIEW v_index_usage AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS times_used,
    idx_tup_read AS rows_read,
    idx_tup_fetch AS rows_fetched,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

COMMENT ON VIEW v_index_usage IS
'Performance monitoring view showing index usage statistics. Use to identify unused indexes.';

-- View to identify tables with high sequential scans (potential missing indexes)
CREATE OR REPLACE VIEW v_tables_needing_indexes AS
SELECT
    schemaname,
    relname AS tablename,
    seq_scan AS sequential_scans,
    idx_scan AS index_scans,
    n_live_tup AS live_rows,
    CASE
        WHEN idx_scan > 0 THEN ROUND((seq_scan::numeric / idx_scan::numeric), 2)
        ELSE seq_scan::numeric
    END AS seq_to_idx_ratio,
    CASE
        WHEN seq_scan > idx_scan AND n_live_tup > 1000 THEN 'REVIEW NEEDED'
        WHEN seq_scan > idx_scan * 10 AND n_live_tup > 100 THEN 'WARNING'
        ELSE 'OK'
    END AS status
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY seq_scan DESC;

COMMENT ON VIEW v_tables_needing_indexes IS
'Performance monitoring view to identify tables that may need additional indexes';

-- View to check for slow/long-running queries in test runs
CREATE OR REPLACE VIEW v_slow_test_runs AS
SELECT
    r.id AS run_id,
    p.name AS package_name,
    s.name AS scenario_name,
    r.status,
    r.duration_ms,
    r.steps_total,
    r.steps_passed,
    r.steps_failed,
    r.created_at,
    r.finished_at
FROM test_runs r
LEFT JOIN qa_packages p ON r.qa_package_id = p.id
LEFT JOIN test_scenarios s ON r.scenario_id = s.id
WHERE r.duration_ms > 30000  -- Runs taking more than 30 seconds
ORDER BY r.duration_ms DESC;

COMMENT ON VIEW v_slow_test_runs IS
'Performance monitoring view showing test runs that took longer than 30 seconds';

-- View to get daily execution statistics
CREATE OR REPLACE VIEW v_daily_execution_stats AS
SELECT
    DATE(created_at) AS execution_date,
    COUNT(*) AS total_runs,
    COUNT(*) FILTER (WHERE status = 'PASSED') AS passed_runs,
    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_runs,
    ROUND(AVG(duration_ms)::numeric, 2) AS avg_duration_ms,
    MAX(duration_ms) AS max_duration_ms,
    SUM(steps_total) AS total_steps,
    SUM(steps_passed) AS total_steps_passed,
    SUM(steps_failed) AS total_steps_failed
FROM test_runs
WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY execution_date DESC;

COMMENT ON VIEW v_daily_execution_stats IS
'Performance monitoring view showing daily test execution statistics for the last 30 days';

-- ============================================================================
-- PART 3: Helper functions for query analysis
-- ============================================================================

-- Function to analyze query patterns for a specific table
CREATE OR REPLACE FUNCTION analyze_table_performance(table_name TEXT)
RETURNS TABLE (
    metric TEXT,
    value TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 'Table Size'::TEXT, pg_size_pretty(pg_total_relation_size(table_name::regclass))::TEXT
    UNION ALL
    SELECT 'Row Count'::TEXT, n_live_tup::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name
    UNION ALL
    SELECT 'Sequential Scans'::TEXT, seq_scan::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name
    UNION ALL
    SELECT 'Index Scans'::TEXT, idx_scan::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name
    UNION ALL
    SELECT 'Inserts'::TEXT, n_tup_ins::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name
    UNION ALL
    SELECT 'Updates'::TEXT, n_tup_upd::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name
    UNION ALL
    SELECT 'Deletes'::TEXT, n_tup_del::TEXT
    FROM pg_stat_user_tables WHERE relname = table_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION analyze_table_performance(TEXT) IS
'Helper function to get performance metrics for a specific table. Usage: SELECT * FROM analyze_table_performance(''qa_packages'')';

-- ============================================================================
-- PART 4: Query pattern documentation
-- ============================================================================

-- Document common query patterns and their recommended indexes
COMMENT ON TABLE qa_packages IS
'Container for full test runs with AI evaluation and coverage reporting.

Common Query Patterns:
1. List packages by status: SELECT * FROM qa_packages WHERE status = ? ORDER BY created_at DESC
   Indexes: idx_qa_packages_status, idx_qa_packages_status_created

2. Search packages by name: SELECT * FROM qa_packages WHERE name ILIKE ?
   Indexes: idx_qa_packages_name_search (GIN trigram)

3. Find incomplete packages: SELECT * FROM qa_packages WHERE status NOT IN (terminal states)
   Indexes: idx_qa_packages_pending, idx_qa_packages_running

4. Count by status for dashboard: SELECT status, COUNT(*) FROM qa_packages GROUP BY status
   Indexes: idx_qa_packages_status
';

COMMENT ON TABLE test_runs IS
'Execution instances of test scenarios.

Common Query Patterns:
1. Runs for a scenario: SELECT * FROM test_runs WHERE scenario_id = ? ORDER BY created_at DESC
   Indexes: idx_test_runs_scenario_id, idx_test_runs_scenario_latest

2. Runs for a package: SELECT * FROM test_runs WHERE qa_package_id = ?
   Indexes: idx_test_runs_qa_package_id, idx_test_runs_qa_package_created

3. Count passed/failed for package: SELECT COUNT(*) FROM test_runs WHERE qa_package_id = ? AND status = ?
   Indexes: idx_test_runs_qa_package_status

4. Recent failed runs: SELECT * FROM test_runs WHERE status = ''FAILED'' ORDER BY created_at DESC
   Indexes: idx_test_runs_failed
';

COMMENT ON TABLE test_scenarios IS
'Test scenarios containing ordered test steps as JSON.

Common Query Patterns:
1. Scenarios for a package: SELECT * FROM test_scenarios WHERE qa_package_id = ?
   Indexes: idx_test_scenarios_qa_package_id, idx_test_scenarios_qa_package_created

2. Scenarios for a suite: SELECT * FROM test_scenarios WHERE suite_id = ?
   Indexes: idx_test_scenarios_suite_id

3. Active scenarios: SELECT * FROM test_scenarios WHERE status = ''ACTIVE''
   Indexes: idx_test_scenarios_status, idx_test_scenarios_active
';

COMMENT ON TABLE test_step_results IS
'Per-step execution results for test runs.

Common Query Patterns:
1. Steps for a run: SELECT * FROM test_step_results WHERE run_id = ? ORDER BY step_index
   Indexes: idx_test_step_results_run_id, idx_test_step_results_run_order

2. Passed/failed steps: SELECT * FROM test_step_results WHERE run_id = ? AND passed = ?
   Indexes: idx_test_step_results_run_passed

3. Steps with errors: SELECT * FROM test_step_results WHERE run_id = ? AND failure_reason IS NOT NULL
   Indexes: idx_test_step_results_run_errors

4. Slowest step in run: SELECT * FROM test_step_results WHERE run_id = ? ORDER BY duration_ms DESC LIMIT 1
   Indexes: idx_test_step_results_run_duration
';

-- ============================================================================
-- PART 5: Enable pg_stat_statements if available (for query analysis)
-- ============================================================================

-- This extension must be enabled by a superuser, so we just create a comment
-- documenting how to enable it for production monitoring
COMMENT ON SCHEMA public IS
'QAWave database schema.

For production query monitoring, enable pg_stat_statements:
1. Add to postgresql.conf: shared_preload_libraries = ''pg_stat_statements''
2. Restart PostgreSQL
3. Run: CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
4. Query: SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 20;

Performance monitoring views available:
- v_index_usage: Check which indexes are being used
- v_tables_needing_indexes: Identify tables that may need indexes
- v_slow_test_runs: Find slow test executions
- v_daily_execution_stats: Daily test execution summary

Helper functions:
- analyze_table_performance(table_name): Get metrics for a specific table
';
