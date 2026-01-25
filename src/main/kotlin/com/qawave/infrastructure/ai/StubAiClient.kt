package com.qawave.infrastructure.ai

import com.qawave.application.port.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

/**
 * Stub AI client for testing and development.
 * Returns predefined responses without calling any external API.
 */
class StubAiClient(
    private val simulatedDelayMs: Long = 100,
    private val defaultResponse: String = DEFAULT_RESPONSE
) : AiClient {

    private val logger = LoggerFactory.getLogger(StubAiClient::class.java)

    override val providerName: String = "stub"

    private var responseOverride: String? = null
    private var shouldFail: Boolean = false
    private var failureException: AiClientException? = null

    /**
     * Sets a custom response for the next request.
     */
    fun setNextResponse(response: String) {
        this.responseOverride = response
    }

    /**
     * Configures the client to fail on the next request.
     */
    fun setNextFailure(exception: AiClientException) {
        this.shouldFail = true
        this.failureException = exception
    }

    /**
     * Resets the client to default behavior.
     */
    fun reset() {
        responseOverride = null
        shouldFail = false
        failureException = null
    }

    override suspend fun complete(request: AiCompletionRequest): AiResponse {
        logger.debug("StubAiClient handling completion request")

        // Simulate network delay
        delay(simulatedDelayMs)

        // Check for configured failure
        if (shouldFail) {
            shouldFail = false
            throw failureException ?: AiClientException("Stub failure", provider = providerName)
        }

        val content = responseOverride ?: generateResponse(request)
        responseOverride = null

        return AiResponse(
            content = content,
            model = "stub-model",
            usage = TokenUsage(
                promptTokens = request.systemPrompt.length + request.userPrompt.length,
                completionTokens = content.length,
                totalTokens = request.systemPrompt.length + request.userPrompt.length + content.length
            ),
            finishReason = "stop",
            metadata = mapOf("provider" to "stub")
        )
    }

    override fun streamComplete(request: AiCompletionRequest): Flow<AiResponseChunk> = flow {
        logger.debug("StubAiClient handling streaming completion request")

        // Simulate network delay
        delay(simulatedDelayMs)

        if (shouldFail) {
            shouldFail = false
            throw failureException ?: AiClientException("Stub failure", provider = providerName)
        }

        val content = responseOverride ?: generateResponse(request)
        responseOverride = null

        // Stream word by word
        val words = content.split(" ")
        words.forEachIndexed { index, word ->
            delay(10) // Simulate streaming delay
            emit(AiResponseChunk(
                content = if (index > 0) " $word" else word,
                isComplete = index == words.lastIndex,
                finishReason = if (index == words.lastIndex) "stop" else null
            ))
        }
    }

    override suspend fun healthCheck(): Boolean {
        return true
    }

    private fun generateResponse(request: AiCompletionRequest): String {
        // Check if the request is for JSON format
        if (request.responseFormat == ResponseFormat.JSON) {
            return when {
                request.userPrompt.contains("scenario", ignoreCase = true) -> STUB_SCENARIO_JSON
                request.userPrompt.contains("evaluate", ignoreCase = true) -> STUB_EVALUATION_JSON
                else -> """{"message": "Stub response"}"""
            }
        }
        return defaultResponse
    }

    companion object {
        const val DEFAULT_RESPONSE = "This is a stub response from the AI client."

        const val STUB_SCENARIO_JSON = """
{
  "name": "User Registration Flow",
  "description": "Tests the user registration process",
  "steps": [
    {
      "index": 0,
      "name": "Create new user",
      "method": "POST",
      "endpoint": "/api/users",
      "headers": {"Content-Type": "application/json"},
      "body": {"email": "test@example.com", "password": "secret123"},
      "expected": {
        "status": 201,
        "bodyFields": {"id": "<any>", "email": "test@example.com"}
      },
      "extractions": {"userId": "$.id"}
    },
    {
      "index": 1,
      "name": "Get user by ID",
      "method": "GET",
      "endpoint": "/api/users/{userId}",
      "expected": {
        "status": 200,
        "bodyFields": {"email": "test@example.com"}
      }
    }
  ]
}
"""

        const val STUB_EVALUATION_JSON = """
{
  "verdict": "PASS",
  "summary": "All tests passed successfully",
  "passedCount": 2,
  "failedCount": 0,
  "findings": [],
  "recommendations": []
}
"""
    }
}
