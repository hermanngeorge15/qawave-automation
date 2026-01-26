#!/bin/bash
#
# View E2E test results from Hetzner VPS staging cluster
#
# Usage:
#   ./scripts/view-e2e-results.sh           # View latest results
#   ./scripts/view-e2e-results.sh --summary # Show summary only
#   ./scripts/view-e2e-results.sh --full    # Show full logs
#   ./scripts/view-e2e-results.sh --watch   # Watch live logs
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
KUBECONFIG_PATH="$PROJECT_ROOT/infrastructure/terraform/environments/staging/kubeconfig"
NAMESPACE="staging"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

export KUBECONFIG="$KUBECONFIG_PATH"

check_kubeconfig() {
    if [ ! -f "$KUBECONFIG_PATH" ]; then
        echo -e "${RED}[ERROR]${NC} Kubeconfig not found at $KUBECONFIG_PATH"
        exit 1
    fi
}

get_latest_pod() {
    kubectl get pods -n "$NAMESPACE" -l app=e2e-tests \
        --sort-by=.metadata.creationTimestamp \
        -o jsonpath='{.items[-1].metadata.name}' 2>/dev/null
}

show_summary() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}                  E2E TEST RESULTS SUMMARY                  ${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""

    # Get job status
    JOB_STATUS=$(kubectl get jobs -n "$NAMESPACE" -l app=e2e-tests -o jsonpath='{.items[-1].status.conditions[0].type}' 2>/dev/null)
    POD=$(get_latest_pod)

    if [ -z "$POD" ]; then
        echo -e "${YELLOW}No E2E test runs found.${NC}"
        echo "Run tests with: ./scripts/run-e2e-tests.sh"
        exit 0
    fi

    POD_STATUS=$(kubectl get pod "$POD" -n "$NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null)

    echo -e "Pod:    ${BLUE}$POD${NC}"
    echo -e "Status: $(status_color "$POD_STATUS")"
    echo ""

    # Get test results from logs
    LOGS=$(kubectl logs -n "$NAMESPACE" "$POD" 2>/dev/null || echo "")

    if [ -n "$LOGS" ]; then
        # Count passed/failed
        PASSED=$(echo "$LOGS" | grep -c "✓\|passed\|✔" 2>/dev/null || echo "0")
        FAILED=$(echo "$LOGS" | grep -c "✗\|failed\|✘" 2>/dev/null || echo "0")

        echo -e "${GREEN}Passed:${NC} $PASSED"
        echo -e "${RED}Failed:${NC} $FAILED"
        echo ""

        # Show failed tests
        if [ "$FAILED" -gt 0 ]; then
            echo -e "${RED}Failed Tests:${NC}"
            echo "$LOGS" | grep -E "✗|failed|Error:|FAILED" | head -20
            echo ""
        fi

        # Show timing
        echo -e "${BLUE}Test Output (last 30 lines):${NC}"
        echo "─────────────────────────────────────────────────────────────"
        echo "$LOGS" | tail -30
    else
        echo -e "${YELLOW}No logs available yet. Test may still be running.${NC}"
    fi

    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

status_color() {
    case "$1" in
        Succeeded|Completed)
            echo -e "${GREEN}$1${NC}"
            ;;
        Failed|Error)
            echo -e "${RED}$1${NC}"
            ;;
        Running|Pending)
            echo -e "${YELLOW}$1${NC}"
            ;;
        *)
            echo "$1"
            ;;
    esac
}

show_full() {
    POD=$(get_latest_pod)
    if [ -z "$POD" ]; then
        echo -e "${YELLOW}No E2E test runs found.${NC}"
        exit 0
    fi

    echo -e "${BLUE}Full logs for pod: $POD${NC}"
    echo "═══════════════════════════════════════════════════════════"
    kubectl logs -n "$NAMESPACE" "$POD"
}

watch_logs() {
    POD=$(get_latest_pod)
    if [ -z "$POD" ]; then
        echo -e "${YELLOW}No E2E test runs found.${NC}"
        exit 0
    fi

    echo -e "${BLUE}Watching logs for pod: $POD${NC}"
    echo "Press Ctrl+C to stop"
    echo "═══════════════════════════════════════════════════════════"
    kubectl logs -n "$NAMESPACE" -f "$POD"
}

# Main
check_kubeconfig

case "${1:-}" in
    --summary|-s)
        show_summary
        ;;
    --full|-f)
        show_full
        ;;
    --watch|-w)
        watch_logs
        ;;
    *)
        show_summary
        ;;
esac
