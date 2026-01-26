# Sealed Secrets for QAWave

Sealed Secrets allows you to encrypt Kubernetes secrets that can be safely stored in Git.

## How It Works

```
                    ┌──────────────────┐
                    │   Developer      │
                    │   Workstation    │
                    └────────┬─────────┘
                             │
                    kubeseal --cert public.pem
                             │
                             ▼
                    ┌──────────────────┐
                    │  SealedSecret    │◄──── Can be stored in Git
                    │  (encrypted)     │      (safe to commit!)
                    └────────┬─────────┘
                             │
                    kubectl apply
                             │
                             ▼
            ┌────────────────────────────────┐
            │         Kubernetes Cluster      │
            │  ┌───────────────────────────┐  │
            │  │  Sealed Secrets Controller │  │
            │  │  (has private key)         │  │
            │  └─────────────┬─────────────┘  │
            │                │                 │
            │           decrypts               │
            │                │                 │
            │                ▼                 │
            │  ┌───────────────────────────┐  │
            │  │      Secret (plain)       │  │
            │  │   (never leaves cluster)  │  │
            │  └───────────────────────────┘  │
            └────────────────────────────────┘
```

## Installation

### 1. Install Sealed Secrets Controller

```bash
# Add Helm repo
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm repo update

# Install controller
helm install sealed-secrets sealed-secrets/sealed-secrets \
  --namespace kube-system \
  --values values.yaml
```

### 2. Install kubeseal CLI

```bash
# macOS
brew install kubeseal

# Linux (amd64)
KUBESEAL_VERSION=$(curl -s https://api.github.com/repos/bitnami-labs/sealed-secrets/releases/latest | jq -r '.tag_name' | cut -c2-)
wget "https://github.com/bitnami-labs/sealed-secrets/releases/download/v${KUBESEAL_VERSION}/kubeseal-${KUBESEAL_VERSION}-linux-amd64.tar.gz"
tar -xvzf kubeseal-${KUBESEAL_VERSION}-linux-amd64.tar.gz kubeseal
sudo install -m 755 kubeseal /usr/local/bin/kubeseal

# Verify
kubeseal --version
```

### 3. Fetch the Public Key

```bash
# Get the public key from the cluster
kubeseal --fetch-cert \
  --controller-name=sealed-secrets \
  --controller-namespace=kube-system \
  > public-key.pem

# Store this file safely - you'll need it to seal secrets
```

## Creating Sealed Secrets

### Method 1: From Literal Values

```bash
# Create a regular secret (don't apply it!)
kubectl create secret generic my-secret \
  --from-literal=username=admin \
  --from-literal=password=super-secret-password \
  --dry-run=client -o yaml > my-secret.yaml

# Seal it
kubeseal --format yaml \
  --cert public-key.pem \
  < my-secret.yaml > my-sealed-secret.yaml

# Apply the sealed secret
kubectl apply -f my-sealed-secret.yaml

# Verify the secret was created
kubectl get secret my-secret
```

### Method 2: From Files

```bash
# Create secret from file
kubectl create secret generic tls-secret \
  --from-file=tls.crt=server.crt \
  --from-file=tls.key=server.key \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > tls-sealed-secret.yaml
```

### Method 3: Using stdin

```bash
echo -n "my-database-password" | \
kubectl create secret generic db-password \
  --from-file=password=/dev/stdin \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > db-sealed-secret.yaml
```

## QAWave Secrets

### Database Password

```bash
# Create database credentials
kubectl create secret generic qawave-db-credentials \
  --namespace qawave \
  --from-literal=DB_USER=qawave \
  --from-literal=DB_PASSWORD=$(openssl rand -base64 32) \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > qawave-db-sealed-secret.yaml
```

### Redis Password

```bash
kubectl create secret generic qawave-redis-credentials \
  --namespace qawave \
  --from-literal=REDIS_PASSWORD=$(openssl rand -base64 32) \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > qawave-redis-sealed-secret.yaml
```

### AI API Key

```bash
kubectl create secret generic qawave-ai-credentials \
  --namespace qawave \
  --from-literal=AI_API_KEY=sk-your-openai-key-here \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > qawave-ai-sealed-secret.yaml
```

## Important Notes

### Namespace Binding

By default, sealed secrets are **namespace-bound**. A sealed secret can only be decrypted in the namespace it was created for.

```bash
# Create secret for specific namespace
kubectl create secret generic my-secret \
  --namespace production \
  --from-literal=key=value \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > my-sealed-secret.yaml
```

### Cluster-Wide Secrets

For secrets that need to work across namespaces:

```bash
kubeseal --format yaml \
  --cert public-key.pem \
  --scope cluster-wide \
  < my-secret.yaml > my-sealed-secret.yaml
```

### Updating Secrets

To update a sealed secret, create a new one with the same name:

```bash
# Create new version
kubectl create secret generic my-secret \
  --from-literal=password=new-password \
  --dry-run=client -o yaml | \
kubeseal --format yaml --cert public-key.pem \
  > my-sealed-secret.yaml

# Apply (will update existing)
kubectl apply -f my-sealed-secret.yaml
```

## Backup and Recovery

### Backup the Encryption Key

**CRITICAL**: Back up the sealing key pair! If lost, all sealed secrets become unrecoverable.

```bash
# Export the encryption key (run on cluster with admin access)
kubectl get secret -n kube-system sealed-secrets-key -o yaml > sealed-secrets-key-backup.yaml

# Store this SECURELY (encrypted, offline storage)
# Do NOT commit to Git!
```

### Disaster Recovery

If you need to restore on a new cluster:

```bash
# Apply the backup key BEFORE installing sealed-secrets
kubectl apply -f sealed-secrets-key-backup.yaml

# Then install sealed-secrets controller
helm install sealed-secrets sealed-secrets/sealed-secrets \
  --namespace kube-system \
  --values values.yaml
```

## Troubleshooting

### Secret Not Decrypting

```bash
# Check controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=sealed-secrets

# Check sealed secret status
kubectl describe sealedsecret my-sealed-secret
```

### Wrong Namespace Error

```
error: cannot decrypt sealed secret: no key could decrypt
```

This usually means the secret was sealed for a different namespace. Re-seal with the correct namespace.

### Key Rotation

The controller generates a new key every 30 days by default. Old keys are kept for decryption.

```bash
# List all keys
kubectl get secret -n kube-system -l sealedsecrets.bitnami.com/sealed-secrets-key

# Force key rotation (creates new key)
kubectl delete pod -n kube-system -l app.kubernetes.io/name=sealed-secrets
```

## Security Best Practices

1. **Never commit the private key** - Only the public key is safe to share
2. **Use namespace-bound secrets** when possible
3. **Rotate secrets regularly** - Create new sealed secrets periodically
4. **Audit access** - The controller has access to all secrets
5. **Backup encryption keys** - Store securely offline

## References

- [Sealed Secrets GitHub](https://github.com/bitnami-labs/sealed-secrets)
- [Helm Chart](https://github.com/bitnami-labs/sealed-secrets/tree/main/helm/sealed-secrets)
- [kubeseal CLI Docs](https://github.com/bitnami-labs/sealed-secrets#kubeseal)
