# QAWave Staging Infrastructure
# Hetzner Cloud + K0s Kubernetes Cluster
#
# Cost estimate: ~17/month
#   - CX11 (Control Plane): ~4/month
#   - CX21 (Worker): ~6/month
#   - LB11 (Load Balancer): ~6/month
#   - Volumes: ~1/month

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
    key    = "staging/terraform.tfstate"
    region = "eu-central-1"
    # Use any S3-compatible backend (e.g., Backblaze B2, Wasabi)
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

# =============================================================================
# Local Values
# =============================================================================

locals {
  environment = "staging"
  # Staging uses smaller instances than production
  control_plane_type = "cx11" # 1 vCPU, 2GB RAM
  worker_type        = "cx21" # 2 vCPU, 4GB RAM
  worker_count       = 1      # Single worker for staging

  # Network range different from production (10.0.x.x)
  network_range = "10.1.0.0/16"
  subnet_range  = "10.1.1.0/24"

  common_labels = {
    environment = local.environment
    managed_by  = "terraform"
    project     = "qawave"
  }
}

# =============================================================================
# SSH Key
# =============================================================================

resource "hcloud_ssh_key" "main" {
  name       = "qawave-${local.environment}"
  public_key = file(var.ssh_public_key_path)
}

# =============================================================================
# Network (Isolated from production)
# =============================================================================

resource "hcloud_network" "main" {
  name     = "qawave-${local.environment}"
  ip_range = local.network_range

  labels = local.common_labels
}

resource "hcloud_network_subnet" "nodes" {
  network_id   = hcloud_network.main.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = local.subnet_range
}

# =============================================================================
# Firewall
# =============================================================================

resource "hcloud_firewall" "k8s" {
  name = "qawave-k8s-${local.environment}"

  labels = local.common_labels

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
    source_ips = [local.network_range]
  }

  rule {
    direction  = "in"
    protocol   = "udp"
    port       = "any"
    source_ips = [local.network_range]
  }
}

# =============================================================================
# Control Plane Server
# =============================================================================

resource "hcloud_server" "control_plane" {
  name         = "qawave-cp-${local.environment}"
  server_type  = local.control_plane_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.10"
  }

  labels = merge(local.common_labels, {
    role = "control-plane"
  })

  user_data = <<-EOF
    #!/bin/bash
    set -e

    # Update system
    apt-get update && apt-get upgrade -y

    # Install k0s
    curl -sSLf https://get.k0s.sh | sudo sh

    # Install k0s as controller (single node for staging)
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

  lifecycle {
    ignore_changes = [user_data]
  }
}

# =============================================================================
# Worker Node (Single worker for staging)
# =============================================================================

resource "hcloud_server" "worker" {
  name         = "qawave-worker-${local.environment}"
  server_type  = local.worker_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.11"
  }

  labels = merge(local.common_labels, {
    role = "worker"
  })

  depends_on = [hcloud_server.control_plane]
}

# =============================================================================
# Load Balancer
# =============================================================================

resource "hcloud_load_balancer" "main" {
  name               = "qawave-lb-${local.environment}"
  load_balancer_type = "lb11"
  location           = var.location

  labels = local.common_labels
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
# Volumes for Data Services (Smaller than production)
# =============================================================================

resource "hcloud_volume" "postgres" {
  name     = "qawave-postgres-${local.environment}"
  size     = 20 # Smaller than prod (50GB)
  location = var.location
  format   = "ext4"

  labels = merge(local.common_labels, {
    service = "postgresql"
  })
}

resource "hcloud_volume" "redis" {
  name     = "qawave-redis-${local.environment}"
  size     = 10
  location = var.location
  format   = "ext4"

  labels = merge(local.common_labels, {
    service = "redis"
  })
}
