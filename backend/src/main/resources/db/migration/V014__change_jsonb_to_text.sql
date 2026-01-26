-- V014__change_jsonb_to_text.sql
-- Description: Change JSONB columns to TEXT for R2DBC compatibility
-- Author: Database Agent
-- Date: 2026-01-26

-- Change config_json from JSONB to TEXT
ALTER TABLE qa_packages
ALTER COLUMN config_json TYPE TEXT USING config_json::text;

-- Change coverage_json from JSONB to TEXT
ALTER TABLE qa_packages
ALTER COLUMN coverage_json TYPE TEXT USING coverage_json::text;

-- Change qa_recommendations from JSONB to TEXT (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'qa_packages' AND column_name = 'qa_recommendations') THEN
        ALTER TABLE qa_packages ALTER COLUMN qa_recommendations TYPE TEXT USING qa_recommendations::text;
    END IF;
END $$;

-- Change payload_json from JSONB to TEXT (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'qa_packages' AND column_name = 'payload_json') THEN
        ALTER TABLE qa_packages ALTER COLUMN payload_json TYPE TEXT USING payload_json::text;
    END IF;
END $$;

-- Change error_details from JSONB to TEXT (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'qa_packages' AND column_name = 'error_details') THEN
        ALTER TABLE qa_packages ALTER COLUMN error_details TYPE TEXT USING error_details::text;
    END IF;
END $$;
