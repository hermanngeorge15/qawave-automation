-- V006__create_test_runs_table.sql
-- Description: Create test_runs table for tracking test executions
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE test_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_id UUID NOT NULL REFERENCES test_scenarios(id) ON DELETE CASCADE,
    qa_package_id UUID,
    base_url VARCHAR(2048) NOT NULL,
    status run_status NOT NULL DEFAULT 'PENDING',
    triggered_by VARCHAR(255),
    trigger_source VARCHAR(50) DEFAULT 'MANUAL',
    config_json JSONB DEFAULT '{}',
    summary_json JSONB,
    steps_total INTEGER DEFAULT 0,
    steps_passed INTEGER DEFAULT 0,
    steps_failed INTEGER DEFAULT 0,
    steps_skipped INTEGER DEFAULT 0,
    duration_ms BIGINT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_base_url_format CHECK (base_url ~ '^https?://'),
    CONSTRAINT chk_trigger_source CHECK (trigger_source IN ('MANUAL', 'SCHEDULED', 'CI_CD', 'API', 'REPLAY')),
    CONSTRAINT chk_steps_counts CHECK (
        steps_total >= 0 AND
        steps_passed >= 0 AND
        steps_failed >= 0 AND
        steps_skipped >= 0 AND
        steps_passed + steps_failed + steps_skipped <= steps_total
    ),
    CONSTRAINT chk_duration_positive CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_test_runs_scenario_id ON test_runs(scenario_id);
CREATE INDEX idx_test_runs_qa_package_id ON test_runs(qa_package_id) WHERE qa_package_id IS NOT NULL;
CREATE INDEX idx_test_runs_status ON test_runs(status);
CREATE INDEX idx_test_runs_created_at ON test_runs(created_at DESC);
CREATE INDEX idx_test_runs_started_at ON test_runs(started_at DESC) WHERE started_at IS NOT NULL;

-- Composite indexes for common query patterns
CREATE INDEX idx_test_runs_scenario_status ON test_runs(scenario_id, status);
CREATE INDEX idx_test_runs_scenario_latest ON test_runs(scenario_id, created_at DESC);

-- Partial indexes for active/recent runs
CREATE INDEX idx_test_runs_pending ON test_runs(created_at) WHERE status = 'PENDING';
CREATE INDEX idx_test_runs_running ON test_runs(started_at) WHERE status = 'RUNNING';
CREATE INDEX idx_test_runs_failed ON test_runs(created_at DESC) WHERE status = 'FAILED';

-- Table comments
COMMENT ON TABLE test_runs IS 'Execution instances of test scenarios';
COMMENT ON COLUMN test_runs.qa_package_id IS 'Reference to parent QA package if part of a batch run';
COMMENT ON COLUMN test_runs.trigger_source IS 'How the run was triggered (manual, scheduled, CI/CD, API)';
COMMENT ON COLUMN test_runs.config_json IS 'Run-specific configuration overrides';
COMMENT ON COLUMN test_runs.summary_json IS 'Execution summary and statistics';
COMMENT ON COLUMN test_runs.duration_ms IS 'Total execution duration in milliseconds';
