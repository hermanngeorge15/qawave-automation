package com.qawave.unit

import com.qawave.infrastructure.ai.*
import com.qawave.infrastructure.resilience.*
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for resilience components.
 */
class ResilienceTest {

    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry
    private lateinit var rateLimiterRegistry: RateLimiterRegistry
    private lateinit var retryRegistry: RetryRegistry
    private lateinit var bulkheadRegistry: BulkheadRegistry
    private lateinit var fallbackHandler: AiFallbackHandler
    private lateinit var stubClient: TestableAiClient
    private lateinit var resilientClient: ResilientAiClient

    @BeforeEach
    fun setup() {
        // Create registries with test-friendly configurations
        circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50f)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build()
        )

        rateLimiterRegistry = RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build()
        )

        retryRegistry = RetryRegistry.of(
            RetryConfig.custom<Any>()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(AiClientException::class.java)
                .build()
        )

        bulkheadRegistry = BulkheadRegistry.of(
            BulkheadConfig.custom()
                .maxConcurrentCalls(3)
                .maxWaitDuration(Duration.ofMillis(100))
                .build()
        )

        fallbackHandler = AiFallbackHandler()
        stubClient = TestableAiClient()

        resilientClient = ResilientAiClient(
            delegate = stubClient,
            circuitBreakerRegistry = circuitBreakerRegistry,
            rateLimiterRegistry = rateLimiterRegistry,
            retryRegistry = retryRegistry,
            bulkheadRegistry = bulkheadRegistry,
            fallbackHandler = fallbackHandler
        )
    }

    @Nested
    inner class SuccessfulRequestTests {

        @Test
        fun `complete returns response when delegate succeeds`() = runTest {
            val request = AiCompletionRequest(prompt = "Test prompt")

            val response = resilientClient.complete(request)

            assertNotNull(response)
            assertEquals("Test response", response.content)
            assertEquals(FinishReason.STOP, response.finishReason)
        }

        @Test
        fun `isHealthy returns true when delegate is healthy`() = runTest {
            stubClient.setHealthy(true)

            assertTrue(resilientClient.isHealthy())
        }
    }

    @Nested
    inner class CircuitBreakerTests {

        @Test
        fun `circuit breaker opens after failure threshold`() = runTest {
            stubClient.setShouldFail(true)
            val request = AiCompletionRequest(prompt = "Test prompt")

            // Trigger failures to open circuit breaker
            repeat(5) {
                try {
                    resilientClient.complete(request)
                } catch (e: Exception) {
                    // Expected failures
                }
            }

            val circuitBreaker = circuitBreakerRegistry.circuitBreaker(ResilienceConfig.AI_CLIENT_CIRCUIT_BREAKER)
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
        }

        @Test
        fun `returns fallback when circuit breaker is open`() = runTest {
            // Force circuit breaker to open
            val circuitBreaker = circuitBreakerRegistry.circuitBreaker(ResilienceConfig.AI_CLIENT_CIRCUIT_BREAKER)
            circuitBreaker.transitionToOpenState()

            val request = AiCompletionRequest(prompt = "Generate scenario")
            val response = resilientClient.complete(request)

            assertEquals(FinishReason.ERROR, response.finishReason)
            assertEquals("fallback", response.model)
        }

        @Test
        fun `isHealthy returns false when circuit breaker is open`() = runTest {
            val circuitBreaker = circuitBreakerRegistry.circuitBreaker(ResilienceConfig.AI_CLIENT_CIRCUIT_BREAKER)
            circuitBreaker.transitionToOpenState()

            assertFalse(resilientClient.isHealthy())
        }
    }

    @Nested
    inner class RetryTests {

        @Test
        fun `retries on failure and succeeds on second attempt`() = runTest {
            stubClient.setFailCount(1) // Fail first attempt only
            val request = AiCompletionRequest(prompt = "Test prompt")

            val response = resilientClient.complete(request)

            assertNotNull(response)
            assertEquals(2, stubClient.callCount) // One failure + one success
        }
    }

    @Nested
    inner class FallbackHandlerTests {

        @Test
        fun `generates scenario fallback for scenario requests`() {
            val request = AiCompletionRequest(prompt = "Generate test scenarios")
            val response = fallbackHandler.handleCompletionFallback(
                request,
                AiClientException("Test error")
            )

            assertTrue(response.content.contains("scenarios"))
            assertTrue(response.content.contains("fallback"))
            assertEquals(FinishReason.ERROR, response.finishReason)
        }

        @Test
        fun `generates evaluation fallback for evaluate requests`() {
            val request = AiCompletionRequest(prompt = "Evaluate test results")
            val response = fallbackHandler.handleCompletionFallback(
                request,
                AiClientException("Test error")
            )

            assertTrue(response.content.contains("overallVerdict"))
            assertTrue(response.content.contains("INCONCLUSIVE"))
            assertEquals(FinishReason.ERROR, response.finishReason)
        }

        @Test
        fun `generates coverage fallback for coverage requests`() {
            val request = AiCompletionRequest(prompt = "Analyze coverage")
            val response = fallbackHandler.handleCompletionFallback(
                request,
                AiClientException("Test error")
            )

            assertTrue(response.content.contains("totalOperations"))
            assertTrue(response.content.contains("coveragePercentage"))
            assertEquals(FinishReason.ERROR, response.finishReason)
        }

        @Test
        fun `getFallbackMessage returns appropriate message`() {
            val message = fallbackHandler.getFallbackMessage()
            assertTrue(message.contains("temporarily unavailable"))
        }
    }

    @Nested
    inner class MetricsTests {

        @Test
        fun `getMetrics returns current resilience state`() {
            val metrics = resilientClient.getMetrics()

            assertNotNull(metrics)
            assertEquals("CLOSED", metrics.circuitBreakerState)
            assertTrue(metrics.rateLimiterAvailablePermissions > 0)
            assertTrue(metrics.bulkheadAvailableConcurrentCalls > 0)
        }
    }
}

/**
 * Testable AI client that can be configured for testing scenarios.
 */
class TestableAiClient : AiClient {
    private var shouldFail = false
    private var failCount = 0
    private var currentFailures = 0
    private var healthy = true
    var callCount = 0
        private set

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    fun setFailCount(count: Int) {
        failCount = count
        currentFailures = 0
    }

    fun setHealthy(isHealthy: Boolean) {
        healthy = isHealthy
    }

    override suspend fun complete(request: AiCompletionRequest): AiCompletionResponse {
        callCount++

        if (shouldFail) {
            throw AiClientException("Simulated failure")
        }

        if (currentFailures < failCount) {
            currentFailures++
            throw AiClientException("Simulated failure ${currentFailures}/${failCount}")
        }

        return AiCompletionResponse(
            content = "Test response",
            model = "test-model",
            promptTokens = 10,
            completionTokens = 20,
            totalTokens = 30,
            finishReason = FinishReason.STOP
        )
    }

    override fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk> {
        return flowOf(
            AiStreamChunk("Test ", isComplete = false),
            AiStreamChunk("response", isComplete = true, finishReason = FinishReason.STOP)
        )
    }

    override suspend fun isHealthy(): Boolean {
        return healthy
    }
}
