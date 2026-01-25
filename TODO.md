# QAWave - Master TODO & Backlog

> This document is the **source of work** for all agents. The Orchestrator uses this to create GitHub issues and assign tasks.

## How Work Flows

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Work Assignment Flow                             │
└─────────────────────────────────────────────────────────────────────────┘

  BUSINESS_REQUIREMENTS.md          TODO.md              GitHub Issues
  (What we need to build)    (Broken into tasks)    (Assigned to agents)
           │                        │                        │
           ▼                        ▼                        ▼
    ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
    │ Features &  │          │ Prioritized │          │ Agent picks │
    │ User Stories│─────────▶│ Task List   │─────────▶│ up & works  │
    │             │          │ by Phase    │          │             │
    └─────────────┘          └─────────────┘          └─────────────┘
           │                        │                        │
           │    Orchestrator        │    Orchestrator        │
           │    reads & analyzes    │    creates issues      │
           └────────────────────────┴────────────────────────┘
```

---

## Current Status: MVP at ~70%

Based on `BUSINESS_REQUIREMENTS.md`, here's what exists and what needs to be built:

### ✅ Already Implemented (from existing codebase)
- Core AI scenario generation
- Test execution pipeline
- Result evaluation
- Basic web dashboard
- PostgreSQL persistence
- Kafka event streaming

### ⏳ Needs to be Built (this project)
- Spring WebFlux migration (from Ktor)
- Keycloak authentication
- Infrastructure (Hetzner/K0s)
- CI/CD pipelines
- E2E test suite
- Security hardening

---

## Phase 0: Project Bootstrap (Week 1)
> **Goal**: Set up infrastructure and repository so agents can start working

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P0-001 | Initialize Git repository with folder structure | DevOps | Critical | None | 1h |
| P0-002 | Set up GitHub repository with branch protection | DevOps | Critical | P0-001 | 1h |
| P0-003 | Create GitHub labels for agents | Orchestrator | Critical | P0-002 | 30m |
| P0-004 | Provision Hetzner VPS with Terraform | DevOps | Critical | P0-002 | 4h |
| P0-005 | Bootstrap K0s Kubernetes cluster | DevOps | Critical | P0-004 | 4h |
| P0-006 | Deploy ArgoCD for GitOps | DevOps | Critical | P0-005 | 2h |
| P0-007 | Set up GitHub Actions CI workflows | DevOps | Critical | P0-002 | 4h |
| P0-008 | Deploy PostgreSQL to Kubernetes | DevOps | High | P0-005 | 2h |
| P0-009 | Deploy Redis to Kubernetes | DevOps | High | P0-005 | 1h |
| P0-010 | Deploy Kafka to Kubernetes (Strimzi) | DevOps | High | P0-005 | 3h |
| P0-011 | Set up Sealed Secrets for K8s | Security | High | P0-005 | 2h |
| P0-012 | Create initial database migrations | Database | High | P0-008 | 4h |

---

## Phase 1: Backend Foundation (Week 2)
> **Goal**: Migrate to Spring WebFlux and establish core services

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P1-001 | Initialize Spring Boot project with WebFlux | Backend | Critical | P0-007 | 2h |
| P1-002 | Configure R2DBC for PostgreSQL | Backend | Critical | P1-001, P0-012 | 3h |
| P1-003 | Configure Spring Data Redis | Backend | High | P1-001 | 2h |
| P1-004 | Configure Spring Kafka | Backend | High | P1-001 | 3h |
| P1-005 | Implement domain models (QaPackage, Scenario, TestRun) | Backend | Critical | P1-001 | 4h |
| P1-006 | Implement R2DBC repositories with coroutines | Backend | Critical | P1-002, P1-005 | 6h |
| P1-007 | Implement QaPackageService (suspend functions) | Backend | Critical | P1-006 | 4h |
| P1-008 | Implement ScenarioService | Backend | Critical | P1-006 | 4h |
| P1-009 | Implement TestExecutionService | Backend | Critical | P1-006 | 6h |
| P1-010 | Implement AI client (OpenAI/Venice) with WebClient | Backend | Critical | P1-001 | 4h |
| P1-011 | Add Resilience4j circuit breaker for AI calls | Backend | High | P1-010 | 3h |
| P1-012 | Implement REST controllers with suspend functions | Backend | Critical | P1-007, P1-008 | 6h |
| P1-013 | Create OpenAPI specification | Backend | High | P1-012 | 3h |
| P1-014 | Write unit tests for services | Backend | High | P1-007, P1-008 | 6h |
| P1-015 | Write integration tests with Testcontainers | Backend | High | P1-012 | 6h |
| P1-016 | Review backend security (input validation, auth) | Security | High | P1-012 | 4h |
| P1-017 | Optimize database queries and indexes | Database | Medium | P1-006 | 4h |

---

## Phase 2: Frontend Foundation (Week 2-3)
> **Goal**: Build React frontend with TanStack

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P2-001 | Initialize Vite + React + TypeScript project | Frontend | Critical | P0-007 | 2h |
| P2-002 | Configure TanStack Router | Frontend | Critical | P2-001 | 2h |
| P2-003 | Configure TanStack Query | Frontend | Critical | P2-001 | 2h |
| P2-004 | Set up Tailwind CSS + HeroUI | Frontend | High | P2-001 | 2h |
| P2-005 | Create base UI components (Button, Card, Modal) | Frontend | High | P2-004 | 6h |
| P2-006 | Implement API client layer | Frontend | Critical | P2-001, P1-013 | 4h |
| P2-007 | Create TanStack Query hooks for packages | Frontend | Critical | P2-003, P2-006 | 4h |
| P2-008 | Implement PackagesListPage | Frontend | Critical | P2-007, P2-005 | 6h |
| P2-009 | Implement PackageDetailPage | Frontend | Critical | P2-007, P2-005 | 6h |
| P2-010 | Implement CreatePackageModal | Frontend | High | P2-005, P2-007 | 4h |
| P2-011 | Implement RunDetailPage with step results | Frontend | High | P2-007 | 6h |
| P2-012 | Implement ScenariosPage | Frontend | Medium | P2-007 | 4h |
| P2-013 | Add loading skeletons and error states | Frontend | High | P2-008 | 3h |
| P2-014 | Implement dark mode support | Frontend | Low | P2-004 | 3h |
| P2-015 | Write component tests with Vitest | Frontend | High | P2-008 | 6h |
| P2-016 | Review frontend security (XSS, storage) | Security | High | P2-008 | 3h |

---

## Phase 3: E2E Testing & Integration (Week 3)
> **Goal**: Ensure BE-FE integration works

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P3-001 | Set up Playwright project structure | QA | Critical | P2-008 | 2h |
| P3-002 | Create Page Object Models | QA | Critical | P3-001 | 4h |
| P3-003 | Write smoke tests (critical paths) | QA | Critical | P3-002 | 4h |
| P3-004 | Write package CRUD E2E tests | QA | High | P3-002 | 6h |
| P3-005 | Write test run E2E tests | QA | High | P3-002 | 6h |
| P3-006 | Write API contract tests | QA | High | P1-013 | 4h |
| P3-007 | Set up test data fixtures | QA | High | P3-001 | 3h |
| P3-008 | Configure Playwright for CI | QA | High | P3-003, P0-007 | 2h |
| P3-009 | Create load tests with k6 | QA | Medium | P1-012 | 4h |
| P3-010 | Document test coverage requirements | QA | Medium | P3-004 | 2h |

---

## Phase 4: Authentication (Week 4) - Milestone 4 from Business Req
> **Goal**: Add Keycloak authentication

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P4-001 | Deploy Keycloak to Kubernetes | DevOps | Critical | P0-005 | 4h |
| P4-002 | Configure Keycloak realm and clients | DevOps | Critical | P4-001 | 3h |
| P4-003 | Add Spring Security WebFlux + OAuth2 | Backend | Critical | P4-002 | 6h |
| P4-004 | Implement JWT validation filter | Backend | Critical | P4-003 | 4h |
| P4-005 | Add role-based access control | Backend | High | P4-004 | 4h |
| P4-006 | Implement frontend auth flow | Frontend | Critical | P4-002 | 6h |
| P4-007 | Add protected routes | Frontend | High | P4-006 | 3h |
| P4-008 | Implement logout and token refresh | Frontend | High | P4-006 | 4h |
| P4-009 | Security audit of auth implementation | Security | Critical | P4-005 | 6h |
| P4-010 | Write auth E2E tests | QA | High | P4-006 | 4h |
| P4-011 | Add user_id to database schema | Database | High | P4-003 | 2h |

---

## Phase 5: Production Hardening (Week 5)
> **Goal**: Make production-ready

| ID | Task | Agent | Priority | Dependencies | Est. |
|----|------|-------|----------|--------------|------|
| P5-001 | Set up Prometheus monitoring | DevOps | High | P0-005 | 4h |
| P5-002 | Set up Grafana dashboards | DevOps | High | P5-001 | 4h |
| P5-003 | Configure alerting rules | DevOps | High | P5-001 | 3h |
| P5-004 | Implement rate limiting | Backend | High | P1-012 | 3h |
| P5-005 | Add request logging and tracing | Backend | High | P1-012 | 4h |
| P5-006 | Optimize container images | DevOps | Medium | P0-007 | 3h |
| P5-007 | Set up horizontal pod autoscaling | DevOps | High | P0-005 | 2h |
| P5-008 | Configure TLS with cert-manager | DevOps | Critical | P0-005 | 3h |
| P5-009 | Security penetration testing | Security | High | P4-009 | 8h |
| P5-010 | Database backup strategy | Database | High | P0-008 | 4h |
| P5-011 | Load testing and optimization | QA | High | P3-009 | 6h |
| P5-012 | Write runbooks for operations | DevOps | Medium | P5-002 | 4h |

---

## Future Phases (from Business Requirements Roadmap)

### Phase 6: Security Testing (Q3 2026) - Milestone 6
- OWASP API Top 10 scanning
- Injection detection
- Security misconfiguration checks

### Phase 7: Performance Testing (Q4 2026) - Milestone 7
- k6 script generation
- Load/stress/soak testing
- Performance reporting

### Phase 8: Contract Testing (Q1 2027) - Milestone 8
- OpenAPI compliance validation
- Breaking change detection
- Pact integration

### Phase 9: Self-Healing Tests (Q3 2027) - Milestone 11
- API change detection
- Auto-fix suggestions
- Test maintenance automation

---

## How Orchestrator Uses This Document

### Daily Workflow

1. **Morning**: Check this TODO list for next priority tasks
2. **Create Issues**: For each task, create GitHub issue with:
   ```markdown
   ## Task: [Task Name]
   
   **From**: TODO.md - [Task ID]
   **Agent**: @[agent-name]
   **Priority**: [Critical/High/Medium/Low]
   **Depends On**: #[issue-numbers]
   
   ## Description
   [Copy from task description or elaborate]
   
   ## Acceptance Criteria
   - [ ] [Specific criterion 1]
   - [ ] [Specific criterion 2]
   - [ ] Tests written and passing
   - [ ] Documentation updated
   
   ## Technical Notes
   [Reference TECH_STACK.md or BUSINESS_REQUIREMENTS.md sections]
   ```

3. **Track Progress**: Update task status in this file
4. **Coordinate**: When dependencies complete, notify blocked agents

### Status Legend

| Status | Meaning |
|--------|---------|
| ⬜ | Not started |
| 🟡 | In progress |
| 🟢 | Completed |
| 🔴 | Blocked |
| ⏸️ | On hold |

---

## Quick Reference: Agent Assignments

| Agent | Primary Phases | Key Deliverables |
|-------|---------------|------------------|
| **DevOps** | P0, P4, P5 | Infrastructure, CI/CD, Kubernetes |
| **Backend** | P1, P4 | Spring WebFlux APIs, services |
| **Frontend** | P2, P4 | React UI, TanStack integration |
| **QA** | P3, P4, P5 | E2E tests, load tests, approval |
| **Security** | P0, P1, P4, P5 | Reviews, secrets, penetration testing |
| **Database** | P0, P1, P4 | Migrations, optimization |
| **Orchestrator** | All | Coordination, issues, PRs |

---

## Related Documents

- `BUSINESS_REQUIREMENTS.md` - Full business context and requirements
- `docs/TECH_STACK.md` - Technical implementation details
- `docs/COORDINATION_PROTOCOL.md` - How agents work together
- `docs/agents/*.md` - Individual agent instructions

---

*Last Updated: January 2026*
*Maintained by: Orchestrator Agent*
