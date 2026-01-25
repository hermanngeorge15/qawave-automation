-- V009__create_ai_logs_table.sql
-- Description: Create ai_logs table for tracking AI interactions
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE ai_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    correlation_id UUID,
    qa_package_id UUID REFERENCES qa_packages(id) ON DELETE SET NULL,

    -- Provider info
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_type VARCHAR(50) NOT NULL,

    -- Token usage
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,

    -- Request/Response
    prompt_hash VARCHAR(64),
    prompt_text TEXT NOT NULL,
    response_text TEXT,
    response_json JSONB,

    -- Status and error handling
    status ai_log_status NOT NULL,
    error_message TEXT,
    error_code VARCHAR(50),

    -- Timing
    duration_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_provider_not_empty CHECK (LENGTH(TRIM(provider)) > 0),
    CONSTRAINT chk_model_not_empty CHECK (LENGTH(TRIM(model)) > 0),
    CONSTRAINT chk_prompt_type CHECK (prompt_type IN (
        'SCENARIO_GENERATION',
        'RESULT_EVALUATION',
        'QA_SUMMARY',
        'REQUIREMENTS_ANALYSIS',
        'SECURITY_SCAN',
        'PERFORMANCE_ANALYSIS',
        'OTHER'
    )),
    CONSTRAINT chk_tokens_positive CHECK (
        (prompt_tokens IS NULL OR prompt_tokens >= 0) AND
        (completion_tokens IS NULL OR completion_tokens >= 0) AND
        (total_tokens IS NULL OR total_tokens >= 0)
    ),
    CONSTRAINT chk_duration_positive CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_ai_logs_correlation_id ON ai_logs(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_ai_logs_qa_package_id ON ai_logs(qa_package_id) WHERE qa_package_id IS NOT NULL;
CREATE INDEX idx_ai_logs_provider ON ai_logs(provider);
CREATE INDEX idx_ai_logs_model ON ai_logs(provider, model);
CREATE INDEX idx_ai_logs_prompt_type ON ai_logs(prompt_type);
CREATE INDEX idx_ai_logs_status ON ai_logs(status);
CREATE INDEX idx_ai_logs_prompt_hash ON ai_logs(prompt_hash) WHERE prompt_hash IS NOT NULL;
CREATE INDEX idx_ai_logs_created_at ON ai_logs(created_at DESC);

-- Partial indexes for error analysis
CREATE INDEX idx_ai_logs_errors ON ai_logs(created_at DESC) WHERE status != 'SUCCESS';
CREATE INDEX idx_ai_logs_rate_limited ON ai_logs(created_at DESC) WHERE status = 'RATE_LIMITED';
CREATE INDEX idx_ai_logs_slow ON ai_logs(duration_ms DESC) WHERE duration_ms > 5000;

-- Table comments
COMMENT ON TABLE ai_logs IS 'Audit log of all AI provider interactions';
COMMENT ON COLUMN ai_logs.correlation_id IS 'Correlation ID for tracking related requests';
COMMENT ON COLUMN ai_logs.prompt_type IS 'Type of AI operation performed';
COMMENT ON COLUMN ai_logs.prompt_hash IS 'SHA-256 hash of prompt for deduplication/caching analysis';
COMMENT ON COLUMN ai_logs.response_json IS 'Parsed JSON response if applicable';
