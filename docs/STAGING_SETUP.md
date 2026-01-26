# QAWave Staging Environment - Complete Setup Guide

This guide walks through setting up the complete QAWave staging environment on Hetzner Cloud with Kubernetes (k0s), ArgoCD, and all required services.

## Architecture Overview

```
                    Internet
                        |
                   [Firewall]
                        |
    +-------------------+-------------------+
    |                   |                   |
[Control Plane]    [Worker 1]          [Worker 2]
  91.99.107.246    46.224.232.46      46.224.203.16
    k0s server       k0s worker         k0s worker
```

**Components:**
- 3 Hetzner Cloud VPS (1 control plane + 2 workers)
- k0s Kubernetes distribution
- ArgoCD for GitOps
- Platform services (cert-manager, ingress-nginx, prometheus, etc.)
- Data services (PostgreSQL, Redis, Kafka)

---

## Prerequisites

1. **Hetzner Cloud Account** with API token
2. **SSH key pair** (`~/.ssh/qawave-staging` and `~/.ssh/qawave-staging.pub`)
3. **Terraform** >= 1.6
4. **kubectl** installed
5. **GitHub repository** access

---

## Step 1: Generate SSH Key

```bash
ssh-keygen -t ed25519 -f ~/.ssh/qawave-staging -C "qawave-staging"
```

---

## Step 2: Configure Terraform

```bash
cd infrastructure/terraform/environments/staging

# Copy example configuration
cp terraform.tfvars.example terraform.tfvars

# Edit with your values
vim terraform.tfvars
```

**terraform.tfvars content:**
```hcl
hcloud_token = "your-hetzner-api-token"
ssh_public_key_path = "~/.ssh/qawave-staging.pub"
ssh_private_key_path = "~/.ssh/qawave-staging"
```

---

## Step 3: Deploy Infrastructure

```bash
# Initialize Terraform
terraform init

# Review the plan
terraform plan

# Apply (creates 3 VPS + firewall + network)
terraform apply
```

**Expected output:**
- Control plane IP: `91.99.107.246`
- Worker 1 IP: `46.224.232.46`
- Worker 2 IP: `46.224.203.16`

---

## Step 4: Get Kubeconfig

```bash
# Fetch kubeconfig from control plane
ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' \
  | sed 's|https://10.1.1.10:6443|https://91.99.107.246:6443|' \
  > kubeconfig

# Set environment variable
export KUBECONFIG=$(pwd)/kubeconfig

# Verify cluster access
kubectl get nodes
```

**Expected output:**
```
NAME                      STATUS   ROLES           AGE   VERSION
qawave-cp-staging         Ready    control-plane   1h    v1.34.3+k0s
qawave-worker-1-staging   Ready    <none>          1h    v1.34.3+k0s
qawave-worker-2-staging   Ready    <none>          1h    v1.34.3+k0s
```

---

## Step 5: Install ArgoCD

```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for pods to be ready
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# Expose ArgoCD via NodePort
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort", "ports": [{"port": 443, "targetPort": 8080, "nodePort": 30080}]}}'
```

---

## Step 6: Get ArgoCD Admin Password

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

**Access ArgoCD UI:** http://91.99.107.246:30080
- Username: `admin`
- Password: (from command above)

---

## Step 7: Deploy Platform Services

### 7.1 Local Path Provisioner (Storage)

```bash
kubectl apply -f gitops/platform/local-path-provisioner/application.yaml
```

### 7.2 Sealed Secrets

```bash
kubectl apply -f gitops/platform/sealed-secrets/application.yaml
```

### 7.3 Cert-Manager

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: cert-manager
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://charts.jetstack.io
    chart: cert-manager
    targetRevision: v1.14.3
    helm:
      values: |
        installCRDs: true
  destination:
    server: https://kubernetes.default.svc
    namespace: cert-manager
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF
```

### 7.4 Ingress-Nginx

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ingress-nginx
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://kubernetes.github.io/ingress-nginx
    chart: ingress-nginx
    targetRevision: 4.9.1
    helm:
      values: |
        controller:
          replicaCount: 2
          service:
            type: NodePort
            nodePorts:
              http: 30080
              https: 30443
  destination:
    server: https://kubernetes.default.svc
    namespace: ingress-nginx
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF
```

### 7.5 Prometheus (Monitoring)

```bash
kubectl apply -f gitops/platform/prometheus/application.yaml
```

### 7.6 Fluent-Bit (Logging)

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: fluent-bit
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://fluent.github.io/helm-charts
    chart: fluent-bit
    targetRevision: 0.43.0
    helm:
      values: |
        config:
          outputs: |
            [OUTPUT]
                Name stdout
                Match *
  destination:
    server: https://kubernetes.default.svc
    namespace: logging
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF
```

---

## Step 8: Deploy Data Services

### 8.1 Create Staging Namespace

```bash
kubectl create namespace staging
```

### 8.2 Deploy via ArgoCD (GitOps)

```bash
kubectl apply -f gitops/envs/staging/apps.yaml
```

This deploys:
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **Kafka** - Event streaming

---

## Step 9: Verify Deployment

### Check All Pods

```bash
kubectl get pods -A
```

### Check ArgoCD Applications

```bash
kubectl get applications -n argocd
```

**Expected output:**
```
NAME                     SYNC STATUS   HEALTH STATUS
cert-manager             Synced        Healthy
fluent-bit               Synced        Healthy
ingress-nginx            Synced        Healthy
local-path-provisioner   Synced        Healthy
prometheus               Synced        Healthy
sealed-secrets           Synced        Healthy
staging-kafka            Synced        Healthy
staging-postgresql       Synced        Healthy
staging-redis            Synced        Healthy
```

### Check Data Services

```bash
kubectl get pods -n staging
```

**Expected output:**
```
NAME           READY   STATUS    RESTARTS   AGE
kafka-0        1/1     Running   0          30m
postgresql-0   1/1     Running   0          42m
redis-0        1/1     Running   0          42m
```

---

## Connection Details

### ArgoCD
- **URL:** http://91.99.107.246:30080
- **Username:** admin
- **Password:** (run command in Step 6)

### PostgreSQL
- **Host:** postgresql.staging.svc.cluster.local
- **Port:** 5432
- **Database:** qawave
- **Username:** qawave
- **Password:** qawave-staging-app-2024

### Redis
- **Host:** redis.staging.svc.cluster.local
- **Port:** 6379
- **Password:** qawave-staging-redis-2024

### Kafka
- **Bootstrap Server:** kafka.staging.svc.cluster.local:9092
- **No authentication** (internal cluster only)

---

## Maintenance

### Update Kubeconfig (if expired)

```bash
ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' \
  | sed 's|https://10.1.1.10:6443|https://91.99.107.246:6443|' \
  > kubeconfig
export KUBECONFIG=$(pwd)/kubeconfig
```

### Force Sync ArgoCD Application

```bash
kubectl patch application <app-name> -n argocd --type merge -p '{"operation": {"sync": {}}}'
```

### View Application Logs

```bash
# PostgreSQL
kubectl logs -n staging postgresql-0

# Redis
kubectl logs -n staging redis-0

# Kafka
kubectl logs -n staging kafka-0
```

### Connect to PostgreSQL

```bash
kubectl exec -it -n staging postgresql-0 -- psql -U qawave -d qawave
```

### Connect to Redis

```bash
kubectl exec -it -n staging redis-0 -- redis-cli -a qawave-staging-redis-2024
```

---

## Troubleshooting

### ArgoCD Application Stuck

```bash
# Check application status
kubectl describe application <app-name> -n argocd

# Force refresh
kubectl patch application <app-name> -n argocd \
  --type merge -p '{"metadata": {"annotations": {"argocd.argoproj.io/refresh": "hard"}}}'
```

### Pod Not Starting

```bash
# Check events
kubectl describe pod <pod-name> -n <namespace>

# Check logs
kubectl logs <pod-name> -n <namespace>
```

### PVC Not Bound

```bash
# Check PVC status
kubectl get pvc -n staging

# Check storage class
kubectl get sc
```

---

## Destroy Environment

```bash
cd infrastructure/terraform/environments/staging
terraform destroy
```

**Warning:** This deletes all resources and data permanently!

---

## File Structure

```
test-env/
├── gitops/
│   ├── envs/
│   │   └── staging/
│   │       ├── apps.yaml          # ArgoCD applications for data services
│   │       ├── kafka/
│   │       │   └── kafka.yaml
│   │       ├── postgresql/
│   │       │   └── postgresql.yaml
│   │       └── redis/
│   │           └── redis.yaml
│   └── platform/
│       ├── local-path-provisioner/
│       ├── prometheus/
│       └── sealed-secrets/
└── infrastructure/
    └── terraform/
        └── environments/
            └── staging/
                ├── main.tf
                ├── variables.tf
                ├── outputs.tf
                └── terraform.tfvars
```
