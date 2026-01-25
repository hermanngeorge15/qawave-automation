#!/usr/bin/env bash
#
# QAWave K0s Cluster Bootstrap Script
# This script automates the K0s cluster setup using k0sctl
#
# Prerequisites:
#   - k0sctl installed: https://github.com/k0sproject/k0sctl#installation
#   - SSH key configured for Hetzner servers
#   - Terraform has been applied and outputs are available
#
# Usage:
#   ./bootstrap.sh [--dry-run]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K0SCTL_CONFIG="${SCRIPT_DIR}/k0sctl.yaml"
K0SCTL_TEMPLATE="${SCRIPT_DIR}/k0sctl.yaml.template"
TERRAFORM_DIR="${SCRIPT_DIR}/../../terraform/environments/production"
KUBECONFIG_OUTPUT="${SCRIPT_DIR}/kubeconfig.yaml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_dependencies() {
    log_info "Checking dependencies..."

    if ! command -v k0sctl &> /dev/null; then
        log_error "k0sctl is not installed. Install from: https://github.com/k0sproject/k0sctl#installation"
        exit 1
    fi

    if ! command -v terraform &> /dev/null; then
        log_error "terraform is not installed"
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        log_error "jq is not installed"
        exit 1
    fi

    log_info "All dependencies present"
}

get_terraform_outputs() {
    log_info "Getting Terraform outputs..."

    if [[ ! -d "${TERRAFORM_DIR}" ]]; then
        log_error "Terraform directory not found: ${TERRAFORM_DIR}"
        exit 1
    fi

    cd "${TERRAFORM_DIR}"

    CONTROL_PLANE_IP=$(terraform output -raw control_plane_ip 2>/dev/null || echo "")
    LOAD_BALANCER_IP=$(terraform output -raw load_balancer_ip 2>/dev/null || echo "")

    # Get worker IPs as array
    WORKER_IPS=$(terraform output -json worker_ips 2>/dev/null || echo "[]")
    WORKER_1_IP=$(echo "${WORKER_IPS}" | jq -r '.[0] // empty')
    WORKER_2_IP=$(echo "${WORKER_IPS}" | jq -r '.[1] // empty')
    WORKER_3_IP=$(echo "${WORKER_IPS}" | jq -r '.[2] // empty')

    if [[ -z "${CONTROL_PLANE_IP}" ]]; then
        log_error "Could not get control_plane_ip from Terraform. Has infrastructure been provisioned?"
        exit 1
    fi

    log_info "Control Plane IP: ${CONTROL_PLANE_IP}"
    log_info "Load Balancer IP: ${LOAD_BALANCER_IP}"
    log_info "Worker 1 IP: ${WORKER_1_IP}"
    log_info "Worker 2 IP: ${WORKER_2_IP}"
    log_info "Worker 3 IP: ${WORKER_3_IP}"

    cd "${SCRIPT_DIR}"
}

generate_k0sctl_config() {
    log_info "Generating k0sctl configuration..."

    # Replace placeholders in template
    sed -e "s/\${CONTROL_PLANE_IP}/${CONTROL_PLANE_IP}/g" \
        -e "s/\${LOAD_BALANCER_IP}/${LOAD_BALANCER_IP}/g" \
        -e "s/\${WORKER_1_IP}/${WORKER_1_IP}/g" \
        -e "s/\${WORKER_2_IP}/${WORKER_2_IP}/g" \
        -e "s/\${WORKER_3_IP}/${WORKER_3_IP}/g" \
        "${K0SCTL_TEMPLATE}" > "${K0SCTL_CONFIG}"

    log_info "Generated k0sctl.yaml"
}

apply_cluster() {
    log_info "Applying K0s cluster configuration..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log_warn "Dry run mode - would execute: k0sctl apply --config ${K0SCTL_CONFIG}"
        return
    fi

    k0sctl apply --config "${K0SCTL_CONFIG}"

    log_info "Cluster applied successfully"
}

get_kubeconfig() {
    log_info "Retrieving kubeconfig..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log_warn "Dry run mode - would execute: k0sctl kubeconfig --config ${K0SCTL_CONFIG}"
        return
    fi

    k0sctl kubeconfig --config "${K0SCTL_CONFIG}" > "${KUBECONFIG_OUTPUT}"
    chmod 600 "${KUBECONFIG_OUTPUT}"

    log_info "Kubeconfig saved to ${KUBECONFIG_OUTPUT}"
    log_info "To use: export KUBECONFIG=${KUBECONFIG_OUTPUT}"
}

verify_cluster() {
    log_info "Verifying cluster health..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log_warn "Dry run mode - skipping verification"
        return
    fi

    export KUBECONFIG="${KUBECONFIG_OUTPUT}"

    log_info "Cluster nodes:"
    kubectl get nodes -o wide

    log_info "System pods:"
    kubectl get pods -n kube-system

    log_info "Ingress controller status:"
    kubectl get pods -n ingress-nginx

    log_info "Cluster verification complete"
}

main() {
    # Parse arguments
    DRY_RUN=false
    if [[ "${1:-}" == "--dry-run" ]]; then
        DRY_RUN=true
        log_warn "Running in dry-run mode"
    fi

    echo "======================================"
    echo "QAWave K0s Cluster Bootstrap"
    echo "======================================"
    echo ""

    check_dependencies
    get_terraform_outputs
    generate_k0sctl_config
    apply_cluster
    get_kubeconfig
    verify_cluster

    echo ""
    echo "======================================"
    log_info "Bootstrap complete!"
    echo "======================================"
    echo ""
    echo "Next steps:"
    echo "  1. Set kubeconfig: export KUBECONFIG=${KUBECONFIG_OUTPUT}"
    echo "  2. Verify cluster: kubectl get nodes"
    echo "  3. Deploy ArgoCD: see ../argocd/README.md"
}

main "$@"
