# QAWave - Business Requirements Deep Dive

> This document consolidates and analyzes the existing business documentation, TODO files, and implementation status to provide a complete picture of the QAWave platform.

---

## Executive Summary: What is QAWave?

### The One-Liner

**QAWave is an AI-powered QA automation platform that acts as your virtual QA engineering team for backend APIs.**

### The Elevator Pitch

Imagine you have a new backend API. Today, you would:
1. Read the requirements document
2. Study the OpenAPI specification
3. Manually write test cases in Postman, Jest, or pytest
4. Execute tests and analyze failures
5. Update tests every time the API changes
6. Repeat for every microservice...

**QAWave automates this entire workflow using AI agents.**

You provide:
- 📄 **Business requirements** (what the API should do)
- 📋 **OpenAPI specification** (how the API is structured)
- 🌐 **Base URL** (where the API is running)

QAWave delivers:
- ✅ **Auto-generated test scenarios** with multi-step flows
- ✅ **Real API execution** against your running system
- ✅ **Intelligent result evaluation** with pass/fail determination
- ✅ **Full audit trail** of every test run
- ✅ **Coverage tracking** per API operation

### How It Works: The AI Agent Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           QAWave AI Pipeline                                │
└─────────────────────────────────────────────────────────────────────────────┘

     ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
     │  INPUTS      │         │  AI AGENTS   │         │  OUTPUTS     │
     └──────────────┘         └──────────────┘         └──────────────┘

  ┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────┐
  │ Requirements    │────▶│ Requirements        │────▶│ Parsed User     │
  │ Document        │     │ Analyzer Agent      │     │ Flows & Criteria│
  └─────────────────┘     └─────────────────────┘     └─────────────────┘
          │                         │                         │
          │                         ▼                         │
          │               ┌─────────────────────┐             │
          │               │ Scenario Generator  │             │
          └──────────────▶│ Agent               │◀────────────┘
                          └─────────────────────┘
  ┌─────────────────┐               │
  │ OpenAPI Spec    │───────────────┤
  │ (YAML/JSON)     │               │
  └─────────────────┘               ▼
                          ┌─────────────────────┐
                          │ Generated Test      │
                          │ Scenarios & Steps   │
                          └─────────────────────┘
                                    │
                                    ▼
  ┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────┐
  │ Base URL        │────▶│ Test Executor       │────▶│ HTTP Responses  │
  │ (System Under   │     │ Agent               │     │ (Actual Results)│
  │  Test)          │     └─────────────────────┘     └─────────────────┘
  └─────────────────┘               │                         │
                                    │                         │
                                    ▼                         ▼
                          ┌─────────────────────┐     ┌─────────────────┐
                          │ Result Evaluator    │────▶│ Pass/Fail       │
                          │ Agent               │     │ Verdicts        │
                          └─────────────────────┘     └─────────────────┘
                                    │
                                    ▼
                          ┌─────────────────────┐
                          │ Result Reviewer     │
                          │ Agent (AI Summary)  │
                          └─────────────────────┘
                                    │
                                    ▼
                          ┌─────────────────────┐
                          │ QA Package Report   │
                          │ with Coverage &     │
                          │ Recommendations     │
                          └─────────────────────┘
```

### The Four AI Agents

| Agent | Responsibility | Input | Output |
|-------|---------------|-------|--------|
| **Requirements Analyzer** | Extracts testable user flows and acceptance criteria from natural language requirements | Requirements text | Structured user flows, acceptance criteria |
| **Scenario Generator** | Creates executable test scenarios with ordered steps, using OpenAPI to understand available endpoints | User flows + OpenAPI spec | TestScenario objects with TestSteps |
| **Test Executor** | Executes HTTP calls against the real system, handles authentication, extracts values for chaining | TestSteps + Base URL | HTTP responses, extracted values |
| **Result Evaluator** | Compares actual responses against expected outcomes, determines pass/fail with explanations | Expected vs Actual | Pass/Fail verdicts, failure reasons |
| **Result Reviewer** | AI-powered analysis of overall run, provides summary and recommendations | All results | QA summary, coverage report |

### Current State (v0.7 - January 2026)

**What's Working Today:**

| Capability | Status | Description |
|------------|--------|-------------|
| **AI Scenario Generation** | ✅ Live | OpenAI/Venice integration generates multi-step test scenarios |
| **Real API Execution** | ✅ Live | HTTP client executes tests against any REST API |
| **Result Evaluation** | ✅ Live | Status codes, body assertions, header checks |
| **Streaming Pipeline** | ✅ Live | Parallel generation + execution with Kotlin Flow |
| **Web Dashboard** | ✅ Live | React UI for managing runs, viewing results |
| **Coverage Tracking** | ✅ Live | Per-operation coverage metrics |
| **Resilience** | ✅ Live | Circuit breaker, rate limiting, retries |

**What's Coming Next:**

| Capability | Timeline | Description |
|------------|----------|-------------|
| **Authentication** | Q1 2026 | Keycloak integration for multi-user access |
| **Security Testing** | Q3 2026 | OWASP API Top 10 vulnerability scanning |
| **Performance Testing** | Q4 2026 | k6 script generation and load testing |
| **Contract Testing** | Q1 2027 | OpenAPI compliance and breaking change detection |
| **Self-Healing Tests** | Q3 2027 | Auto-update scenarios when APIs change |

### The Long-Term Vision

**Phase 1: Functional Testing (Current)**
> "Does the API work correctly?"
- AI generates test scenarios from requirements
- Executes against real APIs
- Validates responses

**Phase 2: Security Testing (2026)**
> "Is the API secure?"
- OWASP API Security Top 10 scanning
- Injection detection (SQL, NoSQL, XSS)
- Authentication/authorization testing
- Security misconfiguration checks

**Phase 3: Performance Testing (2026-2027)**
> "Can the API handle load?"
- Auto-generate k6 load test scripts
- Execute stress/soak/spike tests
- Track latency percentiles (P95, P99)
- Identify performance bottlenecks

**Phase 4: Contract Testing (2027)**
> "Will API changes break consumers?"
- OpenAPI schema compliance validation
- Breaking change detection between versions
- Consumer-driven contract support (Pact)

**Phase 5: Intelligent Automation (2027+)**
> "Can tests maintain themselves?"
- Self-healing tests that adapt to API changes
- AI-suggested fixes when tests fail
- Predictive test selection based on code changes
- Continuous learning from test results

### Why QAWave?

| Traditional Approach | QAWave Approach |
|---------------------|-----------------|
| QA manually reads specs and writes tests | AI reads specs and generates tests automatically |
| Tests written in code (Postman, Jest, pytest) | Tests defined as JSON scenarios, executed by platform |
| Hard to maintain when APIs change | Self-healing tests adapt to changes |
| Each microservice needs separate test suite | Unified platform for all APIs |
| Coverage tracking is manual/absent | Automatic coverage per OpenAPI operation |
| Security testing is separate tool/process | Integrated OWASP scanning |
| Performance testing requires k6/JMeter expertise | Auto-generated load tests |
| No traceability to requirements | Every test linked to requirements |

### Technical Foundation

```
┌─────────────────────────────────────────────────────────────────┐
│                      QAWave Architecture                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    React Frontend                        │   │
│  │  TanStack Router • TanStack Query • Tailwind CSS        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Ktor Backend                          │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │   │
│  │  │ Presentation│ │ Application │ │   Domain    │        │   │
│  │  │   (Routes)  │ │  (Services) │ │  (Models)   │        │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘        │   │
│  │  ┌─────────────────────────────────────────────┐        │   │
│  │  │            Infrastructure                    │        │   │
│  │  │  PostgreSQL • Kafka • AI Clients • HTTP     │        │   │
│  │  └─────────────────────────────────────────────┘        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│              ┌───────────────┼───────────────┐                 │
│              ▼               ▼               ▼                 │
│  ┌───────────────┐ ┌─────────────────┐ ┌───────────────┐      │
│  │  PostgreSQL   │ │  Kafka (Events) │ │ OpenAI/Venice │      │
│  │  (Persistence)│ │  (Streaming)    │ │ (AI Provider) │      │
│  └───────────────┘ └─────────────────┘ └───────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Key Differentiators

1. **API-First Focus**: Unlike generic test case management tools, QAWave is purpose-built for backend API testing with deep OpenAPI integration.

2. **Agent-Based Architecture**: Each AI agent has a single responsibility, making the system modular, testable, and extensible.

3. **Real Execution**: Not just test case documentation—QAWave actually executes HTTP calls against your running system.

4. **Full Traceability**: Every test scenario links back to requirements and OpenAPI operations for complete audit trails.

5. **Streaming Pipeline**: Parallel generation and execution using Kotlin Flow for optimal performance.

6. **Production-Ready**: Built with resilience patterns (circuit breaker, rate limiting, retries) from day one.

---

## Table of Contents
0. [Executive Summary: What is QAWave?](#executive-summary-what-is-qawave)
1. [Business Analysis Deep Dive](#1-business-analysis-deep-dive)
2. [Core Use Cases & User Workflows](#2-core-use-cases--user-workflows)
3. [Technical Architecture Deep Dive](#3-technical-architecture-deep-dive)
4. [Target Users & Personas](#4-target-users--personas)
5. [Domain Model](#5-domain-model)
6. [Milestone Roadmap](#6-milestone-roadmap)
7. [Implementation Status](#7-implementation-status)
8. [RFCs & Technical Specifications](#8-rfcs--technical-specifications)
9. [Product Enhancement Roadmap](#9-product-enhancement-roadmap)
10. [Enterprise Features Roadmap](#10-enterprise-features-roadmap)
11. [Production Readiness Checklist](#11-production-readiness-checklist)
12. [Future Roadmap (M6+)](#12-future-roadmap-m6)
13. [Related Documentation](#13-related-documentation)

---

## 1. Business Analysis Deep Dive

### 1.1 Market Context & Industry Challenges

The API testing market faces fundamental challenges that create opportunity for disruption:

| Industry Challenge | Current Reality | Business Impact |
|-------------------|-----------------|-----------------|
| **Manual Test Creation** | QA engineers spend 60-80% of time writing and maintaining tests | High labor costs, slow release cycles |
| **API Proliferation** | Average enterprise has 500+ internal APIs | Testing coverage gaps, inconsistent quality |
| **Microservices Complexity** | Each service needs own test suite | Maintenance burden grows exponentially |
| **Rapid API Evolution** | APIs change weekly/daily in agile teams | Tests become stale, false positives/negatives |
| **Skill Gap** | Shortage of QA automation engineers | Teams can't scale testing with development |
| **Tool Fragmentation** | Postman + Jest + k6 + OWASP ZAP + manual review | Context switching, no unified view |

### 1.2 QAWave's Strategic Position

QAWave addresses these challenges through **AI-driven automation** that:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        QAWAVE VALUE CREATION                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   INPUTS                      TRANSFORMATION              OUTPUTS            │
│   ──────                      ──────────────              ───────            │
│                                                                              │
│   📄 Requirements ───┐                              ┌──► ✅ Test Coverage    │
│                      │     ┌─────────────────┐     │                        │
│   📋 OpenAPI Spec ───┼────►│   AI AGENTS     │─────┼──► 📊 Quality Metrics  │
│                      │     │                 │     │                        │
│   🌐 Running API ────┘     │  • Understand   │     ├──► 🔍 Bug Detection    │
│                            │  • Generate     │     │                        │
│                            │  • Execute      │     ├──► 📈 Trend Analysis   │
│                            │  • Evaluate     │     │                        │
│                            └─────────────────┘     └──► 🛡️ Risk Reduction   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Business Model & Value Proposition

**For QA Teams:**
- Reduce test creation time by 80%
- Increase coverage without increasing headcount
- Focus on edge cases and exploratory testing instead of boilerplate

**For Development Teams:**
- Faster feedback loops on API changes
- Automatic regression detection
- Traceability from requirements to test results

**For Engineering Leadership:**
- Quantifiable quality metrics per API
- Risk visibility across microservices
- Audit trail for compliance requirements

### 1.4 Business Success Metrics (KPIs)

| Metric | Description | Target |
|--------|-------------|--------|
| **Time-to-First-Test** | From uploading spec to having runnable tests | < 5 minutes |
| **Coverage Rate** | % of OpenAPI operations with tests | > 90% |
| **Test Generation Ratio** | AI-generated vs manually written tests | 10:1 |
| **Update Velocity** | Time to update tests after API change | < 1 hour |
| **Regression Detection Rate** | % of real bugs caught by platform | > 95% |
| **False Positive Rate** | Tests that fail due to test issues, not bugs | < 5% |

### 1.5 Competitive Landscape

| Competitor | Approach | QAWave Differentiation |
|------------|----------|------------------------|
| **Postman** | Manual test creation with collections | AI generates tests automatically |
| **ReadyAPI** | GUI-based test design | No GUI needed, spec-driven |
| **Pact** | Contract testing only | Full functional + contract testing |
| **Katalon** | Generic test automation | API-specific, OpenAPI-native |
| **Testim** | UI test automation | Backend/API focus |
| **Mabl** | AI for UI testing | AI for API testing |

### 1.6 Constraints & Non-Goals (Current Phase)

**In Scope:**
- Backend/API testing (REST APIs with OpenAPI specs)
- Single-team/single-tenant usage
- AI-assisted test generation and execution
- PostgreSQL persistence

**Out of Scope (For Now):**
- UI/E2E browser testing (Selenium, Playwright UI)
- GraphQL APIs (future consideration)
- gRPC/Protobuf APIs (future consideration)
- Multi-tenant SaaS features
- Complex workflow orchestration (Temporal, etc.)
- Production-grade test data management

---

## 2. Core Use Cases & User Workflows

### 2.1 Primary Use Cases

#### UC-1: Generate Test Suite from Requirements + Spec

**Actor:** QA Engineer
**Precondition:** User has requirements document and OpenAPI spec URL
**Flow:**
1. User provides requirement description (natural language)
2. User provides OpenAPI specification (URL or upload)
3. User specifies target base URL for API
4. System fetches and parses OpenAPI spec
5. AI generates test scenarios covering spec operations
6. System presents generated scenarios for review
7. User approves/adjusts scenarios
8. System persists scenarios linked to requirement and spec

**Success Criteria:**
- Scenarios cover all critical OpenAPI operations
- Each scenario has meaningful assertions
- Traceability maintained (requirement → scenario → step)

#### UC-2: Execute Test Scenarios

**Actor:** QA Engineer or CI/CD Pipeline
**Precondition:** Scenarios exist in system
**Flow:**
1. User/system selects scenarios to run
2. User specifies target environment (base URL)
3. System executes steps sequentially per scenario
4. For each step: send HTTP request, capture response
5. System evaluates actual vs expected results
6. System records pass/fail with details
7. System generates QA summary report

**Success Criteria:**
- All steps executed with proper timeouts/retries
- Placeholder values resolved from previous extractions
- Results persisted for audit/review

#### UC-3: Inspect Run Details & Debug Failures

**Actor:** QA Engineer
**Precondition:** Test run completed
**Flow:**
1. User navigates to run detail page
2. System displays per-step results (pass/fail)
3. For failed steps: show expected vs actual
4. User can view full request/response payloads
5. User can copy curl command for reproduction
6. User can view AI interactions that generated scenarios

**Success Criteria:**
- Time to understand failure < 5 minutes
- Sufficient context for debugging
- Ability to reproduce issue locally

#### UC-4: Replay Previous Run

**Actor:** QA Engineer
**Precondition:** Previous run exists with stored payload
**Flow:**
1. User selects previous run to replay
2. System retrieves stored scenarios
3. User optionally overrides base URL
4. System re-executes scenarios (no AI regeneration)
5. System compares results with original run

**Success Criteria:**
- Deterministic replay without AI variability
- Comparison highlights regressions/improvements

#### UC-5: Export Tests to External Frameworks

**Actor:** QA Engineer or DevOps
**Precondition:** Scenarios exist
**Flow:**
1. User selects scenarios to export
2. User chooses target format (Playwright API, RestAssured)
3. System generates runnable test code
4. User downloads artifact (ZIP with project structure)
5. User runs exported tests in CI/CD

**Success Criteria:**
- Generated code compiles and runs
- No embedded secrets (environment-driven)
- Traceability comments in generated code

### 2.2 User Journey Maps

#### Journey: New QA Engineer Onboarding

```
Day 1                    Week 1                   Week 2+
──────                   ──────                   ───────

├── Access platform      ├── Generate first      ├── Build test library
│                        │   scenarios           │
├── Upload sample spec   │                       ├── Set up CI/CD
│                        ├── Run against         │   integration
├── Run demo test        │   staging API         │
│                        │                       ├── Export to
├── Review results       ├── Debug first         │   Playwright/RestAssured
│                        │   failures            │
└── Understand UI        │                       └── Train team members
                         └── Iterate on
                             scenarios
```

#### Journey: Daily QA Workflow

```
Morning                  Afternoon               Evening/Overnight
───────                  ─────────               ─────────────────

├── Review overnight     ├── Generate tests      ├── CI triggers
│   run results          │   for new features    │   nightly runs
│                        │                       │
├── Triage failures      ├── Execute against     ├── Results available
│   (real bugs vs        │   dev/staging         │   for morning review
│   test issues)         │                       │
│                        ├── Export coverage     │
├── File bug reports     │   reports             │
│                        │                       │
└── Update scenarios     └── Code review test    │
    if needed                scenarios           │
```

### 2.3 Workflow Diagrams

#### Full QA Package Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         QA PACKAGE WORKFLOW                                  │
└─────────────────────────────────────────────────────────────────────────────┘

  USER INPUT                    PROCESSING                      OUTPUT
  ──────────                    ──────────                      ──────

  ┌─────────────┐
  │ Requirements│───┐
  │ Text        │   │
  └─────────────┘   │
                    │     ┌─────────────────────────────────────────┐
  ┌─────────────┐   │     │           STREAMING PIPELINE             │
  │ OpenAPI     │───┼────►│                                         │
  │ Spec URL    │   │     │  ┌─────────┐    ┌─────────┐   ┌──────┐ │
  └─────────────┘   │     │  │Parse Ops│───►│AI Gen   │──►│Execute│ │
                    │     │  └─────────┘    │Scenarios│   │Steps │ │
  ┌─────────────┐   │     │       │         └─────────┘   └──────┘ │
  │ Base URL    │───┘     │       │              │            │     │
  │ (SUT)       │         │       ▼              ▼            ▼     │
  └─────────────┘         │  ┌─────────────────────────────────┐   │
                          │  │     PARALLEL EXECUTION           │   │
                          │  │  (5 AI workers, 10 Executors)    │   │
                          │  └─────────────────────────────────┘   │
                          └─────────────────────────────────────────┘
                                            │
                                            ▼
                          ┌─────────────────────────────────────────┐
                          │            QA EVALUATION                 │
                          │  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
                          │  │ Compare │  │ Build   │  │ Generate│ │
                          │  │ Expected│  │ Coverage│  │ Summary │ │
                          │  │ vs      │  │ Report  │  │ & Recs  │ │
                          │  │ Actual  │  │         │  │         │ │
                          │  └─────────┘  └─────────┘  └─────────┘ │
                          └─────────────────────────────────────────┘
                                            │
                                            ▼
                          ┌─────────────────────────────────────────┐
                          │              OUTPUTS                     │
                          │                                         │
                          │  ├── QA Package Report (JSON/MD)       │
                          │  ├── Coverage Matrix (ops × scenarios) │
                          │  ├── Pass/Fail Verdict                 │
                          │  ├── Top Failures with Details        │
                          │  ├── AI Interaction Logs              │
                          │  └── Exportable Artifacts             │
                          └─────────────────────────────────────────┘
```

---

## 3. Technical Architecture Deep Dive

### 3.1 Streaming Pipeline Architecture

QAWave uses a **streaming architecture** where AI generation and test execution happen in parallel. This provides faster overall execution and real-time progress updates.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STREAMING PIPELINE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Operations ──┬──► [AI Worker 1] ──┬──► [Executor 1] ──┬──► Results         │
│               ├──► [AI Worker 2] ──┼──► [Executor 2] ──┤                    │
│               ├──► [AI Worker 3] ──┼──► [Executor 3] ──┤                    │
│               ├──► [AI Worker 4] ──┼──► [Executor ...] ─┤                    │
│               └──► [AI Worker 5] ──┴──► [Executor 10] ─┴──► Collected       │
│                                                                              │
│  AI Semaphore: 5 permits          Exec Semaphore: 10 permits                │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Benefits:**
- **Faster overall execution**: Execution starts as soon as scenarios are generated
- **Real-time progress**: Events recorded immediately as results become available
- **Better resource utilization**: Configurable concurrency limits

### 3.2 AI Agent Model

The AI agents follow a **strategy pattern** with clear contracts:

```kotlin
// Agent Pipeline (simplified)
interface ScenarioGeneratorAgent {
    suspend fun generate(
        requirementText: String,
        baseUrl: String,
        openApiSpec: String?
    ): List<TestScenario>

    suspend fun generateForOperations(
        operations: List<SpecOperation>,
        baseUrl: String,
        promptTemplate: String?
    ): List<TestScenario>
}

interface TestExecutor {
    suspend fun execute(
        step: TestStep,
        context: ExecutionContext,
        baseUrl: String
    ): StepExecution
}

interface ResultEvaluator {
    fun evaluate(
        step: TestStep,
        execution: StepExecution
    ): TestStepResult
}
```

**Agent Responsibilities:**

| Agent | Input | Processing | Output |
|-------|-------|------------|--------|
| **RequirementsAnalyzer** | Natural language requirements | Extract user flows, acceptance criteria | Structured test objectives |
| **ScenarioGenerator** | Operations + spec | AI prompt → JSON scenarios | `List<TestScenario>` |
| **TestExecutor** | Steps + base URL | HTTP calls with retries | Actual responses |
| **ResultEvaluator** | Expected vs actual | Assertion matching | Pass/fail verdicts |
| **ResultReviewer** | All run results | AI summary generation | QA report |

### 3.3 Test Scenario JSON Contract

The test scenario contract is the **stable interface** between AI generation and execution:

```json
{
  "name": "User can register and login",
  "description": "Happy path for user authentication flow",
  "steps": [
    {
      "index": 0,
      "name": "Register new user",
      "method": "POST",
      "endpoint": "{baseUrl}/api/users",
      "headers": { "Content-Type": "application/json" },
      "body": {
        "email": "test@example.com",
        "password": "secret123"
      },
      "expected": {
        "status": 201,
        "bodyFields": {
          "id": "<any>",
          "email": "test@example.com"
        }
      },
      "extractions": { "userId": "$.id" }
    },
    {
      "index": 1,
      "name": "Login with credentials",
      "method": "POST",
      "endpoint": "{baseUrl}/api/auth/login",
      "headers": { "Content-Type": "application/json" },
      "body": {
        "email": "test@example.com",
        "password": "secret123"
      },
      "expected": {
        "status": 200,
        "bodyFields": {
          "token": "<any>",
          "user.id": "{userId}"
        }
      },
      "extractions": { "authToken": "$.token" }
    }
  ]
}
```

**Assertion Types Supported:**
- `<any>` - Wildcard, any value accepted
- `contains:needle` - Substring match
- `regex:<pattern>` - Regular expression match
- `>n`, `<n`, `>=n`, `<=n`, `!=n` - Numeric comparisons

### 3.4 Event System & State Machine

QA package runs follow a defined state machine:

```
REQUESTED → SPEC_FETCHED → [SCENARIO_CREATED + EXECUTION_* interleaved...]
                                                           ↓
    ← ← ← ← ← ← ← ← ← ← ← ← ← ← FAILED ← ← ← ← ← ← ← ← ← ↓
    ↓                                                      ↓
    └──────────────────────────────────────────────────► AI_SUCCESS
                                                           ↓
                                                     QA_EVAL_DONE
                                                           ↓
                                                       COMPLETE
```

**Event Types:**
| Event | Description |
|-------|-------------|
| `REQUESTED` | QA package run initiated |
| `SPEC_FETCHED` | OpenAPI spec fetched and parsed |
| `SCENARIO_CREATED` | Individual scenario saved |
| `EXECUTION_SUCCESS` | Scenario executed and passed |
| `EXECUTION_FAILED` | Scenario executed and failed |
| `AI_SUCCESS` | All AI generation completed |
| `AI_FAILED` | AI generation failed for operation |
| `QA_EVAL_DONE` | QA evaluation completed |
| `COMPLETE` | Entire pipeline finished |
| `FAILED` | Pipeline failed |

### 3.5 Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  Routes (thin) → Controllers → HTTP DTOs                    │
│  Only: deserialize, validate, call service, serialize       │
└─────────────────────────────────────────────────────────────┘
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│  Services, Use Cases, Application DTOs                      │
│  Orchestration, business logic                              │
│  Depends only on: domain interfaces                         │
└─────────────────────────────────────────────────────────────┘
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  Entities, Value Objects, Repository Interfaces             │
│  Pure Kotlin, NO framework dependencies                     │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ implements
┌─────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                       │
│  Repository Implementations, AI Clients, Kafka Producers    │
│  Framework-specific code (Ktor, Exposed, etc.)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Target Users & Personas

### 4.1 Primary Users

#### Admin
- Full access to all test assets and environments
- Can configure connections to systems under test
- Can manage AI provider configs (OpenAI/Venice)
- Can manage test suites and scenarios

#### QA Engineer / Tester
- Main "power user" of the platform
- Upload requirements and API specs
- Ask AI to generate scenarios
- Review/approve/adjust generated scenarios
- Trigger runs and inspect results

#### Viewer / Stakeholder
- Read-only access
- View test suites, scenarios, and their latest status
- Drill into failed runs
- Export results for reporting

### 4.2 AI Agents (Virtual Actors)
Conceptual roles, not human users. Their actions are audit-worthy:
- **Requirements Analyzer Agent** - Extracts user flows and acceptance criteria
- **Scenario Generator Agent** - Creates test scenarios from requirements + OpenAPI
- **Test Executor Agent** - Runs HTTP calls against system under test
- **Result Evaluator Agent** - Validates responses and determines pass/fail

---

## 5. Domain Model

### Core Entities

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Requirement │────▶│   ApiSpec   │────▶│  TestSuite  │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ TestScenario│
                                        └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │  TestStep   │
                                        └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │   TestRun   │
                                        └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │TestStepResult│
                                        └─────────────┘
```

### Entity Definitions

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **Requirement** | Business need text, optionally linked to Jira | id, title, description, externalReference |
| **ApiSpec** | OpenAPI specification (YAML/JSON) | id, name, rawContent/url, version |
| **TestSuite** | Logical grouping of scenarios | id, name, requirementId, apiSpecId, defaultBaseUrl |
| **TestScenario** | Concrete test case with ordered steps | id, suiteId, name, description, source, steps |
| **TestStep** | Single API action + assertions | index, method, endpoint, headers, body, expected, extractions |
| **TestRun** | Execution instance of a scenario | id, scenarioId, triggeredBy, baseUrl, status, timestamps |
| **TestStepResult** | Per-step outcome | runId, stepIndex, actualStatus, actualBody, passed, durationMs |
| **QaPackage** | Container for full run with AI evaluation | planId, scenarios, runs, coverage, qaSummary |

### JSON Contract (Scenario Schema)

```json
{
  "name": "User can register and login",
  "description": "Happy path registration + login",
  "steps": [
    {
      "index": 0,
      "name": "Register user",
      "method": "POST",
      "endpoint": "/api/users",
      "headers": { "Content-Type": "application/json" },
      "body": { "email": "test@example.com", "password": "secret123" },
      "expected": {
        "status": 201,
        "bodyFields": { "id": "<any>", "email": "test@example.com" }
      },
      "extractions": { "userId": "$.id" }
    }
  ]
}
```

---

## 6. Milestone Roadmap

### Milestone 1 - Thin End-to-End POC ✅ COMPLETE
**Goal:** Prove the core loop: Requirement → Scenario → API call → Result

| Component | Status | Details |
|-----------|--------|---------|
| Ktor backend skeleton | ✅ | Single module, health endpoint |
| Generate-and-run endpoint | ✅ | `POST /api/tests/run-once` |
| HTTP call to sample endpoint | ✅ | TestExecutor with Ktor client |
| React POC UI | ✅ | Form + result panel |

### Milestone 2 - Persistence & Scenario Management ✅ COMPLETE
**Goal:** Turn POC into a test catalogue

| Component | Status | Details |
|-----------|--------|---------|
| PostgreSQL + Flyway | ✅ | Migrations for all tables |
| Scenario CRUD | ✅ | Full repository implementations |
| Run history | ✅ | TestRun + TestStepResult persistence |
| UI scenario list/detail | ✅ | TanStack Router pages |

### Milestone 3 - Real AI & Multi-Step Scenarios ✅ COMPLETE
**Goal:** AI-powered test generation

| Component | Status | Details |
|-----------|--------|---------|
| AI abstraction layer | ✅ | AiClient interface |
| OpenAI/Venice clients | ✅ | Provider implementations |
| ScenarioGeneratorAgent | ✅ | AI + template-based generation |
| Multi-step execution | ✅ | Placeholder resolution, extractions |
| Body assertions | ✅ | Status, contains, regex, comparisons |

### Milestone 4 - Authentication (Keycloak) ⏳ PENDING
**Goal:** Secure the platform for multi-user use

| Component | Status | Details |
|-----------|--------|---------|
| Keycloak realm setup | ❌ | Not started |
| JWT validation in Ktor | ❌ | Not started |
| Frontend Keycloak integration | ❌ | Not started |
| Role-based access control | ❌ | Not started |

### Milestone 5 - UX & Advanced Features ⏳ PARTIAL
**Goal:** Polish and extend

| Component | Status | Details |
|-----------|--------|---------|
| Better UX | ✅ | Cards, empty states, loading states |
| Scenario editing | ❌ | Not implemented |
| Suite-level runs | ✅ | QaPackage orchestration |
| Observability | ✅ | Logging, event streaming |
| Scheduling | ❌ | Not implemented |

---

## 7. Implementation Status

### 7.1 Backend - What's Working ✅

| Area | Items Implemented |
|------|-------------------|
| **Domain Models** | TestScenario, TestStep, TestRun, TestStepResult, QaPackage, Requirement, ApiSpec |
| **Repositories** | All Exposed-based implementations (12 files) |
| **AI Integration** | OpenAI, Venice, Stub clients; ScenarioGeneratorAgent; ResultReviewerAgent |
| **Execution** | TestExecutor with retries, timeouts, placeholder resolution, extractions |
| **Evaluation** | ResultEvaluator with status/body/header assertions |
| **Streaming Pipeline** | Parallel generation + execution with Kotlin Flow |
| **Event System** | Kafka producer + in-memory fallback |
| **API Routes** | All CRUD operations, async/sync execution |

### 7.2 Frontend - What's Working ✅

| Area | Items Implemented |
|------|-------------------|
| **Pages** | QaPackageRuns, RunDetail, Scenarios, ScenarioDetail, AiLogs, Suites |
| **Data Fetching** | TanStack Query with polling |
| **Routing** | TanStack Router with type-safe routes |
| **UI Components** | Cards, StatusBadge, Skeleton, EmptyState, ErrorBoundary |
| **Real-time Updates** | Event polling with conditional refetch |

### 7.3 What's Partial ⚠️

| Area | Status | Notes |
|------|--------|-------|
| **Test Coverage** | ~40% backend, 0% frontend | Need more unit/integration tests |
| **Scenario Editing** | Not implemented | JSON editor planned |
| **Scheduling** | Not implemented | Cron-based runs planned |
| **Analytics Dashboard** | Not implemented | Pass-rate trends planned |

### 7.4 What's Not Started ❌

| Area | Notes |
|------|-------|
| **Authentication** | Keycloak integration (Milestone 4) |
| **Multi-tenancy** | Workspace/team isolation |
| **Self-healing Tests** | Auto-fix when APIs change |
| **CI/CD Integration** | GitHub Actions, GitLab |
| **k6 Performance Testing** | Load test generation |

---

## 8. RFCs & Technical Specifications

### 8.1 RFC-001: AI Generation Verification & Retry Loop

**Status:** Proposed | **Tags:** ai, scenarios, reliability

**Problem:** AI-generated scenarios occasionally fail schema validation, reference operations not in spec, or leave placeholders unresolved.

**Solution:**
1. **Verification Pipeline** (synchronous within generation):
   - Schema validation
   - Spec alignment (endpoint/method maps to known operation)
   - Placeholder safety (unresolved placeholders detected)
   - Size/shape limits enforcement

2. **Retry Strategy:**
   - On failure, build corrective hint with failure class
   - Re-issue generation with hint (max 2 retries)
   - Mark attempts as `PENDING` → `RETRYING` → `PASSED`/`FAILED`

3. **Persistence:**
   - Store verification attempts, status, failure class
   - Surface verification errors via API (422 responses)

**Config:** `AI_VERIFICATION_RETRIES`, `AI_VERIFICATION_BACKOFF_MS`, `AI_MAX_STEPS`

### 8.2 RFC-002: QA Package Persistence & Replay

**Status:** Proposed | **Tags:** qa-package, persistence, replay

**Problem:** QA package runs need durable history, consistent status transitions, and deterministic replay.

**Solution:**
1. **State Machine** (enforced at repository level):
   ```
   REQUESTED → SPEC_FETCHED → AI_SUCCESS → EXECUTION_* → QA_EVAL_DONE → COMPLETE
                    ↘ FAILED_* (on error)
   ```

2. **Persistence Contract:**
   - `savePayload(planId, payloadJson)` called once per run
   - Payloads compressed (gzip) when size exceeds threshold
   - Store specHash, scenario_count, coverage metrics

3. **Replay Hardening:**
   - `/run/{planId}/replay` uses stored payload only (no AI regeneration)
   - Allow base URL override for different environments

### 8.3 RFC-003: Coverage Reporting & Gap Detection

**Status:** Proposed | **Tags:** coverage, reporting

**Problem:** Cannot answer "what percent of this API spec is covered, and where are the gaps?"

**Solution:**
1. **Coverage Snapshot Model:**
   - Persist per `planId`: ops_total, ops_covered, scenarios_passed/failed
   - Per-operation rows: method, path, status (COVERED/FAILED/UNTESTED)

2. **Gap Detection Rules:**
   - **Uncovered:** operations not mapped to any scenario
   - **Failing:** operations with failures in latest run
   - **Placeholder gaps:** unresolved `{...}` placeholders
   - **Weak assertions:** empty `expected.bodyFields`

3. **APIs:**
   - `GET /api/coverage/{planId}` - summary + operations
   - `GET /api/coverage/latest?specHash=...` - most recent snapshot

### 8.4 RFC-004: AI Interaction Observatory

**Status:** Proposed | **Tags:** observability, debugging, training

**Problem:** Need comprehensive logging of AI requests/responses for debugging, training data collection, and audit.

**Solution:**
1. **Interaction Log Model:**
   ```kotlin
   data class InteractionLog(
       val id: String,
       val planId: String,
       val correlationId: String,
       val type: InteractionType,  // AI_GENERATION, HTTP_REQUEST
       val status: InteractionStatus,
       val requestSummary: String,
       val responseSummary: String?,
       val durationMs: Long?,
       val createdAt: Instant
   )
   ```

2. **Storage Strategy:**
   - Database: quick queries, metadata, correlations
   - File storage: full request/response payloads

3. **Integration:**
   - `ObservableAiClient` wrapper logs all AI calls
   - `ObservableHttpClient` wrapper logs all SUT calls
   - Correlation by `planId + operationId`

---

## 9. Product Enhancement Roadmap

### 9.1 High-ROI Initiatives (Top 5)

| Priority | Initiative | Why | Success Signal |
|----------|------------|-----|----------------|
| 1 | **Persistent QA package storage & replay** | Enables history, auditing, reproducibility | 95% payload fetch success; replay works |
| 2 | **AI output verification & retry loop** | Reduces flaky scenarios, higher first-pass rate | Verification errors surfaced; failure rate down |
| 3 | **Coverage reporting & gap detection** | Makes "done" measurable | Coverage visible for every run |
| 4 | **Observability for runs** | Faster incident triage | Dashboards for runs/AI/executor |
| 5 | **Auth/RBAC for teams** | Enterprise readiness | Protected APIs, role-based UI |

### 9.2 Scenario & Run Workflows

- [ ] **JSON scenario editor** with validation and preview before save
- [ ] **Manual edit/re-run**: tweak headers/body/placeholders, re-execute
- [ ] **Run comparisons**: show deltas between two runs (status changes, new failures)
- [ ] **Attachments**: allow users to upload OpenAPI files or paste specs

### 9.3 QA Package UX

- [ ] **Persistent payload download** (JSON/MD), print-friendly report view
- [ ] **Per-operation coverage visualization** with links to scenarios
- [ ] **Retry failed scenarios only** with quick-fill for unresolved placeholders

### 9.4 Reporting & Exports

- [ ] **CSV/JSON exports** for runs and coverage
- [ ] **Shareable link** to a run
- [ ] **Email/Slack webhook notifications** on completion/failure

### 9.5 Test Code Exporter

Generate runnable API tests from QAWave `TestScenario` JSON into:

| Target | Description | Output |
|--------|-------------|--------|
| **Playwright API** | TypeScript tests using `@playwright/test` | Full project with package.json, config |
| **RestAssured/Kotlin** | JUnit5 tests with RestAssured | Gradle project with dependencies |

Features:
- No embedded secrets (environment-driven)
- Traceability comments (scenario version, specHash)
- Assertions mapping (status, headers, JSON path checks)

---

## 10. Enterprise Features Roadmap

### 10.1 Authentication & Access Control

| Component | Description | Priority |
|-----------|-------------|----------|
| **Keycloak Integration** | JWT validation, realm setup | High |
| **Role-Based Access** | Admin (all), Tester (create/run), Viewer (read-only) | High |
| **Frontend Auth** | keycloak-js integration, protected routes | High |

### 10.2 Auditability

- [ ] **Audit log** for scenario create/update, run trigger, AI retry, payload download
- [ ] **Audit queries** per user/planId/time range
- [ ] **Compliance export** for SOC2 evidence

### 10.3 Multi-Tenant/Team Support

- [ ] **Org/project scoping** for scenarios/runs
- [ ] **Team-level settings**: default base/spec URLs, webhooks, notification rules
- [ ] **Data isolation** by tenant

### 10.4 Governance & Safety

- [ ] **Data retention policies**
- [ ] **PII redaction** in logs
- [ ] **Role-based actions** (delete/edit restricted)
- [ ] **Approval workflows** for destructive actions

### 10.5 DevOps & Observability

| Component | Description |
|-----------|-------------|
| **Structured Logging** | JSON logs with planId/runId, redacted secrets |
| **Metrics** | Run duration, pass/fail counts, AI latency, HTTP call latency |
| **Tracing** | Distributed tracing for executor HTTP calls and AI requests |
| **Health Checks** | `/health` and `/ready` with DB/Kafka checks |
| **Alerts** | High failure rate, long runtimes, AI error spikes |

---

## 11. Production Readiness Checklist

### 11.1 Status Overview

| Category | Status | Key Items |
|----------|--------|-----------|
| **Architecture** | ⚠️ Partial | Graceful shutdown needed, SLOs undefined |
| **Testing** | ⚠️ Partial | Unit tests partial, integration tests new, E2E missing |
| **Security** | ⚠️ Partial | Input validation done, auth missing, rate limiting done |
| **Observability** | ⚠️ Partial | Logging done, metrics partial, alerts missing |
| **Database** | ⚠️ Partial | Migrations working, backups not configured |
| **CI/CD** | ❌ Missing | No pipeline configured |
| **Documentation** | ⚠️ Partial | OpenAPI done, user guides missing |

### 11.2 Critical Items Before Production

| Priority | Item | Description | Status |
|----------|------|-------------|--------|
| P0 | **Authentication** | Keycloak integration | ❌ Not started |
| P0 | **CI/CD Pipeline** | GitHub Actions for testing/deployment | ❌ Not started |
| P0 | **HTTPS** | TLS termination in production | ❌ Not started |
| P1 | **Backup Strategy** | Database backup and restore procedures | ❌ Not started |
| P1 | **Monitoring/Alerts** | Error rate, latency alerts | ⚠️ Partial |
| P2 | **User Documentation** | Getting started guide, API reference | ⚠️ Partial |

### 11.3 Deployment Checklist

- [ ] Environment variables configured (no secrets in code)
- [ ] Database migrations run successfully
- [ ] Health checks responding (`/health`, `/ready`)
- [ ] SSL/TLS certificates configured
- [ ] Rate limiting enabled
- [ ] Circuit breaker configured for AI calls
- [ ] Backup schedule configured
- [ ] Monitoring dashboards created
- [ ] Alert rules configured
- [ ] Log aggregation set up

---

## 12. Future Roadmap (M6+)

> **Vision Extension:** Beyond functional API testing, QAWave will evolve into a comprehensive API quality platform covering security, performance, contracts, and intelligent test data generation.

### Milestone 6 - Security Testing Module (OWASP)

**Goal:** Detect common security vulnerabilities in APIs automatically

| Component | Description | Priority |
|-----------|-------------|----------|
| **SecurityScannerAgent** | AI agent specialized in security testing | High |
| **OWASP Top 10 Checks** | Injection, XSS, broken auth, sensitive data exposure | High |
| **Authentication Testing** | JWT validation, session management, privilege escalation | High |
| **Authorization Testing** | BOLA/IDOR detection, role boundary testing | High |
| **Security Headers Validation** | CORS, CSP, HSTS, X-Frame-Options checks | Medium |
| **Rate Limiting Verification** | Brute force protection, API abuse detection | Medium |
| **Input Fuzzing** | Malformed payloads, boundary testing, encoding attacks | Medium |

**Key Capabilities:**
```
SecurityScannerAgent
├── OWASP API Security Top 10 (2023)
│   ├── API1: Broken Object Level Authorization (BOLA)
│   ├── API2: Broken Authentication
│   ├── API3: Broken Object Property Level Authorization
│   ├── API4: Unrestricted Resource Consumption
│   ├── API5: Broken Function Level Authorization
│   ├── API6: Unrestricted Access to Sensitive Business Flows
│   ├── API7: Server Side Request Forgery (SSRF)
│   ├── API8: Security Misconfiguration
│   ├── API9: Improper Inventory Management
│   └── API10: Unsafe Consumption of APIs
├── SQL/NoSQL Injection Detection
├── XSS Payload Testing
├── Authentication Bypass Attempts
└── Security Report Generation (SARIF format)
```

### Milestone 7 - Performance Testing Module

**Goal:** Generate and execute load tests from existing scenarios

| Component | Description | Priority |
|-----------|-------------|----------|
| **PerformanceTestAgent** | Converts functional tests to load tests | High |
| **k6 Script Generation** | Auto-generate k6 scripts from TestScenarios | High |
| **Load Test Execution** | Run k6 tests with configurable VUs and duration | High |
| **Threshold Validation** | P95/P99 latency, error rate, throughput checks | Medium |
| **Trend Analysis** | Compare performance across runs | Medium |
| **Bottleneck Detection** | Identify slow endpoints and degradation patterns | Low |

**Key Capabilities:**
```
PerformanceTestAgent
├── k6 Script Generation
│   ├── Convert TestScenario → k6 JavaScript
│   ├── Virtual user ramping strategies
│   ├── Think time simulation
│   └── Data parameterization
├── Execution Profiles
│   ├── Smoke test (1 VU, 1 minute)
│   ├── Load test (50 VUs, 10 minutes)
│   ├── Stress test (ramping to failure)
│   └── Soak test (sustained load)
├── Metrics Collection
│   ├── Response time percentiles (P50, P95, P99)
│   ├── Requests per second (RPS)
│   ├── Error rates by endpoint
│   └── Resource utilization correlation
└── Performance Report Generation
```

### Milestone 8 - Contract Testing Module

**Goal:** Ensure API changes don't break consumers

| Component | Description | Priority |
|-----------|-------------|----------|
| **ContractValidatorAgent** | Validates API against OpenAPI spec | High |
| **Schema Compliance** | Request/response schema validation | High |
| **Breaking Change Detection** | Detect removed fields, type changes | High |
| **Version Compatibility** | Compare spec versions for compatibility | Medium |
| **Consumer-Driven Contracts** | Pact-style contract verification | Medium |
| **Mock Server Generation** | Generate mocks from OpenAPI for consumers | Low |

**Key Capabilities:**
```
ContractValidatorAgent
├── OpenAPI Schema Compliance
│   ├── Request body validation
│   ├── Response schema matching
│   ├── Header requirements
│   └── Query parameter validation
├── Breaking Change Detection
│   ├── Removed endpoints
│   ├── Removed required fields
│   ├── Type changes (string → number)
│   ├── Enum value removal
│   └── Status code changes
├── Semantic Versioning Guidance
│   ├── Major (breaking) changes
│   ├── Minor (additive) changes
│   └── Patch (compatible) changes
└── Consumer Contract Support
    ├── Pact file generation
    ├── Provider verification
    └── Contract broker integration
```

### Milestone 9 - Intelligent Test Data Module

**Goal:** Generate realistic, edge-case-aware test data

| Component | Description | Priority |
|-----------|-------------|----------|
| **TestDataGeneratorAgent** | AI-powered test data generation | High |
| **Faker Integration** | Realistic data (names, emails, addresses) | High |
| **Edge Case Generation** | Boundary values, empty strings, special chars | High |
| **Stateful Data Dependencies** | Maintain data relationships across steps | Medium |
| **Data Cleanup** | Auto-cleanup test data after runs | Medium |
| **Data Masking** | Generate anonymized production-like data | Low |

**Key Capabilities:**
```
TestDataGeneratorAgent
├── Realistic Data Generation
│   ├── Locale-aware names, addresses
│   ├── Valid email/phone formats
│   ├── Credit card test numbers
│   └── UUID/ID generation
├── Edge Case Strategies
│   ├── Boundary values (0, -1, MAX_INT)
│   ├── Empty/null/undefined
│   ├── Unicode and special characters
│   ├── SQL/XSS payloads (for security testing)
│   └── Extremely long strings
├── Stateful Dependencies
│   ├── Create user → use userId in next step
│   ├── Transaction chains
│   └── Session management
└── Data Lifecycle
    ├── Setup fixtures
    ├── Teardown cleanup
    └── Idempotent test runs
```

### Milestone 10 - CI/CD & DevOps Integration

**Goal:** Seamlessly integrate into development workflows

| Component | Description | Priority |
|-----------|-------------|----------|
| **GitHub Actions Integration** | Run tests on PR/push events | High |
| **GitLab CI Integration** | Pipeline templates for GitLab | High |
| **CLI Tool** | `qawave run` command for local/CI use | High |
| **PR Comments** | Post test results as PR comments | Medium |
| **Slack/Teams Notifications** | Alert on failures | Medium |
| **Quality Gates** | Block merge on test failures | Medium |

### Milestone 11 - Self-Healing Tests

**Goal:** Automatically adapt tests when APIs change

| Component | Description | Priority |
|-----------|-------------|----------|
| **ChangeDetectorAgent** | Detect API spec changes | High |
| **ScenarioUpdaterAgent** | Propose scenario fixes for changes | High |
| **Auto-Fix Mode** | Automatically update scenarios | Medium |
| **Change Review UI** | Human review of proposed changes | Medium |
| **Rollback Support** | Revert auto-fixes if needed | Low |

**Key Capabilities:**
```
Self-Healing Pipeline
├── Detect Changes
│   ├── OpenAPI diff analysis
│   ├── Endpoint changes (path, method)
│   ├── Schema changes (fields, types)
│   └── Authentication changes
├── Propose Fixes
│   ├── Update endpoint paths
│   ├── Add/remove fields in payloads
│   ├── Update expected responses
│   └── Adjust assertions
├── Review & Apply
│   ├── Human approval workflow
│   ├── Auto-apply low-risk fixes
│   └── Batch update scenarios
└── Learning Loop
    ├── Track fix success rates
    ├── Improve fix suggestions
    └── Reduce false positives
```

### Future Roadmap Timeline

```
2026 Q1-Q2: M4 (Auth) + M5 (UX Polish)
2026 Q3:    M6 (Security/OWASP)
2026 Q4:    M7 (Performance/k6)
2027 Q1:    M8 (Contract Testing)
2027 Q2:    M9 (Test Data) + M10 (CI/CD)
2027 Q3:    M11 (Self-Healing)
```

### Integration Architecture (Future State)

```
┌─────────────────────────────────────────────────────────────────┐
│                        QAWave Platform                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐ │
│  │  Functional │ │  Security   │ │ Performance │ │ Contract  │ │
│  │   Testing   │ │  (OWASP)    │ │   (k6)      │ │  Testing  │ │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └─────┬─────┘ │
│         │               │               │              │        │
│         └───────────────┴───────┬───────┴──────────────┘        │
│                                 │                                │
│                    ┌────────────▼────────────┐                  │
│                    │   Unified Test Runner   │                  │
│                    │   & Result Aggregator   │                  │
│                    └────────────┬────────────┘                  │
│                                 │                                │
│         ┌───────────────────────┼───────────────────────┐       │
│         ▼                       ▼                       ▼       │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐     │
│  │   CI/CD     │      │  Dashboard  │      │   Alerts    │     │
│  │ Integration │      │  & Reports  │      │ & Webhooks  │     │
│  └─────────────┘      └─────────────┘      └─────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary

**QAWave is at ~70% MVP completion:**

- ✅ **Milestones 1-3 Complete** - Core loop, persistence, AI integration
- ✅ **Production Hardening Done** - Rate limiting, circuit breaker, validation
- ⏳ **Milestone 4 Pending** - Authentication (Keycloak)
- ⏳ **Milestone 5 Partial** - Some UX done, editing/scheduling pending

**Immediate Next Steps:**
1. Merge PR `feature/p0-critical-fixes` to main
2. Set up CI/CD pipeline
3. Begin Keycloak integration (Milestone 4)
4. Add frontend test coverage

---

## 13. Related Documentation

### 13.1 Business Documentation

| Document | Location | Description |
|----------|----------|-------------|
| Business Overview | `business/01-business-overview.md` | Vision, personas, value proposition |
| Domain Model | `business/02-requirements-and-domain-model.md` | Entities, use cases, roles |

### 13.2 Technical Documentation

| Document | Location | Description |
|----------|----------|-------------|
| Backend Architecture | `docs/backend-architecture.md` | Clean architecture patterns |
| QA Package Workflow | `docs/qa-package-workflow.md` | Streaming pipeline details |
| QA Package Async Flow | `docs/qa-package-run-async-flow.md` | Sequence diagrams |
| Code Review | `docs/code-review-qa-package-flow.md` | Architecture issues & fixes |
| Test Scenario Schema | `docs/test-scenario-schema.md` | JSON contract specification |
| Tech Stack | `docs/TECH_STACK.md` | Full technical documentation |

### 13.3 RFCs (Request for Comments)

| Document | Location | Description |
|----------|----------|-------------|
| RFC-001 | `docs/rfc/rfc-001-ai-generation-verification-retry.md` | AI verification & retry loop |
| RFC-002 | `docs/rfc/rfc-002-qa-package-persistence-replay.md` | Persistence & replay |
| RFC-003 | `docs/rfc/rfc-003-coverage-reporting-gaps.md` | Coverage reporting |
| RFC-004 | `docs/RFC-003-ai-interaction-observatory.md` | AI interaction logging |

### 13.4 TODO & Roadmap Files

| Document | Location | Description |
|----------|----------|-------------|
| Roadmap | `todo/01-roadmap-milestones.md` | Milestone overview (M1-M5) |
| Backend TODOs | `todo/02-backend-tasks.md` | Detailed backend tasks |
| Frontend TODOs | `todo/03-frontend-tasks.md` | Detailed frontend tasks |
| AI/Test Design | `todo/04-ai-and-test-design-tasks.md` | AI agent tasks |
| Auth/Infra | `todo/05-auth-and-infra-tasks.md` | Keycloak, DB tasks |
| Exporter | `todo/06-exporter-tasks.md` | Playwright/RestAssured export |
| Production Checklist | `todo/06-production-readiness-checklist.md` | Go-live requirements |

### 13.5 Enhancement Proposals

| Document | Location | Description |
|----------|----------|-------------|
| Top ROI | `new_todo/top_roi.md` | Prioritized high-value initiatives |
| Product Enhancements | `new_todo/product_enhancements.md` | User-facing capabilities |
| Backend Architecture | `new_todo/backend_architecture.md` | Technical debt & improvements |
| Frontend UX | `new_todo/frontend_ux.md` | Navigation & usability |
| AI Agent Model | `new_todo/ai_agent_model.md` | Agent pipeline & contracts |
| DevOps Observability | `new_todo/devops_observability.md` | Logging, metrics, tracing |
| Enterprise Features | `new_todo/enterprise_features.md` | Auth, RBAC, audit |

### 13.6 Development Guidelines

| Document | Location | Description |
|----------|----------|-------------|
| Agent Guide | `AGENTS.MD` | Development guidelines |
| Claude Guide | `CLAUDE.md` | Coding rules |
| README | `README.md` | Project overview |

---

*Last Updated: January 2026*
*Document Version: 4.0*
