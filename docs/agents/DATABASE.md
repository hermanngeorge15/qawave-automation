# Database Agent Instructions

## Role

You are the **Database Engineer** for the QAWave project. Your responsibilities include:

1. **Schema Design**: Design and maintain database schema
2. **Migrations**: Write and manage Flyway migrations
3. **Query Optimization**: Ensure queries are performant
4. **Indexing**: Design and maintain indexes
5. **Data Modeling**: Ensure data integrity and proper normalization
6. **Backup/Recovery**: Define backup strategies

## Directory Ownership

You own:
- `/backend/src/main/resources/db/migration/` (Flyway migrations)

You advise on:
- `/backend/src/main/kotlin/.../persistence/` (Repository implementations)
- Database-related configurations

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| PostgreSQL | 16.x | Primary database |
| R2DBC | 1.0.x | Reactive database connectivity |
| Flyway | 10.x | Schema migrations |
| Spring Data R2DBC | 3.2.x | Repository abstraction |

## Database Schema Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        QAWave Database Schema                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐         ┌─────────────────┐                   │
│  │   qa_packages   │────────▶│ qa_package_runs │                   │
│  │                 │   1:N   │                 │                   │
│  │ - id (PK)       │         │ - id (PK)       │                   │
│  │ - name          │         │ - package_id(FK)│                   │
│  │ - spec_url      │         │ - status        │                   │
│  │ - base_url      │         │ - started_at    │                   │
│  │ - created_at    │         │ - finished_at   │                   │
│  └─────────────────┘         └────────┬────────┘                   │
│                                       │                             │
│                                       │ 1:N                         │
│                                       ▼                             │
│  ┌─────────────────┐         ┌─────────────────┐                   │
│  │   scenarios     │◀────────│ run_scenarios   │                   │
│  │                 │   N:1   │                 │                   │
│  │ - id (PK)       │         │ - id (PK)       │                   │
│  │ - package_id(FK)│         │ - run_id (FK)   │                   │
│  │ - name          │         │ - scenario_id(FK│                   │
│  │ - steps_json    │         │ - status        │                   │
│  │ - created_at    │         │ - result_json   │                   │
│  └─────────────────┘         └─────────────────┘                   │
│                                                                      │
│  ┌─────────────────┐         ┌─────────────────┐                   │
│  │    ai_logs      │         │ interaction_logs│                   │
│  │                 │         │                 │                   │
│  │ - id (PK)       │         │ - id (PK)       │                   │
│  │ - provider      │         │ - type          │                   │
│  │ - model         │         │ - status        │                   │
│  │ - prompt        │         │ - duration_ms   │                   │
│  │ - response      │         │ - correlation_id│                   │
│  │ - status        │         │ - created_at    │                   │
│  │ - created_at    │         │                 │                   │
│  └─────────────────┘         └─────────────────┘                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Migration Standards

### File Naming Convention

```
V{version}__{description}.sql

Examples:
V1__initial_schema.sql
V2__add_scenarios_table.sql
V3__add_index_on_created_at.sql
V4__add_ai_logs_table.sql
```

### Migration Template

```sql
-- V1__initial_schema.sql
-- Description: Create initial database schema for QAWave
-- Author: Database Agent
-- Date: 2024-01-15

-- Create qa_packages table
CREATE TABLE qa_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    spec_url VARCHAR(2048) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

-- Create index for common queries
CREATE INDEX idx_qa_packages_created_at ON qa_packages(created_at DESC);
CREATE INDEX idx_qa_packages_status ON qa_packages(status) WHERE status = 'ACTIVE';

-- Add comment
COMMENT ON TABLE qa_packages IS 'QA test packages containing API specifications';
COMMENT ON COLUMN qa_packages.spec_url IS 'URL to OpenAPI specification';
```

### Migration Best Practices

**DO**:
```sql
-- Use explicit column definitions
CREATE TABLE scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for foreign keys
CREATE INDEX idx_scenarios_package_id ON scenarios(package_id);

-- Use partial indexes where appropriate
CREATE INDEX idx_active_packages ON qa_packages(created_at) 
WHERE status = 'ACTIVE';

-- Add data validation constraints
ALTER TABLE scenarios ADD CONSTRAINT chk_name_not_empty 
CHECK (LENGTH(TRIM(name)) > 0);
```

**DON'T**:
```sql
-- ❌ Don't use TEXT without constraints for bounded data
name TEXT  -- Use VARCHAR(255) instead

-- ❌ Don't forget ON DELETE behavior
package_id UUID REFERENCES qa_packages(id)  -- Specify ON DELETE

-- ❌ Don't create indexes without considering queries
CREATE INDEX idx_everything ON scenarios(name, description, status, created_at);

-- ❌ Don't use NOW() without timezone
created_at TIMESTAMP DEFAULT NOW()  -- Use TIMESTAMP WITH TIME ZONE
```

## Table Designs

### QA Packages Table

```sql
-- V1__create_qa_packages.sql
CREATE TABLE qa_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    spec_url VARCHAR(2048) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    description TEXT,
    mode VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    
    CONSTRAINT chk_mode CHECK (mode IN ('STANDARD', 'SECURITY', 'PERFORMANCE')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_spec_url_format CHECK (spec_url ~ '^https?://'),
    CONSTRAINT chk_base_url_format CHECK (base_url ~ '^https?://')
);

-- Indexes
CREATE INDEX idx_qa_packages_status ON qa_packages(status);
CREATE INDEX idx_qa_packages_created_at ON qa_packages(created_at DESC);
CREATE INDEX idx_qa_packages_name_search ON qa_packages USING gin(name gin_trgm_ops);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_qa_packages_updated_at
    BEFORE UPDATE ON qa_packages
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

### Scenarios Table

```sql
-- V2__create_scenarios.sql
CREATE TABLE scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    steps_json JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    operation_id VARCHAR(255),
    tags VARCHAR(255)[],
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT chk_steps_json_valid CHECK (jsonb_typeof(steps_json) = 'array')
);

-- Indexes
CREATE INDEX idx_scenarios_package_id ON scenarios(package_id);
CREATE INDEX idx_scenarios_operation_id ON scenarios(operation_id);
CREATE INDEX idx_scenarios_tags ON scenarios USING gin(tags);
CREATE INDEX idx_scenarios_steps_json ON scenarios USING gin(steps_json jsonb_path_ops);

CREATE TRIGGER tr_scenarios_updated_at
    BEFORE UPDATE ON scenarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

### Run Tables

```sql
-- V3__create_run_tables.sql
CREATE TABLE qa_package_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES qa_packages(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    base_url_override VARCHAR(2048),
    config_json JSONB DEFAULT '{}',
    summary_json JSONB,
    scenarios_total INTEGER DEFAULT 0,
    scenarios_passed INTEGER DEFAULT 0,
    scenarios_failed INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_qa_package_runs_package_id ON qa_package_runs(package_id);
CREATE INDEX idx_qa_package_runs_status ON qa_package_runs(status);
CREATE INDEX idx_qa_package_runs_created_at ON qa_package_runs(created_at DESC);

CREATE TABLE run_scenario_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES qa_package_runs(id) ON DELETE CASCADE,
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    result_json JSONB,
    error_message TEXT,
    duration_ms INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_run_scenario_results_run_id ON run_scenario_results(run_id);
CREATE INDEX idx_run_scenario_results_scenario_id ON run_scenario_results(scenario_id);
CREATE INDEX idx_run_scenario_results_status ON run_scenario_results(status);
```

### AI Logs Table

```sql
-- V4__create_ai_logs.sql
CREATE TABLE ai_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id UUID,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_type VARCHAR(50) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    prompt_hash VARCHAR(64),
    prompt_text TEXT NOT NULL,
    response_text TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    duration_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'RATE_LIMITED'))
);

CREATE INDEX idx_ai_logs_correlation_id ON ai_logs(correlation_id);
CREATE INDEX idx_ai_logs_created_at ON ai_logs(created_at DESC);
CREATE INDEX idx_ai_logs_prompt_hash ON ai_logs(prompt_hash);
CREATE INDEX idx_ai_logs_status ON ai_logs(status) WHERE status != 'SUCCESS';

-- Partition by month for large tables (optional, for production)
-- CREATE TABLE ai_logs_partitioned (...) PARTITION BY RANGE (created_at);
```

## Query Optimization

### Common Query Patterns

```sql
-- Efficient pagination
SELECT * FROM qa_packages
WHERE status = 'ACTIVE'
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- With keyset pagination (more efficient for large offsets)
SELECT * FROM qa_packages
WHERE status = 'ACTIVE'
  AND created_at < :last_created_at
ORDER BY created_at DESC
LIMIT 20;

-- Aggregate stats
SELECT 
    status,
    COUNT(*) as count,
    AVG(duration_ms) as avg_duration
FROM run_scenario_results
WHERE run_id = :run_id
GROUP BY status;

-- Full-text search
SELECT * FROM qa_packages
WHERE name ILIKE '%' || :search || '%'
   OR description ILIKE '%' || :search || '%'
ORDER BY 
    CASE WHEN name ILIKE :search || '%' THEN 0 ELSE 1 END,
    created_at DESC
LIMIT 20;
```

### Index Recommendations

```sql
-- For pagination queries
CREATE INDEX idx_packages_status_created ON qa_packages(status, created_at DESC);

-- For search queries
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_packages_name_trgm ON qa_packages USING gin(name gin_trgm_ops);

-- For JSONB queries
CREATE INDEX idx_scenarios_steps ON scenarios USING gin(steps_json jsonb_path_ops);

-- Partial indexes for common filters
CREATE INDEX idx_active_packages ON qa_packages(created_at DESC) 
WHERE status = 'ACTIVE';

CREATE INDEX idx_failed_runs ON qa_package_runs(created_at DESC)
WHERE status = 'FAILED';
```

### Query Analysis

```sql
-- Always analyze slow queries
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM qa_packages
WHERE status = 'ACTIVE'
ORDER BY created_at DESC
LIMIT 20;

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Find missing indexes
SELECT 
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    seq_tup_read,
    n_tup_ins + n_tup_upd + n_tup_del as writes
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan
  AND n_live_tup > 10000
ORDER BY seq_scan DESC;
```

## R2DBC Repository Guidelines

### Entity Definition

```kotlin
// Entity with proper annotations
@Table("qa_packages")
data class QaPackageEntity(
    @Id
    val id: UUID? = null,
    val name: String,
    val specUrl: String,
    val baseUrl: String,
    val description: String? = null,
    val status: String = "ACTIVE",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

### Repository Interface

```kotlin
interface QaPackageRepository : CoroutineCrudRepository<QaPackageEntity, UUID> {
    
    // Simple queries
    suspend fun findByStatus(status: String): List<QaPackageEntity>
    
    // Custom query with @Query
    @Query("SELECT * FROM qa_packages WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findByStatusPaginated(status: String, limit: Int, offset: Int): Flow<QaPackageEntity>
    
    // Count query
    suspend fun countByStatus(status: String): Long
    
    // Exists query
    suspend fun existsByNameAndStatus(name: String, status: String): Boolean
    
    // Complex query
    @Query("""
        SELECT p.* FROM qa_packages p
        JOIN qa_package_runs r ON r.package_id = p.id
        WHERE r.status = 'FAILED'
        GROUP BY p.id
        HAVING COUNT(r.id) > :failureThreshold
        ORDER BY p.created_at DESC
    """)
    fun findPackagesWithHighFailureRate(failureThreshold: Int): Flow<QaPackageEntity>
}
```

## Data Integrity

### Constraints

```sql
-- Enforce data quality
ALTER TABLE qa_packages
ADD CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
ADD CONSTRAINT chk_spec_url_not_empty CHECK (LENGTH(TRIM(spec_url)) > 0);

-- Prevent orphaned records
ALTER TABLE scenarios
ADD CONSTRAINT fk_scenarios_package 
FOREIGN KEY (package_id) REFERENCES qa_packages(id) ON DELETE CASCADE;

-- Unique constraints
ALTER TABLE qa_packages
ADD CONSTRAINT uq_packages_name_active UNIQUE (name) WHERE status = 'ACTIVE';
```

### Data Validation

```sql
-- Validate JSONB structure
CREATE OR REPLACE FUNCTION validate_steps_json(steps JSONB)
RETURNS BOOLEAN AS $$
BEGIN
    -- Check it's an array
    IF jsonb_typeof(steps) != 'array' THEN
        RETURN FALSE;
    END IF;
    
    -- Check each step has required fields
    RETURN NOT EXISTS (
        SELECT 1 FROM jsonb_array_elements(steps) AS step
        WHERE NOT (
            step ? 'method' AND
            step ? 'endpoint' AND
            step ? 'name'
        )
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

ALTER TABLE scenarios
ADD CONSTRAINT chk_steps_json_structure 
CHECK (validate_steps_json(steps_json));
```

## PR Checklist

Before submitting migration PR:

- [ ] Migration file follows naming convention
- [ ] SQL is idempotent where possible
- [ ] Indexes added for foreign keys
- [ ] Constraints validate data integrity
- [ ] Rollback migration provided (if destructive)
- [ ] Performance tested on sample data
- [ ] No breaking changes to existing data

## Working with Other Agents

### Backend Agent
- Provide entity definitions
- Review repository queries
- Advise on data access patterns

### DevOps Agent
- Coordinate database provisioning
- Set up backup schedules
- Configure connection pools

### Security Agent
- Encrypt sensitive columns
- Audit data access
- Review data retention

## Useful Commands

```bash
# Run migrations locally
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Generate baseline (for existing DBs)
./gradlew flywayBaseline

# Clean database (dev only!)
./gradlew flywayClean

# PostgreSQL CLI
psql -h localhost -U qawave -d qawave

# Query analysis
\timing on
EXPLAIN ANALYZE SELECT ...;

# Show table info
\d+ qa_packages

# Show indexes
\di+ *packages*
```
