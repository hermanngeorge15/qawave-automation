# QAWave Staging Environment

Terraform configuration for the QAWave staging environment on Hetzner Cloud.

## Overview

| Resource | Type | Specs |
|----------|------|-------|
| Control Plane | CX11 | 1 vCPU, 2GB RAM |
| Worker | CX21 | 2 vCPU, 4GB RAM |
| Load Balancer | LB11 | Standard |
| PostgreSQL Volume | 20GB | ext4 |
| Redis Volume | 10GB | ext4 |

**Estimated Monthly Cost:** ~17

## Prerequisites

1. [Terraform](https://terraform.io/downloads) >= 1.6
2. [Hetzner Cloud Account](https://console.hetzner.cloud)
3. SSH key pair

## Quick Start

1. **Copy example variables:**
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   ```

2. **Edit terraform.tfvars** with your Hetzner API token

3. **Initialize Terraform:**
   ```bash
   terraform init
   ```

4. **Review the plan:**
   ```bash
   terraform plan
   ```

5. **Apply (creates resources):**
   ```bash
   terraform apply
   ```

## Outputs

After applying, Terraform outputs:

- `control_plane_ip` - Public IP of control plane
- `worker_ip` - Public IP of worker
- `load_balancer_ip` - Point DNS here
- `ssh_commands` - SSH commands to connect
- `kubeconfig_command` - Get kubeconfig

## DNS Setup

Point these records to `load_balancer_ip`:

| Record | Type |
|--------|------|
| staging.qawave.io | A |
| api.staging.qawave.io | A |
| argocd.staging.qawave.io | A |

## Network

Staging uses a separate network from production:

- **Staging:** 10.1.0.0/16
- **Production:** 10.0.0.0/16

## SSH Access

```bash
# Control plane
ssh root@<control_plane_ip>

# Worker
ssh root@<worker_ip>
```

## Kubernetes Access

```bash
# Get kubeconfig
ssh root@<control_plane_ip> 'k0s kubeconfig admin' > ~/.kube/qawave-staging.conf

# Use it
export KUBECONFIG=~/.kube/qawave-staging.conf
kubectl get nodes
```

## Destroy

To tear down the staging environment:

```bash
terraform destroy
```

**Warning:** This will delete all resources and data!

## Files

| File | Purpose |
|------|---------|
| main.tf | Resource definitions |
| variables.tf | Input variables |
| outputs.tf | Output values |
| terraform.tfvars.example | Example configuration |
