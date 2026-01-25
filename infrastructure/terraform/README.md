# QAWave Terraform Infrastructure

Terraform configurations for provisioning QAWave infrastructure on Hetzner Cloud.

## Prerequisites

- Terraform >= 1.6.0
- Hetzner Cloud API token
- SSH key pair
- S3-compatible storage for state (e.g., Backblaze B2, Wasabi, AWS S3)

## Directory Structure

```
terraform/
├── environments/
│   ├── production/        # Production environment
│   │   └── main.tf        # Production infrastructure config
│   └── staging/           # Staging environment (future)
├── modules/               # Reusable modules (future)
└── README.md              # This file
```

## Infrastructure Overview

The Terraform configuration provisions:

| Resource | Type | Description |
|----------|------|-------------|
| VPC Network | `hcloud_network` | Private network (10.0.0.0/16) |
| Control Plane | `hcloud_server` | K0s control plane (CX21: 2 vCPU, 4GB) |
| Worker Nodes | `hcloud_server` | K0s workers x3 (CX31: 2 vCPU, 8GB) |
| Load Balancer | `hcloud_load_balancer` | Hetzner LB11 for ingress |
| Firewall | `hcloud_firewall` | K8s ports, SSH, HTTP/HTTPS |
| Volumes | `hcloud_volume` | Persistent storage for PostgreSQL, Redis, Kafka |

## Quick Start

### 1. Configure Backend (First Time Only)

Create S3-compatible bucket for Terraform state:

```bash
# Example with AWS CLI (or compatible)
aws s3 mb s3://qawave-terraform-state --region eu-central-1
```

### 2. Set Environment Variables

```bash
export HCLOUD_TOKEN="your-hetzner-api-token"
export AWS_ACCESS_KEY_ID="your-s3-access-key"
export AWS_SECRET_ACCESS_KEY="your-s3-secret-key"
```

### 3. Initialize Terraform

```bash
cd infrastructure/terraform/environments/production
terraform init
```

### 4. Plan Changes

```bash
terraform plan -var="hcloud_token=$HCLOUD_TOKEN"
```

### 5. Apply Changes

```bash
terraform apply -var="hcloud_token=$HCLOUD_TOKEN"
```

## Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `hcloud_token` | Hetzner Cloud API Token | (required) |
| `environment` | Environment name | `production` |
| `location` | Hetzner datacenter | `nbg1` (Nuremberg) |
| `ssh_public_key_path` | Path to SSH public key | `~/.ssh/id_rsa.pub` |
| `control_plane_type` | Server type for control plane | `cx21` |
| `worker_type` | Server type for workers | `cx31` |
| `worker_count` | Number of worker nodes | `3` |

## Outputs

After applying, Terraform outputs:

| Output | Description |
|--------|-------------|
| `control_plane_ip` | Public IP of control plane |
| `worker_ips` | Public IPs of all workers |
| `load_balancer_ip` | Public IP of load balancer |
| `ssh_command` | SSH command to connect to control plane |
| `kubeconfig_command` | Command to retrieve kubeconfig |

## Network Architecture

```
Internet
    │
    ▼
┌─────────────────────────────────────┐
│   Load Balancer (Hetzner LB11)      │
│   Public IP                         │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │  VPC: 10.0.0.0/16   │
    │                      │
    │  ┌────────────────┐ │
    │  │ Control Plane  │ │
    │  │ 10.0.1.10      │ │
    │  └────────────────┘ │
    │                      │
    │  ┌────────┬────────┐│
    │  │Worker 1│Worker 2││
    │  │.11     │.12     ││
    │  └────────┴────────┘│
    │       Worker 3      │
    │       .13           │
    └─────────────────────┘
```

## Firewall Rules

| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22 | TCP | 0.0.0.0/0 | SSH access |
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |
| 6443 | TCP | 0.0.0.0/0 | Kubernetes API |
| 30000-32767 | TCP | 0.0.0.0/0 | NodePort services |
| any | TCP/UDP | 10.0.0.0/16 | Internal cluster |

## State Management

Terraform state is stored remotely in S3-compatible storage:

```hcl
backend "s3" {
  bucket = "qawave-terraform-state"
  key    = "production/terraform.tfstate"
  region = "eu-central-1"
}
```

## Security Notes

1. **Never commit `terraform.tfvars` with secrets** - Use environment variables
2. **Restrict SSH access** - Consider limiting source IPs in production
3. **Rotate API tokens** - Regularly rotate Hetzner Cloud tokens
4. **Enable state encryption** - Use S3 server-side encryption for state files

## Troubleshooting

### Cannot connect to control plane

```bash
# Check server status
hcloud server list

# Check firewall rules
hcloud firewall describe qawave-k8s-production
```

### Terraform state lock

```bash
# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

## Related Documentation

- [Hetzner Cloud Provider](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs)
- [K0s Installation Guide](../kubernetes/README.md)
- [DevOps Agent Instructions](../../docs/agents/DEVOPS.md)
