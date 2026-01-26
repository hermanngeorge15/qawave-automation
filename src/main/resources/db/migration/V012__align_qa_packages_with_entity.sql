-- V012__align_qa_packages_with_entity.sql
-- Description: Align qa_packages table with QaPackageEntity
-- Author: Database Agent
-- Date: 2026-01-26

-- Add missing columns
ALTER TABLE qa_packages ADD COLUMN IF NOT EXISTS triggered_by VARCHAR(255);
ALTER TABLE qa_packages ADD COLUMN IF NOT EXISTS requirements TEXT;
ALTER TABLE qa_packages ADD COLUMN IF NOT EXISTS qa_summary_json TEXT;
ALTER TABLE qa_packages ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;

-- Copy data from existing columns where applicable
UPDATE qa_packages SET triggered_by = created_by WHERE triggered_by IS NULL AND created_by IS NOT NULL;
UPDATE qa_packages SET completed_at = finished_at WHERE completed_at IS NULL AND finished_at IS NOT NULL;
UPDATE qa_packages SET qa_summary_json = qa_summary WHERE qa_summary_json IS NULL AND qa_summary IS NOT NULL;

-- Set default for triggered_by (required by entity)
UPDATE qa_packages SET triggered_by = 'system' WHERE triggered_by IS NULL;
ALTER TABLE qa_packages ALTER COLUMN triggered_by SET NOT NULL;

-- Add index for triggered_by
CREATE INDEX IF NOT EXISTS idx_qa_packages_triggered_by ON qa_packages(triggered_by);

COMMENT ON COLUMN qa_packages.triggered_by IS 'User or system that triggered package creation';
COMMENT ON COLUMN qa_packages.requirements IS 'User-provided requirements text';
COMMENT ON COLUMN qa_packages.qa_summary_json IS 'JSON format of QA summary';
COMMENT ON COLUMN qa_packages.completed_at IS 'Timestamp when package execution completed';
