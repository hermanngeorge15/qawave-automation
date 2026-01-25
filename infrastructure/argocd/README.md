# QAWave ArgoCD Configuration

ArgoCD setup for GitOps-based continuous deployment of QAWave services.

## Directory Structure

```
argocd/
├── install/              # ArgoCD installation manifests
│   └── kustomization.yaml
├── ingress/              # Ingress configuration
│   └── ingress.yaml
├── applications/         # ArgoCD Application definitions
│   └── qawave.yaml       # All QAWave apps
├── setup.sh              # Installation script
└── README.md             # This file
```

## Quick Start

```bash
# Ensure kubectl is configured for your cluster
kubectl cluster-info

# Run setup script
./setup.sh
```

## Manual Installation

### 1. Create Namespace

```bash
kubectl create namespace argocd
```

### 2. Install ArgoCD

```bash
kubectl apply -k install/
```

### 3. Wait for Ready

```bash
kubectl wait --for=condition=available --timeout=300s \
    deployment/argocd-server -n argocd
```

### 4. Get Admin Password

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
    -o jsonpath="{.data.password}" | base64 -d
```

### 5. Access UI

```bash
# Port forward for local access
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Open: https://localhost:8080
# Username: admin
# Password: (from step 4)
```

## Applications

The following applications are configured:

| Application | Environment | Path | Auto-Sync |
|-------------|-------------|------|-----------|
| qawave-backend-production | production | kubernetes/overlays/production | Yes |
| qawave-backend-staging | staging | kubernetes/overlays/staging | Yes |
| qawave-frontend-production | production | kubernetes/overlays/production | Yes |
| qawave-data-services | production | kubernetes/base/data-services | Yes (no prune) |

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           GitHub Repository                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │
│  │ infrastructure/ │  │ backend/        │  │ frontend/       │      │
│  │ kubernetes/     │  │                 │  │                 │      │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘      │
└───────────┼───────────────────┼───────────────────┼─────────────────┘
            │                    │                   │
            ▼                    ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                              ArgoCD                                  │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Application Controller                      │  │
│  │  - Watches Git repository                                      │  │
│  │  - Compares desired state vs live state                        │  │
│  │  - Syncs resources when drift detected                         │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                            │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ qawave NS      │  │ qawave-staging │  │ argocd NS      │        │
│  │ ┌────────────┐ │  │ ┌────────────┐ │  │ ┌────────────┐ │        │
│  │ │ Backend    │ │  │ │ Backend    │ │  │ │ ArgoCD     │ │        │
│  │ │ Frontend   │ │  │ │ Frontend   │ │  │ │ Server     │ │        │
│  │ │ PostgreSQL │ │  │ │            │ │  │ └────────────┘ │        │
│  │ │ Redis      │ │  │ │            │ │  │                │        │
│  │ │ Kafka      │ │  │ │            │ │  │                │        │
│  │ └────────────┘ │  │ └────────────┘ │  │                │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Sync Policies

### Automated Sync

Staging environments have automated sync enabled:
- Auto-prune: Removes resources deleted from Git
- Self-heal: Reverts manual changes in cluster

### Production Sync

Production can be configured for:
- Manual sync (safer)
- Automated with approval gates

## ArgoCD CLI

```bash
# Install CLI
brew install argocd

# Login
argocd login argocd.qawave.io

# List applications
argocd app list

# Sync application
argocd app sync qawave-backend-staging

# View app status
argocd app get qawave-backend-production

# View app diff
argocd app diff qawave-backend-production

# Rollback
argocd app rollback qawave-backend-production
```

## Repository Configuration

For private repositories, create a secret:

```bash
kubectl create secret generic repo-qawave \
    --namespace argocd \
    --from-literal=url=https://github.com/your-org/qawave.git \
    --from-literal=username=git \
    --from-literal=password=<github-pat>

kubectl label secret repo-qawave -n argocd argocd.argoproj.io/secret-type=repository
```

## RBAC

ArgoCD RBAC is configured via ConfigMap. Default admin has full access.

To add read-only users:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-rbac-cm
  namespace: argocd
data:
  policy.csv: |
    p, role:readonly, applications, get, */*, allow
    p, role:readonly, logs, get, */*, allow
    g, developers, role:readonly
```

## Troubleshooting

### Application stuck in "Progressing"

```bash
# Check events
argocd app get <app-name> --show-operation

# Check logs
kubectl logs -n argocd deployment/argocd-application-controller
```

### Sync failed

```bash
# View sync details
argocd app sync <app-name> --dry-run

# Force sync
argocd app sync <app-name> --force
```

### Can't access UI

```bash
# Check pod status
kubectl get pods -n argocd

# Check service
kubectl get svc -n argocd

# Port forward
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

## Security Notes

1. **Change default password** immediately after installation
2. **Enable SSO** for production environments
3. **Restrict network access** to ArgoCD UI
4. **Use RBAC** to limit permissions
5. **Audit logs** for compliance

## Related Documentation

- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Kubernetes Setup](../kubernetes/README.md)
- [DevOps Instructions](../../docs/agents/DEVOPS.md)
