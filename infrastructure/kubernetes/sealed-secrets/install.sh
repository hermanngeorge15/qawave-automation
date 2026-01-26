#!/bin/bash
# Sealed Secrets Installation Script for QAWave
# Usage: ./install.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="kube-system"
RELEASE_NAME="sealed-secrets"

echo "=== Sealed Secrets Installation ==="
echo ""

# Check prerequisites
command -v kubectl >/dev/null 2>&1 || { echo "kubectl is required but not installed. Aborting." >&2; exit 1; }
command -v helm >/dev/null 2>&1 || { echo "helm is required but not installed. Aborting." >&2; exit 1; }

# Verify cluster access
echo "Checking cluster access..."
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "Error: Cannot connect to Kubernetes cluster. Check your kubeconfig."
    exit 1
fi
echo "Cluster access verified."
echo ""

# Add Helm repo
echo "Adding Sealed Secrets Helm repo..."
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm repo update
echo ""

# Install Sealed Secrets
echo "Installing Sealed Secrets controller..."
helm upgrade --install "${RELEASE_NAME}" sealed-secrets/sealed-secrets \
    --namespace "${NAMESPACE}" \
    --values "${SCRIPT_DIR}/values.yaml" \
    --wait

echo ""
echo "Waiting for controller to be ready..."
kubectl rollout status deployment/"${RELEASE_NAME}" -n "${NAMESPACE}" --timeout=120s

echo ""
echo "=== Installation Complete ==="
echo ""

# Fetch public key
echo "Fetching public key for sealing secrets..."
sleep 5  # Give controller time to generate key

kubeseal --fetch-cert \
    --controller-name="${RELEASE_NAME}" \
    --controller-namespace="${NAMESPACE}" \
    > "${SCRIPT_DIR}/public-key.pem"

echo ""
echo "Public key saved to: ${SCRIPT_DIR}/public-key.pem"
echo ""
echo "=== Next Steps ==="
echo ""
echo "1. Install kubeseal CLI (if not already installed):"
echo "   brew install kubeseal  # macOS"
echo ""
echo "2. Create a sealed secret:"
echo "   kubectl create secret generic my-secret \\"
echo "     --from-literal=password=my-password \\"
echo "     --dry-run=client -o yaml | \\"
echo "   kubeseal --format yaml --cert ${SCRIPT_DIR}/public-key.pem \\"
echo "     > my-sealed-secret.yaml"
echo ""
echo "3. Apply the sealed secret:"
echo "   kubectl apply -f my-sealed-secret.yaml"
echo ""
echo "See README.md for more detailed instructions."
