# Staging Environment

## Overview

Staging is a production-like environment for QA testing and integration validation before releases.

| Property | Value |
|----------|-------|
| **Frontend URL** | https://staging.qawave.io |
| **API URL** | https://api.staging.qawave.io |
| **ArgoCD URL** | https://argocd.staging.qawave.io |
| **Branch** | `develop` |
| **Monthly Cost** | ~€17 |

## Infrastructure

### Hetzner VPS Instances

| Role | Type | Specs | IP | Private IP |
|------|------|-------|-----|------------|
| Control Plane | CX11 | 1 vCPU, 2GB RAM | 91.99.107.246 | 10.1.1.10 |
| Worker 1 | CX21 | 2 vCPU, 4GB RAM | 46.224.232.46 | 10.1.1.11 |
| Worker 2 | CX21 | 2 vCPU, 4GB RAM | 46.224.203.16 | 10.1.1.12 |

### Architecture Diagram

```
                         Internet
                             │
                    ┌────────┴────────┐
                    │  Load Balancer  │
                    │     (LB11)      │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
┌────────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
│  Control Plane  │ │    Worker 1     │ │    Worker 2     │
│   91.99.107.246 │ │  46.224.232.46  │ │  46.224.203.16  │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│ K0s Server      │ │ K0s Agent       │ │ K0s Agent       │
│ etcd            │ │ Frontend Pod    │ │ Frontend Pod    │
│ API Server      │ │ Backend Pod     │ │ Backend Pod     │
│ Scheduler       │ │ PostgreSQL      │ │ Kafka           │
│ ArgoCD          │ │ Redis           │ │                 │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                   │                   │
         └───────────────────┴───────────────────┘
                    Private Network
                     10.1.0.0/16
```

### Network Configuration

| Component | CIDR | Purpose |
|-----------|------|---------|
| VPC Network | 10.1.0.0/16 | Private communication |
| Node Subnet | 10.1.1.0/24 | K8s node IPs |
| Pod Network | 10.244.0.0/16 | K8s pod IPs (Calico) |
| Service Network | 10.96.0.0/12 | K8s service IPs |

### Firewall Rules

| Port | Protocol | Source | Description |
|------|----------|--------|-------------|
| 22 | TCP | 0.0.0.0/0 | SSH access |
| 6443 | TCP | 0.0.0.0/0 | Kubernetes API |
| 80 | TCP | 0.0.0.0/0 | HTTP traffic |
| 443 | TCP | 0.0.0.0/0 | HTTPS traffic |
| 30000-32767 | TCP | 0.0.0.0/0 | NodePort services |

## Access

### SSH Access

```bash
# Control plane
ssh -i ~/.ssh/qawave-staging root@91.99.107.246

# Worker 1
ssh -i ~/.ssh/qawave-staging root@46.224.232.46

# Worker 2
ssh -i ~/.ssh/qawave-staging root@46.224.203.16
```

### Kubernetes Access

```bash
# Set kubeconfig
export KUBECONFIG=~/.kube/config-qawave-staging

# Verify connection
kubectl get nodes

# Expected output:
# NAME                      STATUS   ROLES           AGE   VERSION
# qawave-cp-staging         Ready    control-plane   17h   v1.34.3+k0s
# qawave-worker-1-staging   Ready    <none>          17h   v1.34.3+k0s
# qawave-worker-2-staging   Ready    <none>          17h   v1.34.3+k0s
```

**Getting Kubeconfig:**
```bash
ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' \
  | sed 's|https://10.1.1.10:6443|https://91.99.107.246:6443|' \
  > ~/.kube/config-qawave-staging
```

### ArgoCD Access

- **URL:** https://argocd.staging.qawave.io (or http://91.99.107.246:30080)
- **Username:** admin
- **Password:** Get with:
  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
  ```

## Services

### Application Services

| Service | Namespace | Type | Port | Internal URL |
|---------|-----------|------|------|--------------|
| Frontend | staging | NodePort | 30000 | frontend.staging.svc.cluster.local:80 |
| Backend | staging | ClusterIP | 8080 | backend.staging.svc.cluster.local:8080 |

### Data Services

| Service | Namespace | Port | Internal URL | Credentials |
|---------|-----------|------|--------------|-------------|
| PostgreSQL | staging | 5432 | postgresql.staging.svc.cluster.local | qawave / qawave-staging-app-2024 |
| Redis | staging | 6379 | redis.staging.svc.cluster.local | qawave-staging-redis-2024 |
| Kafka | staging | 9092 | kafka.staging.svc.cluster.local | No auth (internal) |

### Platform Services

| Service | Namespace | Purpose |
|---------|-----------|---------|
| ArgoCD | argocd | GitOps deployment |
| cert-manager | cert-manager | TLS certificates |
| ingress-nginx | ingress-nginx | Ingress controller |
| prometheus | monitoring | Metrics collection |
| fluent-bit | logging | Log aggregation |
| sealed-secrets | kube-system | Secret encryption |
| local-path-provisioner | kube-system | Storage provisioner |

## Deployment

### Automatic Deployment (GitOps)

Staging deploys automatically when:
1. PR is merged to `develop` branch
2. GitHub Actions builds and pushes container images
3. ArgoCD detects new images and syncs (every 3 minutes)

### Manual Deployment

```bash
# Trigger GitHub Actions workflow
gh workflow run "Build and Deploy to Staging" \
  --ref develop \
  -f deploy_backend=true \
  -f deploy_frontend=true

# Force ArgoCD sync
kubectl patch application qawave-staging -n argocd \
  --type merge -p '{"operation": {"sync": {}}}'
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -n staging

# Check ArgoCD applications
kubectl get applications -n argocd

# Check logs
kubectl logs -f deployment/backend -n staging
kubectl logs -f deployment/frontend -n staging
```

## Testing

### Running E2E Tests

```bash
# Set environment
export BASE_URL=http://46.224.232.46:30000
export KUBECONFIG=~/.kube/config-qawave-staging

# Run tests
cd e2e-tests
npm install
npx playwright test --config=playwright.staging.config.ts

# Or use script
./scripts/run-e2e-tests.sh
```

### Health Checks

```bash
# Frontend
curl -s -o /dev/null -w "%{http_code}" http://46.224.232.46:30000
# Expected: 200

# Backend (via port-forward)
kubectl port-forward svc/backend 8080:8080 -n staging &
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## Monitoring

### Prometheus Metrics

- **URL:** http://91.99.107.246:30090 (NodePort)
- Key metrics:
  - `kube_pod_status_phase` - Pod health
  - `container_memory_usage_bytes` - Memory usage
  - `container_cpu_usage_seconds_total` - CPU usage

### Log Access

```bash
# Application logs
kubectl logs -f deployment/backend -n staging
kubectl logs -f deployment/frontend -n staging

# System logs (SSH to node)
journalctl -u k0scontroller -f  # Control plane
journalctl -u k0sworker -f      # Workers
```

## Maintenance

### Restart Services

```bash
# Restart deployment
kubectl rollout restart deployment/backend -n staging
kubectl rollout restart deployment/frontend -n staging

# Restart all pods in namespace
kubectl delete pods --all -n staging
```

### Update Secrets

```bash
# Update GHCR pull secret
kubectl delete secret ghcr-secret -n staging
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=$GITHUB_USERNAME \
  --docker-password=$GITHUB_TOKEN \
  -n staging
```

### Database Operations

```bash
# Connect to PostgreSQL
kubectl exec -it -n staging postgresql-0 -- psql -U qawave -d qawave

# Connect to Redis
kubectl exec -it -n staging redis-0 -- redis-cli -a qawave-staging-redis-2024
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| ImagePullBackOff | Missing GHCR credentials | Recreate ghcr-secret |
| Pending pods | No resources | Check node capacity |
| CrashLoopBackOff | App error | Check logs |
| Service unavailable | Pod not ready | Check readiness probes |

### Debug Commands

```bash
# Describe pod for events
kubectl describe pod <pod-name> -n staging

# Get all resources
kubectl get all -n staging

# Check events
kubectl get events -n staging --sort-by='.lastTimestamp'

# Check PVC
kubectl get pvc -n staging
```

## Related Documentation

- [Deploy to Staging Runbook](../runbooks/DEPLOY_TO_STAGING.md)
- [Rollback Deployment](../runbooks/ROLLBACK_DEPLOYMENT.md)
- [Complete Setup Guide](../STAGING_SETUP.md)
- [Learning Guide](../LEARNING_GUIDE_STAGING_DEPLOYMENT.md)
