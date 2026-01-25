package com.qawave.infrastructure.ai

import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI completion clients.
 * Supports both synchronous completion and streaming responses.
 */
interface AiClient {
    /**
     * Sends a completion request and returns the full response.
     */
    suspend fun complete(request: AiCompletionRequest): AiCompletionResponse

    /**
     * Sends a completion request and returns a stream of response chunks.
     */
    fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk>

    /**
     * Checks if the AI client is healthy and able to process requests.
     */
    suspend fun isHealthy(): Boolean
}

/**
 * Request for AI completion.
 */
data class AiCompletionRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val model: String? = null,
    val temperature: Double = 0.2,
    val maxTokens: Int = 4096,
    val stopSequences: List<String> = emptyList(),
)

/**
 * Response from AI completion.
 */
data class AiCompletionResponse(
    val content: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val finishReason: FinishReason,
)

/**
 * A chunk from a streaming AI response.
 */
data class AiStreamChunk(
    val content: String,
    val isComplete: Boolean = false,
    val finishReason: FinishReason? = null,
)

/**
 * Reason for completion finishing.
 */
enum class FinishReason {
    STOP, // Natural end of generation
    LENGTH, // Max tokens reached
    CONTENT_FILTER, // Content was filtered
    ERROR, // An error occurred
}

/**
 * Base exception for AI client errors.
 */
open class AiClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception for rate limiting from AI provider.
 */
class AiRateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null,
) : AiClientException(message)

/**
 * Exception for AI provider errors (5xx responses).
 */
class AiProviderException(
    message: String,
    val statusCode: Int,
) : AiClientException(message)
