#!/bin/bash
#
# Run E2E tests on Hetzner VPS Kubernetes cluster
#
# Usage:
#   ./scripts/run-e2e-tests.sh           # Run tests and follow logs
#   ./scripts/run-e2e-tests.sh --watch   # Watch logs only (if job already running)
#   ./scripts/run-e2e-tests.sh --status  # Check job status
#   ./scripts/run-e2e-tests.sh --cleanup # Delete completed jobs
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_PATH="$PROJECT_ROOT/infrastructure/terraform/environments/staging/kubeconfig"
JOB_MANIFEST="$PROJECT_ROOT/gitops/envs/staging/e2e-tests/job.yaml"
NAMESPACE="staging"
JOB_NAME="e2e-tests"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

export KUBECONFIG="$KUBECONFIG_PATH"

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_kubeconfig() {
    if [ ! -f "$KUBECONFIG_PATH" ]; then
        log_error "Kubeconfig not found at $KUBECONFIG_PATH"
        log_info "Run: ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' | sed 's|https://10.1.1.10:6443|https://91.99.107.246:6443|' > $KUBECONFIG_PATH"
        exit 1
    fi
}

show_status() {
    log_info "Current E2E test jobs:"
    kubectl get jobs -n "$NAMESPACE" -l app=e2e-tests -o wide 2>/dev/null || echo "No jobs found"
    echo ""
    log_info "Pods:"
    kubectl get pods -n "$NAMESPACE" -l app=e2e-tests -o wide 2>/dev/null || echo "No pods found"
}

cleanup_jobs() {
    log_info "Cleaning up completed E2E test jobs..."
    kubectl delete jobs -n "$NAMESPACE" -l app=e2e-tests --field-selector status.successful=1 2>/dev/null || true
    kubectl delete jobs -n "$NAMESPACE" -l app=e2e-tests --field-selector status.failed=1 2>/dev/null || true
    log_info "Cleanup complete"
}

watch_logs() {
    POD=$(kubectl get pods -n "$NAMESPACE" -l app=e2e-tests --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[-1].metadata.name}' 2>/dev/null)
    if [ -z "$POD" ]; then
        log_error "No E2E test pod found"
        exit 1
    fi
    log_info "Following logs for pod: $POD"
    kubectl logs -n "$NAMESPACE" -f "$POD"
}

run_tests() {
    log_info "Starting E2E tests on Hetzner VPS..."

    # Delete existing job if any
    kubectl delete job "$JOB_NAME" -n "$NAMESPACE" 2>/dev/null || true

    # Apply ConfigMap and Job
    log_info "Creating E2E test job..."
    kubectl apply -f "$JOB_MANIFEST"

    # Wait for pod to be created
    log_info "Waiting for test pod to start..."
    sleep 5

    # Get pod name
    POD=""
    for i in {1..30}; do
        POD=$(kubectl get pods -n "$NAMESPACE" -l app=e2e-tests --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[-1].metadata.name}' 2>/dev/null)
        if [ -n "$POD" ]; then
            break
        fi
        sleep 2
    done

    if [ -z "$POD" ]; then
        log_error "Failed to get test pod"
        exit 1
    fi

    log_info "Test pod: $POD"

    # Wait for pod to be running
    kubectl wait --for=condition=Ready pod/"$POD" -n "$NAMESPACE" --timeout=120s 2>/dev/null || true

    # Follow logs
    log_info "Following test logs..."
    echo "=========================================="
    kubectl logs -n "$NAMESPACE" -f "$POD" || true
    echo "=========================================="

    # Check job status
    STATUS=$(kubectl get job "$JOB_NAME" -n "$NAMESPACE" -o jsonpath='{.status.conditions[0].type}' 2>/dev/null)
    if [ "$STATUS" = "Complete" ]; then
        log_info "E2E tests completed successfully!"
        exit 0
    else
        log_error "E2E tests failed!"
        exit 1
    fi
}

# Main
check_kubeconfig

case "${1:-}" in
    --status)
        show_status
        ;;
    --cleanup)
        cleanup_jobs
        ;;
    --watch)
        watch_logs
        ;;
    *)
        run_tests
        ;;
esac
