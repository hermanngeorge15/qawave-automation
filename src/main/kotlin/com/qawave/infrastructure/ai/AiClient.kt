package com.qawave.infrastructure.ai

import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI provider clients.
 * Provides suspend functions for non-blocking AI interactions.
 */
interface AiClient {

    /**
     * Generates a completion for the given prompt.
     *
     * @param request The completion request
     * @return The AI response
     */
    suspend fun complete(request: AiCompletionRequest): AiCompletionResponse

    /**
     * Generates a streaming completion for the given prompt.
     * Returns chunks of the response as they are generated.
     *
     * @param request The completion request
     * @return Flow of response chunks
     */
    fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk>

    /**
     * Checks if the AI provider is available.
     *
     * @return true if the provider is healthy
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
    val stopSequences: List<String> = emptyList()
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
    val finishReason: FinishReason
)

/**
 * A chunk of streaming response.
 */
data class AiStreamChunk(
    val content: String,
    val isComplete: Boolean = false,
    val finishReason: FinishReason? = null
)

/**
 * Reason for completion finish.
 */
enum class FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    ERROR
}

/**
 * Exception thrown when AI completion fails.
 */
open class AiClientException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when rate limited by AI provider.
 */
class AiRateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null
) : AiClientException(message)

/**
 * Exception thrown when AI provider returns an error.
 */
class AiProviderException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null
) : AiClientException(message, cause)
