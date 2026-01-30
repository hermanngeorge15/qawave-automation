package com.qawave.infrastructure.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for rate limiting.
 */
@ConfigurationProperties(prefix = "qawave.rate-limit")
data class RateLimitConfig(
    val enabled: Boolean = true,
    val defaultLimit: Int = 1000,
    val defaultWindowSeconds: Long = 60,
    val endpoints: Map<String, EndpointLimit> = emptyMap(),
)

/**
 * Rate limit configuration for a specific endpoint pattern.
 */
data class EndpointLimit(
    val authenticated: Int = 1000,
    val anonymous: Int = 100,
    val windowSeconds: Long = 60,
)

/**
 * Default rate limit configurations for common endpoint patterns.
 */
object RateLimitDefaults {
    val ENDPOINT_LIMITS =
        mapOf(
            "GET:/api/**" to EndpointLimit(authenticated = 1000, anonymous = 100, windowSeconds = 60),
            "POST:/api/qa/packages" to EndpointLimit(authenticated = 50, anonymous = 5, windowSeconds = 60),
            "POST:/api/qa/packages/*/runs" to EndpointLimit(authenticated = 20, anonymous = 0, windowSeconds = 60),
            "POST:/api/ai/**" to EndpointLimit(authenticated = 10, anonymous = 0, windowSeconds = 60),
        )
}
