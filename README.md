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
- 📄 Business requirements (what the API should do)
- 📋 OpenAPI specification (how the API is structured)
- 🌐 Base URL (where the API is running)

### What QAWave Delivers
- ✅ Auto-generated test scenarios
- ✅ Real API execution
- ✅ Intelligent result evaluation
- ✅ Full audit trail
- ✅ Coverage tracking

## Quick Start

### Prerequisites

- JDK 21+
- Node.js 20+
- Docker & Docker Compose
- (Optional) Kubernetes access for deployment

### Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/qawave.git
cd qawave

# Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# Start backend (in one terminal)
cd backend
./gradlew bootRun

# Start frontend (in another terminal)
cd frontend
npm install
npm run dev
```

Access the application at http://localhost:5173

### Environment Variables

Create a `.env` file in the root:

```env
# AI Configuration
AI_PROVIDER=openai
AI_API_KEY=sk-...
AI_MODEL=gpt-4o-mini

# Database (defaults work with docker-compose)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=qawave
DB_USER=qawave
DB_PASSWORD=qawave_dev_password
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        QAWave Architecture                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────┐         ┌─────────────────────┐       │
│  │   React Frontend    │────────▶│  Spring WebFlux     │       │
│  │   (TypeScript)      │         │  Backend (Kotlin)   │       │
│  └─────────────────────┘         └──────────┬──────────┘       │
│                                              │                   │
│         ┌────────────────────────────────────┼──────────────┐   │
│         │                                    │              │   │
│         ▼                                    ▼              ▼   │
│  ┌─────────────┐                  ┌─────────────┐   ┌─────────┐│
│  │ PostgreSQL  │                  │    Redis    │   │  Kafka  ││
│  │   (R2DBC)   │                  │   (Cache)   │   │ (Events)││
│  └─────────────┘                  └─────────────┘   └─────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

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

## Project Structure

```
qawave/
├── backend/                  # Kotlin Spring WebFlux backend
├── frontend/                 # React TypeScript frontend
├── infrastructure/           # Terraform & Kubernetes configs
│   ├── terraform/
│   ├── kubernetes/
│   └── argocd/
├── e2e-tests/               # Playwright E2E tests
├── api-specs/               # OpenAPI specifications
├── security/                # Security policies
└── docs/                    # Documentation
    └── agents/              # AI agent instructions
```

## Development

### Running Tests

```bash
# Backend tests
cd backend
./gradlew test

# Frontend tests
cd frontend
npm test

# E2E tests
cd e2e-tests
npm test
```

### Code Style

```bash
# Backend
./gradlew ktlintCheck
./gradlew ktlintFormat

# Frontend
npm run lint
npm run format
```

## Multi-Agent Development

This project uses a multi-agent development system with specialized AI agents:

| Agent | Responsibility |
|-------|---------------|
| Orchestrator | Project management, task assignment |
| Backend | Kotlin/Spring WebFlux development |
| Frontend | React/TypeScript development |
| DevOps | Infrastructure, CI/CD |
| QA | Testing, quality assurance |
| Security | Security reviews |
| Database | Schema design, migrations |

See [CLAUDE.md](./CLAUDE.md) for the coordination protocol.

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

## Contributing

1. Create an issue describing your change
2. Create a feature branch: `feature/{agent}-{description}`
3. Submit a PR referencing the issue
4. Wait for CI and agent reviews
5. Merge when approved

## License

[MIT License](./LICENSE)

## Links

- [Technical Documentation](./docs/TECH_STACK.md)
- [Business Requirements](./docs/BUSINESS_REQUIREMENTS.md)
- [API Documentation](https://api.qawave.io/swagger-ui.html)
