# Backend Security Review Report

**Date:** 2026-01-26
**Reviewer:** Security Agent
**Issue:** #36 (P1-016)
**Status:** REVIEW COMPLETE - ACTION REQUIRED

---

## Executive Summary

The QAWave backend has been reviewed for security vulnerabilities. While the codebase follows several good security practices (parameterized queries, input validation on some fields), there are **critical security gaps** that must be addressed before production deployment.

**Overall Risk Level: HIGH**

---

## Findings Summary

| Severity | Count | Status |
|----------|-------|--------|
| Critical | 4 | Action Required |
| High | 5 | Action Required |
| Medium | 4 | Should Fix |
| Low | 2 | Consider Fixing |
| Good Practices | 7 | No Action |

---

## Critical Findings

### C1: No Authentication/Authorization Implemented
**File:** All controllers
**Risk:** Critical
**OWASP:** A01:2021 - Broken Access Control

**Description:**
All API endpoints are publicly accessible without any authentication. The `X-User-Id` header is accepted but not validated.

```kotlin
// QaPackageController.kt:53
@RequestHeader("X-User-Id", required = false) userId: String?,
// ... uses: triggeredBy = userId ?: "anonymous"
```

**Impact:**
- Any user can create, read, update, delete all QA packages
- No audit trail of who performed actions
- No resource isolation between users

**Recommendation:**
- Implement JWT or API key authentication
- Add Spring Security WebFlux configuration
- Validate and verify user identity

---

### C2: No CORS Configuration
**File:** `application.yml`
**Risk:** Critical
**OWASP:** A05:2021 - Security Misconfiguration

**Description:**
No CORS policy is configured. Spring WebFlux defaults to allowing all origins in development mode.

**Impact:**
- Cross-origin requests allowed from any domain
- Potential for CSRF attacks
- API can be called from malicious websites

**Recommendation:**
```kotlin
@Configuration
class CorsConfig {
    @Bean
    fun corsFilter(): CorsWebFilter {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("https://qawave.example.com")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)
        return CorsWebFilter(source)
    }
}
```

---

### C3: SSRF Vulnerability in HttpStepExecutor
**File:** `HttpStepExecutor.kt:39`
**Risk:** Critical
**OWASP:** A10:2021 - Server-Side Request Forgery

**Description:**
The HTTP step executor accepts arbitrary URLs without validation, allowing requests to internal services.

```kotlin
val url = context.resolve("$baseUrl${step.endpoint}")
// ... directly used in webClient.uri(url)
```

**Impact:**
- Attacker can access internal services (metadata endpoints, internal APIs)
- Port scanning of internal network
- Access to cloud metadata (AWS: 169.254.169.254)
- Potential data exfiltration

**Proof of Concept:**
```json
{
  "baseUrl": "http://169.254.169.254/latest/meta-data/",
  "endpoint": "iam/security-credentials/"
}
```

**Recommendation:**
```kotlin
private fun validateUrl(url: String): Boolean {
    val uri = URI.create(url)
    // Block internal IPs
    val blockedPatterns = listOf(
        "^127\\.", "^10\\.", "^172\\.(1[6-9]|2[0-9]|3[01])\\.",
        "^192\\.168\\.", "^169\\.254\\.", "^localhost$"
    )
    val host = uri.host ?: return false
    return blockedPatterns.none { host.matches(Regex(it)) }
}
```

---

### C4: Missing Global Exception Handler
**File:** None (missing)
**Risk:** Critical
**OWASP:** A09:2021 - Security Logging and Monitoring Failures

**Description:**
No `@ControllerAdvice` global exception handler exists. Unhandled exceptions may expose stack traces to clients.

**Impact:**
- Stack traces reveal internal implementation details
- Database schema exposed in SQL exceptions
- File paths and configuration leaked
- Aids attackers in crafting exploits

**Recommendation:**
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception", e)
        return ResponseEntity.status(500)
            .body(ErrorResponse("Internal server error", "ERR_INTERNAL"))
    }
}
```

---

## High Severity Findings

### H1: Missing URL Validation on User Input
**File:** `QaPackageRequests.kt:15-18`
**Risk:** High

**Description:**
`specUrl` and `baseUrl` fields accept any string without URL format validation.

```kotlin
val specUrl: String? = null,
val specContent: String? = null,
@field:NotBlank(message = "Base URL is required")
val baseUrl: String,  // No @URL or @Pattern validation
```

**Recommendation:**
```kotlin
@field:Pattern(
    regexp = "^https?://.*",
    message = "URL must start with http:// or https://"
)
val baseUrl: String,
```

---

### H2: No Size Limit on specContent
**File:** `QaPackageRequests.kt:16`
**Risk:** High
**OWASP:** A04:2021 - Insecure Design

**Description:**
`specContent` field has no size limit, allowing arbitrarily large payloads.

**Impact:**
- Denial of Service via memory exhaustion
- Database storage attacks
- Slow query attacks

**Recommendation:**
```kotlin
@field:Size(max = 100000, message = "Spec content too large")
val specContent: String? = null,
```

---

### H3: Actuator Endpoints Exposed
**File:** `application.yml:84-93`
**Risk:** High

**Description:**
Actuator endpoints are exposed with `show-details: always`.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always  # DANGEROUS
```

**Impact:**
- Internal health status exposed (database connection info)
- Prometheus metrics reveal internal operations
- Can aid reconnaissance attacks

**Recommendation:**
```yaml
endpoint:
  health:
    show-details: when_authorized
```

---

### H4: Header Injection in HTTP Executor
**File:** `HttpStepExecutor.kt:84`
**Risk:** High

**Description:**
Headers from step definition are passed directly without sanitization.

```kotlin
headers.forEach { (k, v) -> header(k, v) }
```

**Impact:**
- HTTP header injection
- Response splitting attacks
- Host header attacks

---

### H5: Unvalidated Status Enum in Query Parameter
**File:** `QaPackageController.kt:117`
**Risk:** High

**Description:**
Status parameter is converted via `valueOf()` without try-catch:

```kotlin
val statusEnum = QaPackageStatus.valueOf(status)  // Can throw
```

This is caught by Spring's default error handling but may leak enum values.

---

## Medium Severity Findings

### M1: No Rate Limiting on API Endpoints
Only AI operations have rate limiting. Main CRUD endpoints are unprotected.

### M2: No CSRF Protection
WebFlux CSRF is disabled by default. With cookie-based auth, this is a risk.

### M3: Sensitive Data in Logs
Debug logging enabled for `com.qawave` may log sensitive data.

### M4: Redis Password Empty by Default
`REDIS_PASSWORD:` defaults to empty string, allowing unauthenticated access.

---

## Good Security Practices Observed

1. **SQL Injection Prevention** - R2DBC uses parameterized queries throughout
2. **Input Validation** - Name and description have `@Size` constraints
3. **Kafka Security** - Trusted packages restricted to `com.qawave.*`
4. **WebClient Timeouts** - Connection and response timeouts configured
5. **Domain Validation** - `require()` checks in domain model constructors
6. **Status State Machine** - Invalid status transitions are rejected
7. **Database Constraints** - URL format validation at DB level (V008 migration)

---

## Recommendations Priority

### Immediate (Before Production)
1. Implement authentication (JWT/API Key)
2. Configure CORS policy
3. Add global exception handler
4. Fix SSRF vulnerability in HttpStepExecutor

### Short-term (Sprint)
1. Add URL validation annotations
2. Add size limits to all text fields
3. Restrict actuator endpoints
4. Add request logging for audit

### Medium-term (Quarter)
1. Implement rate limiting
2. Add security headers (CSP, X-Frame-Options)
3. Set up vulnerability scanning in CI
4. Security testing automation

---

## Verification Checklist

- [x] Input validation review (all endpoints) - ISSUES FOUND
- [x] SQL injection prevention verified - PASS
- [ ] XSS prevention in responses - NEEDS GLOBAL HANDLER
- [ ] CORS configuration reviewed - NOT CONFIGURED
- [x] Error message information disclosure check - ISSUES FOUND
- [ ] Authentication preparation review - NOT IMPLEMENTED
- [ ] Rate limiting configuration review - PARTIAL (AI only)

---

## Sign-off

**Security Review Status:** NOT APPROVED FOR PRODUCTION

The backend requires critical security fixes before deployment. Issues C1-C4 and H1-H5 must be addressed.

---

*Report generated by Security Agent*
*Refs: #36*
