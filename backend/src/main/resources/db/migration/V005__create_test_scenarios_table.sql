-- V005__create_test_scenarios_table.sql
-- Description: Create test_scenarios table with embedded test steps
-- Author: Database Agent
-- Date: 2026-01-25

CREATE TABLE test_scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    suite_id UUID REFERENCES test_suites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source scenario_source NOT NULL DEFAULT 'AI_GENERATED',
    operation_id VARCHAR(255),
    steps_json JSONB NOT NULL,
    status scenario_status NOT NULL DEFAULT 'ACTIVE',
    tags VARCHAR(100)[] DEFAULT '{}',
    priority INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_steps_json_is_array CHECK (jsonb_typeof(steps_json) = 'array'),
    CONSTRAINT chk_steps_not_empty CHECK (jsonb_array_length(steps_json) > 0),
    CONSTRAINT chk_priority_range CHECK (priority >= 0 AND priority <= 10)
);

-- Create function to validate steps_json structure
CREATE OR REPLACE FUNCTION validate_test_steps(steps JSONB)
RETURNS BOOLEAN AS $$
DECLARE
    step JSONB;
BEGIN
    -- Check it's an array
    IF jsonb_typeof(steps) != 'array' THEN
        RETURN FALSE;
    END IF;

    -- Check each step has required fields
    FOR step IN SELECT * FROM jsonb_array_elements(steps)
    LOOP
        IF NOT (
            step ? 'name' AND
            step ? 'method' AND
            step ? 'endpoint'
        ) THEN
            RETURN FALSE;
        END IF;
    END LOOP;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Add constraint using the validation function
ALTER TABLE test_scenarios
ADD CONSTRAINT chk_steps_json_structure CHECK (validate_test_steps(steps_json));

-- Indexes for common queries
CREATE INDEX idx_test_scenarios_suite_id ON test_scenarios(suite_id);
CREATE INDEX idx_test_scenarios_status ON test_scenarios(status);
CREATE INDEX idx_test_scenarios_source ON test_scenarios(source);
CREATE INDEX idx_test_scenarios_operation_id ON test_scenarios(operation_id) WHERE operation_id IS NOT NULL;
CREATE INDEX idx_test_scenarios_created_at ON test_scenarios(created_at DESC);
CREATE INDEX idx_test_scenarios_tags ON test_scenarios USING gin(tags);
CREATE INDEX idx_test_scenarios_steps_json ON test_scenarios USING gin(steps_json jsonb_path_ops);
CREATE INDEX idx_test_scenarios_name_search ON test_scenarios USING gin(name gin_trgm_ops);

-- Partial index for active scenarios
CREATE INDEX idx_test_scenarios_active ON test_scenarios(suite_id, created_at DESC) WHERE status = 'ACTIVE';

-- Trigger for updated_at
CREATE TRIGGER tr_test_scenarios_updated_at
    BEFORE UPDATE ON test_scenarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Table comments
COMMENT ON TABLE test_scenarios IS 'Test scenarios containing ordered test steps as JSON';
COMMENT ON COLUMN test_scenarios.source IS 'How the scenario was created (AI, manual, imported)';
COMMENT ON COLUMN test_scenarios.operation_id IS 'OpenAPI operation ID this scenario tests';
COMMENT ON COLUMN test_scenarios.steps_json IS 'Array of test steps with method, endpoint, headers, body, expected, extractions';
COMMENT ON COLUMN test_scenarios.version IS 'Scenario version for tracking changes';
COMMENT ON COLUMN test_scenarios.priority IS 'Execution priority (0-10, higher = more important)';
COMMENT ON FUNCTION validate_test_steps(JSONB) IS 'Validates test steps JSON structure';
