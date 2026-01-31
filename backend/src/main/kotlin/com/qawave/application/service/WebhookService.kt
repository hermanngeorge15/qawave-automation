package com.qawave.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.qawave.domain.model.TestRun
import com.qawave.domain.model.WebhookConfig
import com.qawave.domain.model.WebhookDelivery
import com.qawave.domain.model.WebhookDeliveryId
import com.qawave.domain.model.WebhookDeliveryStatus
import com.qawave.domain.model.WebhookEvent
import com.qawave.domain.model.WebhookId
import com.qawave.domain.model.WebhookType
import com.qawave.infrastructure.persistence.entity.WebhookConfigEntity
import com.qawave.infrastructure.persistence.entity.WebhookDeliveryEntity
import com.qawave.infrastructure.persistence.repository.WebhookConfigR2dbcRepository
import com.qawave.infrastructure.persistence.repository.WebhookDeliveryR2dbcRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service for managing webhooks and sending notifications.
 */
interface WebhookService {
    /**
     * Create a new webhook configuration.
     */
    suspend fun createWebhook(
        name: String,
        url: String,
        webhookType: WebhookType,
        events: Set<WebhookEvent>,
        headers: Map<String, String>,
        secret: String?,
        createdBy: String,
    ): WebhookConfig

    /**
     * Get a webhook by ID.
     */
    suspend fun getWebhook(id: WebhookId): WebhookConfig?

    /**
     * List all webhooks for a user.
     */
    suspend fun listWebhooks(userId: String): List<WebhookConfig>

    /**
     * Update a webhook configuration.
     */
    suspend fun updateWebhook(
        id: WebhookId,
        name: String? = null,
        url: String? = null,
        events: Set<WebhookEvent>? = null,
        headers: Map<String, String>? = null,
        secret: String? = null,
        isActive: Boolean? = null,
    ): WebhookConfig

    /**
     * Delete a webhook.
     */
    suspend fun deleteWebhook(id: WebhookId): Boolean

    /**
     * Trigger webhooks for an event.
     */
    suspend fun triggerEvent(
        event: WebhookEvent,
        payload: Any,
    )

    /**
     * Process pending webhook deliveries.
     */
    suspend fun processDeliveries()

    /**
     * Get recent deliveries for a webhook.
     */
    suspend fun getRecentDeliveries(
        webhookId: WebhookId,
        limit: Int = 10,
    ): List<WebhookDelivery>
}

@Service
class WebhookServiceImpl(
    private val webhookConfigRepository: WebhookConfigR2dbcRepository,
    private val webhookDeliveryRepository: WebhookDeliveryR2dbcRepository,
    private val objectMapper: ObjectMapper,
    private val webClient: WebClient,
) : WebhookService {
    private val logger = LoggerFactory.getLogger(WebhookServiceImpl::class.java)

    override suspend fun createWebhook(
        name: String,
        url: String,
        webhookType: WebhookType,
        events: Set<WebhookEvent>,
        headers: Map<String, String>,
        secret: String?,
        createdBy: String,
    ): WebhookConfig {
        val webhook =
            WebhookConfig.create(
                name = name,
                url = url,
                webhookType = webhookType,
                events = events,
                headers = headers,
                secret = secret,
                createdBy = createdBy,
            )

        val entity = webhook.toEntity()
        val saved = webhookConfigRepository.save(entity)

        logger.info("Created webhook {} for user {}", saved.id, createdBy)
        return saved.toDomain()
    }

    override suspend fun getWebhook(id: WebhookId): WebhookConfig? {
        return webhookConfigRepository.findById(id.value)?.toDomain()
    }

    override suspend fun listWebhooks(userId: String): List<WebhookConfig> {
        return webhookConfigRepository.findByCreatedBy(userId)
            .map { it.toDomain() }
            .toList()
    }

    override suspend fun updateWebhook(
        id: WebhookId,
        name: String?,
        url: String?,
        events: Set<WebhookEvent>?,
        headers: Map<String, String>?,
        secret: String?,
        isActive: Boolean?,
    ): WebhookConfig {
        val existing =
            webhookConfigRepository.findById(id.value)
                ?: throw WebhookNotFoundException("Webhook not found: $id")

        val updated =
            existing.toDomain().update(
                name = name,
                url = url,
                events = events,
                headers = headers,
                secret = secret,
                isActive = isActive,
            )

        val saved = webhookConfigRepository.save(updated.toEntity())
        logger.info("Updated webhook {}", id)
        return saved.toDomain()
    }

    override suspend fun deleteWebhook(id: WebhookId): Boolean {
        val exists = webhookConfigRepository.existsById(id.value)
        if (!exists) return false

        webhookDeliveryRepository.deleteByWebhookConfigId(id.value)
        webhookConfigRepository.deleteById(id.value)
        logger.info("Deleted webhook {}", id)
        return true
    }

    override suspend fun triggerEvent(
        event: WebhookEvent,
        payload: Any,
    ) {
        logger.info("Triggering webhooks for event {}", event)

        val webhooks =
            webhookConfigRepository.findActiveByEvent(event.name)
                .map { it.toDomain() }
                .toList()

        if (webhooks.isEmpty()) {
            logger.debug("No webhooks configured for event {}", event)
            return
        }

        val payloadJson = objectMapper.writeValueAsString(payload)

        webhooks.forEach { webhook ->
            val delivery =
                WebhookDelivery.create(
                    webhookConfigId = webhook.id,
                    eventType = event,
                    payload = formatPayload(webhook.webhookType, event, payloadJson),
                )

            webhookDeliveryRepository.save(delivery.toEntity())
            logger.debug("Created delivery {} for webhook {}", delivery.id, webhook.id)
        }
    }

    override suspend fun processDeliveries() {
        val now = Instant.now()

        // Process pending deliveries
        val pending = webhookDeliveryRepository.findPendingDeliveries(BATCH_SIZE).toList()
        pending.forEach { entity ->
            processDelivery(entity.toDomain())
        }

        // Process retries
        val retries = webhookDeliveryRepository.findDeliveriesForRetry(now, BATCH_SIZE).toList()
        retries.forEach { entity ->
            processDelivery(entity.toDomain())
        }
    }

    @CircuitBreaker(name = "webhook", fallbackMethod = "deliveryFallback")
    private suspend fun processDelivery(delivery: WebhookDelivery) {
        val webhook = webhookConfigRepository.findById(delivery.webhookConfigId.value)?.toDomain()

        if (webhook == null) {
            logger.warn("Webhook config {} not found for delivery {}", delivery.webhookConfigId, delivery.id)
            return
        }

        try {
            val response = sendWebhook(webhook, delivery.payload)
            val updatedDelivery = delivery.markSuccess(response.statusCode, response.body)
            webhookDeliveryRepository.save(updatedDelivery.toEntity())
            logger.info("Webhook delivery {} succeeded with status {}", delivery.id, response.statusCode)
        } catch (e: Exception) {
            logger.error("Webhook delivery {} failed: {}", delivery.id, e.message)
            val updatedDelivery = delivery.markFailed(errorMessage = e.message)
            webhookDeliveryRepository.save(updatedDelivery.toEntity())
        }
    }

    @Suppress("UnusedParameter", "UnusedPrivateMember")
    private suspend fun deliveryFallback(
        delivery: WebhookDelivery,
        ex: Exception,
    ) {
        logger.warn("Circuit breaker open for webhook delivery {}", delivery.id)
        val updatedDelivery = delivery.markFailed(errorMessage = "Circuit breaker open: ${ex.message}")
        webhookDeliveryRepository.save(updatedDelivery.toEntity())
    }

    private suspend fun sendWebhook(
        webhook: WebhookConfig,
        payload: String,
    ): WebhookResponse {
        val requestBuilder =
            webClient.post()
                .uri(webhook.url)
                .contentType(
                    if (webhook.webhookType == WebhookType.SLACK) {
                        MediaType.APPLICATION_JSON
                    } else {
                        MediaType.APPLICATION_JSON
                    },
                )

        // Add custom headers
        webhook.headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        // Add signature if secret is configured
        webhook.secret?.let { secret ->
            val signature = signPayload(payload, secret)
            requestBuilder.header("X-Webhook-Signature", signature)
        }

        return requestBuilder
            .bodyValue(payload)
            .awaitExchange { response ->
                val status = response.statusCode().value()
                WebhookResponse(status, null)
            }
    }

    private fun signPayload(
        payload: String,
        secret: String,
    ): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret.toByteArray(), algorithm))
        val hash = mac.doFinal(payload.toByteArray())
        return "sha256=" + hash.joinToString("") { "%02x".format(it) }
    }

    private fun formatPayload(
        webhookType: WebhookType,
        event: WebhookEvent,
        payloadJson: String,
    ): String {
        return when (webhookType) {
            WebhookType.SLACK -> formatSlackPayload(event, payloadJson)
            else -> wrapPayload(event, payloadJson)
        }
    }

    private fun formatSlackPayload(
        event: WebhookEvent,
        payloadJson: String,
    ): String {
        val eventEmoji =
            when (event) {
                WebhookEvent.RUN_COMPLETED -> ":white_check_mark:"
                WebhookEvent.RUN_FAILED -> ":x:"
                WebhookEvent.COVERAGE_THRESHOLD_BREACH -> ":warning:"
            }

        val eventTitle =
            when (event) {
                WebhookEvent.RUN_COMPLETED -> "Test Run Completed"
                WebhookEvent.RUN_FAILED -> "Test Run Failed"
                WebhookEvent.COVERAGE_THRESHOLD_BREACH -> "Coverage Threshold Breach"
            }

        return """
            {
                "blocks": [
                    {
                        "type": "header",
                        "text": {
                            "type": "plain_text",
                            "text": "$eventEmoji $eventTitle",
                            "emoji": true
                        }
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "```$payloadJson```"
                        }
                    }
                ]
            }
            """.trimIndent()
    }

    private fun wrapPayload(
        event: WebhookEvent,
        payloadJson: String,
    ): String {
        return """
            {
                "event": "${event.name}",
                "timestamp": "${Instant.now()}",
                "data": $payloadJson
            }
            """.trimIndent()
    }

    override suspend fun getRecentDeliveries(
        webhookId: WebhookId,
        limit: Int,
    ): List<WebhookDelivery> {
        return webhookDeliveryRepository.findRecentByWebhookConfigId(webhookId.value, limit)
            .map { it.toDomain() }
            .toList()
    }

    private fun WebhookConfig.toEntity(): WebhookConfigEntity {
        return WebhookConfigEntity(
            id = id.value,
            name = name,
            url = url,
            webhookType = webhookType.name,
            events = events.joinToString(",") { it.name },
            headersJson = if (headers.isNotEmpty()) objectMapper.writeValueAsString(headers) else null,
            secret = secret,
            isActive = isActive,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun WebhookConfigEntity.toDomain(): WebhookConfig {
        return WebhookConfig(
            id = WebhookId(id!!),
            name = name,
            url = url,
            webhookType = WebhookType.valueOf(webhookType),
            events = events.split(",").map { WebhookEvent.valueOf(it.trim()) }.toSet(),
            headers =
                headersJson?.let {
                    @Suppress("UNCHECKED_CAST")
                    objectMapper.readValue(it, Map::class.java) as Map<String, String>
                } ?: emptyMap(),
            secret = secret,
            isActive = isActive,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun WebhookDelivery.toEntity(): WebhookDeliveryEntity {
        return WebhookDeliveryEntity(
            id = id.value,
            webhookConfigId = webhookConfigId.value,
            eventType = eventType.name,
            payload = payload,
            status = status.name,
            attemptCount = attemptCount,
            lastAttemptAt = lastAttemptAt,
            nextRetryAt = nextRetryAt,
            responseStatus = responseStatus,
            responseBody = responseBody,
            errorMessage = errorMessage,
            createdAt = createdAt,
            completedAt = completedAt,
        )
    }

    private fun WebhookDeliveryEntity.toDomain(): WebhookDelivery {
        return WebhookDelivery(
            id = WebhookDeliveryId(id!!),
            webhookConfigId = WebhookId(webhookConfigId),
            eventType = WebhookEvent.valueOf(eventType),
            payload = payload,
            status = WebhookDeliveryStatus.valueOf(status),
            attemptCount = attemptCount,
            lastAttemptAt = lastAttemptAt,
            nextRetryAt = nextRetryAt,
            responseStatus = responseStatus,
            responseBody = responseBody,
            errorMessage = errorMessage,
            createdAt = createdAt,
            completedAt = completedAt,
        )
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}

/**
 * Webhook response data.
 */
data class WebhookResponse(
    val statusCode: Int,
    val body: String?,
)

/**
 * Exception thrown when a webhook is not found.
 */
class WebhookNotFoundException(message: String) : RuntimeException(message)

/**
 * Payload for test run events.
 */
data class TestRunWebhookPayload(
    val runId: String,
    val scenarioId: String,
    val status: String,
    val passedSteps: Int,
    val failedSteps: Int,
    val passRate: Double,
    val durationMs: Long?,
    val triggeredBy: String,
    val baseUrl: String,
) {
    companion object {
        fun from(run: TestRun): TestRunWebhookPayload {
            return TestRunWebhookPayload(
                runId = run.id.toString(),
                scenarioId = run.scenarioId.toString(),
                status = run.status.name,
                passedSteps = run.passedSteps,
                failedSteps = run.failedSteps,
                passRate = run.passRate,
                durationMs = run.durationMs,
                triggeredBy = run.triggeredBy,
                baseUrl = run.baseUrl,
            )
        }
    }
}
