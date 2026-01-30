package com.qawave.infrastructure.ratelimit

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * WebFilter that implements rate limiting using Redis.
 *
 * Features:
 * - Per-user rate limiting for authenticated requests
 * - Per-IP rate limiting for anonymous requests
 * - Different limits per endpoint pattern
 * - Rate limit headers in responses
 * - Graceful 429 Too Many Requests responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = ["qawave.rate-limit.enabled"], havingValue = "true", matchIfMissing = true)
class RateLimitFilter(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val rateLimitConfig: RateLimitConfig,
) : WebFilter {
    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    companion object {
        const val RATE_LIMIT_PREFIX = "rate_limit:"
        const val HEADER_RATE_LIMIT = "X-RateLimit-Limit"
        const val HEADER_RATE_REMAINING = "X-RateLimit-Remaining"
        const val HEADER_RATE_RESET = "X-RateLimit-Reset"
        const val HEADER_RETRY_AFTER = "Retry-After"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // Skip rate limiting for actuator and health endpoints
        val path = exchange.request.path.value()
        if (isExcludedPath(path)) {
            return chain.filter(exchange)
        }

        return mono {
            val clientId = getClientIdentifier(exchange)
            val endpointKey = getEndpointKey(exchange)
            val limit = getLimit(exchange, endpointKey)

            // Anonymous users blocked from certain endpoints
            if (limit <= 0) {
                writeRateLimitResponse(exchange, 0, 0, 0)
                return@mono null
            }

            val redisKey = "$RATE_LIMIT_PREFIX$clientId:$endpointKey"
            val windowSeconds = getWindowSeconds(endpointKey)

            // Increment counter
            val count =
                redisTemplate.opsForValue()
                    .increment(redisKey)
                    .awaitSingle()

            // Set expiry on first request
            if (count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds))
                    .awaitSingleOrNull()
            }

            // Get TTL for reset header
            val ttl = redisTemplate.getExpire(redisKey).awaitSingleOrNull() ?: Duration.ofSeconds(windowSeconds)
            val resetTime = System.currentTimeMillis() / 1000 + ttl.seconds

            // Add rate limit headers
            val remaining = (limit - count).coerceAtLeast(0)
            exchange.response.headers.add(HEADER_RATE_LIMIT, limit.toString())
            exchange.response.headers.add(HEADER_RATE_REMAINING, remaining.toString())
            exchange.response.headers.add(HEADER_RATE_RESET, resetTime.toString())

            if (count > limit) {
                logger.warn("Rate limit exceeded for client: {} on endpoint: {}", clientId, endpointKey)
                writeRateLimitResponse(exchange, limit, 0, ttl.seconds.toInt())
                return@mono null
            }

            // Continue with the filter chain
            chain.filter(exchange).awaitSingleOrNull()
        }
    }

    private suspend fun getClientIdentifier(exchange: ServerWebExchange): String {
        // Try to get user ID from security context
        val context = ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()
        val authentication = context?.authentication

        return if (authentication?.isAuthenticated == true && authentication.name != "anonymousUser") {
            "user:${authentication.name}"
        } else {
            // Fall back to IP address
            val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            "ip:$ip"
        }
    }

    private fun getEndpointKey(exchange: ServerWebExchange): String {
        val method = exchange.request.method.name()
        val path = exchange.request.path.value()
        return "$method:$path"
    }

    private suspend fun getLimit(exchange: ServerWebExchange, endpointKey: String): Int {
        val isAuthenticated = isAuthenticated(exchange)

        // Check configured endpoint limits
        for ((pattern, limit) in rateLimitConfig.endpoints) {
            if (matchesPattern(endpointKey, pattern)) {
                return if (isAuthenticated) limit.authenticated else limit.anonymous
            }
        }

        // Check default limits
        for ((pattern, limit) in RateLimitDefaults.ENDPOINT_LIMITS) {
            if (matchesPattern(endpointKey, pattern)) {
                return if (isAuthenticated) limit.authenticated else limit.anonymous
            }
        }

        // Fall back to default
        return if (isAuthenticated) rateLimitConfig.defaultLimit else rateLimitConfig.defaultLimit / 10
    }

    private fun getWindowSeconds(endpointKey: String): Long {
        // Check configured endpoint limits
        for ((pattern, limit) in rateLimitConfig.endpoints) {
            if (matchesPattern(endpointKey, pattern)) {
                return limit.windowSeconds
            }
        }

        // Check default limits
        for ((pattern, limit) in RateLimitDefaults.ENDPOINT_LIMITS) {
            if (matchesPattern(endpointKey, pattern)) {
                return limit.windowSeconds
            }
        }

        return rateLimitConfig.defaultWindowSeconds
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun isAuthenticated(exchange: ServerWebExchange): Boolean {
        val context = ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()
        val authentication = context?.authentication
        return authentication?.isAuthenticated == true && authentication.name != "anonymousUser"
    }

    private fun matchesPattern(endpointKey: String, pattern: String): Boolean {
        val regex =
            pattern
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .toRegex()
        return regex.matches(endpointKey)
    }

    private fun isExcludedPath(path: String): Boolean {
        return path.startsWith("/actuator") ||
            path.startsWith("/health") ||
            path.startsWith("/ready") ||
            path.startsWith("/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs")
    }

    private fun writeRateLimitResponse(
        exchange: ServerWebExchange,
        limit: Int,
        remaining: Long,
        retryAfter: Int,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.contentType = MediaType.APPLICATION_JSON
        response.headers.add(HEADER_RATE_LIMIT, limit.toString())
        response.headers.add(HEADER_RATE_REMAINING, remaining.toString())
        response.headers.add(HEADER_RETRY_AFTER, retryAfter.toString())

        val body = """{"error":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please retry after $retryAfter seconds.","retryAfter":$retryAfter}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}
