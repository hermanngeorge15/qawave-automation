#!/bin/bash
# =============================================================================
# ArgoCD Installation Script
# =============================================================================
# Installs ArgoCD on a Kubernetes cluster.
#
# Usage:
#   ./install-argocd.sh [--ha] [--kubeconfig <path>]
#
# Options:
#   --ha              Install HA (High Availability) version for production
#   --kubeconfig      Path to kubeconfig file (default: $KUBECONFIG or ~/.kube/config)
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

HA_MODE=false
KUBECONFIG_PATH="${KUBECONFIG:-$HOME/.kube/config}"
ARGOCD_NAMESPACE="argocd"
ARGOCD_VERSION="stable"

# =============================================================================
# Parse Arguments
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --ha)
            HA_MODE=true
            shift
            ;;
        --kubeconfig)
            KUBECONFIG_PATH="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [--ha] [--kubeconfig <path>]"
            echo ""
            echo "Options:"
            echo "  --ha              Install HA version for production"
            echo "  --kubeconfig      Path to kubeconfig file"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# =============================================================================
# Validation
# =============================================================================

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is required but not installed"
        exit 1
    fi

    if [ ! -f "$KUBECONFIG_PATH" ]; then
        log_error "Kubeconfig not found at: $KUBECONFIG_PATH"
        exit 1
    fi

    export KUBECONFIG="$KUBECONFIG_PATH"

    if ! kubectl cluster-info &>/dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    log_success "Prerequisites met"
}

# =============================================================================
# Installation
# =============================================================================

create_namespace() {
    log_info "Creating namespace: $ARGOCD_NAMESPACE"

    kubectl create namespace "$ARGOCD_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

    log_success "Namespace ready"
}

install_argocd() {
    local manifest_url

    if [ "$HA_MODE" = true ]; then
        log_info "Installing ArgoCD HA (High Availability) mode..."
        manifest_url="https://raw.githubusercontent.com/argoproj/argo-cd/$ARGOCD_VERSION/manifests/ha/install.yaml"
    else
        log_info "Installing ArgoCD (standard mode)..."
        manifest_url="https://raw.githubusercontent.com/argoproj/argo-cd/$ARGOCD_VERSION/manifests/install.yaml"
    fi

    kubectl apply -n "$ARGOCD_NAMESPACE" -f "$manifest_url"

    log_success "ArgoCD manifests applied"
}

wait_for_argocd() {
    log_info "Waiting for ArgoCD to be ready..."

    local timeout=600
    if [ "$HA_MODE" = true ]; then
        timeout=900  # HA mode takes longer
    fi

    kubectl wait --for=condition=available --timeout="${timeout}s" \
        deployment/argocd-server -n "$ARGOCD_NAMESPACE"

    log_success "ArgoCD is ready"
}

get_admin_password() {
    log_info "Getting admin password..."

    # Wait for secret to be created
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if kubectl -n "$ARGOCD_NAMESPACE" get secret argocd-initial-admin-secret &>/dev/null; then
            ARGOCD_PASSWORD=$(kubectl -n "$ARGOCD_NAMESPACE" get secret argocd-initial-admin-secret \
                -o jsonpath="{.data.password}" | base64 -d)
            return 0
        fi

        log_info "Waiting for admin secret... (attempt $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done

    log_warn "Could not get admin password. It may need to be retrieved manually."
    ARGOCD_PASSWORD="<not available>"
}

setup_ingress_nodeport() {
    log_info "Configuring ArgoCD for NodePort access..."

    # Patch the argocd-server service to use NodePort
    kubectl patch svc argocd-server -n "$ARGOCD_NAMESPACE" \
        -p '{"spec": {"type": "NodePort", "ports": [{"name": "https", "port": 443, "targetPort": 8080, "nodePort": 30443}]}}'

    log_success "ArgoCD accessible via NodePort 30443"
}

# =============================================================================
# CLI Installation (Optional)
# =============================================================================

install_argocd_cli() {
    if command -v argocd &> /dev/null; then
        log_info "ArgoCD CLI already installed"
        return 0
    fi

    log_info "Installing ArgoCD CLI..."

    local os
    local arch

    case "$(uname -s)" in
        Linux*)  os="linux" ;;
        Darwin*) os="darwin" ;;
        *)       log_warn "Unsupported OS, skipping CLI installation"; return 0 ;;
    esac

    case "$(uname -m)" in
        x86_64)  arch="amd64" ;;
        arm64|aarch64) arch="arm64" ;;
        *)       log_warn "Unsupported architecture, skipping CLI installation"; return 0 ;;
    esac

    local cli_url="https://github.com/argoproj/argo-cd/releases/latest/download/argocd-${os}-${arch}"

    if curl -sSL -o /tmp/argocd "$cli_url"; then
        chmod +x /tmp/argocd
        sudo mv /tmp/argocd /usr/local/bin/argocd
        log_success "ArgoCD CLI installed"
    else
        log_warn "Failed to install ArgoCD CLI"
    fi
}

# =============================================================================
# Main
# =============================================================================

print_summary() {
    local nodeport
    nodeport=$(kubectl get svc argocd-server -n "$ARGOCD_NAMESPACE" \
        -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}' 2>/dev/null || echo "N/A")

    echo ""
    echo "=========================================="
    echo -e "${GREEN}  ArgoCD Installation Complete${NC}"
    echo "=========================================="
    echo ""
    echo "  Mode: $([ "$HA_MODE" = true ] && echo "High Availability" || echo "Standard")"
    echo "  Namespace: $ARGOCD_NAMESPACE"
    echo ""
    echo "  Credentials:"
    echo "    Username: admin"
    echo "    Password: $ARGOCD_PASSWORD"
    echo ""
    echo "  Access:"
    echo "    NodePort: $nodeport"
    echo "    URL: https://<node-ip>:$nodeport"
    echo ""
    echo "  Port Forward (local access):"
    echo "    kubectl port-forward svc/argocd-server -n $ARGOCD_NAMESPACE 8080:443"
    echo "    Then open: https://localhost:8080"
    echo ""
    echo "  CLI Login:"
    echo "    argocd login <node-ip>:$nodeport --username admin --password '$ARGOCD_PASSWORD' --insecure"
    echo ""
    echo "=========================================="
}

main() {
    echo "=========================================="
    echo "  ArgoCD Installation"
    echo "=========================================="
    echo ""

    check_prerequisites
    create_namespace
    install_argocd
    wait_for_argocd
    get_admin_password
    setup_ingress_nodeport
    install_argocd_cli
    print_summary
}

main
