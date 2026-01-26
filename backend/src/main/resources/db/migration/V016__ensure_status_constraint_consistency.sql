-- V016__ensure_status_constraint_consistency.sql
-- Description: Ensure qa_packages status constraint is consistent across all environments
-- Author: Database Agent
-- Date: 2026-01-26
-- Issue: #127 - V014 was modified after deployment; this ensures consistency
--
-- Background:
-- V014 was modified in PR #122 after being merged, causing potential checksum issues.
-- Some environments may have run the old V014 (incorrect constraints) while others
-- have the new V014 (correct constraints). This migration ensures all environments
-- end up with the correct constraint values matching the QaPackageStatus enum.
--
-- This migration is idempotent - safe to run regardless of which V014 was applied.

-- ============================================================================
-- PART 1: Ensure status constraint matches QaPackageStatus enum
-- ============================================================================

-- Drop existing constraint if present (idempotent)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_qa_packages_status_valid' AND conrelid = 'qa_packages'::regclass) THEN
        ALTER TABLE qa_packages DROP CONSTRAINT chk_qa_packages_status_valid;
    END IF;
END $$;

-- Add constraint with correct values matching QaPackageStatus enum
-- Source: backend/src/main/kotlin/com/qawave/domain/model/QaPackage.kt
ALTER TABLE qa_packages
ADD CONSTRAINT chk_qa_packages_status_valid
CHECK (status IN (
    'REQUESTED',
    'SPEC_FETCHED',
    'AI_SUCCESS',
    'EXECUTION_IN_PROGRESS',
    'EXECUTION_COMPLETE',
    'QA_EVAL_IN_PROGRESS',
    'QA_EVAL_DONE',
    'COMPLETE',
    'FAILED_SPEC_FETCH',
    'FAILED_GENERATION',
    'FAILED_EXECUTION',
    'CANCELLED'
));

-- ============================================================================
-- PART 2: Recreate partial indexes with correct status values
-- ============================================================================

-- Drop existing partial indexes (idempotent)
DROP INDEX IF EXISTS idx_qa_packages_pending;
DROP INDEX IF EXISTS idx_qa_packages_running;
DROP INDEX IF EXISTS idx_qa_packages_completed;
DROP INDEX IF EXISTS idx_qa_packages_failed;

-- Recreate with correct values matching actual enum
CREATE INDEX idx_qa_packages_pending ON qa_packages(created_at)
WHERE status IN ('REQUESTED', 'SPEC_FETCHED', 'AI_SUCCESS');

CREATE INDEX idx_qa_packages_running ON qa_packages(started_at)
WHERE status IN ('EXECUTION_IN_PROGRESS', 'QA_EVAL_IN_PROGRESS');

CREATE INDEX idx_qa_packages_completed ON qa_packages(completed_at DESC)
WHERE status = 'COMPLETE';

CREATE INDEX idx_qa_packages_failed ON qa_packages(created_at DESC)
WHERE status IN ('FAILED_SPEC_FETCH', 'FAILED_GENERATION', 'FAILED_EXECUTION');

-- ============================================================================
-- PART 3: Documentation
-- ============================================================================

COMMENT ON CONSTRAINT chk_qa_packages_status_valid ON qa_packages IS
    'Status values matching QaPackageStatus enum. Updated in V016 to ensure consistency after V014 modification incident.';
