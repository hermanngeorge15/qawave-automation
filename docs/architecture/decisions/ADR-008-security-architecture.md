# ADR-008: Security Architecture

## Status
Accepted

## Date
2026-01-30

## Context

QAWave handles sensitive data including:
- API specifications (may contain business logic)
- Test credentials and authentication tokens
- Test results and logs
- User information

We need a defense-in-depth security strategy covering:
- Network security
- Application security
- Data security
- Operational security

## Decision

We implement a **multi-layered security architecture** with the following components:

### Security Layers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SECURITY ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Layer 1: NETWORK SECURITY                                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  • WAF / Rate Limiting (Nginx Ingress)                                 │ │
│  │  • TLS 1.3 termination (cert-manager)                                  │ │
│  │  • DDoS protection                                                      │ │
│  │  • Network policies (Kubernetes)                                       │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                        │
│                                     ▼                                        │
│  Layer 2: APPLICATION SECURITY                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  • Authentication (Keycloak OAuth2/OIDC) - see ADR-007                 │ │
│  │  • Authorization (RBAC: admin/tester/viewer)                           │ │
│  │  • Input validation (Jakarta Validation)                               │ │
│  │  • CORS whitelist                                                       │ │
│  │  • Security headers (CSP, HSTS, X-Frame-Options)                       │ │
│  │  • Request size limits                                                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                        │
│                                     ▼                                        │
│  Layer 3: DATA SECURITY                                                     │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  • Encryption at rest (PostgreSQL TDE, S3 SSE)                         │ │
│  │  • Encryption in transit (TLS 1.3)                                     │ │
│  │  • PII handling (minimization, masking)                                │ │
│  │  • Secrets management (Kubernetes Secrets, Sealed Secrets)             │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                        │
│                                     ▼                                        │
│  Layer 4: OPERATIONAL SECURITY                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  • Audit logging (all auth events, data access)                        │ │
│  │  • Security monitoring (Prometheus alerts)                             │ │
│  │  • Incident response procedures                                        │ │
│  │  • Backup encryption (AES-256)                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1. Network Security

**TLS Configuration:**
```yaml
# cert-manager ClusterIssuer for automatic TLS
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx
```

**Rate Limiting (Nginx Ingress):**
```yaml
annotations:
  nginx.ingress.kubernetes.io/limit-rps: "10"
  nginx.ingress.kubernetes.io/limit-connections: "5"
  nginx.ingress.kubernetes.io/limit-burst-multiplier: "5"
```

**Network Policies:**
```yaml
# Allow only backend to access PostgreSQL
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: postgres-access
spec:
  podSelector:
    matchLabels:
      app: postgresql
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: qawave-backend
      ports:
        - port: 5432
```

### 2. Application Security

**Input Validation:**
```kotlin
data class CreateQaPackageRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,

    @field:Size(max = 100000, message = "Spec content too large")
    val specContent: String?,

    @field:Pattern(regexp = "^https?://.*", message = "Invalid URL")
    val baseUrl: String,
)
```

**Security Headers:**
```kotlin
@Bean
fun securityHeaders(): WebFilter = WebFilter { exchange, chain ->
    exchange.response.headers.apply {
        add("X-Content-Type-Options", "nosniff")
        add("X-Frame-Options", "DENY")
        add("X-XSS-Protection", "1; mode=block")
        add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        add("Content-Security-Policy", "default-src 'self'; script-src 'self'")
        add("Referrer-Policy", "strict-origin-when-cross-origin")
    }
    chain.filter(exchange)
}
```

**CORS Configuration:**
```kotlin
@Bean
fun corsConfigurer(): CorsConfigurationSource {
    val config = CorsConfiguration().apply {
        allowedOrigins = listOf(
            "https://qawave.local",
            "https://staging.qawave.local"
        )
        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        allowedHeaders = listOf("*")
        allowCredentials = true
        maxAge = 3600
    }
    return UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration("/api/**", config)
    }
}
```

### 3. Data Security

**Encryption at Rest:**
- PostgreSQL: Filesystem encryption (LUKS)
- S3 Backups: Server-side encryption (AES-256)
- Redis: TLS in-flight, no sensitive data cached

**Secrets Management:**
```yaml
# Sealed Secrets for GitOps-safe secret management
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: db-credentials
spec:
  encryptedData:
    username: AgBy8hC...
    password: AgDJ3kL...
```

**PII Handling:**
- Minimize collection (only necessary user info)
- Mask in logs (email → j***@example.com)
- Retention policy (delete after 90 days inactive)

### 4. Operational Security

**Audit Logging:**
```kotlin
@Aspect
@Component
class AuditAspect(private val auditLogger: AuditLogger) {

    @Around("@annotation(Audited)")
    suspend fun auditMethod(joinPoint: ProceedingJoinPoint): Any? {
        val user = SecurityUtils.getCurrentUser()
        val action = joinPoint.signature.name

        auditLogger.log(
            AuditEvent(
                userId = user?.id,
                action = action,
                resource = joinPoint.target::class.simpleName,
                timestamp = Instant.now(),
                ipAddress = RequestContextHolder.getRequestAttributes()?.remoteAddress
            )
        )

        return joinPoint.proceed()
    }
}
```

**Security Monitoring Alerts:**
```yaml
# Prometheus alert for failed auth attempts
- alert: HighAuthFailureRate
  expr: rate(keycloak_login_failure_total[5m]) > 10
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: High authentication failure rate
```

### 5. API Security

**Request Size Limits:**
```yaml
spring:
  webflux:
    maxInMemorySize: 10MB  # Max request body
  codec:
    max-in-memory-size: 10MB
```

**Rate Limiting (Application Level):**
```kotlin
@Bean
fun rateLimiter(): RateLimiter {
    return RateLimiter.of("api", RateLimiterConfig.custom()
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .limitForPeriod(100)  // 100 requests/minute
        .timeoutDuration(Duration.ofSeconds(5))
        .build()
    )
}
```

## Consequences

### Positive
- Defense in depth protects against multiple attack vectors
- Industry-standard practices (OAuth2, TLS 1.3)
- Audit trail for compliance requirements
- Secrets never stored in Git (Sealed Secrets)

### Negative
- Performance overhead from encryption/validation
- Complexity in deployment and debugging
- Requires security expertise to maintain

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Token theft | Short expiry (5 min), refresh rotation |
| SQL injection | Parameterized queries, ORM |
| XSS | CSP headers, input sanitization |
| Secret leakage | Sealed Secrets, no console logging |
| Data breach | Encryption at rest, access logging |

## OWASP API Security Top 10 Coverage

| Vulnerability | Protection |
|---------------|------------|
| API1: Broken Object Level Authorization | User-scoped queries, ownership checks |
| API2: Broken Authentication | Keycloak, PKCE, short tokens |
| API3: Broken Object Property Level Authorization | DTO projection, field-level auth |
| API4: Unrestricted Resource Consumption | Rate limiting, request size limits |
| API5: Broken Function Level Authorization | RBAC, @PreAuthorize annotations |
| API6: Unrestricted Access to Sensitive Flows | Business logic validation |
| API7: Server-Side Request Forgery | URL validation, allowlist |
| API8: Security Misconfiguration | Hardened defaults, security headers |
| API9: Improper Inventory Management | OpenAPI spec, deprecation policy |
| API10: Unsafe Consumption of APIs | AI provider validation, timeouts |

## References

- [ADR-007: Authentication with Keycloak](ADR-007-authentication-keycloak.md)
- [OWASP API Security Top 10](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
- [Kubernetes Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
