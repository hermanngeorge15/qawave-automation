package com.qawave.infrastructure.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.qawave.application.port.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux

/**
 * OpenAI API client implementation.
 * Uses Spring WebClient for non-blocking HTTP calls.
 */
@Component
class OpenAiClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    @Value("\${qawave.ai.api-key:}") private val apiKey: String,
    @Value("\${qawave.ai.model:gpt-4o-mini}") private val defaultModel: String,
    @Value("\${qawave.ai.base-url:https://api.openai.com}") private val baseUrl: String
) : AiClient {

    private val logger = LoggerFactory.getLogger(OpenAiClient::class.java)

    override val providerName: String = "openai"

    override suspend fun complete(request: AiCompletionRequest): AiResponse {
        logger.debug("Sending completion request to OpenAI: model={}", request.model ?: defaultModel)

        val openAiRequest = buildRequest(request, stream = false)

        try {
            val response = webClient.post()
                .uri("$baseUrl/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToMono(OpenAiCompletionResponse::class.java)
                .awaitFirst()

            return mapResponse(response)
        } catch (e: WebClientResponseException) {
            throw mapException(e)
        }
    }

    override fun streamComplete(request: AiCompletionRequest): Flow<AiResponseChunk> {
        logger.debug("Starting streaming completion request to OpenAI")

        val openAiRequest = buildRequest(request, stream = true)

        return webClient.post()
            .uri("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(openAiRequest)
            .retrieve()
            .bodyToFlux<String>()
            .filter { line -> line.startsWith("data: ") && line != "data: [DONE]" }
            .map { line ->
                val jsonData = line.removePrefix("data: ")
                try {
                    val chunk: OpenAiStreamChunk = objectMapper.readValue(jsonData, OpenAiStreamChunk::class.java)
                    val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                    val finishReason = chunk.choices.firstOrNull()?.finishReason

                    AiResponseChunk(
                        content = content,
                        isComplete = finishReason != null,
                        finishReason = finishReason
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse stream chunk: {}", jsonData, e)
                    AiResponseChunk(content = "", isComplete = false)
                }
            }
            .asFlow()
    }

    override suspend fun healthCheck(): Boolean {
        return try {
            webClient.get()
                .uri("$baseUrl/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .retrieve()
                .toBodilessEntity()
                .awaitFirstOrNull()
            true
        } catch (e: Exception) {
            logger.warn("OpenAI health check failed", e)
            false
        }
    }

    private fun buildRequest(request: AiCompletionRequest, stream: Boolean): OpenAiCompletionRequest {
        val messages = listOf(
            OpenAiMessage(role = "system", content = request.systemPrompt),
            OpenAiMessage(role = "user", content = request.userPrompt)
        )

        return OpenAiCompletionRequest(
            model = request.model ?: defaultModel,
            messages = messages,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            stream = stream,
            responseFormat = if (request.responseFormat == ResponseFormat.JSON) {
                OpenAiResponseFormat(type = "json_object")
            } else null
        )
    }

    private fun mapResponse(response: OpenAiCompletionResponse): AiResponse {
        val choice = response.choices.firstOrNull()
            ?: throw AiClientException("No choices in OpenAI response", provider = providerName)

        return AiResponse(
            content = choice.message.content,
            model = response.model,
            usage = response.usage?.let {
                TokenUsage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens
                )
            },
            finishReason = choice.finishReason,
            metadata = mapOf("id" to response.id)
        )
    }

    private fun mapException(e: WebClientResponseException): AiClientException {
        return when (e.statusCode) {
            HttpStatus.TOO_MANY_REQUESTS -> {
                val retryAfter = e.headers.getFirst("Retry-After")?.toLongOrNull()?.times(1000)
                AiRateLimitException(
                    message = "OpenAI rate limit exceeded",
                    retryAfterMs = retryAfter
                )
            }
            HttpStatus.UNAUTHORIZED -> AiClientException(
                message = "OpenAI authentication failed",
                statusCode = 401,
                provider = providerName
            )
            else -> AiClientException(
                message = "OpenAI request failed: ${e.message}",
                statusCode = e.statusCode.value(),
                provider = providerName,
                cause = e
            )
        }
    }
}

// OpenAI API DTOs

data class OpenAiCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.2,
    @JsonProperty("max_tokens")
    val maxTokens: Int = 4096,
    val stream: Boolean = false,
    @JsonProperty("response_format")
    val responseFormat: OpenAiResponseFormat? = null
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiResponseFormat(
    val type: String
)

data class OpenAiCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage?
)

data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @JsonProperty("finish_reason")
    val finishReason: String?
)

data class OpenAiUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int
)

data class OpenAiStreamChunk(
    val choices: List<OpenAiStreamChoice>
)

data class OpenAiStreamChoice(
    val delta: OpenAiDelta,
    @JsonProperty("finish_reason")
    val finishReason: String?
)

data class OpenAiDelta(
    val content: String?
)
