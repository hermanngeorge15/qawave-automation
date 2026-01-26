# QAWave Infrastructure Scripts

Bootstrap and utility scripts for managing QAWave environments.

## Scripts Overview

| Script | Purpose |
|--------|---------|
| `bootstrap-staging.sh` | Full staging environment setup |
| `bootstrap-production.sh` | Full production environment setup |
| `setup-k0s.sh` | K0s cluster installation |
| `install-argocd.sh` | ArgoCD installation |
| `destroy-environment.sh` | Environment teardown |

## Quick Start

### Staging Environment

```bash
# 1. Configure terraform
cd ../terraform/environments/staging
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your Hetzner token

# 2. Run bootstrap
cd ../../scripts
./bootstrap-staging.sh
```

### Production Environment

```bash
# 1. Configure terraform
cd ../terraform/environments/production
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your Hetzner token

# 2. Run bootstrap (requires confirmation)
cd ../../scripts
./bootstrap-production.sh
```

## Individual Scripts

### bootstrap-staging.sh

Full automated setup of staging environment:
1. Provisions Hetzner infrastructure via Terraform
2. Waits for servers to be ready
3. Installs K0s Kubernetes
4. Configures kubeconfig
5. Installs ArgoCD

**Prerequisites:**
- terraform >= 1.6
- ssh with key configured
- kubectl
- terraform.tfvars configured

**Usage:**
```bash
./bootstrap-staging.sh
```

### bootstrap-production.sh

Same as staging but for production. Includes additional safety confirmations.

**Usage:**
```bash
./bootstrap-production.sh
# Type 'production' to confirm
```

### setup-k0s.sh

Standalone K0s installation script.

**Usage:**
```bash
# Single-node cluster
./setup-k0s.sh <server-ip> --controller --single

# Controller only
./setup-k0s.sh <server-ip> --controller

# Add worker
./setup-k0s.sh <worker-ip> --worker --controller-ip <controller-ip>
```

### install-argocd.sh

Standalone ArgoCD installation.

**Usage:**
```bash
# Standard installation
./install-argocd.sh

# HA installation for production
./install-argocd.sh --ha

# With specific kubeconfig
./install-argocd.sh --kubeconfig ~/.kube/qawave-staging.conf
```

### destroy-environment.sh

Tears down an environment completely.

**Usage:**
```bash
# Destroy staging
./destroy-environment.sh staging

# Destroy production (requires typing 'destroy production')
./destroy-environment.sh production
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KUBECONFIG` | Path to kubeconfig | `~/.kube/config` |

## Output Locations

| Environment | Kubeconfig Path |
|-------------|-----------------|
| Staging | `~/.kube/qawave-staging.conf` |
| Production | `~/.kube/qawave-production.conf` |

## Safety Features

- All destructive scripts require explicit confirmation
- Production scripts require typing specific phrases
- Terraform plans shown before apply
- Colored output for warnings and errors

## Troubleshooting

### SSH Connection Failed

```bash
# Check SSH key
ssh-add -l

# Test connection
ssh -v root@<server-ip>
```

### Terraform Init Failed

```bash
# Clear cache and retry
rm -rf .terraform
terraform init
```

### K0s Not Starting

```bash
# Check status
ssh root@<server-ip> "k0s status"

# View logs
ssh root@<server-ip> "journalctl -u k0scontroller -f"
```

### ArgoCD Not Ready

```bash
# Check pods
kubectl get pods -n argocd

# Check events
kubectl get events -n argocd --sort-by='.lastTimestamp'
```
