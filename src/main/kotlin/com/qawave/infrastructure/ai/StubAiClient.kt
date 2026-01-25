package com.qawave.infrastructure.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Stub AI client for testing and development.
 * Returns predefined responses without making actual API calls.
 */
@Component
@ConditionalOnProperty(name = ["qawave.ai.provider"], havingValue = "stub")
class StubAiClient : AiClient {

    private val logger = LoggerFactory.getLogger(StubAiClient::class.java)

    private var responseDelay: Long = 100
    private var customResponses: MutableMap<String, String> = mutableMapOf()
    private var shouldFail: Boolean = false
    private var failureMessage: String = "Simulated failure"

    override suspend fun complete(request: AiCompletionRequest): AiCompletionResponse {
        logger.debug("Stub AI completing request: promptLength={}", request.prompt.length)

        delay(responseDelay)

        if (shouldFail) {
            throw AiClientException(failureMessage)
        }

        val content = customResponses[request.prompt.hashCode().toString()]
            ?: generateDefaultResponse(request)

        return AiCompletionResponse(
            content = content,
            model = request.model ?: "stub-model",
            promptTokens = request.prompt.length / 4,
            completionTokens = content.length / 4,
            totalTokens = (request.prompt.length + content.length) / 4,
            finishReason = FinishReason.STOP
        )
    }

    override fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk> = flow {
        logger.debug("Stub AI streaming request: promptLength={}", request.prompt.length)

        if (shouldFail) {
            emit(AiStreamChunk("", isComplete = true, finishReason = FinishReason.ERROR))
            return@flow
        }

        val content = customResponses[request.prompt.hashCode().toString()]
            ?: generateDefaultResponse(request)

        // Emit content in chunks
        val words = content.split(" ")
        for ((index, word) in words.withIndex()) {
            delay(responseDelay / words.size)
            val chunk = if (index < words.size - 1) "$word " else word
            emit(AiStreamChunk(
                content = chunk,
                isComplete = index == words.size - 1,
                finishReason = if (index == words.size - 1) FinishReason.STOP else null
            ))
        }
    }

    override suspend fun isHealthy(): Boolean {
        return !shouldFail
    }

    /**
     * Sets the delay before returning responses.
     */
    fun setResponseDelay(delayMs: Long) {
        this.responseDelay = delayMs
    }

    /**
     * Adds a custom response for a specific prompt.
     */
    fun addCustomResponse(promptHash: String, response: String) {
        customResponses[promptHash] = response
    }

    /**
     * Clears all custom responses.
     */
    fun clearCustomResponses() {
        customResponses.clear()
    }

    /**
     * Configures the client to fail on requests.
     */
    fun setShouldFail(fail: Boolean, message: String = "Simulated failure") {
        this.shouldFail = fail
        this.failureMessage = message
    }

    private fun generateDefaultResponse(request: AiCompletionRequest): String {
        return when {
            request.prompt.contains("scenario", ignoreCase = true) -> generateScenarioResponse()
            request.prompt.contains("evaluate", ignoreCase = true) -> generateEvaluationResponse()
            request.prompt.contains("summary", ignoreCase = true) -> generateSummaryResponse()
            else -> "This is a stub AI response for testing purposes."
        }
    }

    private fun generateScenarioResponse(): String {
        return """
        {
            "scenarios": [
                {
                    "name": "Test User Login",
                    "description": "Verify user can login with valid credentials",
                    "steps": [
                        {
                            "name": "Send login request",
                            "method": "POST",
                            "path": "/api/auth/login",
                            "body": {"username": "testuser", "password": "password123"},
                            "expectedStatus": 200
                        },
                        {
                            "name": "Verify token returned",
                            "assertions": ["response.body.token != null"]
                        }
                    ]
                },
                {
                    "name": "Test Get User Profile",
                    "description": "Verify authenticated user can retrieve their profile",
                    "steps": [
                        {
                            "name": "Get user profile",
                            "method": "GET",
                            "path": "/api/users/me",
                            "headers": {"Authorization": "Bearer {{token}}"},
                            "expectedStatus": 200
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
    }

    private fun generateEvaluationResponse(): String {
        return """
        {
            "verdict": "PASS",
            "summary": "All test scenarios passed successfully.",
            "findings": [
                {
                    "type": "INFO",
                    "title": "Good API Response Times",
                    "description": "All endpoints responded within acceptable time limits."
                }
            ],
            "recommendations": [
                {
                    "priority": "LOW",
                    "title": "Consider adding rate limiting tests",
                    "description": "The API would benefit from rate limiting validation."
                }
            ]
        }
        """.trimIndent()
    }

    private fun generateSummaryResponse(): String {
        return """
        {
            "overallVerdict": "PASS",
            "passedScenarios": 5,
            "failedScenarios": 0,
            "erroredScenarios": 0,
            "coveragePercentage": 85.5,
            "qualityScore": 92,
            "stabilityScore": 88,
            "summary": "The API is functioning correctly with good test coverage."
        }
        """.trimIndent()
    }
}
