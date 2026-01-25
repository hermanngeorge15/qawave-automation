# QAWave - Multi-Agent Development System

## Project Overview

QAWave is an AI-powered QA automation platform that acts as a virtual QA engineering team for backend APIs. This repository is developed by a team of specialized AI agents, each with distinct responsibilities.

## Agent Team

| Agent | Role | Directory Ownership | GitHub Label |
|-------|------|---------------------|--------------|
| **Orchestrator** | Project Manager, Task Assignment, PR Coordination | `/docs/`, project board | `agent:orchestrator` |
| **Backend** | Kotlin/Spring WebFlux Development | `/backend/` | `agent:backend` |
| **Frontend** | React/TypeScript Development | `/frontend/` | `agent:frontend` |
| **DevOps** | CI/CD, Terraform, Kubernetes, ArgoCD | `/infrastructure/`, `/.github/` | `agent:devops` |
| **QA** | E2E Tests, Integration Tests, Approval | `/e2e-tests/` | `agent:qa` |
| **Security** | Security Reviews, Vulnerability Scanning | `/security/` | `agent:security` |
| **Database** | Migrations, Schema Design, Query Optimization | `/backend/src/main/resources/db/` | `agent:database` |

## Repository Structure

```
qawave/
├── .github/
│   ├── workflows/           # CI/CD pipelines (DevOps)
│   ├── CODEOWNERS           # Enforce ownership
│   └── ISSUE_TEMPLATE/      # Issue templates per agent
├── backend/                  # Kotlin/Spring WebFlux (Backend)
│   ├── src/main/kotlin/
│   ├── src/main/resources/
│   │   └── db/migration/    # Flyway migrations (Database)
│   └── src/test/
├── frontend/                 # React/TypeScript (Frontend)
│   ├── src/
│   └── tests/
├── infrastructure/           # DevOps
│   ├── terraform/           # Hetzner provisioning
│   ├── kubernetes/          # K8s manifests
│   └── argocd/              # GitOps config
├── e2e-tests/               # Playwright/API tests (QA)
├── api-specs/               # OpenAPI contracts (Backend owns, Frontend reads)
├── security/                # Security policies (Security)
├── docs/
│   ├── agents/              # Agent-specific instructions
│   ├── architecture/        # ADRs
│   └── TECH_STACK.md
└── CLAUDE.md                # This file
```

## Agent Coordination Protocol

### Communication via GitHub

1. **Issues**: Task assignment and tracking
2. **Pull Requests**: Code review and approval
3. **PR Comments**: Agent-to-agent communication
4. **Labels**: Agent identification and status

### Task Flow

```
1. Orchestrator creates Issue with task description
2. Orchestrator assigns label (e.g., agent:backend)
3. Assigned agent picks up issue, creates feature branch
4. Agent implements, creates PR referencing issue
5. CI runs automatically
6. If tests pass → QA agent reviews and approves
7. If Security-relevant → Security agent reviews
8. Orchestrator merges PR when approved
9. ArgoCD deploys to staging
10. QA runs E2E tests against staging
11. If E2E passes → Orchestrator promotes to production
```

### Branch Strategy

- `main` - Production-ready code
- `staging` - Pre-production testing
- `feature/{agent}-{description}` - Feature branches

### PR Requirements

1. All CI checks must pass
2. QA agent approval required
3. Security agent approval for security-related changes
4. No direct commits to `main`

## Technology Stack

### Backend
- **Language**: Kotlin 1.9
- **Framework**: Spring Boot 3.2 + WebFlux
- **Async Model**: Kotlin Coroutines (suspend functions, no Mono/Flux)
- **Database**: PostgreSQL 16 + R2DBC
- **Cache**: Redis 7
- **Messaging**: Apache Kafka
- **Resilience**: Resilience4j

### Frontend
- **Language**: TypeScript 5
- **Framework**: React 18
- **Routing**: TanStack Router
- **Data Fetching**: TanStack Query
- **Styling**: Tailwind CSS + HeroUI

### Infrastructure
- **Cloud**: Hetzner VPS
- **Kubernetes**: K0s
- **GitOps**: ArgoCD
- **IaC**: Terraform
- **CI/CD**: GitHub Actions

## Key Guidelines

### Code Style

**Kotlin (Backend)**:
- Use suspend functions, never expose Mono/Flux in service layer
- Follow Clean Architecture (domain → application → infrastructure → presentation)
- Use value classes for domain IDs
- Prefer immutable data classes

**TypeScript (Frontend)**:
- Strict TypeScript mode
- Functional components with hooks
- Co-locate tests with components

### Commit Messages

```
type(scope): description

[optional body]

Refs: #issue-number
Agent: agent-name
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`

### PR Title Format

```
[Agent] type: description (#issue)
```

Example: `[Backend] feat: implement scenario generation service (#42)`

## Getting Started

See individual agent instructions in `/docs/agents/`:

- [Orchestrator Instructions](docs/agents/ORCHESTRATOR.md)
- [Backend Instructions](docs/agents/BACKEND.md)
- [Frontend Instructions](docs/agents/FRONTEND.md)
- [DevOps Instructions](docs/agents/DEVOPS.md)
- [QA Instructions](docs/agents/QA.md)
- [Security Instructions](docs/agents/SECURITY.md)
- [Database Instructions](docs/agents/DATABASE.md)

## Environment Setup

```bash
# Clone
git clone https://github.com/your-org/qawave.git
cd qawave

# Start local services
docker compose up -d

# Backend
cd backend && ./gradlew bootRun

# Frontend  
cd frontend && npm install && npm run dev
```

## Contact

For issues with the multi-agent system, create an issue with label `agent:orchestrator`.
