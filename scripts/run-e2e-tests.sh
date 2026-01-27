#!/bin/bash
#
# Run E2E tests on Hetzner VPS Kubernetes cluster
#
# Usage:
#   ./scripts/run-e2e-tests.sh                # Run tests locally against staging
#   ./scripts/run-e2e-tests.sh --local        # Run tests locally (explicit)
#   ./scripts/run-e2e-tests.sh --k8s          # Run tests as Kubernetes Job
#   ./scripts/run-e2e-tests.sh --k8s --build  # Build image and run in K8s
#   ./scripts/run-e2e-tests.sh --smoke        # Run only smoke tests
#   ./scripts/run-e2e-tests.sh --api          # Run only API tests
#   ./scripts/run-e2e-tests.sh --status       # Check job status
#   ./scripts/run-e2e-tests.sh --cleanup      # Delete completed jobs
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
E2E_DIR="$PROJECT_ROOT/e2e-tests"
KUBECONFIG_PATH="$PROJECT_ROOT/infrastructure/terraform/environments/staging/kubeconfig"
KUBECONFIG_ALT="${HOME}/.kube/config-qawave-staging"
JOB_TEMPLATE="$PROJECT_ROOT/gitops/envs/staging/e2e-tests/e2e-job-template.yaml"
NAMESPACE="staging"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
WORKER_IP="${WORKER_IP:-46.224.232.46}"
FRONTEND_URL="http://${WORKER_IP}:30000"
API_URL="http://localhost:8080"  # Uses port-forward

# Options
RUN_MODE="local"
BUILD_IMAGE=false
TEST_FILTER=""

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --local)
                RUN_MODE="local"
                shift
                ;;
            --k8s)
                RUN_MODE="k8s"
                shift
                ;;
            --build)
                BUILD_IMAGE=true
                shift
                ;;
            --smoke)
                TEST_FILTER="--grep @smoke"
                shift
                ;;
            --api)
                TEST_FILTER="--project=staging-api"
                shift
                ;;
            --status)
                RUN_MODE="status"
                shift
                ;;
            --cleanup)
                RUN_MODE="cleanup"
                shift
                ;;
            --watch)
                RUN_MODE="watch"
                shift
                ;;
            --help|-h)
                print_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
        esac
    done
}

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Run Modes:"
    echo "  --local     Run tests locally against staging (default)"
    echo "  --k8s       Run tests as Kubernetes Job in cluster"
    echo "  --status    Check job status"
    echo "  --cleanup   Delete completed jobs"
    echo "  --watch     Watch logs of running job"
    echo ""
    echo "Options:"
    echo "  --build     Build and push Docker image (with --k8s)"
    echo "  --smoke     Run only smoke tests (@smoke tag)"
    echo "  --api       Run only API tests"
    echo ""
    echo "Examples:"
    echo "  $0                    # Run all tests locally"
    echo "  $0 --smoke            # Run smoke tests locally"
    echo "  $0 --k8s              # Run tests in K8s cluster"
    echo "  $0 --k8s --build      # Build image and run in K8s"
}

get_kubeconfig() {
    if [ -f "$KUBECONFIG_PATH" ]; then
        export KUBECONFIG="$KUBECONFIG_PATH"
    elif [ -f "$KUBECONFIG_ALT" ]; then
        export KUBECONFIG="$KUBECONFIG_ALT"
    else
        log_error "Kubeconfig not found"
        log_info "Fetch it with: ssh -i ~/.ssh/qawave-staging root@91.99.107.246 'k0s kubeconfig admin' > $KUBECONFIG_ALT"
        log_info "Then fix server: sed -i '' 's|10.1.1.10|91.99.107.246|g' $KUBECONFIG_ALT"
        exit 1
    fi
}

build_and_push_image() {
    log_info "Building E2E tests Docker image..."

    cd "$E2E_DIR"

    docker build -t ghcr.io/hermanngeorge15/qawave-e2e-tests:latest .

    log_info "Pushing image to GHCR..."

    if [ -n "${GITHUB_TOKEN:-}" ]; then
        echo "$GITHUB_TOKEN" | docker login ghcr.io -u hermanngeorge15 --password-stdin
    fi

    docker push ghcr.io/hermanngeorge15/qawave-e2e-tests:latest

    cd "$PROJECT_ROOT"
    log_success "Image built and pushed"
}

setup_port_forward() {
    log_info "Setting up port-forward to backend..."

    # Kill any existing port-forward
    pkill -f "kubectl port-forward.*backend" 2>/dev/null || true

    get_kubeconfig

    # Start port-forward in background
    kubectl port-forward svc/backend 8080:8080 -n staging &>/dev/null &
    PF_PID=$!

    # Wait for port-forward to be ready
    sleep 3

    if ! curl -s http://localhost:8080/actuator/health &>/dev/null; then
        log_warn "Port-forward may not be ready, continuing anyway..."
    else
        log_success "Port-forward ready (PID: $PF_PID)"
    fi

    echo $PF_PID
}

cleanup_port_forward() {
    pkill -f "kubectl port-forward.*backend" 2>/dev/null || true
}

run_local() {
    log_info "Running E2E tests locally against staging..."

    cd "$E2E_DIR"

    # Install dependencies
    if [ ! -d "node_modules" ]; then
        log_info "Installing dependencies..."
        npm ci
    fi

    # Setup port-forward for API access
    PF_PID=$(setup_port_forward)

    trap cleanup_port_forward EXIT

    log_info "Test configuration:"
    echo "  Frontend URL: $FRONTEND_URL"
    echo "  API URL: $API_URL"
    echo "  Test filter: ${TEST_FILTER:-all tests}"
    echo ""

    # Run tests
    set +e
    BASE_URL="$FRONTEND_URL" \
    API_URL="$API_URL" \
    npx playwright test \
        --config=playwright.staging.config.ts \
        $TEST_FILTER \
        --reporter=list,html

    EXIT_CODE=$?
    set -e

    cd "$PROJECT_ROOT"

    echo ""
    if [ $EXIT_CODE -eq 0 ]; then
        log_success "All tests passed!"
    else
        log_error "Tests failed (exit code: $EXIT_CODE)"
    fi

    echo ""
    log_info "HTML report: $E2E_DIR/playwright-report/index.html"
    log_info "View report: cd e2e-tests && npx playwright show-report"

    return $EXIT_CODE
}

run_k8s() {
    log_info "Running E2E tests as Kubernetes Job..."

    get_kubeconfig

    RUN_ID="$(date +%s)"
    JOB_NAME="e2e-tests-$RUN_ID"

    log_info "Creating job: $JOB_NAME"

    # Apply job from template
    if [ -f "$JOB_TEMPLATE" ]; then
        cat "$JOB_TEMPLATE" | sed "s/\${RUN_ID}/$RUN_ID/g" | kubectl apply -f -
    else
        log_error "Job template not found: $JOB_TEMPLATE"
        exit 1
    fi

    log_info "Waiting for job to complete (timeout: 15 min)..."

    # Wait for completion
    set +e
    if kubectl wait --for=condition=complete job/$JOB_NAME -n $NAMESPACE --timeout=900s 2>/dev/null; then
        EXIT_CODE=0
    elif kubectl wait --for=condition=failed job/$JOB_NAME -n $NAMESPACE --timeout=10s 2>/dev/null; then
        EXIT_CODE=1
    else
        EXIT_CODE=1
    fi
    set -e

    # Get logs
    POD_NAME=$(kubectl get pods -n $NAMESPACE -l job-name=$JOB_NAME -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

    if [ -n "$POD_NAME" ]; then
        echo ""
        echo "=========================================="
        echo "TEST OUTPUT"
        echo "=========================================="
        kubectl logs "$POD_NAME" -n $NAMESPACE || true
        echo "=========================================="
    fi

    if [ $EXIT_CODE -eq 0 ]; then
        log_success "E2E tests passed!"
    else
        log_error "E2E tests failed!"
    fi

    return $EXIT_CODE
}

show_status() {
    get_kubeconfig

    log_info "E2E test jobs:"
    kubectl get jobs -n $NAMESPACE -l component=e2e-tests -o wide 2>/dev/null || echo "No jobs found"
    echo ""
    log_info "Pods:"
    kubectl get pods -n $NAMESPACE -l component=e2e-tests -o wide 2>/dev/null || echo "No pods found"
}

cleanup_jobs() {
    get_kubeconfig

    log_info "Cleaning up completed E2E test jobs..."
    kubectl delete jobs -n $NAMESPACE -l component=e2e-tests --field-selector status.successful=1 2>/dev/null || true
    kubectl delete jobs -n $NAMESPACE -l component=e2e-tests --field-selector status.failed=1 2>/dev/null || true
    log_success "Cleanup complete"
}

watch_logs() {
    get_kubeconfig

    POD=$(kubectl get pods -n $NAMESPACE -l component=e2e-tests --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[-1].metadata.name}' 2>/dev/null)
    if [ -z "$POD" ]; then
        log_error "No E2E test pod found"
        exit 1
    fi
    log_info "Following logs for: $POD"
    kubectl logs -n $NAMESPACE -f "$POD"
}

# Main
main() {
    echo ""
    echo "=========================================="
    echo "QAWave E2E Tests"
    echo "=========================================="
    echo ""

    parse_args "$@"

    case "$RUN_MODE" in
        local)
            if [ "$BUILD_IMAGE" = true ]; then
                log_warn "--build ignored in local mode"
            fi
            run_local
            ;;
        k8s)
            if [ "$BUILD_IMAGE" = true ]; then
                build_and_push_image
            fi
            run_k8s
            ;;
        status)
            show_status
            ;;
        cleanup)
            cleanup_jobs
            ;;
        watch)
            watch_logs
            ;;
    esac
}

main "$@"
