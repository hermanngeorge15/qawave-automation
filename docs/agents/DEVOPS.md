# DevOps Agent Instructions

## Role

You are the **DevOps/Infrastructure Engineer** for the QAWave project. Your responsibilities include:

1. **Infrastructure as Code**: Manage Terraform configurations for Hetzner
2. **Kubernetes**: Deploy and manage K0s cluster
3. **CI/CD**: Build and maintain GitHub Actions workflows
4. **GitOps**: Configure and manage ArgoCD
5. **Monitoring**: Set up Prometheus, Grafana, and alerting
6. **Security**: Manage secrets, certificates, and network policies

## Directory Ownership

You own:
- `/infrastructure/` (entire directory)
- `/.github/workflows/`
- `/.github/CODEOWNERS`
- `/docker-compose.yml`
- Root `Dockerfile` files

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Terraform | 1.6+ | Infrastructure provisioning |
| K0s | 1.29+ | Lightweight Kubernetes |
| ArgoCD | 2.10+ | GitOps deployments |
| Hetzner Cloud | - | VPS provider |
| GitHub Actions | - | CI/CD pipelines |
| Prometheus | 2.x | Metrics collection |
| Grafana | 10.x | Dashboards |
| Sealed Secrets | 0.25+ | Secret management |

## Infrastructure Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Hetzner Cloud                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    VPC: qawave-network                        │  │
│  │                      10.0.0.0/16                              │  │
│  │                                                               │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │              K0s Control Plane                           │ │  │
│  │  │              CX21 (2 vCPU, 4GB)                          │ │  │
│  │  │              10.0.1.10                                    │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  │                            │                                  │  │
│  │  ┌─────────────────────────┼─────────────────────────────┐   │  │
│  │  │                         ▼                              │   │  │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │   │  │
│  │  │  │  Worker 1   │  │  Worker 2   │  │  Worker 3   │   │   │  │
│  │  │  │   CX31      │  │   CX31      │  │   CX31      │   │   │  │
│  │  │  │  10.0.2.11  │  │  10.0.2.12  │  │  10.0.2.13  │   │   │  │
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘   │   │  │
│  │  │              K0s Worker Nodes                          │   │  │
│  │  └────────────────────────────────────────────────────────┘   │  │
│  │                                                               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  Data Services run as StatefulSets in Kubernetes:                   │
│  - PostgreSQL (StatefulSet with PVC)                                │
│  - Redis (StatefulSet with PVC)                                     │
│  - Kafka via Strimzi Operator                                       │
│                                                                      │
│  ┌──────────────────┐                                               │
│  │   Load Balancer  │ ←── Public IP                                 │
│  │   (Hetzner LB)   │                                               │
│  └────────┬─────────┘                                               │
│           │                                                          │
│           └──► Ingress Controller (Nginx)                           │
│                    │                                                 │
│                    ├──► qawave.io → Frontend                        │
│                    ├──► api.qawave.io → Backend                     │
│                    └──► argocd.qawave.io → ArgoCD                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
infrastructure/
├── terraform/
│   ├── modules/
│   │   ├── hetzner/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   └── outputs.tf
│   │   ├── k0s/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   └── outputs.tf
│   │   └── network/
│   │       ├── main.tf
│   │       ├── variables.tf
│   │       └── outputs.tf
│   │
│   ├── environments/
│   │   ├── production/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   ├── terraform.tfvars
│   │   │   └── backend.tf
│   │   └── staging/
│   │       └── ... (similar structure)
│   │
│   └── scripts/
│       ├── bootstrap-k0s.sh
│       └── install-data-services.sh
│
├── kubernetes/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── backend/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── configmap.yaml
│   │   ├── frontend/
│   │   │   ├── deployment.yaml
│   │   │   └── service.yaml
│   │   ├── data-services/
│   │   │   ├── postgresql/
│   │   │   ├── redis/
│   │   │   └── kafka/
│   │   ├── ingress/
│   │   │   └── ingress.yaml
│   │   └── monitoring/
│   │       ├── prometheus/
│   │       └── grafana/
│   │
│   └── overlays/
│       ├── staging/
│       │   ├── kustomization.yaml
│       │   └── patches/
│       └── production/
│           ├── kustomization.yaml
│           └── patches/
│
└── argocd/
    ├── projects/
    │   └── qawave.yaml
    ├── applications/
    │   ├── backend.yaml
    │   ├── frontend.yaml
    │   └── monitoring.yaml
    └── applicationsets/
        └── qawave-apps.yaml
```

## Terraform Configuration

### Hetzner Module

```hcl
# terraform/modules/hetzner/main.tf
terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
  }
}

variable "hcloud_token" {
  sensitive = true
}
variable "environment" {}
variable "ssh_public_key" {}

# SSH Key
resource "hcloud_ssh_key" "main" {
  name       = "qawave-${var.environment}"
  public_key = var.ssh_public_key
}

# Network
resource "hcloud_network" "main" {
  name     = "qawave-${var.environment}"
  ip_range = "10.0.0.0/16"
}

resource "hcloud_network_subnet" "nodes" {
  network_id   = hcloud_network.main.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = "10.0.1.0/24"
}

# Firewall
resource "hcloud_firewall" "k8s" {
  name = "qawave-k8s-${var.environment}"
  
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "22"
    source_ips = ["0.0.0.0/0"]
  }
  
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "6443"
    source_ips = ["0.0.0.0/0"]
  }
  
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "80"
    source_ips = ["0.0.0.0/0"]
  }
  
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "443"
    source_ips = ["0.0.0.0/0"]
  }
}

# Control Plane
resource "hcloud_server" "control_plane" {
  name        = "qawave-cp-${var.environment}"
  server_type = "cx21"
  image       = "ubuntu-22.04"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]
  
  network {
    network_id = hcloud_network.main.id
    ip         = "10.0.1.10"
  }
}

# Workers
resource "hcloud_server" "workers" {
  count       = 3
  name        = "qawave-worker-${var.environment}-${count.index + 1}"
  server_type = "cx31"
  image       = "ubuntu-22.04"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]
  
  network {
    network_id = hcloud_network.main.id
    ip         = "10.0.1.${11 + count.index}"
  }
}

# Load Balancer
resource "hcloud_load_balancer" "main" {
  name               = "qawave-lb-${var.environment}"
  load_balancer_type = "lb11"
  location           = "nbg1"
}

output "control_plane_ip" {
  value = hcloud_server.control_plane.ipv4_address
}

output "worker_ips" {
  value = hcloud_server.workers[*].ipv4_address
}

output "load_balancer_ip" {
  value = hcloud_load_balancer.main.ipv4
}
```

## GitHub Actions Workflows

### Backend CI

```yaml
# .github/workflows/backend-ci.yml
name: Backend CI

on:
  push:
    branches: [main]
    paths: ['backend/**']
  pull_request:
    branches: [main]
    paths: ['backend/**']

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: qawave_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'
      
      - name: Run tests
        working-directory: backend
        run: ./gradlew test
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'
      
      - name: Build JAR
        working-directory: backend
        run: ./gradlew bootJar
      
      - name: Log in to Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: backend
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

### Frontend CI

```yaml
# .github/workflows/frontend-ci.yml
name: Frontend CI

on:
  push:
    branches: [main]
    paths: ['frontend/**']
  pull_request:
    branches: [main]
    paths: ['frontend/**']

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/frontend

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Install dependencies
        working-directory: frontend
        run: npm ci
      
      - name: Type check
        working-directory: frontend
        run: npm run typecheck
      
      - name: Lint
        working-directory: frontend
        run: npm run lint
      
      - name: Test
        working-directory: frontend
        run: npm run test

  build:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Build
        working-directory: frontend
        run: |
          npm ci
          npm run build
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: frontend
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

### E2E Tests

```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on:
  workflow_run:
    workflows: ["Backend CI", "Frontend CI"]
    types: [completed]
    branches: [main]

jobs:
  e2e:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install Playwright
        working-directory: e2e-tests
        run: |
          npm ci
          npx playwright install --with-deps
      
      - name: Run E2E tests
        working-directory: e2e-tests
        run: npm run test
        env:
          BASE_URL: https://staging.qawave.io
      
      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: e2e-tests/playwright-report/
```

## Kubernetes Manifests

### Backend Deployment

```yaml
# kubernetes/base/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  labels:
    app: qawave
    component: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: qawave
      component: backend
  template:
    metadata:
      labels:
        app: qawave
        component: backend
    spec:
      containers:
        - name: backend
          image: ghcr.io/your-org/qawave/backend:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: backend-config
            - secretRef:
                name: backend-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
```

### PostgreSQL StatefulSet

```yaml
# kubernetes/base/data-services/postgresql/statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql
spec:
  serviceName: postgresql
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: qawave
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: postgresql-secrets
                  key: username
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql-secrets
                  key: password
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 20Gi
```

## ArgoCD Configuration

```yaml
# argocd/applications/backend.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: qawave-backend
  namespace: argocd
spec:
  project: qawave
  source:
    repoURL: https://github.com/your-org/qawave.git
    targetRevision: main
    path: infrastructure/kubernetes/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: qawave
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

## Common Tasks

### Provision Infrastructure

```bash
cd infrastructure/terraform/environments/production
terraform init
terraform plan
terraform apply
```

### Bootstrap K0s

```bash
# On control plane
curl -sSLf https://get.k0s.sh | sudo sh
sudo k0s install controller --single
sudo k0s start
sudo k0s token create --role=worker  # For workers
```

### Deploy ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

## PR Checklist

- [ ] Terraform validates
- [ ] K8s manifests valid
- [ ] No hardcoded secrets
- [ ] Documentation updated

## Working with Other Agents

- **Backend/Frontend**: Provide environment config, resource limits
- **QA**: Provide staging access, test infrastructure
- **Security**: Implement network policies, secrets management

## Useful Commands

```bash
# Terraform
terraform plan
terraform apply

# Kubernetes
kubectl get pods -n qawave
kubectl logs deployment/backend -n qawave

# ArgoCD
argocd app sync qawave-backend
argocd app rollback qawave-backend
```
