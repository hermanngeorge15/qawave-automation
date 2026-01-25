-- V011__create_coverage_table.sql
-- Description: Create coverage tracking tables for per-operation metrics
-- Author: Database Agent
-- Date: 2026-01-25

-- Coverage status enum
CREATE TYPE coverage_status AS ENUM ('COVERED', 'FAILED', 'UNTESTED', 'SKIPPED');

-- Coverage snapshots per QA package run
CREATE TABLE coverage_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    qa_package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    spec_hash VARCHAR(64) NOT NULL,

    -- Summary metrics
    operations_total INTEGER NOT NULL DEFAULT 0,
    operations_covered INTEGER NOT NULL DEFAULT 0,
    operations_failed INTEGER NOT NULL DEFAULT 0,
    operations_untested INTEGER NOT NULL DEFAULT 0,
    coverage_percent DECIMAL(5,2) DEFAULT 0.00,

    -- Scenarios summary
    scenarios_total INTEGER NOT NULL DEFAULT 0,
    scenarios_passed INTEGER NOT NULL DEFAULT 0,
    scenarios_failed INTEGER NOT NULL DEFAULT 0,

    -- Gap analysis
    gaps_json JSONB DEFAULT '[]',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_coverage_percent_range CHECK (coverage_percent >= 0 AND coverage_percent <= 100),
    CONSTRAINT chk_counts_positive CHECK (
        operations_total >= 0 AND
        operations_covered >= 0 AND
        operations_failed >= 0 AND
        operations_untested >= 0 AND
        scenarios_total >= 0 AND
        scenarios_passed >= 0 AND
        scenarios_failed >= 0
    ),
    CONSTRAINT uq_package_snapshot UNIQUE (qa_package_id)
);

-- Per-operation coverage details
CREATE TABLE operation_coverage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    snapshot_id UUID NOT NULL REFERENCES coverage_snapshots(id) ON DELETE CASCADE,

    -- Operation identification
    operation_id VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(2048) NOT NULL,
    summary VARCHAR(500),

    -- Coverage status
    status coverage_status NOT NULL DEFAULT 'UNTESTED',
    scenarios_count INTEGER DEFAULT 0,
    scenarios_passed INTEGER DEFAULT 0,
    scenarios_failed INTEGER DEFAULT 0,

    -- Quality metrics
    has_weak_assertions BOOLEAN DEFAULT FALSE,
    has_unresolved_placeholders BOOLEAN DEFAULT FALSE,

    -- Related scenarios
    scenario_ids UUID[] DEFAULT '{}',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_http_method CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS')),
    CONSTRAINT uq_snapshot_operation UNIQUE (snapshot_id, operation_id)
);

-- Indexes for coverage_snapshots
CREATE INDEX idx_coverage_snapshots_package_id ON coverage_snapshots(qa_package_id);
CREATE INDEX idx_coverage_snapshots_spec_hash ON coverage_snapshots(spec_hash);
CREATE INDEX idx_coverage_snapshots_created_at ON coverage_snapshots(created_at DESC);

-- Indexes for operation_coverage
CREATE INDEX idx_operation_coverage_snapshot_id ON operation_coverage(snapshot_id);
CREATE INDEX idx_operation_coverage_status ON operation_coverage(status);
CREATE INDEX idx_operation_coverage_operation_id ON operation_coverage(operation_id);
CREATE INDEX idx_operation_coverage_method_path ON operation_coverage(http_method, path);

-- Partial indexes for gap detection
CREATE INDEX idx_operation_coverage_untested ON operation_coverage(snapshot_id) WHERE status = 'UNTESTED';
CREATE INDEX idx_operation_coverage_failed ON operation_coverage(snapshot_id) WHERE status = 'FAILED';
CREATE INDEX idx_operation_coverage_weak ON operation_coverage(snapshot_id) WHERE has_weak_assertions = TRUE;

-- Table comments
COMMENT ON TABLE coverage_snapshots IS 'Coverage summary snapshots per QA package run';
COMMENT ON COLUMN coverage_snapshots.gaps_json IS 'List of identified coverage gaps';
COMMENT ON TABLE operation_coverage IS 'Per-operation coverage details within a snapshot';
COMMENT ON COLUMN operation_coverage.has_weak_assertions IS 'Flag for scenarios with empty expected.bodyFields';
COMMENT ON COLUMN operation_coverage.has_unresolved_placeholders IS 'Flag for unresolved {placeholder} values';
