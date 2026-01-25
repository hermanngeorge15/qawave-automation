# QAWave Production Infrastructure
# Hetzner Cloud + K0s Kubernetes Cluster

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.4"
    }
  }

  backend "s3" {
    bucket = "qawave-terraform-state"
    key    = "production/terraform.tfstate"
    region = "eu-central-1"
    # Use any S3-compatible backend (e.g., Backblaze B2, Wasabi)
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

# =============================================================================
# Variables
# =============================================================================

variable "hcloud_token" {
  description = "Hetzner Cloud API Token"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "location" {
  description = "Hetzner datacenter location"
  type        = string
  default     = "nbg1" # Nuremberg
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "control_plane_type" {
  description = "Server type for control plane"
  type        = string
  default     = "cx21" # 2 vCPU, 4GB RAM
}

variable "worker_type" {
  description = "Server type for workers"
  type        = string
  default     = "cx31" # 2 vCPU, 8GB RAM
}

variable "worker_count" {
  description = "Number of worker nodes"
  type        = number
  default     = 3
}

# =============================================================================
# SSH Key
# =============================================================================

resource "hcloud_ssh_key" "main" {
  name       = "qawave-${var.environment}"
  public_key = file(var.ssh_public_key_path)
}

# =============================================================================
# Network
# =============================================================================

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

# =============================================================================
# Firewall
# =============================================================================

resource "hcloud_firewall" "k8s" {
  name = "qawave-k8s-${var.environment}"

  # SSH
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "22"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # Kubernetes API
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "6443"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # HTTP
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "80"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # HTTPS
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "443"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # NodePort range (for ingress)
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "30000-32767"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # Internal cluster communication
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "any"
    source_ips = ["10.0.0.0/16"]
  }

  rule {
    direction  = "in"
    protocol   = "udp"
    port       = "any"
    source_ips = ["10.0.0.0/16"]
  }
}

# =============================================================================
# Control Plane Server
# =============================================================================

resource "hcloud_server" "control_plane" {
  name         = "qawave-cp-${var.environment}"
  server_type  = var.control_plane_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.0.1.10"
  }

  labels = {
    role        = "control-plane"
    environment = var.environment
    managed_by  = "terraform"
  }

  user_data = <<-EOF
    #!/bin/bash
    set -e
    
    # Update system
    apt-get update && apt-get upgrade -y
    
    # Install k0s
    curl -sSLf https://get.k0s.sh | sudo sh
    
    # Install k0s as controller
    k0s install controller --single
    k0s start
    
    # Wait for k0s to be ready
    sleep 30
    
    # Install kubectl
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    chmod +x kubectl
    mv kubectl /usr/local/bin/
    
    # Set up kubeconfig
    mkdir -p /root/.kube
    k0s kubeconfig admin > /root/.kube/config
  EOF
}

# =============================================================================
# Worker Nodes
# =============================================================================

resource "hcloud_server" "workers" {
  count        = var.worker_count
  name         = "qawave-worker-${var.environment}-${count.index + 1}"
  server_type  = var.worker_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.0.1.${11 + count.index}"
  }

  labels = {
    role        = "worker"
    environment = var.environment
    managed_by  = "terraform"
    worker_id   = tostring(count.index + 1)
  }

  depends_on = [hcloud_server.control_plane]
}

# =============================================================================
# Load Balancer
# =============================================================================

resource "hcloud_load_balancer" "main" {
  name               = "qawave-lb-${var.environment}"
  load_balancer_type = "lb11"
  location           = var.location

  labels = {
    environment = var.environment
    managed_by  = "terraform"
  }
}

resource "hcloud_load_balancer_network" "main" {
  load_balancer_id = hcloud_load_balancer.main.id
  network_id       = hcloud_network.main.id
  ip               = "10.0.1.100"
}

resource "hcloud_load_balancer_target" "workers" {
  count            = var.worker_count
  type             = "server"
  load_balancer_id = hcloud_load_balancer.main.id
  server_id        = hcloud_server.workers[count.index].id
  use_private_ip   = true
}

resource "hcloud_load_balancer_service" "http" {
  load_balancer_id = hcloud_load_balancer.main.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 30080

  health_check {
    protocol = "tcp"
    port     = 30080
    interval = 10
    timeout  = 5
    retries  = 3
  }
}

resource "hcloud_load_balancer_service" "https" {
  load_balancer_id = hcloud_load_balancer.main.id
  protocol         = "tcp"
  listen_port      = 443
  destination_port = 30443

  health_check {
    protocol = "tcp"
    port     = 30443
    interval = 10
    timeout  = 5
    retries  = 3
  }
}

# =============================================================================
# Volumes for Data Services
# =============================================================================

resource "hcloud_volume" "postgres" {
  name     = "qawave-postgres-${var.environment}"
  size     = 50
  location = var.location
  format   = "ext4"

  labels = {
    service     = "postgresql"
    environment = var.environment
  }
}

resource "hcloud_volume" "redis" {
  name     = "qawave-redis-${var.environment}"
  size     = 10
  location = var.location
  format   = "ext4"

  labels = {
    service     = "redis"
    environment = var.environment
  }
}

resource "hcloud_volume" "kafka" {
  name     = "qawave-kafka-${var.environment}"
  size     = 50
  location = var.location
  format   = "ext4"

  labels = {
    service     = "kafka"
    environment = var.environment
  }
}

# =============================================================================
# Outputs
# =============================================================================

output "control_plane_ip" {
  description = "Public IP of the control plane"
  value       = hcloud_server.control_plane.ipv4_address
}

output "control_plane_private_ip" {
  description = "Private IP of the control plane"
  value       = "10.0.1.10"
}

output "worker_ips" {
  description = "Public IPs of worker nodes"
  value       = hcloud_server.workers[*].ipv4_address
}

output "worker_private_ips" {
  description = "Private IPs of worker nodes"
  value       = [for i in range(var.worker_count) : "10.0.1.${11 + i}"]
}

output "load_balancer_ip" {
  description = "Public IP of the load balancer"
  value       = hcloud_load_balancer.main.ipv4
}

output "network_id" {
  description = "ID of the private network"
  value       = hcloud_network.main.id
}

output "ssh_command" {
  description = "SSH command to connect to control plane"
  value       = "ssh root@${hcloud_server.control_plane.ipv4_address}"
}

output "kubeconfig_command" {
  description = "Command to get kubeconfig"
  value       = "ssh root@${hcloud_server.control_plane.ipv4_address} 'k0s kubeconfig admin' > kubeconfig.yaml"
}
