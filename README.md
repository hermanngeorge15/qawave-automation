# QAWave

> AI-powered QA automation platform that acts as your virtual QA engineering team for backend APIs.

[![Backend CI](https://github.com/your-org/qawave/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/your-org/qawave/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/your-org/qawave/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/your-org/qawave/actions/workflows/frontend-ci.yml)
[![E2E Tests](https://github.com/your-org/qawave/actions/workflows/e2e-tests.yml/badge.svg)](https://github.com/your-org/qawave/actions/workflows/e2e-tests.yml)

## Overview

QAWave automates the entire API testing workflow using AI agents:

1. **Requirements Analyzer** - Extracts testable flows from requirements
2. **Scenario Generator** - Creates executable test scenarios from OpenAPI specs
3. **Test Executor** - Runs tests against your live APIs
4. **Result Evaluator** - Determines pass/fail with intelligent analysis

### What You Provide
- Business requirements (what the API should do)
- OpenAPI specification (how the API is structured)
- Base URL (where the API is running)

### What QAWave Delivers
- Auto-generated test scenarios
- Real API execution
- Intelligent result evaluation
- Full audit trail
- Coverage tracking

---

## Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/qawave.git
cd qawave

# One-command setup (installs dependencies, starts infrastructure)
make setup

# Or manually:
./scripts/setup-local.sh

# Start development
make up
```

After setup, open http://localhost:5173 in your browser.

---

## Prerequisites

### Required

| Tool | Version | Check Command | Install |
|------|---------|---------------|---------|
| Docker | 20.0+ | `docker --version` | [Docker Desktop](https://docs.docker.com/get-docker/) |
| Node.js | 20+ | `node --version` | [nodejs.org](https://nodejs.org/) |
| Java | 21+ | `java --version` | [Adoptium](https://adoptium.net/) |
| Make | Any | `make --version` | Pre-installed on macOS/Linux |

### Optional

| Tool | Purpose |
|------|---------|
| `psql` | Direct database access |
| `redis-cli` | Direct Redis access |
| `kubectl` | Kubernetes deployment |
| `gh` | GitHub CLI for PRs |

### Verify Prerequisites

```bash
make check-deps
# Or run the full check:
./scripts/setup-local.sh --help
```

---

## Development Commands

### Quick Reference

```bash
make help              # Show all available commands
make setup             # First-time project setup
make up                # Start infrastructure (PostgreSQL, Redis, Kafka)
make down              # Stop all services
make health            # Check service health
```

### Full Command Reference

#### Infrastructure

| Command | Description |
|---------|-------------|
| `make up` | Start core infrastructure (PostgreSQL, Redis, Kafka) |
| `make down` | Stop all services |
| `make restart` | Restart all services |
| `make logs` | View service logs |
| `make clean` | Remove all containers and volumes |
| `make health` | Check health of all services |

#### Backend

| Command | Description |
|---------|-------------|
| `make backend-run` | Start Spring Boot backend (hot reload) |
| `make backend-test` | Run backend tests |
| `make backend-build` | Build backend JAR |
| `make backend-lint` | Check code style |

#### Frontend

| Command | Description |
|---------|-------------|
| `make frontend-install` | Install npm dependencies |
| `make frontend-run` | Start Vite dev server (hot reload) |
| `make frontend-test` | Run frontend tests |
| `make frontend-build` | Build for production |

#### Database

| Command | Description |
|---------|-------------|
| `make db-shell` | Open PostgreSQL shell |
| `make db-migrate` | Run Flyway migrations |
| `make db-seed` | Load sample data |
| `make db-reset` | Reset database (destructive!) |

#### Testing

| Command | Description |
|---------|-------------|
| `make test` | Run all tests |
| `make test-unit` | Run unit tests only |
| `make test-e2e` | Run E2E tests |
| `make test-coverage` | Generate coverage report |

---

## Docker Compose Profiles

The `docker-compose.yml` supports profiles for selective service startup:

```bash
# Core only (default) - PostgreSQL, Redis, Kafka
docker compose up -d

# With applications (backend + frontend in containers)
docker compose --profile apps up -d

# With authentication (Keycloak)
docker compose --profile auth up -d

# With observability (Prometheus, Grafana, Jaeger)
docker compose --profile observability up -d

# With debug tools (Kafka UI, Redis Commander, pgAdmin)
docker compose --profile debug up -d

# Everything
docker compose --profile full up -d
```

### Service Ports

| Service | Port | Profile | URL |
|---------|------|---------|-----|
| PostgreSQL | 5432 | (core) | - |
| Redis | 6379 | (core) | - |
| Kafka | 9094 | (core) | - |
| Backend | 8080 | apps | http://localhost:8080 |
| Frontend | 5173 | apps | http://localhost:5173 |
| Keycloak | 8180 | auth | http://localhost:8180 |
| Prometheus | 9090 | observability | http://localhost:9090 |
| Grafana | 3000 | observability | http://localhost:3000 |
| Jaeger | 16686 | observability | http://localhost:16686 |
| Kafka UI | 8090 | debug | http://localhost:8090 |
| Redis Commander | 8091 | debug | http://localhost:8091 |
| pgAdmin | 8092 | debug | http://localhost:8092 |

---

## Environment Configuration

### Setup

Copy the example environment file:

```bash
cp .env.example .env
```

### Required Variables

```env
# AI Provider (required for scenario generation)
AI_PROVIDER=openai          # openai, anthropic, azure, venice, stub
AI_API_KEY=sk-...           # Your API key
AI_MODEL=gpt-4o-mini        # Model to use
```

### Optional Variables

```env
# Database (defaults work with docker-compose)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=qawave
DB_USER=qawave
DB_PASSWORD=qawave_dev_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9094

# Keycloak (if using auth profile)
KEYCLOAK_ENABLED=true
KEYCLOAK_URL=http://localhost:8180
```

See [.env.example](.env.example) for all available options.

---

## Troubleshooting

### Port Conflicts

**Symptom**: `Bind for 0.0.0.0:5432 failed: port is already allocated`

**Solution**:
```bash
# Find what's using the port
lsof -i :5432

# Stop conflicting service or change port in docker-compose.yml
```

### Docker Memory Issues

**Symptom**: Containers crash or behave erratically

**Solution**:
1. Open Docker Desktop preferences
2. Go to Resources > Advanced
3. Increase Memory to at least 4GB (8GB recommended)
4. Restart Docker

### Database Connection Issues

**Symptom**: `Connection refused` or `FATAL: role "qawave" does not exist`

**Solution**:
```bash
# Reset the database
make clean
make up

# Verify PostgreSQL is healthy
docker compose ps postgres
make health
```

### Kafka Not Starting

**Symptom**: Kafka container keeps restarting

**Solution**:
```bash
# Kafka needs more startup time
docker compose logs kafka

# If still failing, clean and restart
make clean
make up
```

### Backend Won't Start

**Symptom**: Backend fails with connection errors

**Solution**:
```bash
# Ensure infrastructure is running first
make up

# Wait for health checks
make health

# Then start backend
make backend-run
```

### Health Check Failed

Run the health check to diagnose:
```bash
make health

# For detailed output:
./scripts/health-check-local.sh

# For JSON output (CI):
./scripts/health-check-local.sh --json
```

---

## Architecture

```
                        QAWave Architecture
+-------------------------------------------------------------------+
|                                                                   |
|  +---------------------+         +---------------------+          |
|  |   React Frontend    |-------->|  Spring WebFlux     |          |
|  |   (TypeScript)      |         |  Backend (Kotlin)   |          |
|  +---------------------+         +----------+----------+          |
|                                             |                     |
|         +-----------------------------------+--------+            |
|         |                   |               |        |            |
|         v                   v               v        v            |
|  +-------------+    +-------------+  +---------+ +--------+       |
|  | PostgreSQL  |    |    Redis    |  |  Kafka  | |   AI   |       |
|  |   (R2DBC)   |    |   (Cache)   |  | (Events)| | (LLM)  |       |
|  +-------------+    +-------------+  +---------+ +--------+       |
|                                                                   |
+-------------------------------------------------------------------+
```

---

## Tech Stack

### Backend
- **Kotlin 1.9** with Coroutines
- **Spring Boot 3.2** + WebFlux
- **R2DBC** for reactive database access
- **PostgreSQL 16** for persistence
- **Redis 7** for caching
- **Apache Kafka** for event streaming
- **Resilience4j** for fault tolerance

### Frontend
- **React 18** with TypeScript
- **TanStack Router** for routing
- **TanStack Query** for data fetching
- **Tailwind CSS** + HeroUI for styling

### Infrastructure
- **Terraform** for Hetzner provisioning
- **K0s** for Kubernetes
- **ArgoCD** for GitOps deployments
- **GitHub Actions** for CI/CD

---

## Project Structure

```
qawave/
├── backend/                  # Kotlin Spring WebFlux backend
├── frontend/                 # React TypeScript frontend
├── infrastructure/           # Terraform & Kubernetes configs
│   ├── terraform/           # Hetzner VPS provisioning
│   ├── kubernetes/          # K8s manifests
│   └── argocd/             # GitOps configurations
├── e2e-tests/               # Playwright E2E tests
├── api-specs/               # OpenAPI specifications
├── security/                # Security policies
├── scripts/                 # Development scripts
│   ├── setup-local.sh      # First-time setup
│   ├── seed-data.sh        # Load sample data
│   └── health-check-local.sh  # Health checks
├── docs/                    # Documentation
│   ├── agents/             # AI agent instructions
│   ├── architecture/       # ADRs
│   └── runbooks/          # Operational guides
├── Makefile                # Development commands
├── docker-compose.yml      # Local services
└── CLAUDE.md              # Multi-agent coordination
```

---

## Running Tests

```bash
# All tests
make test

# Backend only
make backend-test

# Frontend only
make frontend-test

# E2E tests (requires running services)
make up
make test-e2e

# With coverage
make test-coverage
```

---

## Multi-Agent Development

This project uses a multi-agent development system with specialized AI agents:

| Agent | Responsibility | Directory |
|-------|---------------|-----------|
| Orchestrator | Project management, task assignment | `/docs/` |
| Backend | Kotlin/Spring WebFlux development | `/backend/` |
| Frontend | React/TypeScript development | `/frontend/` |
| DevOps | Infrastructure, CI/CD | `/infrastructure/`, `/.github/` |
| QA | Testing, quality assurance | `/e2e-tests/` |
| Security | Security reviews | `/security/` |
| Database | Schema design, migrations | `/backend/src/main/resources/db/` |

See [CLAUDE.md](./CLAUDE.md) for the coordination protocol.

---

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for detailed guidelines.

### Quick Start

1. Create an issue describing your change
2. Create a feature branch: `feature/{agent}-{description}`
3. Submit a PR referencing the issue
4. Wait for CI and agent reviews
5. Merge when approved

### Commit Message Format

```
type(scope): description

Refs: #issue-number
Agent: agent-name
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`

---

## Deployment

### Staging

Automatic deployment on merge to `main` via ArgoCD.

### Production

Manual promotion after QA approval:

```bash
# Create release tag
git tag v1.0.0
git push origin v1.0.0

# ArgoCD will auto-sync production
```

---

## License

[MIT License](./LICENSE)

---

## Links

- [Technical Documentation](./docs/TECH_STACK.md)
- [Business Requirements](./docs/BUSINESS_REQUIREMENTS.md)
- [API Documentation](https://api.qawave.io/swagger-ui.html)
- [Agent Instructions](./docs/agents/)
- [Operational Runbooks](./docs/runbooks/)
