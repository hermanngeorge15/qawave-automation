package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for Share Token.
 * Maps to the share_tokens table in PostgreSQL.
 */
@Table("share_tokens")
data class ShareTokenEntity(
    @Id
    val id: UUID? = null,
    @Column("run_id")
    val runId: UUID,
    @Column("token")
    val token: String,
    @Column("expires_at")
    val expiresAt: Instant,
    @Column("view_count")
    val viewCount: Int = 0,
    @Column("created_by")
    val createdBy: String,
    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @Column("revoked_at")
    val revokedAt: Instant? = null,
)
