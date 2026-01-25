# QAWave Staging Environment Outputs

# =============================================================================
# Server IPs
# =============================================================================

output "control_plane_ip" {
  description = "Public IP of the control plane"
  value       = hcloud_server.control_plane.ipv4_address
}

output "control_plane_private_ip" {
  description = "Private IP of the control plane"
  value       = "10.1.1.10"
}

output "worker_ip" {
  description = "Public IP of the worker node"
  value       = hcloud_server.worker.ipv4_address
}

output "worker_private_ip" {
  description = "Private IP of the worker node"
  value       = "10.1.1.11"
}

output "load_balancer_ip" {
  description = "Public IP of the load balancer - Point staging.qawave.io DNS here"
  value       = hcloud_load_balancer.main.ipv4
}

# =============================================================================
# Network
# =============================================================================

output "network_id" {
  description = "ID of the private network"
  value       = hcloud_network.main.id
}

output "network_range" {
  description = "IP range of the private network"
  value       = hcloud_network.main.ip_range
}

# =============================================================================
# SSH Commands
# =============================================================================

output "ssh_commands" {
  description = "SSH commands to connect to servers"
  value = {
    control_plane = "ssh root@${hcloud_server.control_plane.ipv4_address}"
    worker        = "ssh root@${hcloud_server.worker.ipv4_address}"
  }
}

# =============================================================================
# Kubernetes Commands
# =============================================================================

output "kubeconfig_command" {
  description = "Command to get kubeconfig from control plane"
  value       = "ssh root@${hcloud_server.control_plane.ipv4_address} 'k0s kubeconfig admin' > ~/.kube/qawave-staging.conf"
}

output "kubectl_export" {
  description = "Export command for kubectl"
  value       = "export KUBECONFIG=~/.kube/qawave-staging.conf"
}

# =============================================================================
# DNS Configuration
# =============================================================================

output "dns_records" {
  description = "DNS records to create (point these to load_balancer_ip)"
  value = {
    frontend = "staging.qawave.io"
    api      = "api.staging.qawave.io"
    argocd   = "argocd.staging.qawave.io"
  }
}

# =============================================================================
# Cost Estimate
# =============================================================================

output "monthly_cost_estimate" {
  description = "Estimated monthly cost for staging environment"
  value       = "~17/month (CX11: ~4 + CX21: ~6 + LB11: ~6 + Volumes: ~1)"
}

# =============================================================================
# Environment Info
# =============================================================================

output "environment_summary" {
  description = "Summary of the staging environment"
  value = {
    environment   = "staging"
    control_plane = "CX11 (1 vCPU, 2GB RAM)"
    worker        = "CX21 (2 vCPU, 4GB RAM)"
    worker_count  = 1
    load_balancer = "LB11"
    location      = var.location
    network_range = "10.1.0.0/16"
  }
}
