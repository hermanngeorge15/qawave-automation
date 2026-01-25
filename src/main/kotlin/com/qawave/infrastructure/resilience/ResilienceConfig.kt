package com.qawave.infrastructure.resilience

import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.retry.event.RetryOnErrorEvent
import io.github.resilience4j.retry.event.RetryOnRetryEvent
import io.github.resilience4j.retry.event.RetryOnSuccessEvent
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Resilience4j configuration for AI client calls.
 * Provides circuit breaker, rate limiter, retry, and bulkhead patterns.
 */
@Configuration
class ResilienceConfig(
    @Value("\${qawave.ai.resilience.circuit-breaker.failure-rate-threshold:50}")
    private val failureRateThreshold: Float,

    @Value("\${qawave.ai.resilience.circuit-breaker.wait-duration-in-open-state-seconds:60}")
    private val waitDurationInOpenState: Long,

    @Value("\${qawave.ai.resilience.circuit-breaker.permitted-calls-in-half-open:3}")
    private val permittedCallsInHalfOpen: Int,

    @Value("\${qawave.ai.resilience.circuit-breaker.sliding-window-size:10}")
    private val slidingWindowSize: Int,

    @Value("\${qawave.ai.resilience.rate-limiter.limit-for-period:10}")
    private val limitForPeriod: Int,

    @Value("\${qawave.ai.resilience.rate-limiter.limit-refresh-period-seconds:1}")
    private val limitRefreshPeriod: Long,

    @Value("\${qawave.ai.resilience.rate-limiter.timeout-duration-seconds:5}")
    private val rateLimiterTimeout: Long,

    @Value("\${qawave.ai.resilience.retry.max-attempts:3}")
    private val maxRetryAttempts: Int,

    @Value("\${qawave.ai.resilience.retry.wait-duration-ms:500}")
    private val retryWaitDuration: Long,

    @Value("\${qawave.ai.resilience.bulkhead.max-concurrent-calls:5}")
    private val maxConcurrentCalls: Int,

    @Value("\${qawave.ai.resilience.bulkhead.max-wait-duration-ms:1000}")
    private val bulkheadMaxWaitDuration: Long
) {

    private val logger = LoggerFactory.getLogger(ResilienceConfig::class.java)

    companion object {
        const val AI_CLIENT_CIRCUIT_BREAKER = "aiClient"
        const val AI_CLIENT_RATE_LIMITER = "aiClient"
        const val AI_CLIENT_RETRY = "aiClient"
        const val AI_CLIENT_BULKHEAD = "aiClient"
    }

    @Bean
    fun circuitBreakerRegistry(meterRegistry: MeterRegistry): CircuitBreakerRegistry {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
            .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
            .slidingWindowSize(slidingWindowSize)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException::class.java,
                java.util.concurrent.TimeoutException::class.java,
                com.qawave.infrastructure.ai.AiClientException::class.java,
                com.qawave.infrastructure.ai.AiProviderException::class.java
            )
            .ignoreExceptions(
                com.qawave.infrastructure.ai.AiRateLimitException::class.java
            )
            .build()

        val registry = CircuitBreakerRegistry.of(config)

        // Register circuit breaker for AI client
        val circuitBreaker = registry.circuitBreaker(AI_CLIENT_CIRCUIT_BREAKER)

        // Add event listeners for monitoring
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                logger.info(
                    "Circuit breaker '{}' state changed: {} -> {}",
                    event.circuitBreakerName,
                    event.stateTransition.fromState,
                    event.stateTransition.toState
                )
            }
            .onError { event ->
                logger.warn(
                    "Circuit breaker '{}' recorded error: {}",
                    event.circuitBreakerName,
                    event.throwable.message
                )
            }
            .onSuccess { event ->
                logger.debug(
                    "Circuit breaker '{}' recorded success, duration: {}ms",
                    event.circuitBreakerName,
                    event.elapsedDuration.toMillis()
                )
            }

        return registry
    }

    @Bean
    fun rateLimiterRegistry(): RateLimiterRegistry {
        val config = RateLimiterConfig.custom()
            .limitForPeriod(limitForPeriod)
            .limitRefreshPeriod(Duration.ofSeconds(limitRefreshPeriod))
            .timeoutDuration(Duration.ofSeconds(rateLimiterTimeout))
            .build()

        val registry = RateLimiterRegistry.of(config)

        // Register rate limiter for AI client
        val rateLimiter = registry.rateLimiter(AI_CLIENT_RATE_LIMITER)

        rateLimiter.eventPublisher
            .onSuccess { event ->
                logger.debug(
                    "Rate limiter '{}' acquired permission, available: {}",
                    event.rateLimiterName,
                    rateLimiter.metrics.availablePermissions
                )
            }
            .onFailure { event ->
                logger.warn(
                    "Rate limiter '{}' rejected request",
                    event.rateLimiterName
                )
            }

        return registry
    }

    @Bean
    fun retryRegistry(): RetryRegistry {
        val intervalFunction = IntervalFunction.ofExponentialBackoff(
            retryWaitDuration,
            2.0
        )

        val config: RetryConfig = RetryConfig.custom<Any>()
            .maxAttempts(maxRetryAttempts)
            .intervalFunction(intervalFunction)
            .retryExceptions(
                java.io.IOException::class.java,
                java.util.concurrent.TimeoutException::class.java,
                com.qawave.infrastructure.ai.AiClientException::class.java,
                com.qawave.infrastructure.ai.AiProviderException::class.java
            )
            .ignoreExceptions(
                com.qawave.infrastructure.ai.AiRateLimitException::class.java
            )
            .failAfterMaxAttempts(true)
            .build()

        val registry = RetryRegistry.of(config)

        // Register retry for AI client
        val retry = registry.retry(AI_CLIENT_RETRY)

        retry.eventPublisher
            .onRetry { event: RetryOnRetryEvent ->
                logger.info(
                    "Retry '{}' attempt #{}, waiting {}ms before next attempt. Error: {}",
                    event.name,
                    event.numberOfRetryAttempts,
                    event.waitInterval.toMillis(),
                    event.lastThrowable?.message
                )
            }
            .onError { event: RetryOnErrorEvent ->
                logger.error(
                    "Retry '{}' failed after {} attempts: {}",
                    event.name,
                    event.numberOfRetryAttempts,
                    event.lastThrowable?.message
                )
            }
            .onSuccess { event: RetryOnSuccessEvent ->
                if (event.numberOfRetryAttempts > 0) {
                    logger.info(
                        "Retry '{}' succeeded after {} attempts",
                        event.name,
                        event.numberOfRetryAttempts
                    )
                }
            }

        return registry
    }

    @Bean
    fun bulkheadRegistry(): BulkheadRegistry {
        val config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxWaitDuration(Duration.ofMillis(bulkheadMaxWaitDuration))
            .build()

        val registry = BulkheadRegistry.of(config)

        // Register bulkhead for AI client
        val bulkhead = registry.bulkhead(AI_CLIENT_BULKHEAD)

        bulkhead.eventPublisher
            .onCallPermitted { event ->
                logger.debug(
                    "Bulkhead '{}' permitted call, available: {}",
                    event.bulkheadName,
                    bulkhead.metrics.availableConcurrentCalls
                )
            }
            .onCallRejected { event ->
                logger.warn(
                    "Bulkhead '{}' rejected call, max concurrent calls reached",
                    event.bulkheadName
                )
            }
            .onCallFinished { event ->
                logger.debug(
                    "Bulkhead '{}' call finished, available: {}",
                    event.bulkheadName,
                    bulkhead.metrics.availableConcurrentCalls
                )
            }

        return registry
    }
}
