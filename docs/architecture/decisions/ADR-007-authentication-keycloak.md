# ADR-007: Authentication with Keycloak OAuth2/OIDC

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs authentication and authorization to:
- Protect API endpoints from unauthorized access
- Support multi-user/multi-team scenarios
- Provide role-based access control (RBAC)
- Enable future enterprise features (SSO, LDAP integration)

We evaluated several options:
1. **Custom JWT implementation** - Build our own auth
2. **Auth0** - SaaS identity provider
3. **Keycloak** - Open-source identity server
4. **Firebase Auth** - Google's auth service

## Decision

We chose **Keycloak** as our identity provider with:
- **Frontend**: PKCE flow with `qawave-frontend` public client
- **Backend**: OAuth2 Resource Server with JWT validation
- **Roles**: Realm-level roles (admin, tester, viewer)

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                                  │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────┐         ┌─────────────┐         ┌─────────┐         ┌─────────┐
  │ Browser │         │  Keycloak   │         │ Frontend│         │ Backend │
  └────┬────┘         └──────┬──────┘         └────┬────┘         └────┬────┘
       │                     │                      │                   │
       │ 1. Access app       │                      │                   │
       │────────────────────────────────────────────►                   │
       │                     │                      │                   │
       │                     │ 2. Check auth state  │                   │
       │                     │◄─────────────────────│                   │
       │                     │                      │                   │
       │ 3. Redirect to login│                      │                   │
       │◄────────────────────────────────────────────                   │
       │                     │                      │                   │
       │ 4. Login (PKCE)     │                      │                   │
       │────────────────────►│                      │                   │
       │                     │                      │                   │
       │ 5. Auth code        │                      │                   │
       │◄────────────────────│                      │                   │
       │                     │                      │                   │
       │ 6. Exchange code    │                      │                   │
       │────────────────────►│                      │                   │
       │                     │                      │                   │
       │ 7. Access + Refresh │                      │                   │
       │    tokens (JWT)     │                      │                   │
       │◄────────────────────│                      │                   │
       │                     │                      │                   │
       │ 8. Store tokens     │                      │                   │
       │────────────────────────────────────────────►                   │
       │                     │                      │                   │
       │                     │                      │ 9. API call with  │
       │                     │                      │    Bearer token   │
       │                     │                      │──────────────────►│
       │                     │                      │                   │
       │                     │                      │       10. Validate│
       │                     │                      │           JWT     │
       │                     │◄──────────────────────────────────────────
       │                     │                      │                   │
       │                     │ 11. Token valid,     │                   │
       │                     │     return claims    │                   │
       │                     │──────────────────────────────────────────►
       │                     │                      │                   │
       │                     │                      │ 12. Check roles,  │
       │                     │                      │     authorize     │
       │                     │                      │◄──────────────────│
       │                     │                      │                   │
       │                     │                      │ 13. Response      │
       │◄────────────────────────────────────────────◄──────────────────│
       │                     │                      │                   │
```

### Keycloak Configuration

**Realm:** `qawave`

**Clients:**

| Client | Type | Flow | Use Case |
|--------|------|------|----------|
| `qawave-frontend` | Public | Authorization Code + PKCE | SPA frontend |
| `qawave-backend` | Confidential | Client Credentials | Service-to-service |

**Roles:**

| Role | Description | Permissions |
|------|-------------|-------------|
| `admin` | Full access | All operations, user management |
| `tester` | Test operations | Create/run packages, view results |
| `viewer` | Read-only | View packages and results |

Role hierarchy: `admin` includes `tester`, `tester` includes `viewer`.

### Backend Implementation

```kotlin
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(*PUBLIC_ENDPOINTS).permitAll()
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtConverter())
                }
            }
            .build()
    }
}
```

**Public Endpoints:**
- `/api/health`, `/health`, `/ready`
- `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`
- `/api-docs/**`, `/swagger-ui/**`

**Role Extraction:**
Keycloak stores roles in JWT claims:
- `realm_access.roles` - Realm-level roles
- `resource_access.{client}.roles` - Client-specific roles

### Frontend Implementation

```typescript
// PKCE flow initialization
const keycloak = new Keycloak({
  url: KEYCLOAK_URL,
  realm: 'qawave',
  clientId: 'qawave-frontend',
})

keycloak.init({
  onLoad: 'login-required',
  pkceMethod: 'S256',
  checkLoginIframe: false,
})

// Token refresh before expiry
keycloak.onTokenExpired = () => {
  keycloak.updateToken(300) // 5 minutes before expiry
}
```

**Token Storage:**
- Access token: In-memory only (security best practice)
- Refresh token: Managed by Keycloak JS adapter
- No localStorage for tokens (XSS protection)

### Security Measures

1. **PKCE (S256)**: Prevents authorization code interception
2. **Short-lived tokens**: Access tokens expire in 5 minutes
3. **Secure cookies**: Refresh tokens in httpOnly cookies
4. **CORS configuration**: Whitelist frontend origins only
5. **Brute force protection**: Keycloak lockout after 5 failed attempts
6. **Password policy**: Minimum 8 chars, uppercase, digit, special char

## Consequences

### Positive
- **Enterprise-ready**: Keycloak supports SSO, LDAP, social login
- **Standards-compliant**: OAuth2/OIDC industry standards
- **Secure defaults**: PKCE, short tokens, proper refresh
- **Role management**: UI for managing users and roles
- **Audit logging**: All auth events logged by Keycloak

### Negative
- **Infrastructure complexity**: Additional service to deploy/maintain
- **Development overhead**: Must run Keycloak for local dev (or mock)
- **Learning curve**: OAuth2/OIDC concepts for team

### Risks
- **Single point of failure**: Keycloak outage blocks all auth
  - Mitigation: Deploy HA Keycloak cluster
- **Token theft**: Compromised token grants access
  - Mitigation: Short expiry, token refresh, secure storage

## Configuration

### Backend (`application.yml`)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/qawave}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:8180/realms/qawave/protocol/openid-connect/certs}

qawave:
  security:
    enabled: ${SECURITY_ENABLED:true}
```

### Frontend (`.env`)

```bash
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=qawave
VITE_KEYCLOAK_CLIENT_ID=qawave-frontend
```

## Testing Without Keycloak

For local development without Keycloak:

**Backend:** Set `qawave.security.enabled=false`

**Frontend:** Don't set `VITE_KEYCLOAK_URL` - uses dev user fallback

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/index.html)
- [OAuth 2.0 PKCE](https://oauth.net/2/pkce/)
- [ADR-002: Clean Architecture](ADR-002-clean-architecture-layers.md)
