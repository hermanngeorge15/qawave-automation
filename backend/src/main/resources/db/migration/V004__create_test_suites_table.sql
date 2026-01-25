-- V004__create_test_suites_table.sql
-- Description: Create test_suites table for grouping test scenarios
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE test_suites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    requirement_id UUID REFERENCES requirements(id) ON DELETE SET NULL,
    api_spec_id UUID REFERENCES api_specs(id) ON DELETE SET NULL,
    default_base_url VARCHAR(2048),
    status test_suite_status NOT NULL DEFAULT 'ACTIVE',
    tags VARCHAR(100)[] DEFAULT '{}',
    config JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_base_url_format CHECK (default_base_url IS NULL OR default_base_url ~ '^https?://')
);

-- Indexes for common queries
CREATE INDEX idx_test_suites_status ON test_suites(status);
CREATE INDEX idx_test_suites_requirement_id ON test_suites(requirement_id);
CREATE INDEX idx_test_suites_api_spec_id ON test_suites(api_spec_id);
CREATE INDEX idx_test_suites_created_at ON test_suites(created_at DESC);
CREATE INDEX idx_test_suites_tags ON test_suites USING gin(tags);
CREATE INDEX idx_test_suites_name_search ON test_suites USING gin(name gin_trgm_ops);

-- Partial index for active suites
CREATE INDEX idx_test_suites_active ON test_suites(created_at DESC) WHERE status = 'ACTIVE';

-- Trigger for updated_at
CREATE TRIGGER tr_test_suites_updated_at
    BEFORE UPDATE ON test_suites
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Table comments
COMMENT ON TABLE test_suites IS 'Logical grouping of test scenarios';
COMMENT ON COLUMN test_suites.requirement_id IS 'Optional link to source requirement';
COMMENT ON COLUMN test_suites.api_spec_id IS 'Optional link to API specification';
COMMENT ON COLUMN test_suites.default_base_url IS 'Default base URL for test execution';
COMMENT ON COLUMN test_suites.config IS 'Suite-level configuration (timeouts, retries, etc.)';
