# Kubernetes Access Guide

This guide explains how to access and manage QAWave Kubernetes clusters.

## Prerequisites

- kubectl installed
- SSH access to cluster nodes (for kubeconfig retrieval)
- Appropriate permissions for the target environment

### Install kubectl

**macOS:**
```bash
brew install kubectl
```

**Linux:**
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

**Verify:**
```bash
kubectl version --client
```

## Kubeconfig Setup

### Staging Cluster

**Get kubeconfig from control plane:**
```bash
# SSH to control plane and extract kubeconfig
ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' \
  | sed 's|https://10.1.1.10:6443|https://91.99.107.246:6443|' \
  > ~/.kube/config-qawave-staging

# Set permissions
chmod 600 ~/.kube/config-qawave-staging
```

**Use the kubeconfig:**
```bash
export KUBECONFIG=~/.kube/config-qawave-staging
kubectl get nodes
```

### Production Cluster

Production kubeconfig is distributed only to authorized personnel. Contact DevOps for access.

```bash
# Assuming you have the kubeconfig file
export KUBECONFIG=~/.kube/config-qawave-prod
kubectl get nodes
```

### Multiple Clusters

**Option 1: Environment variable per session**
```bash
# For staging work
export KUBECONFIG=~/.kube/config-qawave-staging

# For production work (separate terminal)
export KUBECONFIG=~/.kube/config-qawave-prod
```

**Option 2: Merge kubeconfigs**
```bash
# Set KUBECONFIG to multiple files
export KUBECONFIG=~/.kube/config-qawave-staging:~/.kube/config-qawave-prod

# Switch context
kubectl config use-context qawave-staging
kubectl config use-context qawave-prod
```

**Option 3: kubectx (recommended)**
```bash
# Install
brew install kubectx

# Switch clusters
kubectx qawave-staging
kubectx qawave-prod

# Switch namespaces
kubens staging
```

## Basic Operations

### Cluster Information

```bash
# View cluster info
kubectl cluster-info

# View nodes
kubectl get nodes

# Node details
kubectl describe node <node-name>
```

### Namespace Operations

```bash
# List namespaces
kubectl get namespaces

# Set default namespace
kubectl config set-context --current --namespace=staging

# Create namespace
kubectl create namespace my-namespace
```

### Pod Operations

```bash
# List pods
kubectl get pods -n staging

# List all pods in all namespaces
kubectl get pods -A

# Pod details
kubectl describe pod <pod-name> -n staging

# Pod logs
kubectl logs <pod-name> -n staging

# Follow logs
kubectl logs -f <pod-name> -n staging

# Previous container logs (after crash)
kubectl logs <pod-name> -n staging --previous

# Exec into pod
kubectl exec -it <pod-name> -n staging -- /bin/sh
```

### Deployment Operations

```bash
# List deployments
kubectl get deployments -n staging

# Deployment details
kubectl describe deployment <name> -n staging

# Scale deployment
kubectl scale deployment <name> --replicas=3 -n staging

# Restart deployment
kubectl rollout restart deployment/<name> -n staging

# Rollout status
kubectl rollout status deployment/<name> -n staging

# Rollout history
kubectl rollout history deployment/<name> -n staging

# Rollback
kubectl rollout undo deployment/<name> -n staging
```

### Service Operations

```bash
# List services
kubectl get svc -n staging

# Service details
kubectl describe svc <name> -n staging

# Port forward (access internal service)
kubectl port-forward svc/<name> 8080:8080 -n staging
```

### Secret Operations

```bash
# List secrets
kubectl get secrets -n staging

# View secret (base64 encoded)
kubectl get secret <name> -n staging -o yaml

# Decode secret value
kubectl get secret <name> -n staging -o jsonpath='{.data.password}' | base64 -d

# Create secret
kubectl create secret generic my-secret \
  --from-literal=username=admin \
  --from-literal=password=secret123 \
  -n staging
```

## Debugging

### Pod Issues

```bash
# Check pod events
kubectl describe pod <pod-name> -n staging | grep -A 20 Events

# Check all events in namespace
kubectl get events -n staging --sort-by='.lastTimestamp'

# Watch pods in real-time
kubectl get pods -n staging -w
```

### Common Pod States

| State | Meaning | Common Causes |
|-------|---------|---------------|
| Pending | Not scheduled yet | No resources, node selector |
| ContainerCreating | Pulling image | Large image, network issues |
| ImagePullBackOff | Can't pull image | Wrong image, no credentials |
| CrashLoopBackOff | Container crashing | App error, missing config |
| Running | Container running | Normal state |
| Completed | Container exited 0 | Job completed |

### Resource Usage

```bash
# Node resources
kubectl top nodes

# Pod resources
kubectl top pods -n staging

# Container resources
kubectl top pods --containers -n staging
```

## Advanced Operations

### Apply Manifests

```bash
# Apply single file
kubectl apply -f deployment.yaml

# Apply directory
kubectl apply -f ./manifests/

# Apply with kustomize
kubectl apply -k ./overlays/staging/
```

### Delete Resources

```bash
# Delete pod
kubectl delete pod <name> -n staging

# Delete by label
kubectl delete pods -l app=backend -n staging

# Delete all pods (dangerous!)
kubectl delete pods --all -n staging

# Delete namespace (deletes everything in it!)
kubectl delete namespace staging
```

### Labels and Selectors

```bash
# Add label
kubectl label pod <name> environment=staging

# Remove label
kubectl label pod <name> environment-

# Select by label
kubectl get pods -l app=backend -n staging
kubectl get pods -l 'environment in (staging, production)'
```

### Annotations

```bash
# Add annotation
kubectl annotate pod <name> description="My pod"

# Remove annotation
kubectl annotate pod <name> description-
```

## ArgoCD Integration

### ArgoCD CLI Setup

```bash
# Install
brew install argocd

# Login (get password first)
ARGOCD_PASS=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
argocd login argocd.staging.qawave.io --username admin --password $ARGOCD_PASS
```

### ArgoCD Operations

```bash
# List applications
argocd app list

# Get application details
argocd app get qawave-staging

# Sync application
argocd app sync qawave-staging

# History
argocd app history qawave-staging

# Rollback
argocd app rollback qawave-staging <revision>
```

## Access Control

### Role-Based Access (RBAC)

```bash
# View roles
kubectl get roles -n staging
kubectl get clusterroles

# View role bindings
kubectl get rolebindings -n staging
kubectl get clusterrolebindings

# Check permissions
kubectl auth can-i create pods -n staging
kubectl auth can-i delete deployments -n staging
```

### Service Accounts

```bash
# List service accounts
kubectl get serviceaccounts -n staging

# Create service account
kubectl create serviceaccount my-sa -n staging

# Get service account token
kubectl create token my-sa -n staging
```

## Tips and Shortcuts

### Aliases

Add to `~/.bashrc` or `~/.zshrc`:
```bash
alias k='kubectl'
alias kgp='kubectl get pods'
alias kgs='kubectl get svc'
alias kgd='kubectl get deployments'
alias kgn='kubectl get nodes'
alias kdp='kubectl describe pod'
alias kds='kubectl describe svc'
alias kdd='kubectl describe deployment'
alias kl='kubectl logs -f'
alias ke='kubectl exec -it'
```

### Auto-completion

**Bash:**
```bash
echo 'source <(kubectl completion bash)' >> ~/.bashrc
```

**Zsh:**
```bash
echo 'source <(kubectl completion zsh)' >> ~/.zshrc
```

### Quick JSON Queries

```bash
# Get pod IPs
kubectl get pods -n staging -o jsonpath='{.items[*].status.podIP}'

# Get image names
kubectl get pods -n staging -o jsonpath='{.items[*].spec.containers[*].image}'

# Custom columns
kubectl get pods -n staging -o custom-columns='NAME:.metadata.name,STATUS:.status.phase,IP:.status.podIP'
```

## Troubleshooting

### Cannot Connect to Cluster

```bash
# Check kubeconfig
echo $KUBECONFIG
cat $KUBECONFIG | head -20

# Test connection
kubectl cluster-info

# Check API server port
nc -zv 91.99.107.246 6443
```

### Permission Denied

```bash
# Check current context
kubectl config current-context

# Check authentication
kubectl auth can-i '*' '*' --all-namespaces
```

### Slow Commands

```bash
# Limit output
kubectl get pods -n staging --limit=100

# Use watch instead of repeated calls
kubectl get pods -n staging -w
```

## Related Documentation

- [Staging Environment](../environments/STAGING.md)
- [Production Environment](../environments/PRODUCTION.md)
- [Deploy to Staging Runbook](../runbooks/DEPLOY_TO_STAGING.md)
- [Rollback Deployment](../runbooks/ROLLBACK_DEPLOYMENT.md)
