# QAWave Architecture Documentation

## Overview

This directory contains the architecture documentation for QAWave, an AI-powered QA automation platform for backend APIs.

## Documentation Structure

```
docs/architecture/
├── README.md                    # This file
├── decisions/                   # Architecture Decision Records (ADRs)
│   ├── ADR-001-spring-webflux-kotlin-coroutines.md
│   ├── ADR-002-clean-architecture-layers.md
│   ├── ADR-003-ai-agent-pipeline.md
│   ├── ADR-004-streaming-pipeline.md
│   ├── ADR-005-event-driven-kafka.md
│   └── ADR-006-test-scenario-json-contract.md
├── diagrams/                    # Architecture diagrams
│   ├── system-context.md
│   ├── component-diagram.md
│   └── sequence-qa-package-run.md
├── data-model/                  # Entity relationships and schemas
│   └── entity-relationships.md
└── patterns/                    # Design patterns documentation
    └── (to be added)
```

## Quick Reference

### Key Architectural Decisions

| ADR | Decision | Rationale |
|-----|----------|-----------|
| [ADR-001](decisions/ADR-001-spring-webflux-kotlin-coroutines.md) | Spring WebFlux + Kotlin Coroutines | Non-blocking I/O with clean suspend functions |
| [ADR-002](decisions/ADR-002-clean-architecture-layers.md) | Clean Architecture | Separation of concerns, testability, flexibility |
| [ADR-003](decisions/ADR-003-ai-agent-pipeline.md) | AI Agent Pipeline | Modular AI integration with resilience |
| [ADR-004](decisions/ADR-004-streaming-pipeline.md) | Streaming Pipeline | Parallel generation + execution for performance |
| [ADR-005](decisions/ADR-005-event-driven-kafka.md) | Event-Driven with Kafka | Decoupling, audit trail, real-time updates |
| [ADR-006](decisions/ADR-006-test-scenario-json-contract.md) | Test Scenario JSON Contract | Stable interface for AI, execution, and export |

### Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Backend** | Spring WebFlux + Kotlin Coroutines | Reactive web framework |
| **Database** | PostgreSQL + R2DBC | Reactive persistence |
| **Cache** | Redis | Session, rate limiting |
| **Messaging** | Kafka | Event streaming |
| **AI** | OpenAI / Venice | Test scenario generation |
| **Frontend** | React + TanStack | Modern SPA |
| **Infrastructure** | K0s + ArgoCD | Kubernetes GitOps |

### Layer Structure

```
┌─────────────────────────────────────────┐
│           PRESENTATION LAYER            │
│  Controllers, DTOs, Filters             │
├─────────────────────────────────────────┤
│           APPLICATION LAYER             │
│  Services, Use Cases, Ports             │
├─────────────────────────────────────────┤
│             DOMAIN LAYER                │
│  Entities, Value Objects, Interfaces    │
├─────────────────────────────────────────┤
│          INFRASTRUCTURE LAYER           │
│  Repositories, AI Clients, Kafka, HTTP  │
└─────────────────────────────────────────┘
```

## How to Add Documentation

### Adding a New ADR

1. Create a new file: `decisions/ADR-XXX-short-title.md`
2. Use the ADR template:

```markdown
# ADR-XXX: Title

## Status
Proposed | Accepted | Deprecated | Superseded

## Date
YYYY-MM-DD

## Context
Why is this decision needed?

## Decision
What is the change?

## Consequences
What are the trade-offs?
```

3. Link from this README

### Adding Diagrams

1. Create a new file in `diagrams/`
2. Use ASCII/Mermaid diagrams (no binary images)
3. Include context explaining the diagram

## Key Principles

### 1. Non-Blocking Everything
All I/O operations use non-blocking APIs. No `runBlocking` in production code.

### 2. Suspend Functions Over Mono/Flux
Business logic uses `suspend` functions, not Reactor types directly.

### 3. Domain Independence
Domain layer has no framework dependencies. Pure Kotlin.

### 4. Port/Adapter Pattern
External integrations implement domain interfaces (ports).

### 5. Event-Driven Communication
Components communicate via events for decoupling.

## Diagrams

### System Context
[View full diagram](diagrams/system-context.md)

```
Users ─────► QAWave Platform ─────► AI APIs
                   │                  │
                   └──────────────────┴──► Target APIs
```

### QA Package Run Flow
[View full diagram](diagrams/sequence-qa-package-run.md)

```
Request → Fetch Spec → Generate Scenarios (parallel) → Execute Steps → Evaluate → Complete
```

## Related Documentation

- [BUSINESS_REQUIREMENTS.md](../../BUSINESS_REQUIREMENTS.md) - Business context and roadmap
- [TECH_STACK.md](../TECH_STACK.md) - Detailed technology documentation
- [CLAUDE.md](../../CLAUDE.md) - Development guidelines

## Contributing

When making architectural changes:
1. Create an ADR documenting the decision
2. Update relevant diagrams
3. Update this README if adding new sections
4. Get architect review before merging

---

*Last Updated: January 2026*
*Maintainer: Architecture Team*
