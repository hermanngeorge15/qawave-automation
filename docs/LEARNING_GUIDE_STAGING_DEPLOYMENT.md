# Learning Guide: Staging Environment Deployment on Hetzner VPS

## Overview

This guide documents the complete process of deploying a staging environment for QAWave on Hetzner VPS with Kubernetes. It's written as a learning resource so you can understand and reproduce each step.

**What we achieved:**
- Fixed image pull issues preventing deployment
- Got frontend and backend pods running on staging
- Set up E2E testing infrastructure
- Created automation scripts for on-demand staging

**Technologies used:**
- Hetzner Cloud (VPS provider)
- Kubernetes (K0s distribution)
- ArgoCD (GitOps)
- GitHub Container Registry (GHCR)
- Playwright (E2E testing)

---

## Table of Contents

1. [Understanding the Problem](#1-understanding-the-problem)
2. [Kubernetes Image Pull Secrets](#2-kubernetes-image-pull-secrets)
3. [Connecting to the Cluster](#3-connecting-to-the-cluster)
4. [Fixing the Deployment](#4-fixing-the-deployment)
5. [Triggering CI/CD Build](#5-triggering-cicd-build)
6. [Verifying the Deployment](#6-verifying-the-deployment)
7. [Running E2E Tests](#7-running-e2e-tests)
8. [Automation Scripts](#8-automation-scripts)
9. [Key Concepts Explained](#9-key-concepts-explained)
10. [Troubleshooting Guide](#10-troubleshooting-guide)

---

## 1. Understanding the Problem

### Initial State

When we started, the pods were failing with `ImagePullBackOff`:

```bash
$ kubectl get pods -n staging
NAME                       READY   STATUS             RESTARTS   AGE
backend-cf7f4945c-wbx4w    0/1     ImagePullBackOff   0          14h
frontend-964458d87-965nh   0/1     ImagePullBackOff   0          14h
```

### What is ImagePullBackOff?

`ImagePullBackOff` means Kubernetes cannot pull the container image. Common causes:

1. **Image doesn't exist** - The image hasn't been built/pushed
2. **Authentication required** - Private registry needs credentials
3. **Wrong image name** - Typo in image path
4. **Network issues** - Can't reach the registry

### Diagnosing the Issue

To find out why, we used `kubectl describe`:

```bash
kubectl describe pod backend-cf7f4945c-wbx4w -n staging
```

The Events section showed:
```
Failed to pull image "ghcr.io/hermanngeorge15/qawave-backend:latest":
failed to authorize: 403 Forbidden
```

**Root causes identified:**
1. No images existed in GHCR (never built)
2. No `imagePullSecrets` configured (authentication)

---

## 2. Kubernetes Image Pull Secrets

### What are Image Pull Secrets?

When using private container registries (like GHCR for private repos), Kubernetes needs credentials to pull images. These are stored as `Secrets` of type `docker-registry`.

### How Authentication Works

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Kubernetes    │────▶│  Pull Image  │────▶│      GHCR       │
│     kubelet     │     │   Request    │     │  (Private Reg)  │
└─────────────────┘     └──────────────┘     └─────────────────┘
        │                                            │
        │ Uses imagePullSecrets                      │
        │ from Pod spec                              │
        ▼                                            ▼
┌─────────────────┐                         ┌─────────────────┐
│  ghcr-secret    │────────────────────────▶│   Authenticate  │
│  (K8s Secret)   │     Token/Password      │   & Authorize   │
└─────────────────┘                         └─────────────────┘
```

### Creating the Secret

#### Step 1: Get a GitHub Personal Access Token (PAT)

1. Go to GitHub → Settings → Developer settings → Personal access tokens
2. Generate new token (classic) with scopes:
   - `read:packages` - Pull images
   - `write:packages` - Push images (for CI)

#### Step 2: Create the Kubernetes Secret

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_PAT \
  -n staging
```

**Explanation:**
- `docker-registry` - Secret type for container registries
- `ghcr-secret` - Name we chose for the secret
- `--docker-server=ghcr.io` - GitHub Container Registry URL
- `-n staging` - Create in staging namespace

#### Step 3: Reference in Deployment

Add `imagePullSecrets` to the Pod spec:

```yaml
# deployment.yaml
spec:
  template:
    spec:
      imagePullSecrets:        # Add this section
        - name: ghcr-secret    # Reference the secret
      containers:
        - name: backend
          image: ghcr.io/hermanngeorge15/qawave-backend:latest
```

---

## 3. Connecting to the Cluster

### Cluster Architecture

Our staging environment has:
- 1 Control Plane node (91.99.107.246) - Runs K8s API server
- 2 Worker nodes (46.224.232.46, 46.224.203.16) - Run workloads

```
┌─────────────────────────────────────────────────────────────┐
│                    Hetzner Cloud                             │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │  Control Plane   │  │    Worker 1      │  │  Worker 2  │ │
│  │  91.99.107.246   │  │  46.224.232.46   │  │  46.224... │ │
│  │                  │  │                  │  │            │ │
│  │  - K8s API       │  │  - Frontend pod  │  │  - Pods    │ │
│  │  - etcd          │  │  - Backend pod   │  │            │ │
│  │  - Scheduler     │  │  - PostgreSQL    │  │            │ │
│  │                  │  │  - Redis         │  │            │ │
│  └──────────────────┘  └──────────────────┘  └────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Getting Kubeconfig

Kubeconfig is the file that tells `kubectl` how to connect to the cluster.

#### Step 1: SSH to Control Plane

```bash
ssh -i ~/.ssh/qawave-staging root@91.99.107.246
```

#### Step 2: Get Kubeconfig from K0s

```bash
# On the control plane server
k0s kubeconfig admin
```

This outputs the kubeconfig YAML.

#### Step 3: Save Locally

```bash
# From your local machine
ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' > ~/.kube/config-qawave-staging
```

#### Step 4: Fix Server Address

The kubeconfig has an internal IP. Replace it with the public IP:

```bash
# On macOS
sed -i '' 's/10.1.1.10/91.99.107.246/g' ~/.kube/config-qawave-staging

# On Linux
sed -i 's/10.1.1.10/91.99.107.246/g' ~/.kube/config-qawave-staging
```

#### Step 5: Use the Kubeconfig

```bash
export KUBECONFIG=~/.kube/config-qawave-staging
kubectl get nodes
```

Expected output:
```
NAME                      STATUS   ROLES           AGE   VERSION
qawave-cp-staging         Ready    control-plane   17h   v1.34.3+k0s
qawave-worker-1-staging   Ready    <none>          17h   v1.34.3+k0s
qawave-worker-2-staging   Ready    <none>          17h   v1.34.3+k0s
```

---

## 4. Fixing the Deployment

### Step 1: Create Image Pull Secret

Using the GitHub CLI token (easier than creating a new PAT):

```bash
# Get token from gh CLI
GH_TOKEN=$(gh auth token)

# Create secret
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=hermanngeorge15 \
  --docker-password="$GH_TOKEN" \
  -n staging
```

### Step 2: Update Deployment Manifests

Edit `gitops/envs/staging/backend/backend.yaml`:

```yaml
# Before
spec:
  template:
    spec:
      containers:
        - name: backend
          image: ghcr.io/hermanngeorge15/qawave-backend:latest

# After
spec:
  template:
    spec:
      imagePullSecrets:           # Add this
        - name: ghcr-secret       # Add this
      containers:
        - name: backend
          image: ghcr.io/hermanngeorge15/qawave-backend:latest
```

Same for `gitops/envs/staging/frontend/frontend.yaml`.

### Step 3: Apply Changes

```bash
kubectl apply -f gitops/envs/staging/backend/backend.yaml
kubectl apply -f gitops/envs/staging/frontend/frontend.yaml
```

**Note:** If ArgoCD is managing these resources, it will sync automatically from Git.

---

## 5. Triggering CI/CD Build

### Why We Need to Build Images

The deployments reference images that don't exist yet:
- `ghcr.io/hermanngeorge15/qawave-backend:latest`
- `ghcr.io/hermanngeorge15/qawave-frontend:latest`

We need to build and push them to GHCR.

### GitHub Actions Workflow

The workflow file `.github/workflows/build-and-deploy.yml` does this:

```yaml
# Simplified version
name: Build and Deploy

on:
  push:
    branches: [main]
  workflow_dispatch:  # Manual trigger

jobs:
  build-backend:
    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/qawave-backend:latest
```

### Triggering Manually

```bash
# Using GitHub CLI
gh workflow run "Build and Deploy to Staging" \
  --ref main \
  -f deploy_backend=true \
  -f deploy_frontend=true
```

### Monitoring the Build

```bash
# List recent runs
gh run list --workflow="Build and Deploy to Staging" --limit 3

# Watch a specific run
gh run watch <run-id>
```

---

## 6. Verifying the Deployment

### Step 1: Check Pods

```bash
kubectl get pods -n staging
```

Expected output (all Running):
```
NAME                        READY   STATUS    RESTARTS   AGE
backend-69bdc54f4b-q6n95    1/1     Running   0          5m
frontend-6bf7b79995-l6q4f   1/1     Running   0          5m
kafka-0                     1/1     Running   0          15h
postgresql-0                1/1     Running   0          16h
redis-0                     1/1     Running   0          16h
```

### Step 2: Check Services

```bash
kubectl get svc -n staging
```

```
NAME         TYPE        CLUSTER-IP       PORT(S)
backend      ClusterIP   10.97.188.141    8080/TCP
frontend     NodePort    10.104.52.204    80:30000/TCP
postgresql   ClusterIP   10.104.2.0       5432/TCP
redis        ClusterIP   10.108.124.44    6379/TCP
```

### Step 3: Test Frontend (External Access)

Frontend is exposed via NodePort 30000:

```bash
curl -s -o /dev/null -w "%{http_code}" http://46.224.232.46:30000
# Should return: 200
```

### Step 4: Test Backend Health

Backend is ClusterIP (internal only), so we exec into a pod:

```bash
kubectl exec deploy/backend -n staging -- wget -qO- http://localhost:8080/actuator/health
```

Expected output:
```json
{
  "status": "UP",
  "components": {
    "r2dbc": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### Alternative: Port Forward

To access backend from your local machine:

```bash
# In one terminal
kubectl port-forward svc/backend 8080:8080 -n staging

# In another terminal
curl http://localhost:8080/actuator/health
```

---

## 7. Running E2E Tests

### Understanding E2E Test Setup

E2E tests use Playwright to:
1. Open a browser
2. Navigate to the frontend
3. Interact with UI elements
4. Make API calls
5. Verify behavior

### Running Tests Locally

```bash
cd e2e-tests

# Install dependencies
npm install

# Set environment variables
export BASE_URL=http://46.224.232.46:30000  # Frontend
export API_URL=http://localhost:8080         # Backend (via port-forward)

# Run tests
npx playwright test --config=playwright.staging.config.ts
```

### Using the Script

```bash
./scripts/run-e2e-tests.sh           # All tests
./scripts/run-e2e-tests.sh --smoke   # Smoke tests only
./scripts/run-e2e-tests.sh --api     # API tests only
```

### Running in Kubernetes (Production-like)

Build and push the test image:

```bash
cd e2e-tests
docker build -t ghcr.io/hermanngeorge15/qawave-e2e-tests:latest .
docker push ghcr.io/hermanngeorge15/qawave-e2e-tests:latest
```

Run as a Kubernetes Job:

```bash
./scripts/run-e2e-tests.sh --k8s --build
```

---

## 8. Automation Scripts

### Scripts Created

| Script | Purpose | Usage |
|--------|---------|-------|
| `setup-staging.sh` | Provision entire staging environment | `./scripts/setup-staging.sh` |
| `teardown-staging.sh` | Destroy staging environment | `./scripts/teardown-staging.sh` |
| `health-check.sh` | Verify all services healthy | `./scripts/health-check.sh` |
| `run-e2e-tests.sh` | Run E2E tests | `./scripts/run-e2e-tests.sh` |

### Setup Script Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    setup-staging.sh                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Check Prerequisites                                      │
│     └─ terraform, kubectl, ssh installed?                    │
│     └─ HCLOUD_TOKEN, GITHUB_TOKEN set?                       │
│                                                              │
│  2. Provision Infrastructure (Terraform)                     │
│     └─ Create Hetzner VPS instances                          │
│     └─ Configure networking                                  │
│     └─ Install K0s Kubernetes                                │
│                                                              │
│  3. Wait for SSH                                             │
│     └─ Retry until SSH is available                          │
│                                                              │
│  4. Fetch Kubeconfig                                         │
│     └─ Get from control plane                                │
│     └─ Fix server address                                    │
│                                                              │
│  5. Create Namespace & Secrets                               │
│     └─ Create 'staging' namespace                            │
│     └─ Create ghcr-secret for image pulls                    │
│                                                              │
│  6. Deploy Infrastructure Services                           │
│     └─ PostgreSQL, Redis, Kafka                              │
│                                                              │
│  7. Deploy Applications                                      │
│     └─ Backend, Frontend                                     │
│                                                              │
│  8. Print Summary                                            │
│     └─ URLs, kubeconfig path, next steps                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Environment Variables

```bash
# Required
export HCLOUD_TOKEN="your-hetzner-api-token"
export GITHUB_TOKEN="your-github-pat"

# Optional
export GITHUB_USERNAME="hermanngeorge15"
export VPS_LOCATION="nbg1"  # Nuremberg datacenter
```

---

## 9. Key Concepts Explained

### Kubernetes Service Types

| Type | Description | Use Case |
|------|-------------|----------|
| `ClusterIP` | Internal only | Backend API, databases |
| `NodePort` | External via node IP:port | Development/testing |
| `LoadBalancer` | External via cloud LB | Production |
| `Ingress` | HTTP routing with host/path | Production with domain |

### GitOps with ArgoCD

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Git       │────▶│   ArgoCD     │────▶│  Kubernetes  │
│  Repository  │     │   (watches)  │     │   Cluster    │
└──────────────┘     └──────────────┘     └──────────────┘

1. Developer pushes changes to Git
2. ArgoCD detects changes
3. ArgoCD applies changes to cluster
4. Cluster state matches Git state
```

**Important:** When ArgoCD manages resources, manual `kubectl apply` changes get reverted! Always commit to Git.

### Container Image Tags

| Tag | Description | Use in |
|-----|-------------|--------|
| `latest` | Most recent build | Development |
| `v1.2.3` | Semantic version | Production |
| `abc123` | Git SHA | Traceability |

**Best Practice:** Don't use `latest` in production - it's not reproducible.

### Namespace Isolation

```
┌─────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │   staging       │  │   production    │  │   argocd    │  │
│  │   namespace     │  │   namespace     │  │  namespace  │  │
│  │                 │  │                 │  │             │  │
│  │ - frontend      │  │ - frontend      │  │ - argocd    │  │
│  │ - backend       │  │ - backend       │  │   server    │  │
│  │ - postgresql    │  │ - postgresql    │  │             │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. Troubleshooting Guide

### Problem: ImagePullBackOff

**Diagnosis:**
```bash
kubectl describe pod <pod-name> -n staging | grep -A10 Events
```

**Solutions:**

| Error Message | Cause | Fix |
|---------------|-------|-----|
| `403 Forbidden` | No auth / private repo | Create imagePullSecret |
| `404 Not Found` | Image doesn't exist | Build and push image |
| `manifest unknown` | Wrong tag | Check image tag exists |

### Problem: Pod CrashLoopBackOff

**Diagnosis:**
```bash
kubectl logs <pod-name> -n staging --previous
```

**Common Causes:**
- Application error on startup
- Missing environment variables
- Database connection failure

### Problem: Service Not Accessible

**Diagnosis:**
```bash
# Check service exists
kubectl get svc -n staging

# Check endpoints (should list pod IPs)
kubectl get endpoints <service-name> -n staging

# Check pod is running
kubectl get pods -l app=<app-name> -n staging
```

**Common Causes:**
- Selector mismatch between Service and Pod
- Pod not running
- Wrong port configuration

### Problem: Cannot Connect to Cluster

**Diagnosis:**
```bash
kubectl cluster-info
```

**Solutions:**
1. Check KUBECONFIG path: `echo $KUBECONFIG`
2. Check server address in kubeconfig
3. Check firewall allows port 6443
4. Verify control plane is running

### Useful Debug Commands

```bash
# Get all resources in namespace
kubectl get all -n staging

# Watch pods in real-time
kubectl get pods -n staging -w

# Get pod logs
kubectl logs -f deploy/backend -n staging

# Execute command in pod
kubectl exec -it deploy/backend -n staging -- /bin/sh

# Port forward for local access
kubectl port-forward svc/backend 8080:8080 -n staging

# Check resource usage
kubectl top pods -n staging
```

---

## Quick Reference

### Commands Cheat Sheet

```bash
# Connect to cluster
export KUBECONFIG=~/.kube/config-qawave-staging

# Check status
kubectl get pods -n staging
kubectl get svc -n staging

# View logs
kubectl logs -f deploy/backend -n staging
kubectl logs -f deploy/frontend -n staging

# Restart deployment
kubectl rollout restart deploy/backend -n staging

# Apply changes
kubectl apply -f gitops/envs/staging/backend/

# Create secret
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=USER \
  --docker-password=TOKEN \
  -n staging

# Run E2E tests
./scripts/run-e2e-tests.sh
```

### File Locations

```
qawave/
├── .github/workflows/
│   └── build-and-deploy.yml      # CI/CD pipeline
├── gitops/envs/staging/
│   ├── backend/backend.yaml      # Backend deployment
│   ├── frontend/frontend.yaml    # Frontend deployment
│   └── e2e-tests/                # E2E test job
├── scripts/
│   ├── setup-staging.sh          # Setup script
│   ├── teardown-staging.sh       # Teardown script
│   ├── health-check.sh           # Health check
│   └── run-e2e-tests.sh          # E2E tests
├── e2e-tests/
│   ├── Dockerfile                # E2E test container
│   └── playwright.staging.config.ts
└── docs/
    ├── STAGING_AND_PRODUCTION_ROADMAP.md
    └── E2E_TESTING_STRATEGY.md
```

### URLs

| Service | URL | Notes |
|---------|-----|-------|
| Frontend | http://46.224.232.46:30000 | NodePort |
| ArgoCD | http://46.224.232.46:30080 | GitOps dashboard |
| Backend | Internal only | Use port-forward |

---

## Summary

### What We Did

1. **Diagnosed** the `ImagePullBackOff` issue
2. **Created** `ghcr-secret` for GHCR authentication
3. **Updated** deployments with `imagePullSecrets`
4. **Triggered** CI/CD to build and push images
5. **Verified** pods running and healthy
6. **Ran** E2E tests (identified missing features)
7. **Created** automation scripts
8. **Documented** everything

### Key Learnings

1. **Private registries need authentication** - Always create imagePullSecrets
2. **GitOps means Git is source of truth** - Don't manually `kubectl apply` if ArgoCD manages it
3. **Debug with `kubectl describe`** - Events section shows why things fail
4. **Port-forward for local access** - Access ClusterIP services locally
5. **Kubeconfig connects to cluster** - Fix internal IPs before using

### Next Steps

1. Implement backend API endpoints (see E2E_TESTING_STRATEGY.md)
2. Implement frontend UI components
3. Get E2E tests passing
4. Set up production environment
5. Add TLS/HTTPS with cert-manager
