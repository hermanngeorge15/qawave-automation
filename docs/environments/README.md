# QAWave Environments

## Overview

QAWave uses three environments with distinct purposes and configurations:

| Environment | URL | Branch | Purpose | Monthly Cost |
|-------------|-----|--------|---------|--------------|
| Development | localhost:5173 | feature/* | Local development | €0 |
| Staging | staging.qawave.io | develop | QA testing, integration | ~€17 |
| Production | qawave.io | main | Real users | ~€46 |

**Total Infrastructure Cost:** ~€63/month

## Architecture Overview

```
                            Internet
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
    ┌───────▼───────┐   ┌───────▼───────┐   ┌───────▼───────┐
    │  Development  │   │    Staging    │   │  Production   │
    │   (Local)     │   │  (Hetzner)    │   │   (Hetzner)   │
    ├───────────────┤   ├───────────────┤   ├───────────────┤
    │ docker-compose│   │ 1 CP (CX11)   │   │ 1 CP (CX21)   │
    │ localhost     │   │ 2 Workers     │   │ 3 Workers     │
    │ Hot reload    │   │ K0s + ArgoCD  │   │ K0s + ArgoCD  │
    └───────────────┘   └───────────────┘   └───────────────┘
```

## Environment Details

| Component | Development | Staging | Production |
|-----------|-------------|---------|------------|
| **Kubernetes** | Docker Desktop / minikube | K0s (Hetzner VPS) | K0s (Hetzner VPS) |
| **Database** | PostgreSQL (container) | PostgreSQL (K8s) | PostgreSQL (K8s + backups) |
| **Cache** | Redis (container) | Redis (K8s) | Redis (K8s cluster) |
| **Messaging** | Kafka (container) | Kafka (K8s) | Kafka (K8s cluster) |
| **GitOps** | N/A | ArgoCD | ArgoCD |
| **TLS** | Self-signed | Let's Encrypt | Let's Encrypt |
| **Monitoring** | Local logs | Prometheus + Grafana | Prometheus + Grafana + Alerts |

## Quick Links

- [Local Development Setup](./LOCAL_DEVELOPMENT.md) - Get started locally
- [Staging Environment](./STAGING.md) - QA and integration testing
- [Production Environment](./PRODUCTION.md) - Live environment

## Environment Selection Guide

### Use Development When:
- Writing new features
- Debugging issues
- Running unit tests
- Fast iteration needed

### Use Staging When:
- Integration testing
- QA verification
- E2E test execution
- Demo to stakeholders
- Pre-production validation

### Use Production When:
- Releasing to users
- Only after staging validation
- Requires proper PR review

## Deployment Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Deployment Pipeline                              │
└─────────────────────────────────────────────────────────────────────────┘

  Developer        feature/*           develop            main
      │                │                  │                 │
      │   Push code    │                  │                 │
      ├───────────────▶│                  │                 │
      │                │                  │                 │
      │         ┌──────┴──────┐           │                 │
      │         │   CI Tests  │           │                 │
      │         │  (Unit/Lint)│           │                 │
      │         └──────┬──────┘           │                 │
      │                │                  │                 │
      │         PR Approved & Merged      │                 │
      │                ├─────────────────▶│                 │
      │                │                  │                 │
      │                │           ┌──────┴──────┐          │
      │                │           │ Build Image │          │
      │                │           │ Deploy Staging          │
      │                │           └──────┬──────┘          │
      │                │                  │                 │
      │                │           ┌──────┴──────┐          │
      │                │           │  E2E Tests  │          │
      │                │           │  QA Review  │          │
      │                │           └──────┬──────┘          │
      │                │                  │                 │
      │                │           Approved & Merged         │
      │                │                  ├────────────────▶│
      │                │                  │                 │
      │                │                  │          ┌──────┴──────┐
      │                │                  │          │ Deploy Prod │
      │                │                  │          │  (ArgoCD)   │
      │                │                  │          └─────────────┘
```

## Access Requirements

| Environment | Access Level | Who |
|-------------|--------------|-----|
| Development | Self-service | All developers |
| Staging | SSH + Kubeconfig | DevOps, QA, Backend, Frontend |
| Production | Limited (view logs) | DevOps, On-call |

## Related Documentation

- [Getting Started Guide](../setup/GETTING_STARTED.md)
- [Hetzner Setup](../setup/HETZNER_SETUP.md)
- [Kubernetes Access](../setup/KUBERNETES_ACCESS.md)
- [Deploy to Staging Runbook](../runbooks/DEPLOY_TO_STAGING.md)
- [Deploy to Production Runbook](../runbooks/DEPLOY_TO_PRODUCTION.md)
