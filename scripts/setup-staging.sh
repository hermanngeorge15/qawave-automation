#!/bin/bash
set -euo pipefail

#######################################
# QAWave Staging Environment Setup
#
# This script provisions a complete staging environment on Hetzner VPS
# with Kubernetes (K0s), databases, and deploys all applications.
#
# Usage:
#   export HCLOUD_TOKEN="your-hetzner-token"
#   export GITHUB_TOKEN="your-github-pat"
#   ./scripts/setup-staging.sh
#######################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GITHUB_USERNAME="${GITHUB_USERNAME:-hermanngeorge15}"
VPS_LOCATION="${VPS_LOCATION:-nbg1}"  # Nuremberg (default from Terraform)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_PATH="${HOME}/.kube/config-qawave-staging"
SSH_KEY="${SSH_KEY:-~/.ssh/qawave-staging}"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    local missing=()

    command -v terraform >/dev/null 2>&1 || missing+=("terraform")
    command -v kubectl >/dev/null 2>&1 || missing+=("kubectl")
    command -v ssh >/dev/null 2>&1 || missing+=("ssh")
    command -v scp >/dev/null 2>&1 || missing+=("scp")

    if [ ${#missing[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing[*]}"
    fi

    if [ -z "${HCLOUD_TOKEN:-}" ]; then
        log_error "HCLOUD_TOKEN environment variable is not set"
    fi

    if [ -z "${GITHUB_TOKEN:-}" ]; then
        log_error "GITHUB_TOKEN environment variable is not set"
    fi

    log_success "All prerequisites met"
}

provision_vps() {
    log_info "Provisioning Hetzner K0s cluster with Terraform..."

    cd "$PROJECT_ROOT/infrastructure/terraform/environments/staging"

    # Initialize Terraform
    terraform init -input=false

    # Apply Terraform configuration
    terraform apply -auto-approve \
        -var="hcloud_token=$HCLOUD_TOKEN" \
        -var="location=$VPS_LOCATION"

    # Get control plane IP
    CONTROL_PLANE_IP=$(terraform output -raw control_plane_ip 2>/dev/null || echo "")
    WORKER_1_IP=$(terraform output -raw worker_1_ip 2>/dev/null || echo "")

    if [ -z "$CONTROL_PLANE_IP" ]; then
        log_error "Failed to get control plane IP from Terraform output"
    fi

    export CONTROL_PLANE_IP
    export WORKER_1_IP
    export VPS_IP="$CONTROL_PLANE_IP"  # For compatibility
    log_success "Cluster provisioned - Control plane: $CONTROL_PLANE_IP, Worker: $WORKER_1_IP"

    cd "$PROJECT_ROOT"
}

wait_for_ssh() {
    log_info "Waiting for SSH to be available on control plane..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if ssh -i ~/.ssh/qawave-staging -o ConnectTimeout=5 -o StrictHostKeyChecking=no root@"$CONTROL_PLANE_IP" "echo 'SSH ready'" 2>/dev/null; then
            log_success "SSH is available"
            return 0
        fi
        log_info "Attempt $attempt/$max_attempts - waiting for SSH..."
        sleep 10
        ((attempt++))
    done

    log_error "SSH not available after $max_attempts attempts"
}

install_k0s() {
    log_info "Checking K0s installation on control plane..."

    # K0s is already installed by Terraform user-data
    # Just verify it's running
    ssh -i ~/.ssh/qawave-staging -o StrictHostKeyChecking=no root@"$CONTROL_PLANE_IP" << 'REMOTE_SCRIPT'
set -euo pipefail

# Check if K0s is running
if k0s status &>/dev/null; then
    echo "K0s is running"
    k0s status
else
    echo "Waiting for K0s to start..."
    sleep 30
    k0s status
fi

# Ensure kubeconfig exists
k0s kubeconfig admin > /root/kubeconfig

echo "K0s check complete"
REMOTE_SCRIPT

    log_success "K0s is running"
}

fetch_kubeconfig() {
    log_info "Fetching kubeconfig from control plane..."

    # Copy kubeconfig from control plane
    scp -i ~/.ssh/qawave-staging -o StrictHostKeyChecking=no root@"$CONTROL_PLANE_IP":/root/kubeconfig "$KUBECONFIG_PATH"

    # Update server address in kubeconfig (localhost -> control plane IP)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/127.0.0.1/$CONTROL_PLANE_IP/g" "$KUBECONFIG_PATH"
        sed -i '' "s/localhost/$CONTROL_PLANE_IP/g" "$KUBECONFIG_PATH"
    else
        sed -i "s/127.0.0.1/$CONTROL_PLANE_IP/g" "$KUBECONFIG_PATH"
        sed -i "s/localhost/$CONTROL_PLANE_IP/g" "$KUBECONFIG_PATH"
    fi

    export KUBECONFIG="$KUBECONFIG_PATH"

    # Test connection
    if kubectl get nodes; then
        log_success "Kubeconfig configured and working"
    else
        log_error "Failed to connect to Kubernetes cluster"
    fi
}

create_namespace() {
    log_info "Creating staging namespace..."

    kubectl create namespace staging --dry-run=client -o yaml | kubectl apply -f -

    log_success "Namespace 'staging' created"
}

create_image_pull_secret() {
    log_info "Creating GHCR image pull secret..."

    kubectl create secret docker-registry ghcr-secret \
        --docker-server=ghcr.io \
        --docker-username="$GITHUB_USERNAME" \
        --docker-password="$GITHUB_TOKEN" \
        -n staging \
        --dry-run=client -o yaml | kubectl apply -f -

    log_success "Image pull secret created"
}

deploy_infrastructure() {
    log_info "Deploying infrastructure services..."

    # Deploy PostgreSQL
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/postgresql" ]; then
        log_info "Deploying PostgreSQL..."
        kubectl apply -f "$PROJECT_ROOT/gitops/envs/staging/postgresql/"
    else
        log_warning "PostgreSQL manifests not found, skipping..."
    fi

    # Deploy Redis
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/redis" ]; then
        log_info "Deploying Redis..."
        kubectl apply -f "$PROJECT_ROOT/gitops/envs/staging/redis/"
    else
        log_warning "Redis manifests not found, skipping..."
    fi

    # Deploy Kafka (optional)
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/kafka" ]; then
        log_info "Deploying Kafka..."
        kubectl apply -f "$PROJECT_ROOT/gitops/envs/staging/kafka/"
    else
        log_warning "Kafka manifests not found, skipping..."
    fi

    log_success "Infrastructure services deployed"
}

wait_for_infrastructure() {
    log_info "Waiting for infrastructure services to be ready..."

    # Wait for PostgreSQL
    if kubectl get deployment postgresql -n staging 2>/dev/null; then
        kubectl rollout status deployment/postgresql -n staging --timeout=300s || true
    fi

    # Wait for Redis
    if kubectl get deployment redis -n staging 2>/dev/null; then
        kubectl rollout status deployment/redis -n staging --timeout=300s || true
    fi

    log_success "Infrastructure services are ready"
}

deploy_applications() {
    log_info "Deploying applications..."

    # Deploy Backend
    if [ -f "$PROJECT_ROOT/gitops/envs/staging/backend/backend.yaml" ]; then
        log_info "Deploying Backend..."
        kubectl apply -f "$PROJECT_ROOT/gitops/envs/staging/backend/"
    else
        log_warning "Backend manifests not found, skipping..."
    fi

    # Deploy Frontend
    if [ -f "$PROJECT_ROOT/gitops/envs/staging/frontend/frontend.yaml" ]; then
        log_info "Deploying Frontend..."
        kubectl apply -f "$PROJECT_ROOT/gitops/envs/staging/frontend/"
    else
        log_warning "Frontend manifests not found, skipping..."
    fi

    log_success "Applications deployed"
}

wait_for_applications() {
    log_info "Waiting for applications to be ready..."

    # Wait for Backend
    if kubectl get deployment backend -n staging 2>/dev/null; then
        kubectl rollout status deployment/backend -n staging --timeout=300s || log_warning "Backend not ready yet"
    fi

    # Wait for Frontend
    if kubectl get deployment frontend -n staging 2>/dev/null; then
        kubectl rollout status deployment/frontend -n staging --timeout=300s || log_warning "Frontend not ready yet"
    fi

    log_success "Applications deployment complete"
}

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${GREEN}Staging Environment Ready!${NC}"
    echo "=========================================="
    echo ""
    echo "Control Plane: $CONTROL_PLANE_IP"
    echo "Worker Node:   $WORKER_1_IP"
    echo ""
    echo "Endpoints:"
    echo "  Frontend: http://$WORKER_1_IP:30000"
    echo "  ArgoCD:   http://$WORKER_1_IP:30080"
    echo "  Backend:  Internal only (ClusterIP)"
    echo ""
    echo "Kubeconfig: $KUBECONFIG_PATH"
    echo ""
    echo "To use kubectl with this cluster:"
    echo "  export KUBECONFIG=$KUBECONFIG_PATH"
    echo ""
    echo "To check pod status:"
    echo "  kubectl get pods -n staging"
    echo ""
    echo "To view logs:"
    echo "  kubectl logs -f deploy/backend -n staging"
    echo "  kubectl logs -f deploy/frontend -n staging"
    echo ""
    echo "To get ArgoCD password:"
    echo "  ssh -i ~/.ssh/qawave-staging root@$CONTROL_PLANE_IP 'cat /root/argocd-password.txt'"
    echo ""
    echo "To teardown:"
    echo "  ./scripts/teardown-staging.sh"
    echo ""
}

# Main execution
main() {
    echo ""
    echo "=========================================="
    echo "QAWave Staging Environment Setup"
    echo "=========================================="
    echo ""

    check_prerequisites
    provision_vps
    wait_for_ssh
    install_k0s
    fetch_kubeconfig
    create_namespace
    create_image_pull_secret
    deploy_infrastructure
    wait_for_infrastructure
    deploy_applications
    wait_for_applications
    print_summary
}

main "$@"
