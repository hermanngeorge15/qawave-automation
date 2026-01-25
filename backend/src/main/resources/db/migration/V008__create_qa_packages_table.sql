-- V008__create_qa_packages_table.sql
-- Description: Create qa_packages table for full test runs with AI evaluation
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE qa_packages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    requirement_id UUID REFERENCES requirements(id) ON DELETE SET NULL,
    api_spec_id UUID REFERENCES api_specs(id) ON DELETE SET NULL,
    spec_url VARCHAR(2048),
    base_url VARCHAR(2048) NOT NULL,
    mode test_mode NOT NULL DEFAULT 'STANDARD',
    status qa_package_status NOT NULL DEFAULT 'REQUESTED',

    -- Cached spec info
    spec_hash VARCHAR(64),
    spec_content TEXT,

    -- Execution config
    config_json JSONB DEFAULT '{}',
    timeout_ms INTEGER DEFAULT 30000,
    max_retries INTEGER DEFAULT 3,

    -- Results summary
    scenarios_total INTEGER DEFAULT 0,
    scenarios_passed INTEGER DEFAULT 0,
    scenarios_failed INTEGER DEFAULT 0,
    scenarios_skipped INTEGER DEFAULT 0,
    operations_total INTEGER DEFAULT 0,
    operations_covered INTEGER DEFAULT 0,

    -- AI evaluation
    qa_summary TEXT,
    qa_recommendations JSONB DEFAULT '[]',
    coverage_json JSONB DEFAULT '{}',

    -- Stored payload for replay
    payload_json JSONB,
    payload_compressed BYTEA,

    -- Error handling
    error_message TEXT,
    error_details JSONB,

    -- Timing
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_base_url_format CHECK (base_url ~ '^https?://'),
    CONSTRAINT chk_spec_url_format CHECK (spec_url IS NULL OR spec_url ~ '^https?://'),
    CONSTRAINT chk_timeout_positive CHECK (timeout_ms > 0),
    CONSTRAINT chk_retries_range CHECK (max_retries >= 0 AND max_retries <= 10),
    CONSTRAINT chk_scenarios_counts CHECK (
        scenarios_total >= 0 AND
        scenarios_passed >= 0 AND
        scenarios_failed >= 0 AND
        scenarios_skipped >= 0
    ),
    CONSTRAINT chk_duration_positive CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_qa_packages_status ON qa_packages(status);
CREATE INDEX idx_qa_packages_mode ON qa_packages(mode);
CREATE INDEX idx_qa_packages_requirement_id ON qa_packages(requirement_id) WHERE requirement_id IS NOT NULL;
CREATE INDEX idx_qa_packages_api_spec_id ON qa_packages(api_spec_id) WHERE api_spec_id IS NOT NULL;
CREATE INDEX idx_qa_packages_spec_hash ON qa_packages(spec_hash) WHERE spec_hash IS NOT NULL;
CREATE INDEX idx_qa_packages_created_at ON qa_packages(created_at DESC);
CREATE INDEX idx_qa_packages_started_at ON qa_packages(started_at DESC) WHERE started_at IS NOT NULL;
CREATE INDEX idx_qa_packages_name_search ON qa_packages USING gin(name gin_trgm_ops);

-- Partial indexes for status filtering
CREATE INDEX idx_qa_packages_pending ON qa_packages(created_at) WHERE status IN ('REQUESTED', 'SPEC_FETCHED', 'GENERATING');
CREATE INDEX idx_qa_packages_running ON qa_packages(started_at) WHERE status IN ('EXECUTING', 'EVALUATING');
CREATE INDEX idx_qa_packages_completed ON qa_packages(finished_at DESC) WHERE status = 'COMPLETED';
CREATE INDEX idx_qa_packages_failed ON qa_packages(created_at DESC) WHERE status = 'FAILED';

-- Trigger for updated_at
CREATE TRIGGER tr_qa_packages_updated_at
    BEFORE UPDATE ON qa_packages
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add foreign key from test_runs to qa_packages (deferred because qa_packages created after test_runs)
ALTER TABLE test_runs
ADD CONSTRAINT fk_test_runs_qa_package
FOREIGN KEY (qa_package_id) REFERENCES qa_packages(id) ON DELETE CASCADE;

-- Table comments
COMMENT ON TABLE qa_packages IS 'Container for full test runs with AI evaluation and coverage reporting';
COMMENT ON COLUMN qa_packages.mode IS 'Test mode: STANDARD (functional), SECURITY (OWASP), PERFORMANCE (load)';
COMMENT ON COLUMN qa_packages.spec_hash IS 'SHA-256 hash of the OpenAPI spec for version tracking';
COMMENT ON COLUMN qa_packages.qa_summary IS 'AI-generated summary of test results';
COMMENT ON COLUMN qa_packages.qa_recommendations IS 'AI-generated recommendations for improvement';
COMMENT ON COLUMN qa_packages.coverage_json IS 'Per-operation coverage details';
COMMENT ON COLUMN qa_packages.payload_json IS 'Stored payload for deterministic replay';
COMMENT ON COLUMN qa_packages.payload_compressed IS 'Gzip-compressed payload for large payloads';
