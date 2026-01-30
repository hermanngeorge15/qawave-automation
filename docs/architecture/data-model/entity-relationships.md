# Entity Relationships

## Overview

This document describes the core domain entities and their relationships in QAWave.

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              ENTITY RELATIONSHIP DIAGRAM                                 │
└─────────────────────────────────────────────────────────────────────────────────────────┘

    ┌───────────────┐          ┌───────────────┐          ┌───────────────┐
    │  Requirement  │─────────▶│    ApiSpec    │─────────▶│   TestSuite   │
    │               │  1:N     │               │  1:N     │               │
    │  id           │          │  id           │          │  id           │
    │  title        │          │  name         │          │  name         │
    │  description  │          │  url/content  │          │  requirementId│
    │  externalRef  │          │  version      │          │  apiSpecId    │
    │  createdAt    │          │  specHash     │          │  defaultBaseUrl
    └───────────────┘          │  createdAt    │          │  createdAt    │
                               └───────────────┘          └───────┬───────┘
                                                                  │
                                                                  │ 1:N
                                                                  ▼
    ┌───────────────┐                                     ┌───────────────┐
    │   QaPackage   │◀────────────────────────────────────│ TestScenario  │
    │   (Run)       │  N:1                                │               │
    │               │                                     │  id           │
    │  planId       │─────────────────────────────────────│  suiteId      │
    │  status       │         1:N                         │  name         │
    │  baseUrl      │                                     │  description  │
    │  specHash     │                                     │  stepsJson    │
    │  triggeredBy  │                                     │  source       │
    │  createdAt    │                                     │  specHash     │
    │  completedAt  │                                     │  createdAt    │
    └───────┬───────┘                                     └───────┬───────┘
            │                                                     │
            │ 1:N                                                  │ contains
            ▼                                                     ▼
    ┌───────────────┐                                     ┌───────────────┐
    │ QaPackageEvent│                                     │   TestStep    │
    │               │                                     │  (embedded)   │
    │  id           │                                     │               │
    │  planId       │                                     │  index        │
    │  type         │                                     │  name         │
    │  payload      │                                     │  method       │
    │  timestamp    │                                     │  endpoint     │
    │               │                                     │  headers      │
    └───────────────┘                                     │  body         │
                                                          │  expected     │
                                                          │  extractions  │
    ┌───────────────┐                                     └───────────────┘
    │   TestRun     │◀────────────────────────────────────────────┘
    │               │  N:1 (scenario)
    │  id           │
    │  scenarioId   │
    │  planId       │─────────┐
    │  baseUrl      │         │
    │  status       │         │
    │  triggeredBy  │         │
    │  startedAt    │         │
    │  completedAt  │         │
    │  durationMs   │         │
    └───────┬───────┘         │
            │                 │ 1:N (run → package)
            │ 1:N             │
            ▼                 ▼
    ┌───────────────┐   ┌───────────────┐
    │TestStepResult │   │ QaPackage     │
    │               │   │  (aggregation)│
    │  id           │   │               │
    │  runId        │   │  scenarios[]  │
    │  stepIndex    │   │  runs[]       │
    │  passed       │   │  coverage     │
    │  actualStatus │   │  qaSummary    │
    │  actualBody   │   └───────────────┘
    │  actualHeaders│
    │  errorMessage │
    │  durationMs   │
    │  timestamp    │
    └───────────────┘


    ┌───────────────┐
    │InteractionLog │
    │               │
    │  id           │
    │  planId       │
    │  correlationId│
    │  type         │
    │  status       │
    │  requestSummary
    │  responseSummary
    │  durationMs   │
    │  createdAt    │
    └───────────────┘
```

## Entity Definitions

### Core Test Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **Requirement** | Business need/user story | id, title, description, externalReference (Jira link) |
| **ApiSpec** | OpenAPI specification | id, name, url/rawContent, version, specHash |
| **TestSuite** | Logical grouping of scenarios | id, name, requirementId, apiSpecId, defaultBaseUrl |
| **TestScenario** | Test case with ordered steps | id, suiteId, name, steps[], source, specHash |
| **TestStep** | Single API action (embedded in scenario) | index, method, endpoint, headers, body, expected, extractions |

### Execution Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **QaPackage** | Full test run orchestration | planId, status, baseUrl, specHash, scenarios[], runs[], coverage |
| **TestRun** | Single scenario execution | id, scenarioId, planId, status, baseUrl, durationMs |
| **TestStepResult** | Per-step execution result | runId, stepIndex, passed, actualStatus, actualBody, errorMessage |
| **QaPackageEvent** | Lifecycle event | planId, type, payload, timestamp |

### Observability Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **InteractionLog** | AI/HTTP interaction record | planId, correlationId, type, requestSummary, responseSummary |

## Relationship Cardinalities

| Relationship | Cardinality | Description |
|--------------|-------------|-------------|
| Requirement → ApiSpec | 1:N | One requirement can have multiple specs (versions) |
| ApiSpec → TestSuite | 1:N | One spec can have multiple test suites |
| TestSuite → TestScenario | 1:N | One suite contains many scenarios |
| TestScenario → TestStep | 1:N (embedded) | Scenario contains ordered steps |
| TestScenario → TestRun | 1:N | Scenario can be run multiple times |
| TestRun → TestStepResult | 1:N | Run has result per step |
| QaPackage → TestScenario | 1:N | Package generates multiple scenarios |
| QaPackage → TestRun | 1:N | Package triggers runs for scenarios |
| QaPackage → QaPackageEvent | 1:N | Package emits lifecycle events |

## Database Schema (PostgreSQL)

### Core Tables

```sql
-- Test Suites
CREATE TABLE test_suites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    requirement_id UUID REFERENCES requirements(id),
    api_spec_id UUID REFERENCES api_specs(id),
    default_base_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Scenarios
CREATE TABLE test_scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    suite_id UUID REFERENCES test_suites(id),
    plan_id VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    steps_json JSONB NOT NULL,
    source VARCHAR(50) DEFAULT 'AI_GENERATED',
    spec_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Runs
CREATE TABLE test_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID REFERENCES test_scenarios(id) NOT NULL,
    plan_id VARCHAR(100),
    base_url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Step Results
CREATE TABLE test_step_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID REFERENCES test_runs(id) NOT NULL,
    step_index INT NOT NULL,
    passed BOOLEAN NOT NULL,
    actual_status INT,
    actual_body TEXT,
    actual_headers JSONB,
    error_message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(run_id, step_index)
);

-- QA Package Runs (orchestration)
CREATE TABLE qa_package_runs (
    plan_id VARCHAR(100) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    spec_url VARCHAR(500),
    spec_hash VARCHAR(64),
    requirement_text TEXT,
    scenarios_total INT DEFAULT 0,
    scenarios_passed INT DEFAULT 0,
    scenarios_failed INT DEFAULT 0,
    coverage_percent DECIMAL(5,2),
    qa_summary TEXT,
    triggered_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- QA Package Events
CREATE TABLE qa_package_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_events_plan_id ON qa_package_events(plan_id);
CREATE INDEX idx_events_created_at ON qa_package_events(created_at);
```

### Indexes for Performance

```sql
-- Scenario lookups
CREATE INDEX idx_scenarios_suite_id ON test_scenarios(suite_id);
CREATE INDEX idx_scenarios_plan_id ON test_scenarios(plan_id);
CREATE INDEX idx_scenarios_spec_hash ON test_scenarios(spec_hash);

-- Run queries
CREATE INDEX idx_runs_scenario_id ON test_runs(scenario_id);
CREATE INDEX idx_runs_plan_id ON test_runs(plan_id);
CREATE INDEX idx_runs_status ON test_runs(status);
CREATE INDEX idx_runs_created_at ON test_runs(created_at DESC);

-- Step results
CREATE INDEX idx_step_results_run_id ON test_step_results(run_id);

-- Package runs
CREATE INDEX idx_package_runs_status ON qa_package_runs(status);
CREATE INDEX idx_package_runs_created_at ON qa_package_runs(created_at DESC);
```

## Domain Model (Kotlin)

```kotlin
// Value classes for type-safe IDs
@JvmInline
value class ScenarioId(val value: UUID)

@JvmInline
value class RunId(val value: UUID)

@JvmInline
value class PlanId(val value: String)

// Core entities
data class TestScenario(
    val id: ScenarioId = ScenarioId(UUID.randomUUID()),
    val suiteId: UUID? = null,
    val planId: PlanId? = null,
    val name: String,
    val description: String? = null,
    val steps: List<TestStep>,
    val source: ScenarioSource = ScenarioSource.AI_GENERATED,
    val specHash: String? = null,
    val createdAt: Instant = Instant.now()
)

data class TestStep(
    val index: Int,
    val name: String,
    val method: HttpMethod,
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    val body: Any? = null,
    val expected: Expected,
    val extractions: Map<String, String> = emptyMap()
)

data class Expected(
    val status: Int,
    val bodyFields: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap()
)

data class TestRun(
    val id: RunId = RunId(UUID.randomUUID()),
    val scenarioId: ScenarioId,
    val planId: PlanId? = null,
    val baseUrl: String,
    val status: RunStatus,
    val triggeredBy: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val durationMs: Long? = null,
    val results: List<TestStepResult> = emptyList()
)

data class TestStepResult(
    val runId: RunId,
    val stepIndex: Int,
    val passed: Boolean,
    val actualStatus: Int?,
    val actualBody: String?,
    val actualHeaders: Map<String, String>? = null,
    val errorMessage: String? = null,
    val durationMs: Long? = null
)

enum class ScenarioSource {
    AI_GENERATED, MANUAL, IMPORTED
}

enum class RunStatus {
    PENDING, RUNNING, PASSED, FAILED, TIMEOUT, ERROR
}
```

## References

- [ADR-006: Test Scenario JSON Contract](../decisions/ADR-006-test-scenario-json-contract.md)
- [ADR-002: Clean Architecture with Domain-Centric Layers](../decisions/ADR-002-clean-architecture-layers.md)
