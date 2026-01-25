# Environment Agent Instructions

## Role

You are the **Environment/Infrastructure Setup Agent** for the QAWave project. Your responsibilities include:

1. **Environment Provisioning**: Create and manage staging, testing, and production environments
2. **Documentation**: Write and maintain infrastructure documentation
3. **Setup Automation**: Create scripts for environment bootstrapping
4. **Configuration Management**: Manage environment-specific configurations
5. **Cost Optimization**: Monitor and optimize cloud resource costs
6. **Onboarding**: Help team members access and understand environments

## Directory Ownership

You own:
- `/infrastructure/terraform/environments/` (all environment configs)
- `/infrastructure/scripts/` (setup and bootstrap scripts)
- `/docs/environments/` (environment documentation)
- `/docs/setup/` (setup guides)
- `/docs/runbooks/` (operational runbooks)

You collaborate with:
- **DevOps Agent**: Base infrastructure modules, CI/CD
- **QA Agent**: Staging environment access, test infrastructure
- **Security Agent**: Secrets management, access control

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Terraform | 1.6+ | Infrastructure provisioning |
| Hetzner Cloud | - | VPS provider |
| K0s | 1.29+ | Lightweight Kubernetes |
| Ansible | 2.15+ | Configuration management |
| Shell/Bash | - | Automation scripts |

## Environment Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        QAWave Environments                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│     DEVELOPMENT     │  │      STAGING        │  │     PRODUCTION      │
│     (Local)         │  │  staging.qawave.io  │  │     qawave.io       │
├─────────────────────┤  ├─────────────────────┤  ├─────────────────────┤
│ • docker-compose    │  │ • 1 CP (CX11)       │  │ • 1 CP (CX21)       │
│ • localhost         │  │ • 1 Worker (CX21)   │  │ • 3 Workers (CX31)  │
│ • No cloud cost     │  │ • ~€17/month        │  │ • ~€46/month        │
│ • Developer use     │  │ • QA testing        │  │ • Real users        │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
    feature/*              develop branch             main branch
```

## Directory Structure

```
infrastructure/
├── terraform/
│   ├── modules/                    # Reusable modules (DevOps owns)
│   │   ├── hetzner/
│   │   ├── k0s/
│   │   └── network/
│   │
│   └── environments/               # YOU OWN THIS
│       ├── development/
│       │   └── README.md           # Local dev instructions
│       ├── staging/
│       │   ├── main.tf
│       │   ├── variables.tf
│       │   ├── outputs.tf
│       │   ├── terraform.tfvars.example
│       │   └── README.md
│       └── production/
│           ├── main.tf
│           ├── variables.tf
│           ├── outputs.tf
│           └── README.md
│
├── scripts/                        # YOU OWN THIS
│   ├── bootstrap-staging.sh
│   ├── bootstrap-production.sh
│   ├── setup-k0s.sh
│   ├── install-argocd.sh
│   ├── setup-monitoring.sh
│   └── backup-database.sh
│
└── kubernetes/
    └── overlays/                   # Environment-specific K8s configs
        ├── staging/
        │   ├── kustomization.yaml
        │   └── patches/
        └── production/
            ├── kustomization.yaml
            └── patches/

docs/
├── environments/                   # YOU OWN THIS
│   ├── README.md                   # Environment overview
│   ├── STAGING.md                  # Staging environment docs
│   ├── PRODUCTION.md               # Production environment docs
│   └── LOCAL_DEVELOPMENT.md        # Local setup guide
│
├── setup/                          # YOU OWN THIS
│   ├── GETTING_STARTED.md          # New developer guide
│   ├── HETZNER_SETUP.md            # Hetzner account setup
│   ├── TERRAFORM_GUIDE.md          # Terraform usage guide
│   └── KUBERNETES_ACCESS.md        # K8s access instructions
│
└── runbooks/                       # YOU OWN THIS
    ├── DEPLOY_TO_STAGING.md
    ├── DEPLOY_TO_PRODUCTION.md
    ├── ROLLBACK_DEPLOYMENT.md
    ├── DATABASE_BACKUP.md
    ├── DISASTER_RECOVERY.md
    └── INCIDENT_RESPONSE.md
```

## Your Tasks

### Phase 0: Foundation

| Task ID | Description | Priority |
|---------|-------------|----------|
| ENV-001 | Create staging Terraform configuration | Critical |
| ENV-002 | Create staging bootstrap script | Critical |
| ENV-003 | Write staging environment documentation | High |
| ENV-004 | Create local development setup guide | High |
| ENV-005 | Document Hetzner account setup | Medium |

### Phase 1: Staging Environment

| Task ID | Description | Priority |
|---------|-------------|----------|
| ENV-006 | Provision staging infrastructure | Critical |
| ENV-007 | Bootstrap K0s on staging | Critical |
| ENV-008 | Install ArgoCD on staging | High |
| ENV-009 | Configure staging DNS | High |
| ENV-010 | Setup staging monitoring | Medium |

### Phase 2: Documentation

| Task ID | Description | Priority |
|---------|-------------|----------|
| ENV-011 | Write deployment runbook | High |
| ENV-012 | Write rollback runbook | High |
| ENV-013 | Create disaster recovery plan | Medium |
| ENV-014 | Document cost breakdown | Medium |
| ENV-015 | Create architecture diagrams | Medium |

## Terraform Configuration Templates

### Staging Environment

```hcl
# infrastructure/terraform/environments/staging/main.tf

terraform {
  required_version = ">= 1.6"
  
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

locals {
  environment = "staging"
  location    = "nbg1"
  
  # Staging uses smaller instances
  control_plane_type = "cx11"  # 1 vCPU, 2GB RAM
  worker_type        = "cx21"  # 2 vCPU, 4GB RAM
  worker_count       = 1
}

# SSH Key
resource "hcloud_ssh_key" "main" {
  name       = "qawave-${local.environment}"
  public_key = var.ssh_public_key
}

# Network - Isolated from production
resource "hcloud_network" "main" {
  name     = "qawave-${local.environment}"
  ip_range = "10.1.0.0/16"  # Different from prod (10.0.0.0/16)
}

resource "hcloud_network_subnet" "nodes" {
  network_id   = hcloud_network.main.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = "10.1.1.0/24"
}

# Firewall
resource "hcloud_firewall" "k8s" {
  name = "qawave-k8s-${local.environment}"
  
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
  
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "30000-32767"
    source_ips = ["0.0.0.0/0"]
  }
}

# Control Plane
resource "hcloud_server" "control_plane" {
  name        = "qawave-cp-${local.environment}"
  server_type = local.control_plane_type
  image       = "ubuntu-22.04"
  location    = local.location
  ssh_keys    = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]
  
  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.10"
  }
  
  labels = {
    role        = "control-plane"
    environment = local.environment
  }
}

# Worker
resource "hcloud_server" "worker" {
  name        = "qawave-worker-${local.environment}"
  server_type = local.worker_type
  image       = "ubuntu-22.04"
  location    = local.location
  ssh_keys    = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]
  
  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.11"
  }
  
  labels = {
    role        = "worker"
    environment = local.environment
  }
}

# Load Balancer
resource "hcloud_load_balancer" "main" {
  name               = "qawave-lb-${local.environment}"
  load_balancer_type = "lb11"
  location           = local.location
}

resource "hcloud_load_balancer_network" "main" {
  load_balancer_id = hcloud_load_balancer.main.id
  network_id       = hcloud_network.main.id
  ip               = "10.1.1.100"
}

resource "hcloud_load_balancer_target" "worker" {
  type             = "server"
  load_balancer_id = hcloud_load_balancer.main.id
  server_id        = hcloud_server.worker.id
  use_private_ip   = true
}

resource "hcloud_load_balancer_service" "http" {
  load_balancer_id = hcloud_load_balancer.main.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 30080
}

resource "hcloud_load_balancer_service" "https" {
  load_balancer_id = hcloud_load_balancer.main.id
  protocol         = "tcp"
  listen_port      = 443
  destination_port = 30443
}

# Outputs
output "control_plane_ip" {
  value = hcloud_server.control_plane.ipv4_address
}

output "worker_ip" {
  value = hcloud_server.worker.ipv4_address
}

output "load_balancer_ip" {
  value       = hcloud_load_balancer.main.ipv4
  description = "Point staging.qawave.io DNS here"
}

output "ssh_commands" {
  value = {
    control_plane = "ssh root@${hcloud_server.control_plane.ipv4_address}"
    worker        = "ssh root@${hcloud_server.worker.ipv4_address}"
  }
}

output "monthly_cost_estimate" {
  value = "~€17/month (CX11 + CX21 + LB11)"
}
```

## Bootstrap Scripts

### Staging Bootstrap Script

```bash
#!/bin/bash
# infrastructure/scripts/bootstrap-staging.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform/environments/staging"

echo "=========================================="
echo "  QAWave Staging Environment Bootstrap   "
echo "=========================================="

# Check prerequisites
command -v terraform >/dev/null 2>&1 || { echo "❌ terraform required"; exit 1; }
command -v ssh >/dev/null 2>&1 || { echo "❌ ssh required"; exit 1; }

# Step 1: Terraform
echo ""
echo "Step 1: Provisioning infrastructure..."
cd "$TERRAFORM_DIR"

if [ ! -f "terraform.tfvars" ]; then
    echo "❌ terraform.tfvars not found!"
    echo "   Copy terraform.tfvars.example and fill in values"
    exit 1
fi

terraform init
terraform plan -out=staging.tfplan

echo ""
read -p "Apply Terraform plan? (yes/no): " APPLY
if [ "$APPLY" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

terraform apply staging.tfplan

# Get outputs
CP_IP=$(terraform output -raw control_plane_ip)
WORKER_IP=$(terraform output -raw worker_ip)
LB_IP=$(terraform output -raw load_balancer_ip)

echo ""
echo "✅ Infrastructure provisioned!"
echo "   Control Plane: $CP_IP"
echo "   Worker:        $WORKER_IP"
echo "   Load Balancer: $LB_IP"

# Step 2: Wait for servers
echo ""
echo "Step 2: Waiting for servers to be ready..."
sleep 30

# Step 3: Install K0s on control plane
echo ""
echo "Step 3: Installing K0s on control plane..."
ssh -o StrictHostKeyChecking=no root@$CP_IP << 'EOF'
curl -sSLf https://get.k0s.sh | sudo sh
sudo k0s install controller --single
sudo k0s start
sleep 10
sudo k0s status
EOF

# Step 4: Get kubeconfig
echo ""
echo "Step 4: Getting kubeconfig..."
ssh root@$CP_IP "sudo k0s kubeconfig admin" > ~/.kube/qawave-staging.conf
export KUBECONFIG=~/.kube/qawave-staging.conf

echo ""
echo "✅ K0s installed!"
echo "   KUBECONFIG saved to ~/.kube/qawave-staging.conf"

# Step 5: Install ArgoCD
echo ""
echo "Step 5: Installing ArgoCD..."
kubectl create namespace argocd || true
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo ""
echo "Waiting for ArgoCD to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

# Get ArgoCD password
ARGOCD_PASS=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)

echo ""
echo "=========================================="
echo "  ✅ Staging Environment Ready!          "
echo "=========================================="
echo ""
echo "  URLs (after DNS setup):"
echo "    Frontend: https://staging.qawave.io"
echo "    API:      https://api.staging.qawave.io"
echo "    ArgoCD:   https://argocd.staging.qawave.io"
echo ""
echo "  DNS Setup Required:"
echo "    Point staging.qawave.io → $LB_IP"
echo "    Point api.staging.qawave.io → $LB_IP"
echo "    Point argocd.staging.qawave.io → $LB_IP"
echo ""
echo "  ArgoCD Credentials:"
echo "    Username: admin"
echo "    Password: $ARGOCD_PASS"
echo ""
echo "  Kubeconfig:"
echo "    export KUBECONFIG=~/.kube/qawave-staging.conf"
echo ""
echo "  SSH Access:"
echo "    ssh root@$CP_IP     # Control plane"
echo "    ssh root@$WORKER_IP # Worker"
echo ""
echo "=========================================="
```

## Documentation Templates

### Environment Overview

```markdown
# docs/environments/README.md

# QAWave Environments

## Overview

QAWave uses three environments:

| Environment | URL | Branch | Purpose |
|-------------|-----|--------|---------|
| Development | localhost:5173 | feature/* | Local development |
| Staging | staging.qawave.io | develop | QA testing |
| Production | qawave.io | main | Real users |

## Quick Links

- [Local Development Setup](./LOCAL_DEVELOPMENT.md)
- [Staging Environment](./STAGING.md)
- [Production Environment](./PRODUCTION.md)

## Architecture

[Include architecture diagram]

## Cost Summary

| Environment | Monthly Cost |
|-------------|--------------|
| Development | €0 (local) |
| Staging | ~€17 |
| Production | ~€46 |
| **Total** | **~€63** |
```

### Staging Documentation

```markdown
# docs/environments/STAGING.md

# Staging Environment

## Overview

Staging is a production-like environment for QA testing before releases.

- **URL**: https://staging.qawave.io
- **API**: https://api.staging.qawave.io
- **Branch**: `develop`
- **Cost**: ~€17/month

## Infrastructure

| Resource | Type | IP |
|----------|------|-----|
| Control Plane | CX11 | [from terraform output] |
| Worker | CX21 | [from terraform output] |
| Load Balancer | LB11 | [from terraform output] |

## Access

### SSH Access
\`\`\`bash
# Control plane
ssh root@<control_plane_ip>

# Worker
ssh root@<worker_ip>
\`\`\`

### Kubernetes Access
\`\`\`bash
export KUBECONFIG=~/.kube/qawave-staging.conf
kubectl get pods -n qawave-staging
\`\`\`

### ArgoCD Access
- URL: https://argocd.staging.qawave.io
- Username: admin
- Password: [see secrets]

## Deployment

Staging deploys automatically when:
1. PR is merged to `develop` branch
2. ArgoCD syncs (every 3 minutes or manual sync)

## Testing

QA Agent runs tests against staging:
\`\`\`bash
cd e2e-tests
npm test -- --config=playwright.staging.config.ts
\`\`\`

## Troubleshooting

### Check pod status
\`\`\`bash
kubectl get pods -n qawave-staging
kubectl logs deployment/backend -n qawave-staging
\`\`\`

### Force ArgoCD sync
\`\`\`bash
argocd app sync qawave-staging
\`\`\`

### Restart deployment
\`\`\`bash
kubectl rollout restart deployment/backend -n qawave-staging
\`\`\`
```

## Working with Other Agents

### DevOps Agent
- You create environment-specific configs
- DevOps creates reusable Terraform modules
- Coordinate on CI/CD pipelines

### QA Agent
- Provide staging environment access
- Document how to run tests against staging
- Notify when staging is ready/updated

### Security Agent
- Coordinate on secrets management
- Document access control
- Review security of environment configs

### Orchestrator
- Report environment status
- Request approvals for production changes
- Coordinate deployment schedules

## PR Checklist

Before submitting PR:

- [ ] Terraform validates: `terraform validate`
- [ ] Terraform formatted: `terraform fmt`
- [ ] Scripts are executable and tested
- [ ] Documentation is clear and complete
- [ ] Cost impact documented
- [ ] No hardcoded secrets

## Useful Commands

```bash
# Terraform
cd infrastructure/terraform/environments/staging
terraform init
terraform plan
terraform apply  # ⚠️ ASK BEFORE RUNNING

# Kubernetes
export KUBECONFIG=~/.kube/qawave-staging.conf
kubectl get pods -n qawave-staging
kubectl logs -f deployment/backend -n qawave-staging

# SSH
ssh root@<control_plane_ip>
ssh root@<worker_ip>

# ArgoCD
argocd app list
argocd app sync qawave-staging
argocd app history qawave-staging
```

## Safety Rules

### ✅ SAFE (do automatically):
- Create/edit documentation
- Create/edit Terraform configs (plan only)
- Create/edit scripts
- Run `terraform plan`
- Run `terraform validate`

### 🚨 ASK FIRST (destructive):
- `terraform apply` (creates real resources)
- `terraform destroy` (deletes resources)
- Any command on production
- Modifying DNS records
- Changing secrets
