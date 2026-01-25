package com.qawave.unit

import com.qawave.infrastructure.ai.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AI client implementations.
 */
class AiClientTest {

    private lateinit var stubClient: StubAiClient

    @BeforeEach
    fun setup() {
        stubClient = StubAiClient()
    }

    @Nested
    inner class StubAiClientTests {

        @Test
        fun `complete returns valid response`() = runTest {
            val request = AiCompletionRequest(
                prompt = "Generate test scenarios",
                systemPrompt = "You are a QA engineer"
            )

            val response = stubClient.complete(request)

            assertNotNull(response)
            assertNotNull(response.content)
            assertTrue(response.content.isNotEmpty())
            assertEquals(FinishReason.STOP, response.finishReason)
            assertEquals("stub-model", response.model)
        }

        @Test
        fun `complete generates scenario response for scenario prompts`() = runTest {
            val request = AiCompletionRequest(
                prompt = "Generate test scenario for login API"
            )

            val response = stubClient.complete(request)

            assertTrue(response.content.contains("scenarios"))
            assertTrue(response.content.contains("steps"))
        }

        @Test
        fun `complete generates evaluation response for evaluate prompts`() = runTest {
            val request = AiCompletionRequest(
                prompt = "Evaluate the test results"
            )

            val response = stubClient.complete(request)

            assertTrue(response.content.contains("verdict"))
            assertTrue(response.content.contains("findings"))
        }

        @Test
        fun `complete generates summary response for summary prompts`() = runTest {
            val request = AiCompletionRequest(
                prompt = "Provide a summary of the test run"
            )

            val response = stubClient.complete(request)

            assertTrue(response.content.contains("overallVerdict"))
            assertTrue(response.content.contains("qualityScore"))
        }

        @Test
        fun `complete uses custom response when set`() = runTest {
            val prompt = "Custom prompt"
            val customResponse = "Custom response content"
            stubClient.addCustomResponse(prompt.hashCode().toString(), customResponse)

            val request = AiCompletionRequest(prompt = prompt)
            val response = stubClient.complete(request)

            assertEquals(customResponse, response.content)
        }

        @Test
        fun `complete throws exception when configured to fail`() = runTest {
            stubClient.setShouldFail(true, "Test failure")

            val request = AiCompletionRequest(prompt = "Any prompt")

            val exception = assertThrows<AiClientException> {
                stubClient.complete(request)
            }

            assertEquals("Test failure", exception.message)
        }

        @Test
        fun `completeStream returns chunks`() = runTest {
            val request = AiCompletionRequest(prompt = "Generate test")

            val chunks = stubClient.completeStream(request).toList()

            assertTrue(chunks.isNotEmpty())
            assertTrue(chunks.last().isComplete)
            assertEquals(FinishReason.STOP, chunks.last().finishReason)
        }

        @Test
        fun `completeStream returns error chunk when configured to fail`() = runTest {
            stubClient.setShouldFail(true)

            val request = AiCompletionRequest(prompt = "Any prompt")
            val chunks = stubClient.completeStream(request).toList()

            assertEquals(1, chunks.size)
            assertTrue(chunks.first().isComplete)
            assertEquals(FinishReason.ERROR, chunks.first().finishReason)
        }

        @Test
        fun `isHealthy returns true by default`() = runTest {
            assertTrue(stubClient.isHealthy())
        }

        @Test
        fun `isHealthy returns false when configured to fail`() = runTest {
            stubClient.setShouldFail(true)
            assertFalse(stubClient.isHealthy())
        }

        @Test
        fun `clearCustomResponses removes all custom responses`() = runTest {
            stubClient.addCustomResponse("hash1", "response1")
            stubClient.addCustomResponse("hash2", "response2")

            stubClient.clearCustomResponses()

            val request = AiCompletionRequest(prompt = "default")
            val response = stubClient.complete(request)

            // Should return default stub response
            assertTrue(response.content.contains("stub AI response"))
        }
    }

    @Nested
    inner class AiCompletionRequestTests {

        @Test
        fun `request has default values`() {
            val request = AiCompletionRequest(prompt = "test")

            assertNotNull(request.prompt)
            assertEquals(0.2, request.temperature)
            assertEquals(4096, request.maxTokens)
            assertTrue(request.stopSequences.isEmpty())
        }

        @Test
        fun `request can override all values`() {
            val request = AiCompletionRequest(
                prompt = "test",
                systemPrompt = "system",
                model = "gpt-4",
                temperature = 0.8,
                maxTokens = 1000,
                stopSequences = listOf("STOP", "END")
            )

            assertEquals("test", request.prompt)
            assertEquals("system", request.systemPrompt)
            assertEquals("gpt-4", request.model)
            assertEquals(0.8, request.temperature)
            assertEquals(1000, request.maxTokens)
            assertEquals(2, request.stopSequences.size)
        }
    }

    @Nested
    inner class AiCompletionResponseTests {

        @Test
        fun `response contains all fields`() {
            val response = AiCompletionResponse(
                content = "test content",
                model = "gpt-4",
                promptTokens = 10,
                completionTokens = 20,
                totalTokens = 30,
                finishReason = FinishReason.STOP
            )

            assertEquals("test content", response.content)
            assertEquals("gpt-4", response.model)
            assertEquals(10, response.promptTokens)
            assertEquals(20, response.completionTokens)
            assertEquals(30, response.totalTokens)
            assertEquals(FinishReason.STOP, response.finishReason)
        }
    }

    @Nested
    inner class AiExceptionTests {

        @Test
        fun `AiClientException contains message`() {
            val exception = AiClientException("Test error")
            assertEquals("Test error", exception.message)
        }

        @Test
        fun `AiRateLimitException contains retry after`() {
            val exception = AiRateLimitException("Rate limited", 60)
            assertEquals("Rate limited", exception.message)
            assertEquals(60, exception.retryAfterSeconds)
        }

        @Test
        fun `AiProviderException contains status code`() {
            val exception = AiProviderException("Server error", 500)
            assertEquals("Server error", exception.message)
            assertEquals(500, exception.statusCode)
        }
    }
}
