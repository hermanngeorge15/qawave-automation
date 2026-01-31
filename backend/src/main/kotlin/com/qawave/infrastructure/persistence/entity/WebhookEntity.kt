package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for Webhook Configuration.
 */
@Table("webhook_configs")
data class WebhookConfigEntity(
    @Id
    val id: UUID? = null,
    @Column("name")
    val name: String,
    @Column("url")
    val url: String,
    @Column("webhook_type")
    val webhookType: String,
    @Column("events")
    val events: String,
    @Column("headers_json")
    val headersJson: String? = null,
    @Column("secret")
    val secret: String? = null,
    @Column("is_active")
    val isActive: Boolean = true,
    @Column("created_by")
    val createdBy: String,
    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),
)

/**
 * R2DBC entity for Webhook Delivery.
 */
@Table("webhook_deliveries")
data class WebhookDeliveryEntity(
    @Id
    val id: UUID? = null,
    @Column("webhook_config_id")
    val webhookConfigId: UUID,
    @Column("event_type")
    val eventType: String,
    @Column("payload")
    val payload: String,
    @Column("status")
    val status: String,
    @Column("attempt_count")
    val attemptCount: Int = 0,
    @Column("last_attempt_at")
    val lastAttemptAt: Instant? = null,
    @Column("next_retry_at")
    val nextRetryAt: Instant? = null,
    @Column("response_status")
    val responseStatus: Int? = null,
    @Column("response_body")
    val responseBody: String? = null,
    @Column("error_message")
    val errorMessage: String? = null,
    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @Column("completed_at")
    val completedAt: Instant? = null,
)
