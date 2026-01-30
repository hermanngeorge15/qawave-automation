# System Context Diagram

## Overview

This document shows QAWave's high-level system context and interactions with external systems.

## System Context

```
                                    ┌─────────────────────────────────────────┐
                                    │            EXTERNAL USERS                │
                                    └─────────────────────────────────────────┘
                                                      │
                      ┌───────────────────────────────┼───────────────────────────────┐
                      ▼                               ▼                               ▼
               ┌─────────────┐                 ┌─────────────┐                 ┌─────────────┐
               │ QA Engineer │                 │   DevOps    │                 │   Viewer    │
               │   (Human)   │                 │  (CI/CD)    │                 │ (Stakeholder│
               └──────┬──────┘                 └──────┬──────┘                 └──────┬──────┘
                      │                               │                               │
                      │ Upload specs                  │ Trigger runs                  │ View reports
                      │ Review scenarios              │ via API                       │
                      │ Analyze results               │                               │
                      │                               │                               │
                      └───────────────────────────────┼───────────────────────────────┘
                                                      │
                                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                                  │
│                                         QAWAVE PLATFORM                                         │
│                                                                                                  │
│    ┌─────────────────────────────────────────────────────────────────────────────────────┐      │
│    │                                   FRONTEND                                           │      │
│    │                          React + TanStack Router/Query                              │      │
│    └─────────────────────────────────────────────────────────────────────────────────────┘      │
│                                              │                                                   │
│                                              ▼                                                   │
│    ┌─────────────────────────────────────────────────────────────────────────────────────┐      │
│    │                                   BACKEND                                            │      │
│    │                     Spring WebFlux + Kotlin Coroutines                              │      │
│    │                                                                                      │      │
│    │    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │      │
│    │    │ QA Package  │    │  Scenario   │    │    Test     │    │   Result    │        │      │
│    │    │  Service    │    │  Generator  │    │  Executor   │    │  Evaluator  │        │      │
│    │    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘        │      │
│    │                                                                                      │      │
│    └─────────────────────────────────────────────────────────────────────────────────────┘      │
│                                              │                                                   │
│                    ┌─────────────────────────┼─────────────────────────┐                        │
│                    ▼                         ▼                         ▼                        │
│           ┌─────────────┐           ┌─────────────┐           ┌─────────────┐                  │
│           │ PostgreSQL  │           │    Kafka    │           │    Redis    │                  │
│           │ (Primary DB)│           │  (Events)   │           │   (Cache)   │                  │
│           └─────────────┘           └─────────────┘           └─────────────┘                  │
│                                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
                      │                               │
                      │                               │
                      ▼                               ▼
┌─────────────────────────────────────┐   ┌─────────────────────────────────────┐
│         EXTERNAL SERVICES           │   │         SYSTEMS UNDER TEST          │
├─────────────────────────────────────┤   ├─────────────────────────────────────┤
│                                     │   │                                     │
│  ┌─────────────────────────────┐    │   │  ┌─────────────────────────────┐    │
│  │        AI Providers         │    │   │  │    Target REST APIs         │    │
│  │  ┌─────────┐  ┌─────────┐   │    │   │  │                             │    │
│  │  │ OpenAI  │  │ Venice  │   │    │   │  │  • Development Environment  │    │
│  │  │ GPT-4   │  │         │   │    │   │  │  • Staging Environment      │    │
│  │  └─────────┘  └─────────┘   │    │   │  │  • Production (read-only)   │    │
│  └─────────────────────────────┘    │   │  │                             │    │
│                                     │   │  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │   │                                     │
│  │      OpenAPI Spec Sources   │    │   │  Customer APIs:                     │
│  │  • Public URLs              │    │   │  • E-commerce APIs                  │
│  │  • User uploads             │    │   │  • Fintech APIs                     │
│  │  • Git repositories         │    │   │  • SaaS APIs                        │
│  └─────────────────────────────┘    │   │                                     │
│                                     │   │                                     │
└─────────────────────────────────────┘   └─────────────────────────────────────┘
```

## Data Flow Summary

| Flow | Description | Protocol |
|------|-------------|----------|
| User → Frontend | Web browser interactions | HTTPS |
| Frontend → Backend | REST API calls | HTTPS/JSON |
| Backend → PostgreSQL | Data persistence | R2DBC/TCP |
| Backend → Redis | Caching | RESP/TCP |
| Backend → Kafka | Event publishing | Kafka protocol |
| Backend → AI Providers | Scenario generation | HTTPS/JSON |
| Backend → Target APIs | Test execution | HTTPS |

## Security Boundaries

```
┌─────────────────────────────────────────────────────────────────┐
│                    PUBLIC INTERNET                               │
│                                                                  │
│    Users ──────► Ingress (TLS) ──────► QAWave Platform          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PRIVATE NETWORK (Kubernetes)                  │
│                                                                  │
│    Frontend ◄────► Backend ◄────► PostgreSQL / Redis / Kafka   │
│                        │                                         │
│                        └────────► AI APIs (via egress)          │
│                        └────────► Target APIs (via egress)       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## References

- [ADR-001: Spring WebFlux with Kotlin Coroutines](../decisions/ADR-001-spring-webflux-kotlin-coroutines.md)
- [ADR-005: Event-Driven Architecture with Kafka](../decisions/ADR-005-event-driven-kafka.md)
