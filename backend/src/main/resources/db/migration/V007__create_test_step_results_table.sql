-- V007__create_test_step_results_table.sql
-- Description: Create test_step_results table for per-step execution results
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE test_step_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    run_id UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status step_result_status NOT NULL DEFAULT 'PENDING',

    -- Request details
    request_method VARCHAR(10) NOT NULL,
    request_url VARCHAR(4096) NOT NULL,
    request_headers JSONB DEFAULT '{}',
    request_body TEXT,

    -- Response details
    actual_status INTEGER,
    actual_headers JSONB,
    actual_body TEXT,
    response_size_bytes INTEGER,

    -- Evaluation details
    expected_status INTEGER,
    expected_body_fields JSONB,
    assertions_json JSONB,
    passed BOOLEAN,
    failure_reason TEXT,

    -- Extracted values for chaining
    extractions_json JSONB DEFAULT '{}',

    -- Timing
    duration_ms INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_step_index_positive CHECK (step_index >= 0),
    CONSTRAINT chk_request_method CHECK (request_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS')),
    CONSTRAINT chk_duration_positive CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT chk_response_size_positive CHECK (response_size_bytes IS NULL OR response_size_bytes >= 0),
    CONSTRAINT uq_run_step UNIQUE (run_id, step_index)
);

-- Indexes for common queries
CREATE INDEX idx_test_step_results_run_id ON test_step_results(run_id);
CREATE INDEX idx_test_step_results_status ON test_step_results(status);
CREATE INDEX idx_test_step_results_passed ON test_step_results(passed) WHERE passed IS NOT NULL;
CREATE INDEX idx_test_step_results_created_at ON test_step_results(created_at DESC);

-- Composite index for ordering steps within a run
CREATE INDEX idx_test_step_results_run_order ON test_step_results(run_id, step_index);

-- Partial indexes for failed/slow steps
CREATE INDEX idx_test_step_results_failed ON test_step_results(run_id) WHERE status = 'FAILED';
CREATE INDEX idx_test_step_results_slow ON test_step_results(duration_ms DESC) WHERE duration_ms > 1000;

-- Table comments
COMMENT ON TABLE test_step_results IS 'Per-step execution results for test runs';
COMMENT ON COLUMN test_step_results.step_index IS 'Zero-based index of the step within the scenario';
COMMENT ON COLUMN test_step_results.assertions_json IS 'Detailed assertion results (expected vs actual)';
COMMENT ON COLUMN test_step_results.extractions_json IS 'Values extracted from response for use in subsequent steps';
COMMENT ON COLUMN test_step_results.failure_reason IS 'Human-readable explanation of why the step failed';
