-- V019__add_coverage_export_indexes.sql
-- Description: Add optimized indexes for coverage reporting and export queries
-- Author: Database Agent
-- Date: 2026-01-31
-- Issue: #322 ENH-012

-- ============================================================================
-- INDEX RATIONALE
-- ============================================================================
-- These indexes optimize common query patterns used in:
-- 1. Coverage dashboard (latest snapshot per package)
-- 2. Export endpoints (CSV/JSON exports of runs and coverage)
-- 3. Failure analysis (filtering by status within runs/suites)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Coverage Snapshots: Latest coverage lookup per package
-- ----------------------------------------------------------------------------
-- Query pattern: SELECT * FROM coverage_snapshots
--                WHERE qa_package_id = ? ORDER BY created_at DESC LIMIT 1
-- Existing: idx_coverage_snapshots_package_id (qa_package_id only)
-- New: Composite index for efficient "latest snapshot" queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coverage_snapshots_package_latest
    ON coverage_snapshots(qa_package_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- Test Step Results: Failure filtering within runs
-- ----------------------------------------------------------------------------
-- Query pattern: SELECT * FROM test_step_results
--                WHERE run_id = ? AND status = 'FAILED'
-- Existing: Separate indexes on run_id and status
-- New: Composite index for efficient failure filtering per run
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_step_results_run_status
    ON test_step_results(run_id, status);

-- Composite index for export queries ordering by step
-- Query pattern: SELECT * FROM test_step_results
--                WHERE run_id = ? ORDER BY step_index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_step_results_run_step_order
    ON test_step_results(run_id, step_index, status);

-- ----------------------------------------------------------------------------
-- Test Scenarios: Suite filtering by status
-- ----------------------------------------------------------------------------
-- Query pattern: SELECT * FROM test_scenarios
--                WHERE suite_id = ? AND status = 'ACTIVE'
-- Existing: Separate indexes on suite_id and status
-- New: Composite index for efficient suite filtering by status
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_scenarios_suite_status
    ON test_scenarios(suite_id, status);

-- Composite for suite export with ordering
-- Query pattern: SELECT * FROM test_scenarios
--                WHERE suite_id = ? ORDER BY priority DESC, created_at DESC
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_scenarios_suite_priority
    ON test_scenarios(suite_id, priority DESC, created_at DESC);

-- ----------------------------------------------------------------------------
-- Operation Coverage: Export and gap analysis optimization
-- ----------------------------------------------------------------------------
-- Query pattern: SELECT * FROM operation_coverage
--                WHERE snapshot_id = ? AND status != 'COVERED'
-- New: Composite for gap export queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_operation_coverage_snapshot_status
    ON operation_coverage(snapshot_id, status);

-- Query pattern: SELECT http_method, COUNT(*) FROM operation_coverage
--                WHERE snapshot_id = ? GROUP BY http_method
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_operation_coverage_method_summary
    ON operation_coverage(snapshot_id, http_method, status);

-- ----------------------------------------------------------------------------
-- Test Runs: Export query optimization (need to check if table exists)
-- ----------------------------------------------------------------------------
-- Query pattern: SELECT * FROM test_runs
--                WHERE scenario_id = ? ORDER BY created_at DESC
-- This may already exist, adding if missing for export optimization

-- Check if test_runs table exists and add index
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'test_runs') THEN
        -- Composite index for scenario run history export
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_test_runs_scenario_created') THEN
            CREATE INDEX idx_test_runs_scenario_created
                ON test_runs(scenario_id, created_at DESC);
        END IF;

        -- Composite index for status filtering in exports
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_test_runs_status_created') THEN
            CREATE INDEX idx_test_runs_status_created
                ON test_runs(status, created_at DESC);
        END IF;
    END IF;
END
$$;

-- ============================================================================
-- STATISTICS UPDATE
-- ============================================================================
-- Update statistics for query planner optimization
ANALYZE coverage_snapshots;
ANALYZE operation_coverage;
ANALYZE test_step_results;
ANALYZE test_scenarios;

-- ============================================================================
-- DOCUMENTATION
-- ============================================================================
COMMENT ON INDEX idx_coverage_snapshots_package_latest IS 'Optimizes latest coverage snapshot lookup per package';
COMMENT ON INDEX idx_test_step_results_run_status IS 'Optimizes failure filtering within test runs';
COMMENT ON INDEX idx_test_step_results_run_step_order IS 'Optimizes step export ordering within runs';
COMMENT ON INDEX idx_test_scenarios_suite_status IS 'Optimizes suite filtering by scenario status';
COMMENT ON INDEX idx_test_scenarios_suite_priority IS 'Optimizes suite export with priority ordering';
COMMENT ON INDEX idx_operation_coverage_snapshot_status IS 'Optimizes coverage gap analysis queries';
COMMENT ON INDEX idx_operation_coverage_method_summary IS 'Optimizes HTTP method summary aggregations';
