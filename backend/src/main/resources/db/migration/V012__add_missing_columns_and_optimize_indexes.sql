-- V012__add_missing_columns_and_optimize_indexes.sql
-- Description: Add missing columns to align with R2DBC entities and optimize query indexes
-- Author: Database Agent
-- Date: 2026-01-26
-- Issue: #96 - Optimize database queries and indexes

-- ============================================================================
-- PART 1: Add missing columns to align entity definitions with schema
-- ============================================================================

-- Add qa_package_id to test_scenarios for direct package-to-scenario relationship
-- This allows scenarios to be linked directly to a QA package without going through test_suites
ALTER TABLE test_scenarios
ADD COLUMN qa_package_id UUID REFERENCES qa_packages(id) ON DELETE CASCADE;

-- Add triggered_by to qa_packages for tracking who initiated the test run
-- This is separate from created_by (who created the record vs who triggered execution)
ALTER TABLE qa_packages
ADD COLUMN triggered_by VARCHAR(255);

-- ============================================================================
-- PART 2: Optimize indexes for common query patterns from R2DBC repositories
-- ============================================================================

-- Index for test_scenarios.qa_package_id (used by TestScenarioR2dbcRepository.findByQaPackageId)
CREATE INDEX idx_test_scenarios_qa_package_id ON test_scenarios(qa_package_id)
WHERE qa_package_id IS NOT NULL;

-- Index for qa_packages.triggered_by (used by QaPackageR2dbcRepository.findByTriggeredBy)
CREATE INDEX idx_qa_packages_triggered_by ON qa_packages(triggered_by)
WHERE triggered_by IS NOT NULL;

-- Composite index for test_runs count queries by package and status
-- Used by: countPassedByQaPackageId, countFailedByQaPackageId
CREATE INDEX idx_test_runs_qa_package_status ON test_runs(qa_package_id, status)
WHERE qa_package_id IS NOT NULL;

-- Composite index for test_step_results queries filtering by run_id and passed
-- Used by: findByRunIdAndPassed, countPassedByRunId, countFailedByRunId
CREATE INDEX idx_test_step_results_run_passed ON test_step_results(run_id, passed)
WHERE passed IS NOT NULL;

-- Composite index for test_step_results with error_message
-- Used by: findWithErrorsByRunId
CREATE INDEX idx_test_step_results_run_errors ON test_step_results(run_id)
WHERE error_message IS NOT NULL;

-- ============================================================================
-- PART 3: Additional performance indexes based on common access patterns
-- ============================================================================

-- Composite index for qa_packages filtering by status and ordering by created_at
-- Used by paginated status queries
CREATE INDEX idx_qa_packages_status_created ON qa_packages(status, created_at DESC);

-- Composite index for test_scenarios by qa_package ordered by created_at
-- Used for fetching scenarios for a package in chronological order
CREATE INDEX idx_test_scenarios_qa_package_created ON test_scenarios(qa_package_id, created_at DESC)
WHERE qa_package_id IS NOT NULL;

-- Composite index for test_runs by qa_package ordered by created_at
-- Used for fetching runs for a package in chronological order
CREATE INDEX idx_test_runs_qa_package_created ON test_runs(qa_package_id, created_at DESC)
WHERE qa_package_id IS NOT NULL;

-- ============================================================================
-- PART 4: Index documentation
-- ============================================================================

COMMENT ON INDEX idx_test_scenarios_qa_package_id IS 'Index for finding scenarios by QA package ID';
COMMENT ON INDEX idx_qa_packages_triggered_by IS 'Index for finding packages triggered by a specific user';
COMMENT ON INDEX idx_test_runs_qa_package_status IS 'Composite index for counting runs by package and status';
COMMENT ON INDEX idx_test_step_results_run_passed IS 'Composite index for filtering step results by passed status';
COMMENT ON INDEX idx_test_step_results_run_errors IS 'Partial index for finding steps with errors';
COMMENT ON INDEX idx_qa_packages_status_created IS 'Composite index for paginated status-based queries';
COMMENT ON INDEX idx_test_scenarios_qa_package_created IS 'Composite index for chronological scenario listing by package';
COMMENT ON INDEX idx_test_runs_qa_package_created IS 'Composite index for chronological run listing by package';
