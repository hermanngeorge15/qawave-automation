package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.WebhookConfigEntity
import com.qawave.infrastructure.persistence.entity.WebhookDeliveryEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * R2DBC repository for Webhook Configurations.
 */
@Repository
interface WebhookConfigR2dbcRepository : CoroutineCrudRepository<WebhookConfigEntity, UUID> {
    /**
     * Find all active webhooks.
     */
    fun findByIsActiveTrue(): Flow<WebhookConfigEntity>

    /**
     * Find webhooks created by a user.
     */
    fun findByCreatedBy(createdBy: String): Flow<WebhookConfigEntity>

    /**
     * Find active webhooks that subscribe to a specific event.
     */
    @Query(
        """
        SELECT * FROM webhook_configs
        WHERE is_active = true
        AND events LIKE '%' || :eventType || '%'
    """,
    )
    fun findActiveByEvent(eventType: String): Flow<WebhookConfigEntity>

    /**
     * Count webhooks by creator.
     */
    suspend fun countByCreatedBy(createdBy: String): Long
}

/**
 * R2DBC repository for Webhook Deliveries.
 */
@Repository
interface WebhookDeliveryR2dbcRepository : CoroutineCrudRepository<WebhookDeliveryEntity, UUID> {
    /**
     * Find deliveries for a webhook config.
     */
    fun findByWebhookConfigId(webhookConfigId: UUID): Flow<WebhookDeliveryEntity>

    /**
     * Find deliveries ready for retry.
     */
    @Query(
        """
        SELECT * FROM webhook_deliveries
        WHERE status = 'RETRYING'
        AND next_retry_at <= :now
        ORDER BY next_retry_at ASC
        LIMIT :limit
    """,
    )
    fun findDeliveriesForRetry(
        now: Instant,
        limit: Int,
    ): Flow<WebhookDeliveryEntity>

    /**
     * Find pending deliveries.
     */
    @Query(
        """
        SELECT * FROM webhook_deliveries
        WHERE status = 'PENDING'
        ORDER BY created_at ASC
        LIMIT :limit
    """,
    )
    fun findPendingDeliveries(limit: Int): Flow<WebhookDeliveryEntity>

    /**
     * Find recent deliveries for a webhook.
     */
    @Query(
        """
        SELECT * FROM webhook_deliveries
        WHERE webhook_config_id = :webhookConfigId
        ORDER BY created_at DESC
        LIMIT :limit
    """,
    )
    fun findRecentByWebhookConfigId(
        webhookConfigId: UUID,
        limit: Int,
    ): Flow<WebhookDeliveryEntity>

    /**
     * Count failed deliveries for a webhook.
     */
    @Query(
        """
        SELECT COUNT(*) FROM webhook_deliveries
        WHERE webhook_config_id = :webhookConfigId
        AND status = 'FAILED'
    """,
    )
    suspend fun countFailedByWebhookConfigId(webhookConfigId: UUID): Long

    /**
     * Delete deliveries for a webhook config.
     */
    suspend fun deleteByWebhookConfigId(webhookConfigId: UUID)

    /**
     * Delete old completed deliveries.
     */
    @Query(
        """
        DELETE FROM webhook_deliveries
        WHERE completed_at IS NOT NULL
        AND completed_at < :before
    """,
    )
    suspend fun deleteCompletedBefore(before: Instant): Int
}
