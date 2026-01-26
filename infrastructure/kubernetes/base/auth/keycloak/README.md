# Keycloak Deployment for QAWave

This directory contains the Kubernetes deployment configuration for Keycloak, the identity and access management server for QAWave.

## Overview

Keycloak provides:
- User authentication and authorization
- OAuth 2.0 / OpenID Connect endpoints
- User federation and identity brokering
- Admin console for user management
- Role-based access control (RBAC)

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │────▶│    Keycloak     │────▶│   PostgreSQL    │
│   (PKCE Auth)   │     │   (auth.qawave) │     │   (keycloak db) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │                       │
        ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│    Backend      │────▶│  Token          │
│ (JWT Validate)  │     │  Introspection  │
└─────────────────┘     └─────────────────┘
```

## Prerequisites

1. **Kubernetes cluster** with ingress controller (nginx-ingress)
2. **PostgreSQL** deployed and accessible
3. **Helm 3.x** installed
4. **cert-manager** for TLS certificates (optional but recommended)

## Installation

### Step 1: Add Bitnami Helm Repository

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### Step 2: Create Namespace (if not exists)

```bash
kubectl create namespace qawave
```

### Step 3: Initialize Keycloak Database

Apply the database initialization ConfigMap and run the init script:

```bash
# Apply ConfigMap
kubectl apply -f init-db-configmap.yaml

# Execute init script on PostgreSQL
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -f /tmp/init-keycloak.sql

# Or manually create database:
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -c "CREATE USER keycloak WITH PASSWORD 'your-password';"
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -c "CREATE DATABASE keycloak OWNER keycloak;"
```

### Step 4: Create Secrets

```bash
# Copy example and customize
cp secrets.yaml.example secrets.yaml
# Edit secrets.yaml with actual passwords

# Apply secrets
kubectl apply -f secrets.yaml

# For production, use Sealed Secrets:
kubeseal --format yaml < secrets.yaml > sealed-secrets.yaml
kubectl apply -f sealed-secrets.yaml
```

### Step 5: Deploy Keycloak

```bash
# Using Helm directly
helm install keycloak bitnami/keycloak \
  --namespace qawave \
  --values values.yaml \
  --set auth.existingSecret=keycloak-secrets \
  --set externalDatabase.existingSecret=keycloak-db-secrets

# Or using ArgoCD (recommended)
kubectl apply -f ../../argocd/applications/keycloak.yaml
```

### Step 6: Verify Deployment

```bash
# Check pods
kubectl get pods -n qawave -l app.kubernetes.io/name=keycloak

# Check service
kubectl get svc -n qawave -l app.kubernetes.io/name=keycloak

# Check ingress
kubectl get ingress -n qawave -l app.kubernetes.io/name=keycloak

# View logs
kubectl logs -n qawave -l app.kubernetes.io/name=keycloak -f
```

## Configuration

### Environment Variables

Key environment variables configured in `values.yaml`:

| Variable | Description | Default |
|----------|-------------|---------|
| `KC_HEALTH_ENABLED` | Enable health endpoints | `true` |
| `KC_METRICS_ENABLED` | Enable Prometheus metrics | `true` |
| `KC_HOSTNAME_STRICT` | Strict hostname validation | `false` |
| `KC_CACHE` | Cache mode (local/ispn) | `local` |

### Database Configuration

Keycloak uses an external PostgreSQL database:
- Host: `postgresql.qawave.svc.cluster.local`
- Port: `5432`
- Database: `keycloak`
- User: `keycloak`

### TLS Configuration

TLS is terminated at the ingress controller. For end-to-end TLS:

1. Enable cert-manager cluster issuer
2. Configure ingress TLS settings in `values.yaml`

## Accessing Keycloak

### Admin Console

- URL: `https://auth.qawave.local/admin`
- Default admin: Configured in secrets

### Account Console

- URL: `https://auth.qawave.local/realms/{realm}/account`

### OpenID Configuration

- URL: `https://auth.qawave.local/realms/{realm}/.well-known/openid-configuration`

## Monitoring

### Health Endpoints

- Liveness: `http://keycloak:8080/health/live`
- Readiness: `http://keycloak:8080/health/ready`
- Started: `http://keycloak:8080/health/started`

### Metrics

Prometheus metrics available at: `http://keycloak:8080/metrics`

## Backup and Recovery

### Export Realm Configuration

```bash
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm qawave
```

### Import Realm Configuration

```bash
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kc.sh import \
  --dir /tmp/import
```

## Troubleshooting

### Common Issues

1. **Pod stuck in CrashLoopBackOff**
   - Check database connectivity
   - Verify secrets are correct
   - Check resource limits

2. **Cannot access admin console**
   - Verify ingress configuration
   - Check TLS certificates
   - Ensure proxy headers are configured

3. **Database connection errors**
   - Verify PostgreSQL is running
   - Check network policies
   - Validate connection credentials

### Useful Commands

```bash
# Get pod logs
kubectl logs -n qawave -l app.kubernetes.io/name=keycloak --tail=100

# Describe pod
kubectl describe pod -n qawave -l app.kubernetes.io/name=keycloak

# Check events
kubectl get events -n qawave --sort-by=.metadata.creationTimestamp

# Port forward for local access
kubectl port-forward svc/keycloak -n qawave 8080:80
```

## Security Considerations

1. **Admin credentials**: Use strong passwords, rotate regularly
2. **Network policies**: Restrict access to Keycloak pods
3. **TLS**: Always use HTTPS in production
4. **Secrets**: Use Sealed Secrets or external secret management
5. **Updates**: Keep Keycloak updated for security patches

## Related Documentation

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Bitnami Keycloak Chart](https://github.com/bitnami/charts/tree/main/bitnami/keycloak)
- [OAuth 2.0 / OIDC](https://oauth.net/2/)
