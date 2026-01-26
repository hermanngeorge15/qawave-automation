#!/bin/bash
# =============================================================================
# K0s Kubernetes Cluster Setup Script
# =============================================================================
# Standalone script to install and configure K0s on a server.
#
# Usage:
#   ./setup-k0s.sh <server-ip> [--controller|--worker] [--controller-ip <ip>]
#
# Examples:
#   ./setup-k0s.sh 1.2.3.4 --controller          # Setup as controller
#   ./setup-k0s.sh 1.2.3.5 --worker --controller-ip 1.2.3.4  # Setup as worker
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
# Usage
# =============================================================================

usage() {
    echo "Usage: $0 <server-ip> [options]"
    echo ""
    echo "Options:"
    echo "  --controller              Setup as K0s controller (default)"
    echo "  --worker                  Setup as K0s worker"
    echo "  --controller-ip <ip>      Controller IP (required for worker)"
    echo "  --single                  Single-node mode (controller + worker)"
    echo "  -h, --help               Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 1.2.3.4 --controller --single    # Single-node cluster"
    echo "  $0 1.2.3.4 --controller             # Controller only"
    echo "  $0 1.2.3.5 --worker --controller-ip 1.2.3.4"
    exit 1
}

# =============================================================================
# Parse Arguments
# =============================================================================

SERVER_IP=""
ROLE="controller"
CONTROLLER_IP=""
SINGLE_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --controller)
            ROLE="controller"
            shift
            ;;
        --worker)
            ROLE="worker"
            shift
            ;;
        --controller-ip)
            CONTROLLER_IP="$2"
            shift 2
            ;;
        --single)
            SINGLE_MODE=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            if [ -z "$SERVER_IP" ]; then
                SERVER_IP="$1"
            else
                log_error "Unknown option: $1"
                usage
            fi
            shift
            ;;
    esac
done

if [ -z "$SERVER_IP" ]; then
    log_error "Server IP is required"
    usage
fi

if [ "$ROLE" = "worker" ] && [ -z "$CONTROLLER_IP" ]; then
    log_error "Controller IP is required for worker setup"
    usage
fi

# =============================================================================
# Setup Functions
# =============================================================================

wait_for_server() {
    log_info "Waiting for server $SERVER_IP to be ready..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no -o BatchMode=yes root@"$SERVER_IP" "echo 'ready'" 2>/dev/null; then
            log_success "Server is ready"
            return 0
        fi

        log_info "Waiting... (attempt $attempt/$max_attempts)"
        sleep 10
        ((attempt++))
    done

    log_error "Timeout waiting for server"
    exit 1
}

install_k0s() {
    log_info "Installing K0s on $SERVER_IP..."

    ssh -o StrictHostKeyChecking=no root@"$SERVER_IP" << 'EOF'
        set -e

        # Update system
        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get upgrade -y

        # Install k0s
        if ! command -v k0s &> /dev/null; then
            echo "Downloading and installing k0s..."
            curl -sSLf https://get.k0s.sh | sudo sh
        else
            echo "k0s is already installed"
        fi

        k0s version
EOF

    log_success "K0s installed"
}

setup_controller() {
    log_info "Setting up K0s controller..."

    local single_flag=""
    if [ "$SINGLE_MODE" = true ]; then
        single_flag="--single"
    fi

    ssh -o StrictHostKeyChecking=no root@"$SERVER_IP" << EOF
        set -e

        # Check if already running
        if systemctl is-active --quiet k0scontroller 2>/dev/null; then
            echo "K0s controller is already running"
            k0s status
            exit 0
        fi

        # Install k0s controller
        echo "Installing k0s controller..."
        k0s install controller $single_flag

        # Start k0s
        echo "Starting k0s..."
        k0s start

        # Wait for cluster to be ready
        echo "Waiting for cluster to initialize..."
        sleep 30

        # Verify
        k0s status

        # Install kubectl
        if ! command -v kubectl &> /dev/null; then
            curl -LO "https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
            chmod +x kubectl
            mv kubectl /usr/local/bin/
        fi

        # Setup kubeconfig
        mkdir -p /root/.kube
        k0s kubeconfig admin > /root/.kube/config
        chmod 600 /root/.kube/config

        # Verify cluster
        echo "Verifying cluster..."
        kubectl get nodes
EOF

    log_success "K0s controller is running"
}

get_join_token() {
    log_info "Getting join token from controller..."

    JOIN_TOKEN=$(ssh -o StrictHostKeyChecking=no root@"$CONTROLLER_IP" "k0s token create --role=worker")

    if [ -z "$JOIN_TOKEN" ]; then
        log_error "Failed to get join token"
        exit 1
    fi

    log_success "Join token obtained"
}

setup_worker() {
    log_info "Setting up K0s worker..."

    # First get the join token
    get_join_token

    ssh -o StrictHostKeyChecking=no root@"$SERVER_IP" << EOF
        set -e

        # Check if already running
        if systemctl is-active --quiet k0sworker 2>/dev/null; then
            echo "K0s worker is already running"
            exit 0
        fi

        # Save token
        echo "$JOIN_TOKEN" > /tmp/k0s-token

        # Install k0s worker
        echo "Installing k0s worker..."
        k0s install worker --token-file /tmp/k0s-token

        # Start k0s
        echo "Starting k0s worker..."
        k0s start

        # Clean up token
        rm -f /tmp/k0s-token

        # Verify
        sleep 10
        k0s status
EOF

    log_success "K0s worker is running"

    # Verify from controller
    log_info "Verifying worker joined cluster..."
    sleep 10
    ssh -o StrictHostKeyChecking=no root@"$CONTROLLER_IP" "kubectl get nodes"
}

# =============================================================================
# Main
# =============================================================================

main() {
    echo "=========================================="
    echo "  K0s Setup: $ROLE on $SERVER_IP"
    echo "=========================================="
    echo ""

    wait_for_server
    install_k0s

    if [ "$ROLE" = "controller" ]; then
        setup_controller
    else
        setup_worker
    fi

    echo ""
    log_success "K0s setup complete!"

    if [ "$ROLE" = "controller" ]; then
        echo ""
        echo "To get kubeconfig:"
        echo "  ssh root@$SERVER_IP 'k0s kubeconfig admin' > ~/.kube/config"
        echo ""
        echo "To add workers:"
        echo "  $0 <worker-ip> --worker --controller-ip $SERVER_IP"
    fi
}

main
