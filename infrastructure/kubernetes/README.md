# QAWave Kubernetes Infrastructure

Kubernetes manifests and cluster configuration for QAWave.

## Directory Structure

```
kubernetes/
├── k0s/                    # K0s cluster configuration
│   ├── k0sctl.yaml.template  # Cluster config template
│   └── bootstrap.sh         # Bootstrap automation script
├── base/                   # Base Kubernetes manifests
│   └── backend/            # Backend deployment
├── overlays/               # Kustomize overlays (future)
│   ├── staging/
│   └── production/
├── ingress/                # Ingress configuration
└── README.md               # This file
```

## K0s Cluster Bootstrap

### Prerequisites

1. **k0sctl** - K0s cluster management tool
   ```bash
   # macOS
   brew install k0sproject/tap/k0sctl

   # Linux
   curl -sSLf https://get.k0sproject.io | sudo sh
   k0sctl version
   ```

2. **Terraform** applied - Infrastructure must be provisioned first
   ```bash
   cd ../terraform/environments/production
   terraform apply
   ```

3. **SSH access** - Ensure your SSH key can access the Hetzner servers

### Quick Start

```bash
cd k0s/

# Automatic bootstrap (reads IPs from Terraform)
./bootstrap.sh

# Or dry-run first
./bootstrap.sh --dry-run
```

### Manual Setup

If you prefer manual setup:

```bash
# 1. Copy template
cp k0sctl.yaml.template k0sctl.yaml

# 2. Get IPs from Terraform
cd ../../terraform/environments/production
terraform output

# 3. Edit k0sctl.yaml with the IPs

# 4. Apply cluster
k0sctl apply --config k0sctl.yaml

# 5. Get kubeconfig
k0sctl kubeconfig --config k0sctl.yaml > kubeconfig.yaml
export KUBECONFIG=$(pwd)/kubeconfig.yaml

# 6. Verify
kubectl get nodes
```

## Cluster Architecture

```
                    ┌─────────────────────────┐
                    │    Load Balancer        │
                    │    (Hetzner LB11)       │
                    └───────────┬─────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
            ▼                   ▼                   ▼
   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
   │   Worker 1      │ │   Worker 2      │ │   Worker 3      │
   │                 │ │                 │ │                 │
   │ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
   │ │ Ingress     │ │ │ │ Ingress     │ │ │ │ Ingress     │ │
   │ │ Controller  │ │ │ │ Controller  │ │ │ │ Controller  │ │
   │ │ (NodePort)  │ │ │ │ (NodePort)  │ │ │ │ (NodePort)  │ │
   │ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
   │                 │ │                 │ │                 │
   │ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
   │ │ App Pods    │ │ │ │ App Pods    │ │ │ │ App Pods    │ │
   │ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
   └─────────────────┘ └─────────────────┘ └─────────────────┘
            │                   │                   │
            └───────────────────┼───────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   Control Plane       │
                    │   (K0s Controller)    │
                    │   - API Server        │
                    │   - etcd              │
                    │   - Scheduler         │
                    │   - Controller Mgr    │
                    └───────────────────────┘
```

## Networking

| Component | CIDR |
|-----------|------|
| Pod Network | 10.244.0.0/16 |
| Service Network | 10.96.0.0/12 |
| VPC Network | 10.0.0.0/16 |

## Ingress

The cluster uses nginx-ingress installed via Helm as a K0s extension:

- **Type**: NodePort
- **HTTP Port**: 30080
- **HTTPS Port**: 30443

The Hetzner Load Balancer forwards:
- Port 80 → NodePort 30080
- Port 443 → NodePort 30443

## Common Commands

```bash
# Set kubeconfig
export KUBECONFIG=/path/to/kubeconfig.yaml

# Check nodes
kubectl get nodes -o wide

# Check all pods
kubectl get pods -A

# Check ingress
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# View logs
kubectl logs -n kube-system deployment/coredns

# Check cluster health
kubectl cluster-info
kubectl get componentstatuses
```

## Troubleshooting

### Nodes not ready

```bash
# Check node conditions
kubectl describe node <node-name>

# Check kubelet logs (on the node)
ssh root@<node-ip> journalctl -u k0sworker -f
```

### Ingress not working

```bash
# Check ingress controller pods
kubectl get pods -n ingress-nginx

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller

# Verify NodePort is accessible
curl http://<worker-ip>:30080
```

### Reset cluster

```bash
# Remove cluster
k0sctl reset --config k0sctl.yaml

# Start fresh
./bootstrap.sh
```

## Next Steps

After cluster bootstrap:

1. **Deploy ArgoCD**: `cd ../argocd && kubectl apply -f install/`
2. **Deploy data services**: PostgreSQL, Redis, Kafka
3. **Deploy applications**: Backend, Frontend via ArgoCD

## Related Documentation

- [K0s Documentation](https://docs.k0sproject.io/)
- [k0sctl Reference](https://github.com/k0sproject/k0sctl)
- [Terraform Configuration](../terraform/README.md)
- [ArgoCD Setup](../argocd/README.md)
