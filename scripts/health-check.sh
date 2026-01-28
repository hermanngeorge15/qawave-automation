#!/bin/bash
set -euo pipefail

#######################################
# QAWave Staging Health Check
#
# Verifies all services in staging are healthy.
#
# Usage:
#   ./scripts/health-check.sh
#######################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

KUBECONFIG_PATH="${HOME}/.kube/config-qawave-staging"
NAMESPACE="staging"

# Track overall health
OVERALL_HEALTHY=true

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    OVERALL_HEALTHY=false
}

check_kubeconfig() {
    if [ ! -f "$KUBECONFIG_PATH" ]; then
        log_error "Kubeconfig not found at $KUBECONFIG_PATH"
        echo "Run ./scripts/setup-staging.sh first"
        exit 1
    fi
    export KUBECONFIG="$KUBECONFIG_PATH"
}

check_cluster_connection() {
    log_info "Checking cluster connection..."

    if kubectl cluster-info &>/dev/null; then
        log_success "Cluster is reachable"
    else
        log_error "Cannot connect to cluster"
        exit 1
    fi
}

check_namespace() {
    log_info "Checking namespace..."

    if kubectl get namespace "$NAMESPACE" &>/dev/null; then
        log_success "Namespace '$NAMESPACE' exists"
    else
        log_error "Namespace '$NAMESPACE' not found"
    fi
}

check_deployment() {
    local name=$1
    local min_ready=${2:-1}

    if ! kubectl get deployment "$name" -n "$NAMESPACE" &>/dev/null; then
        log_warning "Deployment '$name' not found"
        return
    fi

    local ready=$(kubectl get deployment "$name" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local desired=$(kubectl get deployment "$name" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")

    ready=${ready:-0}

    if [ "$ready" -ge "$min_ready" ]; then
        log_success "Deployment '$name': $ready/$desired replicas ready"
    else
        log_error "Deployment '$name': $ready/$desired replicas ready (need at least $min_ready)"
    fi
}

check_pod_status() {
    log_info "Checking pod status..."

    local pods=$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null || echo "")

    if [ -z "$pods" ]; then
        log_warning "No pods found in namespace '$NAMESPACE'"
        return
    fi

    echo "$pods" | while read -r line; do
        local name=$(echo "$line" | awk '{print $1}')
        local ready=$(echo "$line" | awk '{print $2}')
        local status=$(echo "$line" | awk '{print $3}')
        local restarts=$(echo "$line" | awk '{print $4}')

        if [ "$status" == "Running" ]; then
            if [ "$restarts" -gt 5 ]; then
                log_warning "Pod '$name': $status ($ready) - High restarts: $restarts"
            else
                log_success "Pod '$name': $status ($ready)"
            fi
        elif [ "$status" == "Completed" ]; then
            log_success "Pod '$name': $status"
        else
            log_error "Pod '$name': $status ($ready)"
        fi
    done
}

check_services() {
    log_info "Checking services..."

    local services=("backend" "frontend" "postgresql" "redis")

    for svc in "${services[@]}"; do
        if kubectl get service "$svc" -n "$NAMESPACE" &>/dev/null; then
            local type=$(kubectl get service "$svc" -n "$NAMESPACE" -o jsonpath='{.spec.type}')
            local port=$(kubectl get service "$svc" -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].port}')
            log_success "Service '$svc': $type on port $port"
        else
            log_warning "Service '$svc' not found"
        fi
    done
}

check_secrets() {
    log_info "Checking secrets..."

    if kubectl get secret ghcr-secret -n "$NAMESPACE" &>/dev/null; then
        log_success "Image pull secret 'ghcr-secret' exists"
    else
        log_error "Image pull secret 'ghcr-secret' not found"
    fi

    if kubectl get secret backend-secrets -n "$NAMESPACE" &>/dev/null; then
        log_success "Backend secrets exist"
    else
        log_warning "Backend secrets not found"
    fi
}

check_endpoints() {
    log_info "Checking external endpoints..."

    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local project_root="$(dirname "$script_dir")"

    # Get Worker IP from Terraform output (frontend runs on worker nodes via NodePort)
    local worker_ip=""
    if [ -f "$project_root/infrastructure/terraform/environments/staging/terraform.tfstate" ]; then
        worker_ip=$(cd "$project_root/infrastructure/terraform/environments/staging" && terraform output -raw worker_1_ip 2>/dev/null || echo "")
    fi

    if [ -z "$worker_ip" ]; then
        # Try to extract control plane IP from kubeconfig
        worker_ip=$(grep -oE 'server: https://[0-9.]+' "$KUBECONFIG_PATH" 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' || echo "")
    fi

    if [ -n "$worker_ip" ]; then
        # Check frontend
        if curl -s -o /dev/null -w "%{http_code}" "http://$worker_ip:30000" 2>/dev/null | grep -q "200\|304"; then
            log_success "Frontend accessible at http://$worker_ip:30000"
        else
            log_error "Frontend not accessible at http://$worker_ip:30000"
        fi

        # Check ArgoCD
        if curl -s -o /dev/null -w "%{http_code}" "http://$worker_ip:30080" 2>/dev/null | grep -q "200\|301\|302"; then
            log_success "ArgoCD accessible at http://$worker_ip:30080"
        else
            log_warning "ArgoCD not accessible at http://$worker_ip:30080"
        fi
    else
        log_warning "Could not determine worker IP for endpoint checks"
    fi
}

print_summary() {
    echo ""
    echo "=========================================="
    if [ "$OVERALL_HEALTHY" = true ]; then
        echo -e "${GREEN}Health Check: PASSED${NC}"
    else
        echo -e "${RED}Health Check: FAILED${NC}"
    fi
    echo "=========================================="
    echo ""

    # Print resource summary
    log_info "Resource Summary:"
    kubectl get all -n "$NAMESPACE" 2>/dev/null || true
    echo ""
}

# Main execution
main() {
    echo ""
    echo "=========================================="
    echo "QAWave Staging Health Check"
    echo "=========================================="
    echo ""

    check_kubeconfig
    check_cluster_connection
    check_namespace
    echo ""

    log_info "Checking deployments..."
    check_deployment "backend"
    check_deployment "frontend"
    check_deployment "postgresql"
    check_deployment "redis"
    echo ""

    check_pod_status
    echo ""

    check_services
    echo ""

    check_secrets
    echo ""

    check_endpoints
    echo ""

    print_summary

    if [ "$OVERALL_HEALTHY" = false ]; then
        exit 1
    fi
}

main "$@"
