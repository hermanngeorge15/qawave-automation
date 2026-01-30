-- R018__rollback_user_ownership.sql
-- Description: Rollback script for V018__add_user_ownership.sql
-- Author: Database Agent
-- Date: 2026-01-30
-- Issue: #195 - Add user_id to database schema
--
-- WARNING: This rollback removes user ownership data.
-- Only use in development or if you haven't assigned real user IDs yet.
--
-- Usage: Run manually if V018 needs to be reverted. This is NOT a Flyway undo migration.
-- psql -h localhost -U qawave -d qawave -f R018__rollback_user_ownership.sql

-- ============================================================================
-- PART 1: Drop indexes (in reverse order of creation)
-- ============================================================================

-- AI logs user indexes
DROP INDEX IF EXISTS idx_ai_logs_user_created;
DROP INDEX IF EXISTS idx_ai_logs_user_id;

-- Events user index
DROP INDEX IF EXISTS idx_qa_package_events_user_id;

-- Partial indexes
DROP INDEX IF EXISTS idx_test_scenarios_user_active;
DROP INDEX IF EXISTS idx_qa_packages_user_active;

-- Composite indexes
DROP INDEX IF EXISTS idx_test_runs_user_created;
DROP INDEX IF EXISTS idx_test_runs_user_status;
DROP INDEX IF EXISTS idx_test_scenarios_user_status;
DROP INDEX IF EXISTS idx_qa_packages_user_status_created;
DROP INDEX IF EXISTS idx_qa_packages_user_created;
DROP INDEX IF EXISTS idx_qa_packages_user_status;

-- Basic user_id indexes
DROP INDEX IF EXISTS idx_test_runs_user_id;
DROP INDEX IF EXISTS idx_test_scenarios_user_id;
DROP INDEX IF EXISTS idx_qa_packages_user_id;

-- ============================================================================
-- PART 2: Drop user_id columns (in reverse order of creation)
-- ============================================================================

-- Remove from ai_logs
ALTER TABLE ai_logs DROP COLUMN IF EXISTS user_id;

-- Remove from qa_package_events
ALTER TABLE qa_package_events DROP COLUMN IF EXISTS user_id;

-- Remove from test_runs
ALTER TABLE test_runs DROP COLUMN IF EXISTS user_id;

-- Remove from test_scenarios
ALTER TABLE test_scenarios DROP COLUMN IF EXISTS user_id;

-- Remove from qa_packages
ALTER TABLE qa_packages DROP COLUMN IF EXISTS user_id;

-- ============================================================================
-- Verification
-- ============================================================================

-- Run this to verify rollback completed:
-- SELECT
--     table_name,
--     column_name
-- FROM information_schema.columns
-- WHERE column_name = 'user_id'
--   AND table_schema = 'public';
-- Should return 0 rows after rollback.
