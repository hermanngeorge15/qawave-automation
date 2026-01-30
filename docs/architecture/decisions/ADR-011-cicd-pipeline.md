# ADR-011: CI/CD Pipeline Architecture

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs automated CI/CD for:
- Fast feedback on code changes
- Consistent deployments
- Quality gates before production
- GitOps-based infrastructure changes

## Decision

We implement **GitHub Actions for CI** and **ArgoCD for CD** (GitOps).

### Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CI/CD PIPELINE                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    CONTINUOUS INTEGRATION (GitHub Actions)           │    │
│  │                                                                      │    │
│  │   PR Created/Updated                                                │    │
│  │         │                                                            │    │
│  │         ▼                                                            │    │
│  │   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │    │
│  │   │    Lint     │──►│    Test     │──►│   Build     │              │    │
│  │   │  (Detekt)   │   │  (JUnit)    │   │  (Gradle)   │              │    │
│  │   └─────────────┘   └─────────────┘   └─────────────┘              │    │
│  │                                              │                       │    │
│  │                                              ▼                       │    │
│  │                                        ┌─────────────┐              │    │
│  │                                        │ Docker Build│              │    │
│  │                                        │ & Push GHCR │              │    │
│  │                                        └─────────────┘              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                              │                               │
│                                              ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    CONTINUOUS DEPLOYMENT (ArgoCD)                    │    │
│  │                                                                      │    │
│  │   Image Tag Updated in Git                                          │    │
│  │         │                                                            │    │
│  │         ▼                                                            │    │
│  │   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │    │
│  │   │   ArgoCD    │──►│  Staging    │──►│   E2E       │              │    │
│  │   │   Sync      │   │  Deploy     │   │   Tests     │              │    │
│  │   └─────────────┘   └─────────────┘   └─────────────┘              │    │
│  │                                              │                       │    │
│  │                           ┌──────────────────┤                       │    │
│  │                           ▼                  ▼                       │    │
│  │                     ┌─────────┐        ┌─────────┐                  │    │
│  │                     │ Manual  │        │Production│                  │    │
│  │                     │ Approval│───────►│ Deploy   │                  │    │
│  │                     └─────────┘        └─────────┘                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### CI Pipeline (GitHub Actions)

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Detekt
        run: ./gradlew detekt

  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: qawave_test
          POSTGRES_PASSWORD: test
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v4

  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t ghcr.io/${{ github.repository }}/backend:${{ github.sha }} .
      - name: Push to GHCR
        run: docker push ghcr.io/${{ github.repository }}/backend:${{ github.sha }}
```

### CD Pipeline (ArgoCD)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: qawave-backend
  namespace: argocd
spec:
  project: qawave
  source:
    repoURL: https://github.com/org/qawave
    targetRevision: HEAD
    path: infrastructure/kubernetes/overlays/staging
  destination:
    server: https://kubernetes.default.svc
    namespace: qawave-staging
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

### Quality Gates

| Gate | Tool | Threshold |
|------|------|-----------|
| Code Coverage | JaCoCo | ≥ 80% |
| Code Quality | Detekt | 0 errors |
| Security Scan | Trivy | 0 critical |
| E2E Tests | Playwright | 100% pass |
| Load Tests | k6 | P95 < 500ms |

### Deployment Stages

| Stage | Trigger | Environment | Tests |
|-------|---------|-------------|-------|
| PR | Pull Request | Preview | Unit, Integration |
| Staging | Merge to main | staging.qawave.local | E2E, Load |
| Production | Manual approval | qawave.io | Smoke |

### Rollback Strategy

```bash
# Automatic rollback on health check failure
argocd app rollback qawave-backend

# Manual rollback to specific revision
argocd app history qawave-backend
argocd app rollback qawave-backend <revision>
```

## Consequences

### Positive
- Fast, consistent deployments
- GitOps audit trail
- Quality gates prevent bad deploys
- Easy rollbacks

### Negative
- Complex pipeline to maintain
- GHCR/ArgoCD infrastructure needed
- Learning curve for team

## References

- [GitHub Actions](https://docs.github.com/en/actions)
- [ArgoCD](https://argo-cd.readthedocs.io/)
- [ADR-008: Security Architecture](ADR-008-security-architecture.md)
