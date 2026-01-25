package com.qawave.infrastructure.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * OpenAI API client implementation.
 * Uses Spring WebClient for non-blocking HTTP requests.
 */
@Component
@ConditionalOnProperty(name = ["qawave.ai.provider"], havingValue = "openai", matchIfMissing = true)
class OpenAiClient(
    private val objectMapper: ObjectMapper,
    @Value("\${qawave.ai.api-key:}") private val apiKey: String,
    @Value("\${qawave.ai.model:gpt-4o-mini}") private val defaultModel: String,
    @Value("\${qawave.ai.temperature:0.2}") private val defaultTemperature: Double,
    @Value("\${qawave.ai.base-url:https://api.openai.com}") private val baseUrl: String
) : AiClient {

    private val logger = LoggerFactory.getLogger(OpenAiClient::class.java)

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer $apiKey")
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    override suspend fun complete(request: AiCompletionRequest): AiCompletionResponse {
        logger.debug("Sending completion request: model={}, promptLength={}",
            request.model ?: defaultModel, request.prompt.length)

        val openAiRequest = buildRequest(request)

        return try {
            val response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(openAiRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                    clientResponse.bodyToMono<String>().flatMap { body ->
                        when (clientResponse.statusCode().value()) {
                            429 -> Mono.error(AiRateLimitException("Rate limited by OpenAI", parseRetryAfter(body)))
                            else -> Mono.error(AiProviderException("OpenAI error: $body", clientResponse.statusCode().value()))
                        }
                    }
                }
                .onStatus(HttpStatusCode::is5xxServerError) { clientResponse ->
                    clientResponse.bodyToMono<String>().flatMap { body ->
                        Mono.error(AiProviderException("OpenAI server error: $body", clientResponse.statusCode().value()))
                    }
                }
                .bodyToMono<OpenAiChatResponse>()
                .awaitSingle()

            parseResponse(response)
        } catch (e: WebClientResponseException) {
            logger.error("OpenAI request failed: status={}, body={}", e.statusCode, e.responseBodyAsString)
            throw AiProviderException("OpenAI request failed: ${e.message}", e.statusCode.value(), e)
        } catch (e: AiClientException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error during OpenAI request", e)
            throw AiClientException("Unexpected error: ${e.message}", e)
        }
    }

    override fun completeStream(request: AiCompletionRequest): Flow<AiStreamChunk> = flow {
        logger.debug("Starting streaming completion: model={}", request.model ?: defaultModel)

        val openAiRequest = buildRequest(request).copy(stream = true)

        try {
            val chunks = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(openAiRequest)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux<String>()
                .asFlow()

            chunks.collect { chunk ->
                if (chunk.isNotBlank() && chunk != "[DONE]") {
                    try {
                        val parsed = parseStreamChunk(chunk)
                        if (parsed != null) {
                            emit(parsed)
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to parse stream chunk: {}", chunk)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Streaming error", e)
            emit(AiStreamChunk("", isComplete = true, finishReason = FinishReason.ERROR))
        }
    }

    override suspend fun isHealthy(): Boolean {
        return try {
            webClient.get()
                .uri("/v1/models")
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
            true
        } catch (e: Exception) {
            logger.warn("OpenAI health check failed: {}", e.message)
            false
        }
    }

    private fun buildRequest(request: AiCompletionRequest): OpenAiChatRequest {
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
            stop = request.stopSequences.ifEmpty { null }
        )
    }

    private fun parseResponse(response: OpenAiChatResponse): AiCompletionResponse {
        val choice = response.choices.firstOrNull()
            ?: throw AiClientException("No choices in OpenAI response")

        return AiCompletionResponse(
            content = choice.message.content,
            model = response.model,
            promptTokens = response.usage.promptTokens,
            completionTokens = response.usage.completionTokens,
            totalTokens = response.usage.totalTokens,
            finishReason = parseFinishReason(choice.finishReason)
        )
    }

    private fun parseStreamChunk(chunk: String): AiStreamChunk? {
        val data = chunk.removePrefix("data: ").trim()
        if (data.isEmpty() || data == "[DONE]") return null

        return try {
            val parsed = objectMapper.readValue(data, OpenAiStreamResponse::class.java)
            val delta = parsed.choices.firstOrNull()?.delta
            val finishReason = parsed.choices.firstOrNull()?.finishReason

            AiStreamChunk(
                content = delta?.content ?: "",
                isComplete = finishReason != null,
                finishReason = finishReason?.let { parseFinishReason(it) }
            )
        } catch (e: Exception) {
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

    private fun parseRetryAfter(body: String): Int? {
        return try {
            val json = objectMapper.readTree(body)
            json.get("error")?.get("retry_after")?.asInt()
        } catch (e: Exception) {
            null
        }
    }
}

// ==================== OpenAI API DTOs ====================

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.2,
    @JsonProperty("max_tokens") val maxTokens: Int = 4096,
    val stop: List<String>? = null,
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
    val usage: OpenAiUsage
)

data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @JsonProperty("finish_reason") val finishReason: String?
)

data class OpenAiUsage(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int
)

data class OpenAiStreamResponse(
    val id: String,
    val choices: List<OpenAiStreamChoice>
)

data class OpenAiStreamChoice(
    val index: Int,
    val delta: OpenAiDelta?,
    @JsonProperty("finish_reason") val finishReason: String?
)

data class OpenAiDelta(
    val content: String?
)
