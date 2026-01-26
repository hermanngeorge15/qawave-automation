# QAWave Staging Infrastructure
# Hetzner Cloud + K0s Kubernetes Cluster
#
# Architecture:
#   - 1 Control Plane (cx23) - runs k0s controller + worker
#   - 2 Workers (cx23) - general purpose
#   - No external load balancer (use NodePort)
#   - No separate volumes (use internal 40GB disk)
#
# Automated setup:
#   - K0s cluster with kuberouter CNI
#   - Workers auto-join via internal SSH
#   - ingress-nginx on NodePort 30080/30443
#   - ArgoCD installed and exposed

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
  }

  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

# =============================================================================
# Local Values
# =============================================================================

locals {
  environment   = "staging"
  server_type   = "cx23"
  network_range = "10.1.0.0/16"
  subnet_range  = "10.1.1.0/24"

  common_labels = {
    environment = local.environment
    managed_by  = "terraform"
    project     = "qawave"
  }
}

# =============================================================================
# SSH Keys
# =============================================================================

resource "hcloud_ssh_key" "main" {
  name       = "qawave-${local.environment}"
  public_key = file(var.ssh_public_key_path)
}

resource "tls_private_key" "internal" {
  algorithm = "ED25519"
}

# =============================================================================
# Network
# =============================================================================

resource "hcloud_network" "main" {
  name     = "qawave-${local.environment}"
  ip_range = local.network_range
  labels   = local.common_labels
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
  name   = "qawave-k8s-${local.environment}"
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

  # HTTP/HTTPS
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "80"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "443"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # NodePort range
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "30000-32767"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # Private network (intra-cluster)
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "1-65535"
    source_ips = [local.subnet_range]
  }

  rule {
    direction  = "in"
    protocol   = "udp"
    port       = "1-65535"
    source_ips = [local.subnet_range]
  }

  rule {
    direction  = "in"
    protocol   = "icmp"
    source_ips = [local.subnet_range]
  }

  # Pod and Service CIDR (for kube-router/overlay traffic)
  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "1-65535"
    source_ips = ["10.244.0.0/16", "10.96.0.0/12"]
  }

  rule {
    direction  = "in"
    protocol   = "udp"
    port       = "1-65535"
    source_ips = ["10.244.0.0/16", "10.96.0.0/12"]
  }

  rule {
    direction  = "in"
    protocol   = "icmp"
    source_ips = ["10.244.0.0/16", "10.96.0.0/12"]
  }
}

# =============================================================================
# Control Plane
# =============================================================================

resource "hcloud_server" "control_plane" {
  name         = "qawave-cp-${local.environment}"
  server_type  = local.server_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.10"
  }

  labels = merge(local.common_labels, { role = "control-plane" })

  user_data = <<EOF
#!/bin/bash
set -e
exec > /var/log/user-data.log 2>&1
echo "=== Control Plane Setup Started ==="

# Internal SSH key
mkdir -p /root/.ssh
cat > /root/.ssh/id_ed25519 << 'SSHKEY'
${tls_private_key.internal.private_key_openssh}
SSHKEY
chmod 600 /root/.ssh/id_ed25519
echo '${tls_private_key.internal.public_key_openssh}' >> /root/.ssh/authorized_keys

# Kernel modules for Kubernetes
cat > /etc/modules-load.d/k8s.conf << 'MODULES'
overlay
br_netfilter
MODULES
modprobe overlay
modprobe br_netfilter

# Sysctl settings
cat > /etc/sysctl.d/99-kubernetes.conf << 'SYSCTL'
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
SYSCTL
sysctl --system

# Disable swap
swapoff -a
sed -i '/ swap / s/^/#/' /etc/fstab

# System setup
apt-get update && apt-get install -y curl jq

# Install k0s
curl -sSLf https://get.k0s.sh | sh

# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && mv kubectl /usr/local/bin/

# Install Helm
curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Get public IP
PUBLIC_IP=$(curl -s http://169.254.169.254/hetzner/v1/metadata/public-ipv4)

# K0s config with node-ip set to private address
mkdir -p /etc/k0s
cat > /etc/k0s/k0s.yaml << K0SCONF
apiVersion: k0s.k0sproject.io/v1beta1
kind: ClusterConfig
metadata:
  name: qawave-staging
spec:
  api:
    address: 10.1.1.10
    sans:
      - 10.1.1.10
      - $PUBLIC_IP
      - 127.0.0.1
  network:
    provider: kuberouter
    podCIDR: 10.244.0.0/16
    serviceCIDR: 10.96.0.0/12
K0SCONF

# Start k0s with kubelet using private IP
k0s install controller --enable-worker -c /etc/k0s/k0s.yaml --kubelet-extra-args="--node-ip=10.1.1.10"
k0s start

# Wait for k0s
echo "Waiting for k0s to start..."
for i in {1..30}; do
  if k0s status 2>/dev/null | grep -q "running"; then
    echo "k0s is running"
    break
  fi
  sleep 10
done

# Setup kubeconfig
sleep 30
mkdir -p /root/.kube
k0s kubeconfig admin > /root/.kube/config
chmod 600 /root/.kube/config
export KUBECONFIG=/root/.kube/config

# Wait for cluster ready
echo "Waiting for cluster to be ready..."
for i in {1..30}; do
  if kubectl get nodes 2>/dev/null | grep -q "Ready"; then
    echo "Cluster is ready"
    break
  fi
  sleep 10
done

# Generate worker token
k0s token create --role=worker > /root/worker-token.txt
chmod 644 /root/worker-token.txt

# Wait for CoreDNS
echo "Waiting for CoreDNS..."
kubectl wait --for=condition=available --timeout=300s deployment/coredns -n kube-system || true
sleep 30

# Install ingress-nginx
echo "Installing ingress-nginx..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/baremetal/deploy.yaml
sleep 30

# Install ArgoCD
echo "Installing ArgoCD..."
kubectl create namespace argocd || true
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD
echo "Waiting for ArgoCD..."
sleep 60
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd || true

# Expose ArgoCD via NodePort
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort", "ports": [{"name": "http", "port": 80, "targetPort": 8080, "nodePort": 30080}, {"name": "https", "port": 443, "targetPort": 8080, "nodePort": 30443}]}}'

# Get ArgoCD password
sleep 10
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" 2>/dev/null | base64 -d > /root/argocd-password.txt || echo "password-not-ready" > /root/argocd-password.txt

touch /root/setup-complete
echo "=== Control Plane Setup Complete ==="
EOF

  lifecycle {
    ignore_changes = [user_data]
  }
}

# =============================================================================
# Workers
# =============================================================================

resource "hcloud_server" "worker_1" {
  name         = "qawave-worker-1-${local.environment}"
  server_type  = local.server_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.11"
  }

  labels = merge(local.common_labels, { role = "worker" })

  user_data = <<EOF
#!/bin/bash
set -e
exec > /var/log/user-data.log 2>&1
echo "=== Worker 1 Setup Started ==="

# Internal SSH key
mkdir -p /root/.ssh
cat > /root/.ssh/id_ed25519 << 'SSHKEY'
${tls_private_key.internal.private_key_openssh}
SSHKEY
chmod 600 /root/.ssh/id_ed25519
echo '${tls_private_key.internal.public_key_openssh}' >> /root/.ssh/authorized_keys

# Kernel modules for Kubernetes
cat > /etc/modules-load.d/k8s.conf << 'MODULES'
overlay
br_netfilter
MODULES
modprobe overlay
modprobe br_netfilter

# Sysctl settings
cat > /etc/sysctl.d/99-kubernetes.conf << 'SYSCTL'
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
SYSCTL
sysctl --system

# Disable swap
swapoff -a
sed -i '/ swap / s/^/#/' /etc/fstab

# System setup
apt-get update && apt-get install -y curl

# Install k0s
curl -sSLf https://get.k0s.sh | sh

# Wait for control plane token
echo "Waiting for control plane..."
for i in {1..60}; do
  if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 root@10.1.1.10 "test -f /root/worker-token.txt" 2>/dev/null; then
    echo "Control plane ready!"
    break
  fi
  echo "Waiting... $i/60"
  sleep 30
done

# Get token and join with kubelet using private IP
scp -o StrictHostKeyChecking=no root@10.1.1.10:/root/worker-token.txt /root/worker-token.txt
k0s install worker --token-file /root/worker-token.txt --kubelet-extra-args="--node-ip=10.1.1.11"
k0s start

touch /root/setup-complete
echo "=== Worker 1 Setup Complete ==="
EOF

  lifecycle {
    ignore_changes = [user_data]
  }

  depends_on = [hcloud_server.control_plane]
}

resource "hcloud_server" "worker_2" {
  name         = "qawave-worker-2-${local.environment}"
  server_type  = local.server_type
  image        = "ubuntu-22.04"
  location     = var.location
  ssh_keys     = [hcloud_ssh_key.main.id]
  firewall_ids = [hcloud_firewall.k8s.id]

  network {
    network_id = hcloud_network.main.id
    ip         = "10.1.1.12"
  }

  labels = merge(local.common_labels, { role = "worker" })

  user_data = <<EOF
#!/bin/bash
set -e
exec > /var/log/user-data.log 2>&1
echo "=== Worker 2 Setup Started ==="

# Internal SSH key
mkdir -p /root/.ssh
cat > /root/.ssh/id_ed25519 << 'SSHKEY'
${tls_private_key.internal.private_key_openssh}
SSHKEY
chmod 600 /root/.ssh/id_ed25519
echo '${tls_private_key.internal.public_key_openssh}' >> /root/.ssh/authorized_keys

# Kernel modules for Kubernetes
cat > /etc/modules-load.d/k8s.conf << 'MODULES'
overlay
br_netfilter
MODULES
modprobe overlay
modprobe br_netfilter

# Sysctl settings
cat > /etc/sysctl.d/99-kubernetes.conf << 'SYSCTL'
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
SYSCTL
sysctl --system

# Disable swap
swapoff -a
sed -i '/ swap / s/^/#/' /etc/fstab

# System setup
apt-get update && apt-get install -y curl

# Install k0s
curl -sSLf https://get.k0s.sh | sh

# Wait for control plane token
echo "Waiting for control plane..."
for i in {1..60}; do
  if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 root@10.1.1.10 "test -f /root/worker-token.txt" 2>/dev/null; then
    echo "Control plane ready!"
    break
  fi
  echo "Waiting... $i/60"
  sleep 30
done

# Get token and join with kubelet using private IP
scp -o StrictHostKeyChecking=no root@10.1.1.10:/root/worker-token.txt /root/worker-token.txt
k0s install worker --token-file /root/worker-token.txt --kubelet-extra-args="--node-ip=10.1.1.12"
k0s start

touch /root/setup-complete
echo "=== Worker 2 Setup Complete ==="
EOF

  lifecycle {
    ignore_changes = [user_data]
  }

  depends_on = [hcloud_server.control_plane]
}
