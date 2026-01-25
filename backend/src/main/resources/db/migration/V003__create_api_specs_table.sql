-- V003__create_api_specs_table.sql
-- Description: Create api_specs table for OpenAPI specification storage
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE api_specs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    spec_url VARCHAR(2048),
    raw_content TEXT,
    content_hash VARCHAR(64),
    version VARCHAR(50),
    format VARCHAR(20) DEFAULT 'OPENAPI_3',
    status api_spec_status NOT NULL DEFAULT 'ACTIVE',
    operations_count INTEGER DEFAULT 0,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_spec_url_format CHECK (spec_url IS NULL OR spec_url ~ '^https?://'),
    CONSTRAINT chk_has_spec CHECK (spec_url IS NOT NULL OR raw_content IS NOT NULL),
    CONSTRAINT chk_format CHECK (format IN ('OPENAPI_3', 'OPENAPI_2', 'SWAGGER', 'RAML', 'GRAPHQL'))
);

-- Indexes for common queries
CREATE INDEX idx_api_specs_status ON api_specs(status);
CREATE INDEX idx_api_specs_created_at ON api_specs(created_at DESC);
CREATE INDEX idx_api_specs_content_hash ON api_specs(content_hash) WHERE content_hash IS NOT NULL;
CREATE INDEX idx_api_specs_name_search ON api_specs USING gin(name gin_trgm_ops);

-- Partial index for active specs
CREATE INDEX idx_api_specs_active ON api_specs(created_at DESC) WHERE status = 'ACTIVE';

-- Trigger for updated_at
CREATE TRIGGER tr_api_specs_updated_at
    BEFORE UPDATE ON api_specs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Table comments
COMMENT ON TABLE api_specs IS 'OpenAPI specifications for systems under test';
COMMENT ON COLUMN api_specs.spec_url IS 'URL to fetch the OpenAPI specification';
COMMENT ON COLUMN api_specs.raw_content IS 'Cached raw content of the specification (YAML/JSON)';
COMMENT ON COLUMN api_specs.content_hash IS 'SHA-256 hash of raw_content for change detection';
COMMENT ON COLUMN api_specs.operations_count IS 'Number of operations in the spec';
