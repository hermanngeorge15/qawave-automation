# QAWave Keycloak Realm Configuration

This directory contains the Keycloak realm configuration for QAWave authentication and authorization.

## Overview

The `qawave` realm provides:
- OAuth 2.0 / OpenID Connect authentication
- Role-based access control (RBAC)
- Client configurations for frontend and backend
- User management

## Realm Structure

```
qawave (realm)
├── Clients
│   ├── qawave-backend   (confidential, service account)
│   └── qawave-frontend  (public, PKCE)
├── Roles
│   ├── admin    → Can manage everything
│   ├── tester   → Can create/run tests
│   └── viewer   → Read-only access
└── Users
    ├── admin@qawave.local   (admin role)
    ├── tester@qawave.local  (tester role)
    └── viewer@qawave.local  (viewer role)
```

## Files

| File | Description |
|------|-------------|
| `qawave-realm.json` | Full realm export for import |
| `test-users.json` | Test users for development |
| `realm-configmap.yaml` | K8s ConfigMap for realm import |

## Client Configuration

### qawave-backend (Confidential Client)

For backend service-to-service authentication:

| Setting | Value |
|---------|-------|
| Client ID | `qawave-backend` |
| Client Protocol | OpenID Connect |
| Access Type | Confidential |
| Service Accounts | Enabled |
| Standard Flow | Disabled |
| Direct Access Grants | Disabled |

**Use cases:**
- Token introspection
- Service-to-service calls
- Background job authentication

### qawave-frontend (Public Client)

For SPA frontend authentication:

| Setting | Value |
|---------|-------|
| Client ID | `qawave-frontend` |
| Client Protocol | OpenID Connect |
| Access Type | Public |
| PKCE | Required (S256) |
| Standard Flow | Enabled |

**Redirect URIs:**
- `http://localhost:3000/*` (dev)
- `http://localhost:5173/*` (vite dev)
- `https://qawave.local/*` (staging)
- `https://app.qawave.local/*` (production)

## Roles

### Hierarchy

```
admin
  └── tester
        └── viewer
```

### Permissions

| Role | Packages | Scenarios | Runs | Users |
|------|----------|-----------|------|-------|
| admin | CRUD | CRUD | CRUD | CRUD |
| tester | CRU | CRUD | CRU | - |
| viewer | R | R | R | - |

## Token Configuration

| Setting | Value | Description |
|---------|-------|-------------|
| Access Token Lifespan | 5 min | Short-lived for security |
| Refresh Token Lifespan | 30 min | SSO session timeout |
| SSO Session Max | 10 hours | Maximum session length |
| Offline Session | 30 days | Remember me duration |

## Importing the Realm

### Method 1: Keycloak Admin Console

1. Log into Keycloak Admin Console
2. Select "Create realm"
3. Click "Import"
4. Upload `qawave-realm.json`

### Method 2: CLI Import

```bash
# Using kcadm.sh inside container
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password $KEYCLOAK_ADMIN_PASSWORD

kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kcadm.sh create realms \
  -f /opt/bitnami/keycloak/data/import/qawave-realm.json
```

### Method 3: Startup Import

Configure Keycloak to import on startup:

```yaml
# In values.yaml
extraEnvVars:
  - name: KEYCLOAK_EXTRA_ARGS
    value: "--import-realm"

extraVolumes:
  - name: realm-config
    secret:
      secretName: keycloak-realm-import

extraVolumeMounts:
  - name: realm-config
    mountPath: /opt/bitnami/keycloak/data/import
    readOnly: true
```

## Adding Test Users

### Via Admin Console

1. Go to Users > Add user
2. Fill in user details
3. Go to Credentials tab
4. Set password
5. Go to Role Mappings
6. Assign appropriate role

### Via CLI

```bash
# Create user
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kcadm.sh create users \
  -r qawave \
  -s username=newuser \
  -s email=newuser@example.com \
  -s enabled=true

# Set password
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kcadm.sh set-password \
  -r qawave \
  --username newuser \
  --new-password "SecurePassword123!"

# Assign role
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kcadm.sh add-roles \
  -r qawave \
  --uusername newuser \
  --rolename tester
```

## Exporting Realm Configuration

To export the current realm configuration for version control:

```bash
# Export realm without users
kubectl exec -it keycloak-0 -n qawave -- \
  /opt/bitnami/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm qawave \
  --users skip

# Copy export to local
kubectl cp qawave/keycloak-0:/tmp/export/qawave-realm.json ./qawave-realm.json
```

## Integration with Applications

### Backend (Kotlin/Spring)

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.qawave.local/realms/qawave
          jwk-set-uri: https://auth.qawave.local/realms/qawave/protocol/openid-connect/certs

keycloak:
  realm: qawave
  auth-server-url: https://auth.qawave.local
  resource: qawave-backend
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
```

### Frontend (React)

```typescript
// keycloak.ts
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'https://auth.qawave.local',
  realm: 'qawave',
  clientId: 'qawave-frontend',
});

export default keycloak;
```

## Security Considerations

1. **Client Secrets**: Store in Kubernetes Secrets, rotate regularly
2. **Token Lifetime**: Keep access tokens short-lived (5 min)
3. **PKCE**: Always use PKCE for public clients
4. **Redirect URIs**: Whitelist specific URLs only
5. **Password Policy**: Enforce strong passwords
6. **Brute Force**: Protection enabled (5 failures = lockout)

## Troubleshooting

### Token Validation Fails

1. Check clock synchronization between services
2. Verify issuer URI matches Keycloak configuration
3. Check if token is expired
4. Verify JWKS endpoint is accessible

### CORS Issues

Ensure web origins are configured in the client:
- Include all frontend URLs
- Include localhost for development

### Login Redirect Loop

1. Check redirect URI configuration
2. Verify cookies are not being blocked
3. Check browser console for errors
