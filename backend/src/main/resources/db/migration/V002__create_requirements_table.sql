-- V002__create_requirements_table.sql
-- Description: Create requirements table for business requirements tracking
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    external_reference VARCHAR(255),
    external_url VARCHAR(2048),
    status requirement_status NOT NULL DEFAULT 'DRAFT',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_title_not_empty CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_external_url_format CHECK (external_url IS NULL OR external_url ~ '^https?://')
);

-- Indexes for common queries
CREATE INDEX idx_requirements_status ON requirements(status);
CREATE INDEX idx_requirements_created_at ON requirements(created_at DESC);
CREATE INDEX idx_requirements_external_ref ON requirements(external_reference) WHERE external_reference IS NOT NULL;
CREATE INDEX idx_requirements_title_search ON requirements USING gin(title gin_trgm_ops);

-- Trigger for updated_at
CREATE TRIGGER tr_requirements_updated_at
    BEFORE UPDATE ON requirements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Table comments
COMMENT ON TABLE requirements IS 'Business requirements that drive test scenario generation';
COMMENT ON COLUMN requirements.external_reference IS 'Reference to external system (e.g., Jira ticket ID)';
COMMENT ON COLUMN requirements.external_url IS 'URL to external requirement document';
COMMENT ON COLUMN requirements.metadata IS 'Additional structured data (tags, priority, etc.)';
