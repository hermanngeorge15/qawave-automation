package com.qawave.infrastructure.resilience

import com.qawave.infrastructure.ai.*
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.bulkhead.executeSuspendFunction
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.retry.MaxRetriesExceededException
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Resilient wrapper for AI clients providing circuit breaker, rate limiting,
 * retry, and bulkhead patterns.
 *
 * This component wraps the underlying AI client (OpenAI or Stub) with resilience
 * patterns including circuit breaker, rate limiter, retry, and bulkhead.
 */
@Component
@Primary
class ResilientAiClient(
    @Qualifier("delegateAiClient")
    private val delegate: AiClient,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    rateLimiterRegistry: RateLimiterRegistry,
    retryRegistry: RetryRegistry,
    bulkheadRegistry: BulkheadRegistry,
    private val fallbackHandler: AiFallbackHandler
) : AiClient {

    private val logger = LoggerFactory.getLogger(ResilientAiClient::class.java)

    private val circuitBreaker = circuitBreakerRegistry
        .circuitBreaker(ResilienceConfig.AI_CLIENT_CIRCUIT_BREAKER)
    private val rateLimiter = rateLimiterRegistry
        .rateLimiter(ResilienceConfig.AI_CLIENT_RATE_LIMITER)
    private val retry = retryRegistry
        .retry(ResilienceConfig.AI_CLIENT_RETRY)
    private val bulkhead = bulkheadRegistry
        .bulkhead(ResilienceConfig.AI_CLIENT_BULKHEAD)

    /**
     * Executes a completion request with all resilience patterns applied.
     * Order: Bulkhead -> RateLimiter -> CircuitBreaker -> Retry -> Delegate
     */
    override suspend fun complete(request: AiCompletionRequest): AiCompletionResponse {
        logger.debug("Executing resilient AI completion request")

        return try {
            bulkhead.executeSuspendFunction {
                rateLimiter.executeSuspendFunction {
                    circuitBreaker.executeSuspendFunction {
                        retry.executeSuspendFunction {
                            delegate.complete(request)
                        }
                    }
                }
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("Circuit breaker is open, using fallback: {}", e.message)
            fallbackHandler.handleCompletionFallback(request, e)
        } catch (e: RequestNotPermitted) {
            logger.warn("Rate limiter rejected request, using fallback: {}", e.message)
            fallbackHandler.handleCompletionFallback(request, e)
        } catch (e: BulkheadFullException) {
            logger.warn("Bulkhead is full, using fallback: {}", e.message)
            fallbackHandler.handleCompletionFallback(request, e)
        } catch (e: MaxRetriesExceededException) {
            logger.error("Max retries exceeded: {}", e.message)
            throw AiClientException("AI service unavailable after retries", e)
        } catch (e: AiRateLimitException) {
            // Re-throw rate limit exceptions from the provider
            logger.warn("AI provider rate limited, retry after {}s", e.retryAfterSeconds)
            throw e
        } catch (e: AiClientException) {
            logger.error("AI client error: {}", e.message)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error during AI completion: {}", e.message, e)
            throw AiClientException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * Executes a streaming completion request with resilience patterns.
     * Note: Streaming is not retried due to its nature, but circuit breaker and rate limiter apply.
     */
    override fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk> {
        logger.debug("Executing resilient AI streaming request")

        // For streaming, we apply bulkhead and rate limiter at start,
        // circuit breaker on errors, but don't retry (streaming is harder to retry)
        return delegate.completeStream(request)
            .onStart {
                // Check rate limiter and bulkhead before starting stream
                if (!rateLimiter.acquirePermission()) {
                    throw RequestNotPermitted.createRequestNotPermitted(rateLimiter)
                }
                if (!bulkhead.tryAcquirePermission()) {
                    throw BulkheadFullException.createBulkheadFullException(bulkhead)
                }
                circuitBreaker.acquirePermission()
            }
            .catch { e ->
                when (e) {
                    is CallNotPermittedException -> {
                        logger.warn("Circuit breaker is open for stream: {}", e.message)
                        emit(AiStreamChunk(
                            content = fallbackHandler.getFallbackMessage(),
                            isComplete = true,
                            finishReason = FinishReason.ERROR
                        ))
                    }
                    is RequestNotPermitted -> {
                        logger.warn("Rate limiter rejected stream: {}", e.message)
                        emit(AiStreamChunk(
                            content = "Service temporarily unavailable due to rate limiting",
                            isComplete = true,
                            finishReason = FinishReason.ERROR
                        ))
                    }
                    is BulkheadFullException -> {
                        logger.warn("Bulkhead full for stream: {}", e.message)
                        emit(AiStreamChunk(
                            content = "Service temporarily unavailable due to high load",
                            isComplete = true,
                            finishReason = FinishReason.ERROR
                        ))
                    }
                    else -> {
                        circuitBreaker.onError(
                            System.nanoTime() - System.nanoTime(),
                            java.util.concurrent.TimeUnit.NANOSECONDS,
                            e
                        )
                        throw e
                    }
                }
            }
    }

    /**
     * Health check with circuit breaker consideration.
     */
    override suspend fun isHealthy(): Boolean {
        return try {
            // Consider healthy if circuit breaker is closed or half-open
            val cbState = circuitBreaker.state
            val isCircuitBreakerHealthy = cbState != io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN

            if (!isCircuitBreakerHealthy) {
                logger.debug("Circuit breaker is OPEN, reporting unhealthy")
                return false
            }

            delegate.isHealthy()
        } catch (e: Exception) {
            logger.warn("Health check failed: {}", e.message)
            false
        }
    }

    /**
     * Returns current resilience metrics for monitoring.
     */
    fun getMetrics(): ResilienceMetrics {
        return ResilienceMetrics(
            circuitBreakerState = circuitBreaker.state.name,
            circuitBreakerFailureRate = circuitBreaker.metrics.failureRate,
            circuitBreakerSlowCallRate = circuitBreaker.metrics.slowCallRate,
            rateLimiterAvailablePermissions = rateLimiter.metrics.availablePermissions,
            bulkheadAvailableConcurrentCalls = bulkhead.metrics.availableConcurrentCalls,
            bulkheadMaxAllowedConcurrentCalls = bulkhead.metrics.maxAllowedConcurrentCalls
        )
    }
}

/**
 * Metrics snapshot for resilience components.
 */
data class ResilienceMetrics(
    val circuitBreakerState: String,
    val circuitBreakerFailureRate: Float,
    val circuitBreakerSlowCallRate: Float,
    val rateLimiterAvailablePermissions: Int,
    val bulkheadAvailableConcurrentCalls: Int,
    val bulkheadMaxAllowedConcurrentCalls: Int
)
