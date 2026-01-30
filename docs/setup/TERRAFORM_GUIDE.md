# Terraform Guide for QAWave

This guide covers Terraform usage for managing QAWave infrastructure on Hetzner Cloud.

## Prerequisites

- Terraform >= 1.6
- Hetzner Cloud API token (see [Hetzner Setup](./HETZNER_SETUP.md))
- SSH key pair

### Installation

**macOS:**
```bash
brew install terraform
```

**Linux:**
```bash
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform
```

**Verify:**
```bash
terraform version
# Terraform v1.6.x
```

## Project Structure

```
infrastructure/terraform/
├── modules/                    # Reusable modules (DevOps owns)
│   ├── hetzner/               # Hetzner-specific resources
│   │   ├── server/
│   │   ├── network/
│   │   └── firewall/
│   └── k0s/                   # K0s cluster setup
│
└── environments/              # Environment-specific configs (Environment owns)
    ├── staging/
    │   ├── main.tf
    │   ├── variables.tf
    │   ├── outputs.tf
    │   ├── terraform.tfvars.example
    │   └── README.md
    └── production/
        ├── main.tf
        ├── variables.tf
        ├── outputs.tf
        └── README.md
```

## Basic Workflow

### 1. Initialize

```bash
cd infrastructure/terraform/environments/staging
terraform init
```

This downloads providers and initializes the backend.

### 2. Configure Variables

```bash
# Copy example file
cp terraform.tfvars.example terraform.tfvars

# Edit with your values
vim terraform.tfvars
```

**terraform.tfvars:**
```hcl
hcloud_token         = "your-hetzner-api-token"
ssh_public_key_path  = "~/.ssh/qawave-staging.pub"
ssh_private_key_path = "~/.ssh/qawave-staging"
```

### 3. Plan

```bash
terraform plan
```

Review the planned changes before applying.

### 4. Apply

```bash
terraform apply
```

Type `yes` to confirm and create resources.

### 5. Destroy (when needed)

```bash
terraform destroy
```

Type `yes` to confirm deletion.

## Configuration Examples

### Basic Server

```hcl
resource "hcloud_server" "web" {
  name        = "qawave-web"
  server_type = "cx21"
  image       = "ubuntu-22.04"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.main.id]

  labels = {
    environment = "staging"
    role        = "web"
  }
}
```

### Private Network

```hcl
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

# Attach server to network
resource "hcloud_server_network" "web" {
  server_id  = hcloud_server.web.id
  network_id = hcloud_network.main.id
  ip         = "10.1.1.10"
}
```

### Firewall

```hcl
resource "hcloud_firewall" "k8s" {
  name = "qawave-k8s-firewall"

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "22"
    source_ips = ["0.0.0.0/0"]
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "6443"
    source_ips = ["0.0.0.0/0"]
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "80"
    source_ips = ["0.0.0.0/0"]
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "443"
    source_ips = ["0.0.0.0/0"]
  }
}

# Apply firewall to server
resource "hcloud_firewall_attachment" "web" {
  firewall_id = hcloud_firewall.k8s.id
  server_ids  = [hcloud_server.web.id]
}
```

### Load Balancer

```hcl
resource "hcloud_load_balancer" "main" {
  name               = "qawave-lb"
  load_balancer_type = "lb11"
  location           = "nbg1"
}

resource "hcloud_load_balancer_network" "main" {
  load_balancer_id = hcloud_load_balancer.main.id
  network_id       = hcloud_network.main.id
  ip               = "10.1.1.100"
}

resource "hcloud_load_balancer_target" "web" {
  type             = "server"
  load_balancer_id = hcloud_load_balancer.main.id
  server_id        = hcloud_server.web.id
  use_private_ip   = true
}

resource "hcloud_load_balancer_service" "http" {
  load_balancer_id = hcloud_load_balancer.main.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 30080
}
```

## Variables Best Practices

### variables.tf

```hcl
variable "hcloud_token" {
  description = "Hetzner Cloud API token"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "staging"

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be staging or production."
  }
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key"
  type        = string
}

variable "worker_count" {
  description = "Number of worker nodes"
  type        = number
  default     = 2

  validation {
    condition     = var.worker_count >= 1 && var.worker_count <= 10
    error_message = "Worker count must be between 1 and 10."
  }
}
```

### Using Locals

```hcl
locals {
  environment = "staging"
  location    = "nbg1"

  server_types = {
    control_plane = var.environment == "production" ? "cx21" : "cx11"
    worker        = var.environment == "production" ? "cx31" : "cx21"
  }

  common_labels = {
    project     = "qawave"
    environment = local.environment
    managed_by  = "terraform"
  }
}
```

## Outputs

### outputs.tf

```hcl
output "control_plane_ip" {
  description = "Control plane public IP"
  value       = hcloud_server.control_plane.ipv4_address
}

output "worker_ips" {
  description = "Worker node public IPs"
  value       = [for w in hcloud_server.worker : w.ipv4_address]
}

output "load_balancer_ip" {
  description = "Load balancer IP (point DNS here)"
  value       = hcloud_load_balancer.main.ipv4
}

output "ssh_commands" {
  description = "SSH commands for nodes"
  value = {
    control_plane = "ssh root@${hcloud_server.control_plane.ipv4_address}"
    workers       = [for w in hcloud_server.worker : "ssh root@${w.ipv4_address}"]
  }
}

output "monthly_cost_estimate" {
  description = "Estimated monthly cost"
  value       = "~€${local.estimated_cost}/month"
}
```

## State Management

### Local State (Development)

By default, state is stored locally in `terraform.tfstate`.

### Remote State (Recommended for Teams)

```hcl
terraform {
  backend "s3" {
    bucket = "qawave-terraform-state"
    key    = "staging/terraform.tfstate"
    region = "eu-central-1"
  }
}
```

### State Commands

```bash
# List resources in state
terraform state list

# Show resource details
terraform state show hcloud_server.control_plane

# Remove resource from state (doesn't delete actual resource)
terraform state rm hcloud_server.old_server

# Import existing resource
terraform import hcloud_server.web 12345678
```

## Common Operations

### Update Single Resource

```bash
terraform apply -target=hcloud_server.web
```

### Recreate Resource

```bash
terraform taint hcloud_server.web
terraform apply
```

### Format Code

```bash
terraform fmt -recursive
```

### Validate Configuration

```bash
terraform validate
```

### Show Current State

```bash
terraform show
```

## Troubleshooting

### Provider Authentication Error

```
Error: Unable to authenticate
```

**Solution:**
```bash
# Verify token is set
echo $HCLOUD_TOKEN

# Or check terraform.tfvars
cat terraform.tfvars | grep hcloud_token
```

### Resource Already Exists

```
Error: Resource already exists
```

**Solution:**
```bash
# Import the existing resource
terraform import hcloud_server.web <server-id>
```

### Dependency Errors

```
Error: Cycle detected
```

**Solution:** Check for circular dependencies in resource references. Use `depends_on` explicitly if needed.

### State Lock Error

```
Error: Error acquiring the state lock
```

**Solution:**
```bash
# Force unlock (use with caution)
terraform force-unlock <lock-id>
```

## Security Best Practices

1. **Never commit secrets** - Use `.gitignore` for `.tfvars` files
2. **Use variables for sensitive data** - Mark with `sensitive = true`
3. **Lock provider versions** - Pin exact versions in production
4. **Review plans carefully** - Especially for destroy operations
5. **Use remote state** - With encryption and access controls

### .gitignore

```
*.tfvars
*.tfstate
*.tfstate.backup
.terraform/
.terraform.lock.hcl
```

## Related Documentation

- [Hetzner Setup](./HETZNER_SETUP.md)
- [Staging Environment](../environments/STAGING.md)
- [Production Environment](../environments/PRODUCTION.md)
- [Deploy to Staging Runbook](../runbooks/DEPLOY_TO_STAGING.md)
