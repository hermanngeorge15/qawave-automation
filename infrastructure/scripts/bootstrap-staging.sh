#!/bin/bash
# =============================================================================
# QAWave Staging Environment Bootstrap Script
# =============================================================================
# This script provisions and configures the staging environment on Hetzner Cloud.
#
# Prerequisites:
#   - terraform >= 1.6
#   - ssh with key configured
#   - kubectl
#   - Hetzner Cloud API token in terraform.tfvars
#
# Usage:
#   ./bootstrap-staging.sh
#
# =============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform/environments/staging"
KUBECONFIG_PATH="$HOME/.kube/qawave-staging.conf"

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    local missing=()

    command -v terraform >/dev/null 2>&1 || missing+=("terraform")
    command -v ssh >/dev/null 2>&1 || missing+=("ssh")
    command -v kubectl >/dev/null 2>&1 || missing+=("kubectl")

    if [ ${#missing[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing[*]}"
        echo "Please install the missing tools and try again."
        exit 1
    fi

    log_success "All prerequisites met"
}

check_tfvars() {
    if [ ! -f "$TERRAFORM_DIR/terraform.tfvars" ]; then
        log_error "terraform.tfvars not found!"
        echo ""
        echo "Please create terraform.tfvars from the example:"
        echo "  cd $TERRAFORM_DIR"
        echo "  cp terraform.tfvars.example terraform.tfvars"
        echo "  # Edit terraform.tfvars with your Hetzner API token"
        exit 1
    fi

    log_success "terraform.tfvars found"
}

# =============================================================================
# Main Functions
# =============================================================================

provision_infrastructure() {
    log_info "Step 1: Provisioning infrastructure with Terraform..."

    cd "$TERRAFORM_DIR"

    # Initialize Terraform
    log_info "Running terraform init..."
    terraform init -upgrade

    # Plan
    log_info "Running terraform plan..."
    terraform plan -out=staging.tfplan

    echo ""
    echo "=========================================="
    echo "  Review the Terraform plan above"
    echo "=========================================="
    echo ""
    read -p "Apply Terraform plan? (yes/no): " APPLY

    if [ "$APPLY" != "yes" ]; then
        log_warn "Aborted by user"
        exit 0
    fi

    # Apply
    log_info "Running terraform apply..."
    terraform apply staging.tfplan

    # Clean up plan file
    rm -f staging.tfplan

    log_success "Infrastructure provisioned!"
}

get_terraform_outputs() {
    cd "$TERRAFORM_DIR"

    CP_IP=$(terraform output -raw control_plane_ip 2>/dev/null || echo "")
    WORKER_IP=$(terraform output -raw worker_ip 2>/dev/null || echo "")
    LB_IP=$(terraform output -raw load_balancer_ip 2>/dev/null || echo "")

    if [ -z "$CP_IP" ]; then
        log_error "Could not get Terraform outputs. Has infrastructure been provisioned?"
        exit 1
    fi
}

wait_for_servers() {
    log_info "Step 2: Waiting for servers to be ready..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no -o BatchMode=yes root@"$CP_IP" "echo 'ready'" 2>/dev/null; then
            log_success "Control plane is ready"
            return 0
        fi

        log_info "Waiting for control plane... (attempt $attempt/$max_attempts)"
        sleep 10
        ((attempt++))
    done

    log_error "Timeout waiting for control plane to be ready"
    exit 1
}

setup_k0s() {
    log_info "Step 3: Setting up K0s cluster..."

    # Check if k0s is already installed and running
    if ssh -o StrictHostKeyChecking=no root@"$CP_IP" "k0s status" 2>/dev/null | grep -q "running"; then
        log_success "K0s is already running"
        return 0
    fi

    log_info "Installing K0s on control plane..."
    ssh -o StrictHostKeyChecking=no root@"$CP_IP" << 'EOF'
        set -e

        # Install k0s if not present
        if ! command -v k0s &> /dev/null; then
            echo "Installing k0s..."
            curl -sSLf https://get.k0s.sh | sudo sh
        fi

        # Install and start k0s controller
        if ! systemctl is-active --quiet k0scontroller; then
            echo "Configuring k0s controller..."
            k0s install controller --single
            k0s start

            # Wait for k0s to be ready
            echo "Waiting for k0s to start..."
            sleep 30
        fi

        # Verify status
        k0s status
EOF

    log_success "K0s cluster is running"
}

get_kubeconfig() {
    log_info "Step 4: Getting kubeconfig..."

    mkdir -p "$HOME/.kube"

    ssh -o StrictHostKeyChecking=no root@"$CP_IP" "k0s kubeconfig admin" > "$KUBECONFIG_PATH"

    # Update server address to use public IP
    if command -v sed &> /dev/null; then
        sed -i.bak "s|server:.*|server: https://$CP_IP:6443|" "$KUBECONFIG_PATH"
        rm -f "${KUBECONFIG_PATH}.bak"
    fi

    chmod 600 "$KUBECONFIG_PATH"

    log_success "Kubeconfig saved to $KUBECONFIG_PATH"
}

verify_cluster() {
    log_info "Step 5: Verifying cluster..."

    export KUBECONFIG="$KUBECONFIG_PATH"

    if kubectl cluster-info &>/dev/null; then
        log_success "Cluster is accessible"
        kubectl get nodes
    else
        log_error "Cannot connect to cluster"
        exit 1
    fi
}

install_argocd() {
    log_info "Step 6: Installing ArgoCD..."

    export KUBECONFIG="$KUBECONFIG_PATH"

    # Create namespace
    kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

    # Install ArgoCD
    log_info "Applying ArgoCD manifests..."
    kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

    # Wait for ArgoCD to be ready
    log_info "Waiting for ArgoCD to be ready (this may take a few minutes)..."
    kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

    # Get initial password
    ARGOCD_PASS=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)

    log_success "ArgoCD installed"
}

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${GREEN}  STAGING ENVIRONMENT READY${NC}"
    echo "=========================================="
    echo ""
    echo "  Infrastructure:"
    echo "    Control Plane: $CP_IP"
    echo "    Worker:        $WORKER_IP"
    echo "    Load Balancer: $LB_IP"
    echo ""
    echo "  DNS Setup Required:"
    echo "    staging.qawave.io        -> $LB_IP"
    echo "    api.staging.qawave.io    -> $LB_IP"
    echo "    argocd.staging.qawave.io -> $LB_IP"
    echo ""
    echo "  ArgoCD Credentials:"
    echo "    Username: admin"
    echo "    Password: $ARGOCD_PASS"
    echo ""
    echo "  Kubernetes Access:"
    echo "    export KUBECONFIG=$KUBECONFIG_PATH"
    echo "    kubectl get pods -A"
    echo ""
    echo "  SSH Access:"
    echo "    ssh root@$CP_IP      # Control plane"
    echo "    ssh root@$WORKER_IP  # Worker"
    echo ""
    echo "=========================================="
}

# =============================================================================
# Main
# =============================================================================

main() {
    echo "=========================================="
    echo "  QAWave Staging Environment Bootstrap"
    echo "=========================================="
    echo ""

    check_prerequisites
    check_tfvars
    provision_infrastructure
    get_terraform_outputs
    wait_for_servers
    setup_k0s
    get_kubeconfig
    verify_cluster
    install_argocd
    print_summary
}

# Run main function
main "$@"
