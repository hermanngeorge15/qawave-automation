-- V010__create_events_table.sql
-- Description: Create events table for tracking QA package state transitions
-- Author: Database Agent
-- Date: 2026-01-25

-- Event types enum
CREATE TYPE event_type AS ENUM (
    'REQUESTED',
    'SPEC_FETCHED',
    'SPEC_FETCH_FAILED',
    'SCENARIO_CREATED',
    'SCENARIO_GENERATION_FAILED',
    'EXECUTION_STARTED',
    'EXECUTION_SUCCESS',
    'EXECUTION_FAILED',
    'AI_SUCCESS',
    'AI_FAILED',
    'QA_EVAL_STARTED',
    'QA_EVAL_DONE',
    'QA_EVAL_FAILED',
    'COMPLETE',
    'FAILED',
    'CANCELLED'
);

CREATE TABLE qa_package_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    qa_package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    event_type event_type NOT NULL,
    event_data JSONB DEFAULT '{}',
    scenario_id UUID REFERENCES test_scenarios(id) ON DELETE SET NULL,
    run_id UUID REFERENCES test_runs(id) ON DELETE SET NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_qa_package_events_package_id ON qa_package_events(qa_package_id);
CREATE INDEX idx_qa_package_events_type ON qa_package_events(event_type);
CREATE INDEX idx_qa_package_events_created_at ON qa_package_events(created_at DESC);
CREATE INDEX idx_qa_package_events_scenario_id ON qa_package_events(scenario_id) WHERE scenario_id IS NOT NULL;
CREATE INDEX idx_qa_package_events_run_id ON qa_package_events(run_id) WHERE run_id IS NOT NULL;

-- Composite index for event timeline
CREATE INDEX idx_qa_package_events_timeline ON qa_package_events(qa_package_id, created_at);

-- Table comments
COMMENT ON TABLE qa_package_events IS 'Event log for QA package state transitions and progress tracking';
COMMENT ON COLUMN qa_package_events.event_type IS 'Type of event that occurred';
COMMENT ON COLUMN qa_package_events.event_data IS 'Additional event-specific data';
