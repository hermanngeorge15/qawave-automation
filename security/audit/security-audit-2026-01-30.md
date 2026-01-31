# Security Audit Report - QAWave

**Date**: 2026-01-30
**Auditor**: Security Agent
**Status**: IN PROGRESS - SIGNIFICANT IMPROVEMENTS MADE
**Related Issues**: #142, #157, #170
**Last Updated**: 2026-01-30 (evening)

---

## Executive Summary

The QAWave application has made **significant security improvements** since the initial audit. Authentication and authorization are now implemented.

| Severity | Count | Fixed | Remaining |
|----------|-------|-------|-----------|
| CRITICAL | 5 | 4 | 1 |
| HIGH | 5 | 2 | 3 |
| MEDIUM | 5 | 1 | 4 |

**Current Status**: 4 of 5 critical issues fixed. Only API key handling remains critical. Security headers and Swagger protection needed.

---

## REMEDIATION PROGRESS

### FIXED Issues

| ID | Issue | Status | Fixed By |
|----|-------|--------|----------|
| CRIT-001 | No Authentication | :white_check_mark: FIXED | PR #257 - JWT + Keycloak |
| CRIT-002 | Token in Memory | :white_check_mark: FIXED | Keycloak adapter with refresh |
| CRIT-003 | CORS Misconfiguration | :white_check_mark: FIXED | CorsConfig updated |
| CRIT-005 | No Rate Limiting | :white_check_mark: FIXED | PR #266 - Redis rate limiter |
| HIGH-003 | Keycloak Not Integrated | :white_check_mark: FIXED | AuthProvider.tsx |
| HIGH-005 | Auth Endpoints Missing | :white_check_mark: FIXED | Keycloak handles auth |
| MED-005 | No Logout | :white_check_mark: FIXED | Keycloak logout |

### REMAINING Issues

| ID | Issue | Status | Priority |
|----|-------|--------|----------|
| CRIT-004 | API Key in Headers | :warning: OPEN | HIGH |
| CRIT-005 | No Rate Limiting | :white_check_mark: **FIXED** | PR #266 merged |
| HIGH-001 | Swagger UI Public | :x: OPEN | HIGH |
| HIGH-002 | No Security Headers | :x: OPEN | HIGH |
| HIGH-004 | Missing Input Validation | :warning: PARTIAL | MEDIUM |
| MED-001 | Hardcoded Credentials | :x: OPEN | LOW |
| MED-002 | Default Fallbacks | :x: OPEN | LOW |
| MED-003 | No SSL Pinning | :x: OPEN | LOW |
| MED-004 | Potential Secret Logging | :x: OPEN | MEDIUM |

### NEW Issues Found

| ID | Issue | Severity | Location |
|----|-------|----------|----------|
| NEW-001 | Dev Mode Auth Bypass | MEDIUM | AuthProvider.tsx:50-62 |

---

## Original Findings (for reference)

---

## Critical Vulnerabilities

### CRIT-001: No Authentication Implemented

**Severity**: CRITICAL
**CVSS**: 9.8 (Critical)
**Location**: `backend/src/main/kotlin/com/qawave/presentation/controller/QaPackageController.kt:53,65`

**Description**:
The backend accepts an optional `X-User-Id` header that defaults to "anonymous". There is no actual authentication or token validation.

**Vulnerable Code**:
```kotlin
suspend fun createPackage(
    @Valid @RequestBody request: CreateQaPackageRequest,
    @RequestHeader("X-User-Id", required = false) userId: String?,  // Line 53 - OPTIONAL
): ResponseEntity<QaPackageResponse> {
    ...
    triggeredBy = userId ?: "anonymous",  // Line 65 - DEFAULT TO ANONYMOUS
```

**Impact**:
- Any user can perform any operation
- No way to identify who performed actions
- No access control possible

**Remediation**:
1. Integrate Keycloak OAuth2/OIDC with Spring Security WebFlux
2. Require valid JWT token on all protected endpoints
3. Validate token signature, expiry, and claims
4. Extract user identity from token claims

---

### CRIT-002: Frontend Token Storage in Memory

**Severity**: CRITICAL
**CVSS**: 8.1 (High)
**Location**: `frontend/src/api/client.ts:65`

**Description**:
Authentication tokens are stored in a JavaScript variable, which is lost on page refresh and accessible to any XSS attack.

**Vulnerable Code**:
```typescript
let authToken: string | null = null  // Line 65 - IN MEMORY

export function setAuthToken(token: string | null): void {
  authToken = token
}
```

**Impact**:
- Users logged out on every page refresh
- Token accessible via XSS attacks
- No session persistence

**Remediation**:
1. Use `httpOnly` cookies for token storage (preferred)
2. Or use `sessionStorage` with XSS protections
3. Implement token refresh mechanism
4. Add CSP headers to prevent XSS

---

### CRIT-003: CORS Misconfiguration

**Severity**: CRITICAL
**CVSS**: 7.5 (High)
**Location**: `backend/src/main/kotlin/com/qawave/infrastructure/config/CorsConfig.kt:29-30`

**Description**:
CORS configuration uses wildcard headers with credentials enabled, which violates the CORS specification.

**Vulnerable Code**:
```kotlin
allowedHeaders = listOf("*")  // Line 29 - WILDCARD
allowCredentials = true       // Line 30 - WITH CREDENTIALS
```

**Impact**:
- Modern browsers will reject this combination
- Production has NO CORS configuration at all
- Cross-origin requests may fail or be exploited

**Remediation**:
```kotlin
allowedHeaders = listOf(
    "Content-Type",
    "Authorization",
    "X-User-Id",
    "X-Request-ID"
)
allowCredentials = true  // Now valid with explicit headers
```

---

### CRIT-004: API Key Exposed in Default Headers

**Severity**: CRITICAL
**CVSS**: 8.5 (High)
**Location**: `backend/src/main/kotlin/com/qawave/infrastructure/ai/OpenAiClient.kt:40`

**Description**:
The OpenAI API key is embedded in WebClient's default headers, sending it with every request.

**Vulnerable Code**:
```kotlin
private val webClient: WebClient =
    WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")  // Line 40
        .build()
```

**Impact**:
- API key visible in debug tools and logs
- Key sent with every request, even non-OpenAI ones
- Key compromise leads to financial liability

**Remediation**:
```kotlin
private val webClient: WebClient =
    WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

// Add key per-request:
webClient.post()
    .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
    .bodyValue(request)
```

---

### CRIT-005: No Rate Limiting Implemented

**Severity**: CRITICAL
**CVSS**: 7.5 (High)
**Location**: All API endpoints

**Description**:
Documentation claims "100 requests per minute per API key" but no rate limiting code exists.

**Impact**:
- Brute force attacks possible
- Resource exhaustion (DoS)
- AI API cost overruns
- No protection against automated abuse

**Remediation**:
1. Add Resilience4j rate limiter:
```kotlin
@RateLimiter(name = "default", fallbackMethod = "rateLimitFallback")
suspend fun createPackage(...)
```

2. Configure in `application.yml`:
```yaml
resilience4j:
  ratelimiter:
    instances:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 60s
        timeoutDuration: 0
```

---

## High Vulnerabilities

### HIGH-001: Swagger UI Publicly Exposed

**Location**: `/swagger-ui/**`
**Impact**: Attackers can discover all API endpoints and schemas

**Remediation**:
```kotlin
@Bean
fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http
        .authorizeExchange { exchanges ->
            exchanges
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                .pathMatchers("/actuator/health").permitAll()
                .anyExchange().authenticated()
        }
        .build()
}
```

---

### HIGH-002: No Security Headers

**Location**: All HTTP responses
**Impact**: XSS, clickjacking, MIME sniffing attacks possible

**Remediation**:
Add to Spring Security configuration:
```kotlin
.headers { headers ->
    headers
        .contentSecurityPolicy { csp ->
            csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';")
        }
        .frameOptions { it.deny() }
        .contentTypeOptions { }
        .referrerPolicy { it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
}
```

---

### HIGH-003: Keycloak Not Integrated

**Location**: `infrastructure/kubernetes/base/auth/keycloak/`
**Impact**: Authentication infrastructure exists but is not used

**Remediation**:
1. Add Spring Security OAuth2 Resource Server dependency
2. Configure JWT decoder with Keycloak issuer
3. Update frontend to use Keycloak JS adapter
4. Implement token refresh flow

---

### HIGH-004: Missing Input Validation

**Location**: All controllers
**Impact**: SQL injection, XSS, SSRF attacks possible

**Remediation**:
1. Add `@Pattern`, `@Size`, `@URL` annotations
2. Implement custom validators for URLs (SSRF prevention)
3. Sanitize all output for XSS prevention
4. Use parameterized queries (already using R2DBC)

---

### HIGH-005: Auth Endpoints Don't Exist

**Location**: `e2e-tests/src/fixtures/auth.ts:35`
**Impact**: Tests reference `/api/auth/login` that doesn't exist

**Vulnerable Code**:
```typescript
const response = await context.request.post(`${API_URL}/api/auth/login`, {
    data: credentials,
});
```

**Remediation**:
1. Implement auth endpoints when integrating Keycloak
2. Or update tests to use Keycloak login flow directly

---

## Medium Vulnerabilities

### MED-001: Hardcoded Credentials in Local Config

**Location**: `backend/src/main/resources/application-local.yml:5,10`
**Impact**: Credentials in version control

**Remediation**: Use environment variables even for local development

---

### MED-002: Default Environment Variable Fallbacks

**Location**: `backend/src/main/resources/application.yml:8`
**Code**: `password: ${DB_PASSWORD:secret}`
**Impact**: Insecure default could mask configuration errors

**Remediation**: Remove default or fail fast on missing config

---

### MED-003: No SSL/TLS Certificate Pinning

**Location**: `backend/src/main/kotlin/com/qawave/infrastructure/http/WebClientConfig.kt`
**Impact**: MITM attacks possible

**Remediation**: Implement certificate pinning for external APIs

---

### MED-004: Potential Secret Logging

**Location**: All classes using `Logger`
**Impact**: Secrets may appear in logs

**Remediation**:
1. Configure log masking patterns
2. Audit all log statements
3. Use structured logging with field exclusion

---

### MED-005: No Logout Implementation

**Location**: `frontend/src/hooks/useAuth.ts`
**Impact**: Tokens never invalidated

**Remediation**:
1. Implement token revocation endpoint
2. Clear all storage on logout
3. Invalidate refresh tokens server-side

---

## Recommendations by Priority

### Immediate (Before ANY deployment):

1. **Implement Keycloak Integration**
   - Backend: Spring Security OAuth2 Resource Server
   - Frontend: Keycloak JS adapter
   - Validate JWT on every request

2. **Fix Token Storage**
   - Move to httpOnly cookies
   - Implement CSRF protection

3. **Fix CORS Configuration**
   - Whitelist specific headers
   - Add production CORS config

4. **Implement Rate Limiting**
   - Add Resilience4j
   - Configure per-endpoint limits

5. **Secure API Keys**
   - Remove from default headers
   - Implement key rotation

### Short-term (Before Production):

6. Add security headers
7. Protect Swagger UI
8. Implement input validation
9. Add secret scanning in CI/CD
10. Implement audit logging

### Medium-term:

11. Certificate pinning
12. Log sanitization
13. Penetration testing
14. Security monitoring

---

## Affected Files Summary

| File | Issues | Severity |
|------|--------|----------|
| `backend/.../QaPackageController.kt` | CRIT-001 | CRITICAL |
| `frontend/.../api/client.ts` | CRIT-002 | CRITICAL |
| `backend/.../CorsConfig.kt` | CRIT-003 | CRITICAL |
| `backend/.../OpenAiClient.kt` | CRIT-004 | CRITICAL |
| All endpoints | CRIT-005 | CRITICAL |
| `/swagger-ui/**` | HIGH-001 | HIGH |
| All responses | HIGH-002 | HIGH |
| `infrastructure/.../keycloak/` | HIGH-003 | HIGH |
| All controllers | HIGH-004 | HIGH |
| `e2e-tests/.../auth.ts` | HIGH-005 | HIGH |
| `application-local.yml` | MED-001 | MEDIUM |
| `application.yml` | MED-002 | MEDIUM |
| `WebClientConfig.kt` | MED-003 | MEDIUM |
| All loggers | MED-004 | MEDIUM |
| `useAuth.ts` | MED-005 | MEDIUM |

---

## Conclusion

The QAWave application requires significant security work before production deployment. The most critical gap is the complete absence of authentication and authorization. The Keycloak infrastructure exists but is not integrated.

**Estimated effort to reach production-ready security**: Significant development work required to integrate Keycloak with both backend and frontend, implement rate limiting, fix CORS, and add security headers.

---

*Report generated by Security Agent*
*Next audit scheduled: After remediation of CRITICAL issues*
