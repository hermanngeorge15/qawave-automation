-- V018__add_user_ownership.sql
-- Description: Add user_id columns to enable multi-user data isolation
-- Author: Database Agent
-- Date: 2026-01-30
-- Issue: #195 - Add user_id to database schema
--
-- This migration prepares the schema for Keycloak integration.
-- user_id will store the Keycloak 'sub' (subject) claim.
-- Existing data uses 'system' as the default user.

-- ============================================================================
-- PART 1: Add user_id columns to main tables
-- ============================================================================

-- Add user_id to qa_packages (who created/owns the test package)
ALTER TABLE qa_packages
ADD COLUMN user_id VARCHAR(255) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN qa_packages.user_id IS
'Keycloak subject ID of the user who owns this package. Default "system" for pre-auth data.';

-- Add user_id to test_scenarios (who created the scenario)
ALTER TABLE test_scenarios
ADD COLUMN user_id VARCHAR(255) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN test_scenarios.user_id IS
'Keycloak subject ID of the user who created this scenario. Default "system" for pre-auth data.';

-- Add user_id to test_runs (who triggered the test run)
ALTER TABLE test_runs
ADD COLUMN user_id VARCHAR(255) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN test_runs.user_id IS
'Keycloak subject ID of the user who triggered this run. Default "system" for pre-auth data.';

-- ============================================================================
-- PART 2: Create indexes for user-scoped queries
-- ============================================================================

-- Index for finding all packages by user
CREATE INDEX idx_qa_packages_user_id ON qa_packages(user_id);

-- Index for finding all scenarios by user
CREATE INDEX idx_test_scenarios_user_id ON test_scenarios(user_id);

-- Index for finding all runs by user
CREATE INDEX idx_test_runs_user_id ON test_runs(user_id);

-- ============================================================================
-- PART 3: Create composite indexes for common user-scoped query patterns
-- ============================================================================

-- User + status queries (e.g., "my active packages")
CREATE INDEX idx_qa_packages_user_status ON qa_packages(user_id, status);

-- User + created_at queries (e.g., "my recent packages")
CREATE INDEX idx_qa_packages_user_created ON qa_packages(user_id, created_at DESC);

-- User + status + created_at queries (e.g., "my recent active packages with pagination")
CREATE INDEX idx_qa_packages_user_status_created ON qa_packages(user_id, status, created_at DESC);

-- User + scenario status queries
CREATE INDEX idx_test_scenarios_user_status ON test_scenarios(user_id, status);

-- User + run status queries
CREATE INDEX idx_test_runs_user_status ON test_runs(user_id, status);

-- User + run created_at for "my recent runs"
CREATE INDEX idx_test_runs_user_created ON test_runs(user_id, created_at DESC);

-- ============================================================================
-- PART 4: Create partial indexes for active user data
-- ============================================================================

-- Partial index for active packages by user (most common query pattern)
CREATE INDEX idx_qa_packages_user_active ON qa_packages(user_id, created_at DESC)
WHERE status NOT IN ('COMPLETE', 'FAILED_SPEC_FETCH', 'FAILED_GENERATION', 'FAILED_EXECUTION', 'CANCELLED');

-- Partial index for active scenarios by user
CREATE INDEX idx_test_scenarios_user_active ON test_scenarios(user_id, created_at DESC)
WHERE status = 'ACTIVE';

-- ============================================================================
-- PART 5: Update existing events table for user context
-- ============================================================================

-- Add user_id to qa_package_events for audit trail
ALTER TABLE qa_package_events
ADD COLUMN user_id VARCHAR(255);

COMMENT ON COLUMN qa_package_events.user_id IS
'Keycloak subject ID of user who triggered this event (nullable for system events)';

CREATE INDEX idx_qa_package_events_user_id ON qa_package_events(user_id)
WHERE user_id IS NOT NULL;

-- ============================================================================
-- PART 6: Add user_id to AI logs for cost tracking per user
-- ============================================================================

-- Add user_id to ai_logs for per-user AI usage tracking
ALTER TABLE ai_logs
ADD COLUMN user_id VARCHAR(255);

COMMENT ON COLUMN ai_logs.user_id IS
'Keycloak subject ID of user whose action triggered this AI call (nullable for system calls)';

CREATE INDEX idx_ai_logs_user_id ON ai_logs(user_id)
WHERE user_id IS NOT NULL;

-- Composite index for user AI usage queries (e.g., "tokens used by user this month")
CREATE INDEX idx_ai_logs_user_created ON ai_logs(user_id, created_at DESC)
WHERE user_id IS NOT NULL;

-- ============================================================================
-- PART 7: Documentation
-- ============================================================================

COMMENT ON INDEX idx_qa_packages_user_id IS 'Index for filtering packages by owner user';
COMMENT ON INDEX idx_test_scenarios_user_id IS 'Index for filtering scenarios by creator user';
COMMENT ON INDEX idx_test_runs_user_id IS 'Index for filtering runs by triggering user';
COMMENT ON INDEX idx_qa_packages_user_status IS 'Composite index for user + status filtering';
COMMENT ON INDEX idx_qa_packages_user_created IS 'Composite index for user packages sorted by date';
COMMENT ON INDEX idx_qa_packages_user_status_created IS 'Composite index for user dashboard with status and pagination';
COMMENT ON INDEX idx_qa_packages_user_active IS 'Partial index for user active (in-progress) packages';
COMMENT ON INDEX idx_ai_logs_user_created IS 'Composite index for user AI usage tracking';
