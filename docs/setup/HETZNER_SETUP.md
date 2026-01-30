# Hetzner Cloud Setup Guide

This guide explains how to set up a Hetzner Cloud account and configure it for QAWave infrastructure.

## Overview

QAWave uses Hetzner Cloud as its infrastructure provider for:
- Virtual Private Servers (VPS)
- Load Balancers
- Private Networks
- Storage Volumes

## Account Setup

### 1. Create Hetzner Account

1. Go to https://console.hetzner.cloud/
2. Click "Register"
3. Complete registration with email verification
4. Add payment method (credit card or PayPal)

### 2. Create a Project

1. Log in to Hetzner Cloud Console
2. Click "New project"
3. Name: `qawave-production` or `qawave-staging`
4. Click "Create project"

### 3. Generate API Token

1. Select your project
2. Go to Security → API Tokens
3. Click "Generate API Token"
4. Name: `qawave-terraform`
5. Permissions: **Read & Write**
6. Copy and securely store the token

> **Security:** Never commit API tokens to version control. Use environment variables or secret management.

## SSH Key Setup

### Generate SSH Key Pair

```bash
# Generate key for staging
ssh-keygen -t ed25519 -f ~/.ssh/qawave-staging -C "qawave-staging"

# Generate key for production
ssh-keygen -t ed25519 -f ~/.ssh/qawave-prod -C "qawave-prod"
```

### Add SSH Key to Hetzner

**Via Console:**
1. Go to Security → SSH Keys
2. Click "Add SSH Key"
3. Paste contents of `~/.ssh/qawave-staging.pub`
4. Name: `qawave-staging`

**Via Terraform** (recommended):
```hcl
resource "hcloud_ssh_key" "main" {
  name       = "qawave-staging"
  public_key = file("~/.ssh/qawave-staging.pub")
}
```

## Network Planning

### IP Address Allocation

| Environment | Private Network | Node Subnet | Notes |
|-------------|-----------------|-------------|-------|
| Staging | 10.1.0.0/16 | 10.1.1.0/24 | Non-overlapping with prod |
| Production | 10.0.0.0/16 | 10.0.1.0/24 | Non-overlapping with staging |

### Firewall Configuration

| Port | Protocol | Purpose | Environments |
|------|----------|---------|--------------|
| 22 | TCP | SSH | Both |
| 6443 | TCP | Kubernetes API | Both |
| 80 | TCP | HTTP | Both |
| 443 | TCP | HTTPS | Both |
| 30000-32767 | TCP | NodePort | Both |

## Server Types Reference

### VPS Types Used

| Type | vCPU | RAM | Disk | Price/month | Use Case |
|------|------|-----|------|-------------|----------|
| CX11 | 1 | 2 GB | 20 GB | ~€3.50 | Staging control plane |
| CX21 | 2 | 4 GB | 40 GB | ~€6.00 | Staging workers, Prod CP |
| CX31 | 4 | 8 GB | 80 GB | ~€10.00 | Production workers |
| CX41 | 8 | 16 GB | 160 GB | ~€18.00 | Heavy workloads |

### Load Balancer Types

| Type | Max Targets | Throughput | Price/month |
|------|-------------|------------|-------------|
| LB11 | 5 | 25 Mbit/s | ~€5.00 |
| LB21 | 25 | 100 Mbit/s | ~€10.00 |

## Locations

### Available Datacenters

| Location | Code | Region | Latency (EU) |
|----------|------|--------|--------------|
| Nuremberg | nbg1 | Germany | Low |
| Falkenstein | fsn1 | Germany | Low |
| Helsinki | hel1 | Finland | Medium |
| Ashburn | ash | US East | High |
| Hillsboro | hil | US West | High |

**Recommended:** `nbg1` (Nuremberg) for EU-focused deployments.

## Terraform Configuration

### Provider Setup

```hcl
# versions.tf
terraform {
  required_version = ">= 1.6"

  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
  }
}

# provider.tf
provider "hcloud" {
  token = var.hcloud_token
}

# variables.tf
variable "hcloud_token" {
  description = "Hetzner Cloud API token"
  type        = string
  sensitive   = true
}
```

### Environment Variables

```bash
# Set token via environment variable
export HCLOUD_TOKEN="your-api-token"

# Or use terraform.tfvars (gitignored)
echo 'hcloud_token = "your-api-token"' > terraform.tfvars
```

### Basic Resources

```hcl
# Network
resource "hcloud_network" "main" {
  name     = "qawave-network"
  ip_range = "10.1.0.0/16"
}

resource "hcloud_network_subnet" "nodes" {
  network_id   = hcloud_network.main.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = "10.1.1.0/24"
}

# Server
resource "hcloud_server" "node" {
  name        = "qawave-node"
  server_type = "cx21"
  image       = "ubuntu-22.04"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.main.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.10"
  }
}
```

## Cost Management

### Estimated Monthly Costs

| Environment | Resources | Estimated Cost |
|-------------|-----------|----------------|
| Staging | 1 CX11 + 2 CX21 + LB11 | ~€17 |
| Production | 1 CX21 + 3 CX31 + LB21 | ~€46 |
| **Total** | | **~€63** |

### Cost Optimization Tips

1. **Use smaller instances** for non-critical workloads
2. **Delete unused resources** (volumes, snapshots, IPs)
3. **Use reserved IPs** only when needed
4. **Monitor usage** in Hetzner Console

### Budget Alerts

Set up budget alerts in Hetzner Console:
1. Go to Billing → Budget
2. Set monthly budget limit
3. Configure email notifications

## Troubleshooting

### Cannot Connect via SSH

```bash
# Check server status
hcloud server list

# Verify SSH key fingerprint
ssh-keygen -lf ~/.ssh/qawave-staging.pub

# Try verbose connection
ssh -vvv -i ~/.ssh/qawave-staging root@<ip>
```

### Terraform Errors

```bash
# Refresh state
terraform refresh

# Check provider version
terraform providers

# Debug API calls
export TF_LOG=DEBUG
terraform plan
```

### API Token Issues

- Verify token has Read & Write permissions
- Check token is for correct project
- Regenerate token if compromised

## Security Best Practices

1. **Rotate API tokens** every 90 days
2. **Use separate tokens** per environment
3. **Never commit tokens** to Git
4. **Enable MFA** on Hetzner account
5. **Limit SSH access** by IP when possible
6. **Regular audit** of active resources

## Related Documentation

- [Terraform Guide](./TERRAFORM_GUIDE.md)
- [Staging Environment](../environments/STAGING.md)
- [Production Environment](../environments/PRODUCTION.md)
- [Hetzner Official Docs](https://docs.hetzner.com/)
