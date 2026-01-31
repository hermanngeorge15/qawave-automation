# ADR-014: Database Schema and Migration Strategy

## Status
Accepted

## Date
2026-01-30

## Context

QAWave requires a robust database schema design and migration strategy to support:
- QA package and test scenario management
- Test execution results and history
- Event sourcing for audit trails
- AI interaction logging
- Multi-tenant data isolation

## Decision

We implement a **PostgreSQL schema with Flyway migrations** following domain-driven design principles.

### Entity-Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATABASE SCHEMA                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐         ┌──────────────────┐                          │
│  │   qa_packages    │         │      users       │                          │
│  ├──────────────────┤         ├──────────────────┤                          │
│  │ id (PK)          │◄────────│ id (PK)          │                          │
│  │ name             │         │ keycloak_id      │                          │
│  │ description      │         │ email            │                          │
│  │ spec_url         │         │ created_at       │                          │
│  │ spec_content     │         └──────────────────┘                          │
│  │ base_url         │                                                        │
│  │ requirements     │                                                        │
│  │ config (JSONB)   │                                                        │
│  │ status           │                                                        │
│  │ created_by       │────────►                                               │
│  │ created_at       │                                                        │
│  │ updated_at       │                                                        │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           │ 1:N                                                              │
│           ▼                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                          │
│  │  test_scenarios  │         │  scenario_tags   │                          │
│  ├──────────────────┤         ├──────────────────┤                          │
│  │ id (PK)          │◄───────►│ scenario_id (FK) │                          │
│  │ package_id (FK)  │         │ tag              │                          │
│  │ name             │         └──────────────────┘                          │
│  │ description      │                                                        │
│  │ priority         │                                                        │
│  │ steps (JSONB)    │                                                        │
│  │ created_at       │                                                        │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           │ 1:N                                                              │
│           ▼                                                                  │
│  ┌──────────────────┐                                                        │
│  │    test_runs     │                                                        │
│  ├──────────────────┤                                                        │
│  │ id (PK)          │                                                        │
│  │ package_id (FK)  │                                                        │
│  │ scenario_id (FK) │                                                        │
│  │ status           │                                                        │
│  │ started_at       │                                                        │
│  │ completed_at     │                                                        │
│  │ triggered_by     │                                                        │
│  │ environment      │                                                        │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           │ 1:N                                                              │
│           ▼                                                                  │
│  ┌──────────────────┐                                                        │
│  │ test_step_results│                                                        │
│  ├──────────────────┤                                                        │
│  │ id (PK)          │                                                        │
│  │ run_id (FK)      │                                                        │
│  │ step_index       │                                                        │
│  │ status           │                                                        │
│  │ request (JSONB)  │                                                        │
│  │ response (JSONB) │                                                        │
│  │ assertions(JSONB)│                                                        │
│  │ duration_ms      │                                                        │
│  │ error_message    │                                                        │
│  └──────────────────┘                                                        │
│                                                                              │
│  ┌──────────────────┐         ┌──────────────────┐                          │
│  │   run_events     │         │     ai_logs      │                          │
│  ├──────────────────┤         ├──────────────────┤                          │
│  │ id (PK)          │         │ id (PK)          │                          │
│  │ run_id (FK)      │         │ package_id (FK)  │                          │
│  │ event_type       │         │ operation        │                          │
│  │ payload (JSONB)  │         │ model            │                          │
│  │ timestamp        │         │ tokens_in        │                          │
│  └──────────────────┘         │ tokens_out       │                          │
│                               │ latency_ms       │                          │
│                               │ created_at       │                          │
│                               └──────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Table Definitions

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);

-- V2__create_qa_packages_table.sql
CREATE TABLE qa_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    spec_url VARCHAR(2048),
    spec_content TEXT,
    base_url VARCHAR(2048) NOT NULL,
    requirements TEXT,
    config JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'READY', 'RUNNING', 'COMPLETED', 'FAILED', 'ARCHIVED'))
);

CREATE INDEX idx_qa_packages_status ON qa_packages(status);
CREATE INDEX idx_qa_packages_created_by ON qa_packages(created_by);
CREATE INDEX idx_qa_packages_created_at ON qa_packages(created_at DESC);

-- V3__create_test_scenarios_table.sql
CREATE TABLE test_scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    steps JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_test_scenarios_package_id ON test_scenarios(package_id);
CREATE INDEX idx_test_scenarios_priority ON test_scenarios(priority);

-- V4__create_scenario_tags_table.sql
CREATE TABLE scenario_tags (
    scenario_id UUID NOT NULL REFERENCES test_scenarios(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (scenario_id, tag)
);

CREATE INDEX idx_scenario_tags_tag ON scenario_tags(tag);

-- V5__create_test_runs_table.sql
CREATE TABLE test_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES qa_packages(id),
    scenario_id UUID REFERENCES test_scenarios(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    triggered_by UUID REFERENCES users(id),
    environment VARCHAR(50) DEFAULT 'staging',
    metadata JSONB DEFAULT '{}',

    CONSTRAINT chk_run_status CHECK (status IN ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_test_runs_package_id ON test_runs(package_id);
CREATE INDEX idx_test_runs_scenario_id ON test_runs(scenario_id);
CREATE INDEX idx_test_runs_status ON test_runs(status);
CREATE INDEX idx_test_runs_started_at ON test_runs(started_at DESC);

-- V6__create_test_step_results_table.sql
CREATE TABLE test_step_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    request JSONB NOT NULL,
    response JSONB,
    assertions JSONB,
    extractions JSONB,
    duration_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_step_status CHECK (status IN ('PENDING', 'PASSED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_test_step_results_run_id ON test_step_results(run_id);
CREATE UNIQUE INDEX idx_test_step_results_run_step ON test_step_results(run_id, step_index);

-- V7__create_run_events_table.sql
CREATE TABLE run_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_run_events_run_id ON run_events(run_id);
CREATE INDEX idx_run_events_timestamp ON run_events(timestamp DESC);
CREATE INDEX idx_run_events_type ON run_events(event_type);

-- V8__create_ai_logs_table.sql
CREATE TABLE ai_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID REFERENCES qa_packages(id),
    run_id UUID REFERENCES test_runs(id),
    operation VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    tokens_in INTEGER,
    tokens_out INTEGER,
    latency_ms INTEGER,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_ai_logs_package_id ON ai_logs(package_id);
CREATE INDEX idx_ai_logs_created_at ON ai_logs(created_at DESC);
CREATE INDEX idx_ai_logs_operation ON ai_logs(operation);
```

### Index Strategy

| Table | Index | Type | Purpose |
|-------|-------|------|---------|
| qa_packages | status | B-tree | Filter by status |
| qa_packages | created_at DESC | B-tree | Recent packages |
| test_scenarios | package_id | B-tree | FK lookup |
| test_scenarios | (steps) GIN | GIN | JSONB queries |
| test_runs | (package_id, status) | Composite | Dashboard queries |
| run_events | (run_id, timestamp) | Composite | Event replay |
| ai_logs | (created_at, operation) | Composite | Cost analysis |

### Migration Naming Convention

```
V{version}__{description}.sql

Examples:
V1__create_users_table.sql
V2__create_qa_packages_table.sql
V10__add_index_on_status.sql
V11__alter_config_column_default.sql

Repeatable migrations (views, functions):
R__create_package_summary_view.sql
R__create_cleanup_function.sql
```

### Flyway Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    clean-disabled: true  # Prevent accidental data loss
    out-of-order: false
    table: flyway_schema_history
    placeholders:
      schema: qawave
```

### Rollback Strategy

```sql
-- Each migration should have a corresponding undo script
-- stored in db/undo/ directory (not run automatically)

-- U7__drop_run_events_table.sql
DROP TABLE IF EXISTS run_events;

-- For data migrations, use reversible patterns:
-- V15__add_new_status_value.sql
ALTER TABLE qa_packages
  DROP CONSTRAINT chk_status,
  ADD CONSTRAINT chk_status CHECK (
    status IN ('DRAFT', 'READY', 'RUNNING', 'COMPLETED', 'FAILED', 'ARCHIVED', 'PAUSED')
  );

-- U15__remove_new_status_value.sql (manual rollback)
UPDATE qa_packages SET status = 'DRAFT' WHERE status = 'PAUSED';
ALTER TABLE qa_packages
  DROP CONSTRAINT chk_status,
  ADD CONSTRAINT chk_status CHECK (
    status IN ('DRAFT', 'READY', 'RUNNING', 'COMPLETED', 'FAILED', 'ARCHIVED')
  );
```

### Data Migration Procedures

```kotlin
// For complex data migrations, use Kotlin migration classes
@Component
class V20__MigrateConfigFormat : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.prepareStatement("""
            UPDATE qa_packages
            SET config = jsonb_set(
                config,
                '{version}',
                '"2.0"'
            )
            WHERE config->>'version' IS NULL
        """).execute()
    }
}
```

### Query Optimization

```sql
-- Materialized view for dashboard statistics
CREATE MATERIALIZED VIEW package_statistics AS
SELECT
    p.id AS package_id,
    p.name,
    COUNT(DISTINCT s.id) AS scenario_count,
    COUNT(DISTINCT r.id) AS run_count,
    COUNT(DISTINCT CASE WHEN r.status = 'PASSED' THEN r.id END) AS passed_count,
    COUNT(DISTINCT CASE WHEN r.status = 'FAILED' THEN r.id END) AS failed_count,
    MAX(r.completed_at) AS last_run_at
FROM qa_packages p
LEFT JOIN test_scenarios s ON s.package_id = p.id
LEFT JOIN test_runs r ON r.package_id = p.id
GROUP BY p.id, p.name;

CREATE UNIQUE INDEX idx_package_statistics_id ON package_statistics(package_id);

-- Refresh periodically
REFRESH MATERIALIZED VIEW CONCURRENTLY package_statistics;
```

## Consequences

### Positive
- Clear schema evolution with Flyway
- JSONB flexibility for dynamic data
- Proper indexing for query performance
- Audit trail via run_events table

### Negative
- JSONB queries can be slower than normalized data
- Migration ordering must be maintained
- Manual rollback scripts needed
- Materialized views need refresh scheduling

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL JSONB](https://www.postgresql.org/docs/current/datatype-json.html)
- [Index Design](https://use-the-index-luke.com/)
