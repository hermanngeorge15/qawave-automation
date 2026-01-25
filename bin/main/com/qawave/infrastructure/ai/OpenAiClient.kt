package com.qawave.infrastructure.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.core.publisher.Mono

/**
 * OpenAI API client implementation.
 * Supports both completion and streaming responses.
 */
@Component("openAiClient")
@ConditionalOnProperty(name = ["qawave.ai.provider"], havingValue = "openai", matchIfMissing = true)
class OpenAiClient(
    @Value("\${qawave.ai.api-key}")
    private val apiKey: String,

    @Value("\${qawave.ai.base-url:https://api.openai.com}")
    private val baseUrl: String,

    @Value("\${qawave.ai.model:gpt-4o-mini}")
    private val defaultModel: String,

    private val objectMapper: ObjectMapper
) : AiClient {

    private val logger = LoggerFactory.getLogger(OpenAiClient::class.java)

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    override suspend fun complete(request: AiCompletionRequest): AiCompletionResponse {
        logger.debug("Sending completion request to OpenAI: model={}", request.model ?: defaultModel)

        val openAiRequest = buildRequest(request, stream = false)

        val response = webClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(openAiRequest)
            .exchangeToMono { clientResponse ->
                handleResponse(clientResponse)
            }
            .awaitFirst()

        return parseResponse(response)
    }

    override fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk> = flow {
        logger.debug("Starting streaming completion from OpenAI: model={}", request.model ?: defaultModel)

        val openAiRequest = buildRequest(request, stream = true)

        val responseFlow = webClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(openAiRequest)
            .retrieve()
            .bodyToFlow<String>()

        responseFlow.collect { line ->
            if (line.startsWith("data: ") && line != "data: [DONE]") {
                val jsonData = line.removePrefix("data: ")
                try {
                    val chunk = parseStreamChunk(jsonData)
                    if (chunk != null) {
                        emit(chunk)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse stream chunk: {}", e.message)
                }
            }
        }

        emit(AiStreamChunk("", isComplete = true, finishReason = FinishReason.STOP))
    }

    override suspend fun isHealthy(): Boolean {
        return try {
            // Try to list models as a health check
            val response = webClient.get()
                .uri("/v1/models")
                .retrieve()
                .toBodilessEntity()
                .awaitFirst()

            response.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            logger.warn("Health check failed: {}", e.message)
            false
        }
    }

    private fun buildRequest(request: AiCompletionRequest, stream: Boolean): OpenAiChatRequest {
        val messages = mutableListOf<OpenAiMessage>()

        request.systemPrompt?.let {
            messages.add(OpenAiMessage(role = "system", content = it))
        }
        messages.add(OpenAiMessage(role = "user", content = request.prompt))

        return OpenAiChatRequest(
            model = request.model ?: defaultModel,
            messages = messages,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            stop = request.stopSequences.ifEmpty { null },
            stream = stream
        )
    }

    private fun handleResponse(response: ClientResponse): Mono<OpenAiChatResponse> {
        return when {
            response.statusCode().is2xxSuccessful -> {
                response.bodyToMono(OpenAiChatResponse::class.java)
            }
            response.statusCode() == HttpStatus.TOO_MANY_REQUESTS -> {
                response.bodyToMono(String::class.java).flatMap { body ->
                    val retryAfter = response.headers().header("Retry-After").firstOrNull()?.toIntOrNull()
                    Mono.error(AiRateLimitException("Rate limited by OpenAI", retryAfter))
                }
            }
            response.statusCode().is5xxServerError -> {
                response.bodyToMono(String::class.java).flatMap { body ->
                    Mono.error(AiProviderException("OpenAI server error: $body", response.statusCode().value()))
                }
            }
            else -> {
                response.bodyToMono(String::class.java).flatMap { body ->
                    Mono.error(AiClientException("OpenAI API error: $body (${response.statusCode()})"))
                }
            }
        }
    }

    private fun parseResponse(response: OpenAiChatResponse): AiCompletionResponse {
        val choice = response.choices.firstOrNull()
            ?: throw AiClientException("No choices in OpenAI response")

        return AiCompletionResponse(
            content = choice.message.content,
            model = response.model,
            promptTokens = response.usage?.promptTokens ?: 0,
            completionTokens = response.usage?.completionTokens ?: 0,
            totalTokens = response.usage?.totalTokens ?: 0,
            finishReason = parseFinishReason(choice.finishReason)
        )
    }

    private fun parseStreamChunk(json: String): AiStreamChunk? {
        return try {
            val response = objectMapper.readValue(json, OpenAiStreamResponse::class.java)
            val choice = response.choices.firstOrNull() ?: return null
            val content = choice.delta?.content ?: ""

            AiStreamChunk(
                content = content,
                isComplete = choice.finishReason != null,
                finishReason = choice.finishReason?.let { parseFinishReason(it) }
            )
        } catch (e: Exception) {
            logger.debug("Failed to parse stream chunk: {}", e.message)
            null
        }
    }

    private fun parseFinishReason(reason: String?): FinishReason {
        return when (reason) {
            "stop" -> FinishReason.STOP
            "length" -> FinishReason.LENGTH
            "content_filter" -> FinishReason.CONTENT_FILTER
            else -> FinishReason.STOP
        }
    }
}

// ==================== OpenAI API DTOs ====================

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double?,
    @JsonProperty("max_tokens")
    val maxTokens: Int?,
    val stop: List<String>?,
    val stream: Boolean = false
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
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

data class OpenAiStreamResponse(
    val id: String,
    val choices: List<OpenAiStreamChoice>
)

data class OpenAiStreamChoice(
    val index: Int,
    val delta: OpenAiDelta?,
    @JsonProperty("finish_reason")
    val finishReason: String?
)

data class OpenAiDelta(
    val role: String?,
    val content: String?
)
