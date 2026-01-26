#!/bin/bash
# =============================================================================
# Environment Destruction Script
# =============================================================================
# Safely destroys QAWave infrastructure.
#
# CAUTION: This script PERMANENTLY DELETES infrastructure and data!
#
# Usage:
#   ./destroy-environment.sh <staging|production>
#
# =============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-}"

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: $0 <staging|production>"
    exit 1
fi

if [ "$ENVIRONMENT" != "staging" ] && [ "$ENVIRONMENT" != "production" ]; then
    log_error "Environment must be 'staging' or 'production'"
    exit 1
fi

TERRAFORM_DIR="$SCRIPT_DIR/../terraform/environments/$ENVIRONMENT"

# =============================================================================
# Safety Checks
# =============================================================================

confirm_destruction() {
    echo ""
    echo -e "${RED}=========================================="
    echo "  WARNING: DESTRUCTIVE OPERATION"
    echo "==========================================${NC}"
    echo ""
    echo "You are about to PERMANENTLY DESTROY the $ENVIRONMENT environment."
    echo ""
    echo "This will delete:"
    echo "  - All servers (control plane, workers)"
    echo "  - Load balancer"
    echo "  - Network and firewall rules"
    echo "  - All data volumes (PostgreSQL, Redis, Kafka)"
    echo ""
    echo -e "${RED}THIS ACTION CANNOT BE UNDONE!${NC}"
    echo ""

    if [ "$ENVIRONMENT" = "production" ]; then
        echo -e "${RED}THIS IS PRODUCTION! Real user data will be lost!${NC}"
        echo ""
        read -p "Type 'destroy production' to confirm: " CONFIRM
        if [ "$CONFIRM" != "destroy production" ]; then
            log_warn "Aborted."
            exit 0
        fi
    else
        read -p "Type 'destroy' to confirm: " CONFIRM
        if [ "$CONFIRM" != "destroy" ]; then
            log_warn "Aborted."
            exit 0
        fi
    fi
}

check_terraform() {
    if ! command -v terraform &> /dev/null; then
        log_error "terraform is required but not installed"
        exit 1
    fi

    if [ ! -d "$TERRAFORM_DIR" ]; then
        log_error "Terraform directory not found: $TERRAFORM_DIR"
        exit 1
    fi
}

# =============================================================================
# Destruction
# =============================================================================

destroy_infrastructure() {
    log_info "Destroying $ENVIRONMENT infrastructure..."

    cd "$TERRAFORM_DIR"

    # Initialize Terraform
    terraform init -upgrade

    # Show what will be destroyed
    log_info "Planning destruction..."
    terraform plan -destroy

    echo ""
    read -p "Proceed with destruction? (yes/no): " PROCEED

    if [ "$PROCEED" != "yes" ]; then
        log_warn "Aborted."
        exit 0
    fi

    # Destroy
    log_info "Destroying resources..."
    terraform destroy -auto-approve

    log_success "Infrastructure destroyed"
}

cleanup_local() {
    log_info "Cleaning up local files..."

    # Remove kubeconfig
    local kubeconfig="$HOME/.kube/qawave-$ENVIRONMENT.conf"
    if [ -f "$kubeconfig" ]; then
        rm -f "$kubeconfig"
        log_info "Removed $kubeconfig"
    fi

    # Remove terraform state files (if using local backend)
    rm -f "$TERRAFORM_DIR/terraform.tfstate"
    rm -f "$TERRAFORM_DIR/terraform.tfstate.backup"
    rm -f "$TERRAFORM_DIR/*.tfplan"

    log_success "Local cleanup complete"
}

# =============================================================================
# Main
# =============================================================================

main() {
    echo "=========================================="
    echo "  Destroy $ENVIRONMENT Environment"
    echo "=========================================="
    echo ""

    confirm_destruction
    check_terraform
    destroy_infrastructure
    cleanup_local

    echo ""
    echo "=========================================="
    echo -e "${GREEN}  $ENVIRONMENT environment destroyed${NC}"
    echo "=========================================="
}

main
