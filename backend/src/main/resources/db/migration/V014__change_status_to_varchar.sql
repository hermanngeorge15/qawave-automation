-- V013__change_status_to_varchar.sql
-- Description: Change status and mode columns to varchar for R2DBC compatibility
-- Author: Database Agent
-- Date: 2026-01-26

-- IMPORTANT: Drop ALL partial indexes that reference status FIRST (before type change)
DROP INDEX IF EXISTS idx_qa_packages_pending;
DROP INDEX IF EXISTS idx_qa_packages_running;
DROP INDEX IF EXISTS idx_qa_packages_completed;
DROP INDEX IF EXISTS idx_qa_packages_failed;
DROP INDEX IF EXISTS idx_qa_packages_status;
DROP INDEX IF EXISTS idx_qa_packages_mode;

-- Drop any existing constraints
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_qa_packages_status_valid' AND conrelid = 'qa_packages'::regclass) THEN
        ALTER TABLE qa_packages DROP CONSTRAINT chk_qa_packages_status_valid;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_qa_packages_mode_valid' AND conrelid = 'qa_packages'::regclass) THEN
        ALTER TABLE qa_packages DROP CONSTRAINT chk_qa_packages_mode_valid;
    END IF;
END $$;

-- Change status column from enum to varchar
ALTER TABLE qa_packages
ALTER COLUMN status DROP DEFAULT;

ALTER TABLE qa_packages
ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE qa_packages
ALTER COLUMN status SET DEFAULT 'REQUESTED';

-- Change mode column from enum to varchar
ALTER TABLE qa_packages
ALTER COLUMN mode DROP DEFAULT;

ALTER TABLE qa_packages
ALTER COLUMN mode TYPE VARCHAR(50) USING mode::text;

ALTER TABLE qa_packages
ALTER COLUMN mode SET DEFAULT 'STANDARD';

-- Add constraints for valid values
ALTER TABLE qa_packages
ADD CONSTRAINT chk_qa_packages_status_valid
CHECK (status IN ('REQUESTED', 'SPEC_FETCHED', 'GENERATING', 'READY', 'EXECUTING', 'EVALUATING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE qa_packages
ADD CONSTRAINT chk_qa_packages_mode_valid
CHECK (mode IN ('STANDARD', 'SECURITY', 'PERFORMANCE'));

-- Recreate indexes
CREATE INDEX idx_qa_packages_status ON qa_packages(status);
CREATE INDEX idx_qa_packages_mode ON qa_packages(mode);

-- Recreate partial indexes (now with varchar comparison)
CREATE INDEX idx_qa_packages_pending ON qa_packages(created_at)
WHERE status IN ('REQUESTED', 'SPEC_FETCHED', 'GENERATING');

CREATE INDEX idx_qa_packages_running ON qa_packages(started_at)
WHERE status IN ('EXECUTING', 'EVALUATING');

CREATE INDEX idx_qa_packages_completed ON qa_packages(finished_at DESC)
WHERE status = 'COMPLETED';

CREATE INDEX idx_qa_packages_failed ON qa_packages(created_at DESC)
WHERE status = 'FAILED';
