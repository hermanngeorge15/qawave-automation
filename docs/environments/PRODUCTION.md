# Production Environment

## Overview

Production is the live environment serving real users. Changes require thorough testing in staging and proper approval workflow.

| Property | Value |
|----------|-------|
| **Frontend URL** | https://qawave.io |
| **API URL** | https://api.qawave.io |
| **ArgoCD URL** | https://argocd.qawave.io |
| **Branch** | `main` |
| **Monthly Cost** | ~€46 |

> **Warning:** All changes to production require approval and should follow the [Deploy to Production Runbook](../runbooks/DEPLOY_TO_PRODUCTION.md).

## Infrastructure

### Hetzner VPS Instances

| Role | Type | Specs | Purpose |
|------|------|-------|---------|
| Control Plane | CX21 | 2 vCPU, 4GB RAM | K8s control plane, etcd |
| Worker 1 | CX31 | 4 vCPU, 8GB RAM | Application workloads |
| Worker 2 | CX31 | 4 vCPU, 8GB RAM | Application workloads |
| Worker 3 | CX31 | 4 vCPU, 8GB RAM | Application workloads |

### Architecture Diagram

```
                         Internet
                             │
                    ┌────────┴────────┐
                    │   Cloudflare    │
                    │   (DNS + CDN)   │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │  Load Balancer  │
                    │     (LB21)      │
                    └────────┬────────┘
                             │
    ┌────────────────────────┼────────────────────────┐
    │                        │                        │
┌───┴───┐  ┌─────────┐  ┌────┴────┐  ┌─────────┐  ┌───┴───┐
│  CP   │  │Worker 1 │  │Worker 2 │  │Worker 3 │  │Backup │
└───────┘  └─────────┘  └─────────┘  └─────────┘  └───────┘
    │            │            │            │           │
    └────────────┴────────────┴────────────┴───────────┘
                       Private Network
                        10.0.0.0/16
```

### Network Configuration

| Component | CIDR | Purpose |
|-----------|------|---------|
| VPC Network | 10.0.0.0/16 | Private communication |
| Node Subnet | 10.0.1.0/24 | K8s node IPs |
| Pod Network | 10.244.0.0/16 | K8s pod IPs |
| Service Network | 10.96.0.0/12 | K8s service IPs |

## High Availability

### Application Layer

- **Frontend:** 3 replicas across workers
- **Backend:** 3 replicas with anti-affinity
- **Pod Disruption Budgets:** Minimum 2 pods available during maintenance

### Data Layer

| Service | HA Configuration |
|---------|------------------|
| PostgreSQL | Primary + Streaming replica |
| Redis | 3-node cluster with sentinel |
| Kafka | 3 brokers, replication factor 2 |

### Backup Strategy

| Data | Frequency | Retention | Storage |
|------|-----------|-----------|---------|
| PostgreSQL | Daily + WAL | 30 days | Hetzner Storage Box |
| Redis (AOF) | Continuous | 7 days | Local + S3 |
| Kubernetes (etcd) | Hourly | 7 days | Hetzner Storage Box |

## Access Control

### Access Levels

| Role | Access | Purpose |
|------|--------|---------|
| DevOps | Full SSH + kubectl | Infrastructure management |
| On-call | Read-only kubectl + logs | Incident response |
| Developers | None (use staging) | N/A |

### SSH Access

```bash
# Requires VPN connection and authorized SSH key
ssh -i ~/.ssh/qawave-prod root@<control-plane-ip>
```

### Kubernetes Access

```bash
# Production kubeconfig (restricted distribution)
export KUBECONFIG=~/.kube/config-qawave-prod
kubectl get nodes
```

> Production kubeconfig is distributed only to authorized personnel and should never be committed to version control.

## Deployment

### GitOps Flow

1. PR merged to `main` branch
2. GitHub Actions builds and pushes images with semantic version tag
3. ArgoCD detects new image and creates sync request
4. Manual approval required for production sync
5. ArgoCD applies changes with gradual rollout

### Deployment Strategy

| Service | Strategy | Parameters |
|---------|----------|------------|
| Frontend | RollingUpdate | maxSurge: 1, maxUnavailable: 0 |
| Backend | RollingUpdate | maxSurge: 1, maxUnavailable: 0 |

### Release Checklist

Before deploying to production:

- [ ] All staging E2E tests passed
- [ ] QA approval obtained
- [ ] Security review complete (if applicable)
- [ ] Database migrations tested
- [ ] Rollback plan documented
- [ ] On-call team notified

### Manual Deployment (Emergency Only)

```bash
# Only for emergency fixes - requires approval
gh workflow run "Deploy to Production" \
  --ref main \
  -f version=v1.2.3

# Force sync (emergency only)
argocd app sync qawave-production --prune
```

## Monitoring & Alerting

### Monitoring Stack

| Tool | Purpose | URL |
|------|---------|-----|
| Prometheus | Metrics collection | Internal |
| Grafana | Dashboards | https://grafana.qawave.io |
| AlertManager | Alert routing | Internal |
| PagerDuty | Incident management | External |

### Key Dashboards

- **Application Health:** Request rate, latency, errors
- **Infrastructure:** CPU, memory, disk, network
- **Database:** Connections, query time, replication lag
- **Business:** User signups, API usage, feature adoption

### Alert Rules

| Alert | Severity | Condition | Response |
|-------|----------|-----------|----------|
| High Error Rate | Critical | >5% errors for 5m | Immediate investigation |
| High Latency | Warning | p99 >2s for 5m | Check database/cache |
| Pod Restart | Warning | >3 restarts in 15m | Check logs |
| Disk Usage | Warning | >80% used | Clean up / expand |
| Certificate Expiry | Warning | <7 days | Renew certificate |

### On-Call Procedures

1. Receive alert via PagerDuty
2. Acknowledge within 15 minutes
3. Assess severity and impact
4. Follow runbook for incident type
5. Escalate if needed
6. Post-incident review

## Security

### Network Security

- Firewall allows only necessary ports (80, 443, 6443)
- All internal communication over private network
- External access via load balancer only

### Secret Management

- All secrets stored in Kubernetes Secrets
- Secrets encrypted with Sealed Secrets
- Rotation policy: 90 days

### Compliance

- Regular security audits
- Dependency vulnerability scanning (Dependabot)
- Container image scanning (Trivy)

## Disaster Recovery

### RTO/RPO Targets

| Metric | Target |
|--------|--------|
| Recovery Time Objective (RTO) | 4 hours |
| Recovery Point Objective (RPO) | 1 hour |

### Recovery Procedures

See [Disaster Recovery Runbook](../runbooks/DISASTER_RECOVERY.md) for:
- Complete cluster restoration
- Database recovery from backup
- Service restoration priority

## Cost Breakdown

| Resource | Type | Monthly Cost |
|----------|------|--------------|
| Control Plane | CX21 | €6.00 |
| Worker 1 | CX31 | €10.00 |
| Worker 2 | CX31 | €10.00 |
| Worker 3 | CX31 | €10.00 |
| Load Balancer | LB21 | €6.00 |
| Storage | Volume | €4.00 |
| **Total** | | **~€46.00** |

## Related Documentation

- [Deploy to Production Runbook](../runbooks/DEPLOY_TO_PRODUCTION.md)
- [Rollback Deployment](../runbooks/ROLLBACK_DEPLOYMENT.md)
- [Disaster Recovery](../runbooks/DISASTER_RECOVERY.md)
- [Incident Response](../runbooks/INCIDENT_RESPONSE.md)
