#!/usr/bin/env bash
#
# QAWave ArgoCD Setup Script
# Installs and configures ArgoCD for GitOps deployment
#
# Usage:
#   ./setup.sh [--skip-ingress]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    log_info "Prerequisites OK"
}

create_namespace() {
    log_info "Creating argocd namespace..."

    if kubectl get namespace argocd &> /dev/null; then
        log_warn "Namespace argocd already exists"
    else
        kubectl create namespace argocd
    fi
}

install_argocd() {
    log_info "Installing ArgoCD..."

    kubectl apply -k "${SCRIPT_DIR}/install/"

    log_info "Waiting for ArgoCD to be ready..."
    kubectl wait --for=condition=available --timeout=300s \
        deployment/argocd-server -n argocd

    log_info "ArgoCD installed successfully"
}

setup_ingress() {
    if [[ "${SKIP_INGRESS:-false}" == "true" ]]; then
        log_warn "Skipping ingress setup"
        return
    fi

    log_info "Setting up ArgoCD ingress..."
    kubectl apply -f "${SCRIPT_DIR}/ingress/ingress.yaml"
    log_info "Ingress configured"
}

apply_applications() {
    log_info "Applying ArgoCD applications..."

    kubectl apply -f "${SCRIPT_DIR}/applications/qawave.yaml"

    log_info "Applications configured"
}

get_admin_password() {
    log_info "Getting initial admin password..."

    ADMIN_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret \
        -o jsonpath="{.data.password}" 2>/dev/null | base64 -d || echo "")

    if [[ -n "${ADMIN_PASSWORD}" ]]; then
        echo ""
        echo "=========================================="
        echo "ArgoCD Admin Credentials"
        echo "=========================================="
        echo "Username: admin"
        echo "Password: ${ADMIN_PASSWORD}"
        echo ""
        echo "IMPORTANT: Change this password after first login!"
        echo "=========================================="
    else
        log_warn "Initial admin secret not found. ArgoCD may use a different auth method."
    fi
}

print_access_info() {
    echo ""
    echo "=========================================="
    echo "ArgoCD Access Information"
    echo "=========================================="
    echo ""
    echo "1. Port Forward (Development):"
    echo "   kubectl port-forward svc/argocd-server -n argocd 8080:443"
    echo "   Access: https://localhost:8080"
    echo ""
    echo "2. Via Ingress (Production):"
    echo "   Access: https://argocd.qawave.io"
    echo ""
    echo "3. ArgoCD CLI:"
    echo "   argocd login argocd.qawave.io"
    echo "   argocd app list"
    echo ""
    echo "=========================================="
}

main() {
    # Parse args
    SKIP_INGRESS=false
    for arg in "$@"; do
        case $arg in
            --skip-ingress)
                SKIP_INGRESS=true
                ;;
        esac
    done

    echo "======================================"
    echo "QAWave ArgoCD Setup"
    echo "======================================"
    echo ""

    check_prerequisites
    create_namespace
    install_argocd
    setup_ingress
    apply_applications
    get_admin_password
    print_access_info

    echo ""
    log_info "ArgoCD setup complete!"
}

main "$@"
