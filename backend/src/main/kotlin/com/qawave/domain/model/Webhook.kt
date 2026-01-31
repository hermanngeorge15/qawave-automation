package com.qawave.domain.model

import java.time.Instant

/**
 * Types of webhooks supported.
 */
enum class WebhookType {
    SLACK,
    GENERIC,
    EMAIL,
}

/**
 * Events that can trigger webhook notifications.
 */
enum class WebhookEvent {
    RUN_COMPLETED,
    RUN_FAILED,
    COVERAGE_THRESHOLD_BREACH,
}

/**
 * Status of webhook delivery.
 */
enum class WebhookDeliveryStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRYING,
}

/**
 * Webhook configuration domain model.
 */
data class WebhookConfig(
    val id: WebhookId,
    val name: String,
    val url: String,
    val webhookType: WebhookType,
    val events: Set<WebhookEvent>,
    val headers: Map<String, String> = emptyMap(),
    val secret: String? = null,
    val isActive: Boolean = true,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    companion object {
        fun create(
            name: String,
            url: String,
            webhookType: WebhookType,
            events: Set<WebhookEvent>,
            headers: Map<String, String> = emptyMap(),
            secret: String? = null,
            createdBy: String,
        ): WebhookConfig {
            return WebhookConfig(
                id = WebhookId.generate(),
                name = name,
                url = url,
                webhookType = webhookType,
                events = events,
                headers = headers,
                secret = secret,
                createdBy = createdBy,
            )
        }
    }

    /**
     * Check if this webhook should be triggered for the given event.
     */
    fun shouldTriggerFor(event: WebhookEvent): Boolean = isActive && events.contains(event)

    /**
     * Create a copy with updated fields.
     */
    fun update(
        name: String? = null,
        url: String? = null,
        events: Set<WebhookEvent>? = null,
        headers: Map<String, String>? = null,
        secret: String? = null,
        isActive: Boolean? = null,
    ): WebhookConfig {
        return copy(
            name = name ?: this.name,
            url = url ?: this.url,
            events = events ?: this.events,
            headers = headers ?: this.headers,
            secret = secret ?: this.secret,
            isActive = isActive ?: this.isActive,
            updatedAt = Instant.now(),
        )
    }

    /**
     * Deactivate the webhook.
     */
    fun deactivate(): WebhookConfig = copy(isActive = false, updatedAt = Instant.now())

    /**
     * Activate the webhook.
     */
    fun activate(): WebhookConfig = copy(isActive = true, updatedAt = Instant.now())
}

/**
 * Webhook delivery record for tracking delivery attempts.
 */
data class WebhookDelivery(
    val id: WebhookDeliveryId,
    val webhookConfigId: WebhookId,
    val eventType: WebhookEvent,
    val payload: String,
    val status: WebhookDeliveryStatus,
    val attemptCount: Int = 0,
    val lastAttemptAt: Instant? = null,
    val nextRetryAt: Instant? = null,
    val responseStatus: Int? = null,
    val responseBody: String? = null,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
) {
    companion object {
        private const val MAX_RETRIES = 3

        fun create(
            webhookConfigId: WebhookId,
            eventType: WebhookEvent,
            payload: String,
        ): WebhookDelivery {
            return WebhookDelivery(
                id = WebhookDeliveryId.generate(),
                webhookConfigId = webhookConfigId,
                eventType = eventType,
                payload = payload,
                status = WebhookDeliveryStatus.PENDING,
            )
        }

        /**
         * Calculate next retry delay with exponential backoff.
         * Base delay of 30 seconds, doubling each attempt.
         */
        fun calculateNextRetryDelay(attemptCount: Int): Long {
            val baseDelaySeconds = 30L
            return baseDelaySeconds * (1L shl attemptCount.coerceAtMost(5))
        }
    }

    /**
     * Mark delivery as successful.
     */
    fun markSuccess(responseStatus: Int, responseBody: String?): WebhookDelivery {
        return copy(
            status = WebhookDeliveryStatus.SUCCESS,
            attemptCount = attemptCount + 1,
            lastAttemptAt = Instant.now(),
            responseStatus = responseStatus,
            responseBody = responseBody?.take(1000),
            completedAt = Instant.now(),
        )
    }

    /**
     * Mark delivery as failed, scheduling retry if attempts remain.
     */
    fun markFailed(
        responseStatus: Int? = null,
        errorMessage: String?,
    ): WebhookDelivery {
        val newAttemptCount = attemptCount + 1
        val shouldRetry = newAttemptCount < MAX_RETRIES

        return copy(
            status = if (shouldRetry) WebhookDeliveryStatus.RETRYING else WebhookDeliveryStatus.FAILED,
            attemptCount = newAttemptCount,
            lastAttemptAt = Instant.now(),
            nextRetryAt =
                if (shouldRetry) {
                    Instant.now().plusSeconds(calculateNextRetryDelay(newAttemptCount))
                } else {
                    null
                },
            responseStatus = responseStatus,
            errorMessage = errorMessage,
            completedAt = if (!shouldRetry) Instant.now() else null,
        )
    }

    /**
     * Check if this delivery has more retry attempts remaining.
     */
    fun hasRetriesRemaining(): Boolean = attemptCount < MAX_RETRIES

    /**
     * Check if this delivery is ready for retry.
     */
    fun isReadyForRetry(): Boolean {
        return status == WebhookDeliveryStatus.RETRYING &&
            nextRetryAt != null &&
            nextRetryAt.isBefore(Instant.now())
    }
}
