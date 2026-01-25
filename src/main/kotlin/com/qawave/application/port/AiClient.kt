package com.qawave.application.port

import kotlinx.coroutines.flow.Flow

/**
 * Port interface for AI provider interactions.
 * Abstracts the underlying AI provider (OpenAI, Venice, etc.)
 * to enable easy switching and testing.
 */
interface AiClient {

    /**
     * Sends a completion request to the AI provider.
     *
     * @param request The completion request with system and user prompts
     * @return The AI's response
     */
    suspend fun complete(request: AiCompletionRequest): AiResponse

    /**
     * Sends a streaming completion request to the AI provider.
     * Returns a Flow for real-time token streaming.
     *
     * @param request The completion request with system and user prompts
     * @return A Flow of response chunks
     */
    fun streamComplete(request: AiCompletionRequest): Flow<AiResponseChunk>

    /**
     * Checks if the AI provider is available and responding.
     */
    suspend fun healthCheck(): Boolean

    /**
     * Returns the name of the AI provider.
     */
    val providerName: String
}

/**
 * Request for AI completion.
 */
data class AiCompletionRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val temperature: Double = 0.2,
    val maxTokens: Int = 4096,
    val model: String? = null,
    val responseFormat: ResponseFormat = ResponseFormat.TEXT,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Response format options.
 */
enum class ResponseFormat {
    TEXT,
    JSON
}

/**
 * Response from AI completion.
 */
data class AiResponse(
    val content: String,
    val model: String,
    val usage: TokenUsage?,
    val finishReason: String?,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Token usage statistics.
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * A chunk from streaming response.
 */
data class AiResponseChunk(
    val content: String,
    val isComplete: Boolean,
    val finishReason: String? = null
)

/**
 * Exception thrown when AI request fails.
 */
open class AiClientException(
    message: String,
    open val statusCode: Int? = null,
    open val provider: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when AI rate limit is exceeded.
 */
class AiRateLimitException(
    message: String,
    val retryAfterMs: Long? = null,
    provider: String? = null
) : AiClientException(message, statusCode = 429, provider = provider)

/**
 * Exception thrown when AI request times out.
 */
class AiTimeoutException(
    message: String,
    val timeoutMs: Long,
    provider: String? = null
) : AiClientException(message, provider = provider)
