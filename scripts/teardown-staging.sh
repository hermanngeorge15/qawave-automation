#!/bin/bash
set -euo pipefail

#######################################
# QAWave Staging Environment Teardown
#
# This script destroys the staging environment on Hetzner VPS.
#
# Usage:
#   export HCLOUD_TOKEN="your-hetzner-token"
#   ./scripts/teardown-staging.sh [--apps-only]
#
# Options:
#   --apps-only    Only delete applications, keep VPS and infrastructure
#######################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_PATH="${HOME}/.kube/config-qawave-staging"

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

delete_applications() {
    log_info "Deleting applications..."

    export KUBECONFIG="$KUBECONFIG_PATH"

    # Delete Frontend
    if [ -f "$PROJECT_ROOT/gitops/envs/staging/frontend/frontend.yaml" ]; then
        kubectl delete -f "$PROJECT_ROOT/gitops/envs/staging/frontend/" --ignore-not-found || true
    fi

    # Delete Backend
    if [ -f "$PROJECT_ROOT/gitops/envs/staging/backend/backend.yaml" ]; then
        kubectl delete -f "$PROJECT_ROOT/gitops/envs/staging/backend/" --ignore-not-found || true
    fi

    log_success "Applications deleted"
}

delete_infrastructure() {
    log_info "Deleting infrastructure services..."

    export KUBECONFIG="$KUBECONFIG_PATH"

    # Delete Kafka
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/kafka" ]; then
        kubectl delete -f "$PROJECT_ROOT/gitops/envs/staging/kafka/" --ignore-not-found || true
    fi

    # Delete Redis
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/redis" ]; then
        kubectl delete -f "$PROJECT_ROOT/gitops/envs/staging/redis/" --ignore-not-found || true
    fi

    # Delete PostgreSQL
    if [ -d "$PROJECT_ROOT/gitops/envs/staging/postgresql" ]; then
        kubectl delete -f "$PROJECT_ROOT/gitops/envs/staging/postgresql/" --ignore-not-found || true
    fi

    # Delete secrets
    kubectl delete secret ghcr-secret -n staging --ignore-not-found || true

    # Delete namespace
    kubectl delete namespace staging --ignore-not-found || true

    log_success "Infrastructure services deleted"
}

destroy_vps() {
    log_info "Destroying Hetzner VPS..."

    if [ -z "${HCLOUD_TOKEN:-}" ]; then
        log_error "HCLOUD_TOKEN environment variable is not set"
    fi

    cd "$PROJECT_ROOT/infrastructure/terraform/environments/staging"

    # Check if Terraform state exists
    if [ ! -f "terraform.tfstate" ]; then
        log_warning "No Terraform state found, nothing to destroy"
        return 0
    fi

    # Destroy infrastructure
    terraform destroy -auto-approve \
        -var="hcloud_token=$HCLOUD_TOKEN" \
        -var="location=nbg1"

    cd "$PROJECT_ROOT"

    # Clean up kubeconfig
    if [ -f "$KUBECONFIG_PATH" ]; then
        rm -f "$KUBECONFIG_PATH"
        log_info "Removed kubeconfig at $KUBECONFIG_PATH"
    fi

    log_success "VPS destroyed"
}

apps_only_mode() {
    echo ""
    echo "=========================================="
    echo "QAWave Staging - Delete Applications Only"
    echo "=========================================="
    echo ""

    if [ ! -f "$KUBECONFIG_PATH" ]; then
        log_error "Kubeconfig not found at $KUBECONFIG_PATH"
    fi

    delete_applications

    echo ""
    log_success "Applications deleted. VPS and infrastructure services still running."
    echo ""
}

full_teardown() {
    echo ""
    echo "=========================================="
    echo "QAWave Staging Environment Teardown"
    echo "=========================================="
    echo ""

    echo -e "${YELLOW}WARNING: This will destroy the entire staging environment!${NC}"
    echo ""
    read -p "Are you sure you want to continue? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        log_info "Teardown cancelled"
        exit 0
    fi

    if [ -f "$KUBECONFIG_PATH" ]; then
        delete_applications
        delete_infrastructure
    else
        log_warning "Kubeconfig not found, skipping Kubernetes cleanup"
    fi

    destroy_vps

    echo ""
    log_success "Staging environment completely destroyed"
    echo ""
}

# Parse arguments
APPS_ONLY=false
for arg in "$@"; do
    case $arg in
        --apps-only)
            APPS_ONLY=true
            shift
            ;;
        *)
            ;;
    esac
done

# Main execution
if [ "$APPS_ONLY" = true ]; then
    apps_only_mode
else
    full_teardown
fi
