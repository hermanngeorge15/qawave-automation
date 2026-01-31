package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.ShareTokenEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * R2DBC repository for Share Tokens.
 * Uses Kotlin coroutines for non-blocking database access.
 */
@Repository
interface ShareTokenR2dbcRepository : CoroutineCrudRepository<ShareTokenEntity, UUID> {
    /**
     * Find a share token by its token string.
     */
    suspend fun findByToken(token: String): ShareTokenEntity?

    /**
     * Find all share tokens for a run.
     */
    fun findByRunId(runId: UUID): Flow<ShareTokenEntity>

    /**
     * Find all active (non-expired, non-revoked) share tokens for a run.
     */
    @Query(
        """
        SELECT * FROM share_tokens
        WHERE run_id = :runId
        AND revoked_at IS NULL
        AND expires_at > NOW()
        ORDER BY created_at DESC
    """,
    )
    fun findActiveByRunId(runId: UUID): Flow<ShareTokenEntity>

    /**
     * Find all share tokens created by a user.
     */
    fun findByCreatedBy(createdBy: String): Flow<ShareTokenEntity>

    /**
     * Count active share tokens for a run.
     */
    @Query(
        """
        SELECT COUNT(*) FROM share_tokens
        WHERE run_id = :runId
        AND revoked_at IS NULL
        AND expires_at > NOW()
    """,
    )
    suspend fun countActiveByRunId(runId: UUID): Long

    /**
     * Delete all share tokens for a run.
     */
    suspend fun deleteByRunId(runId: UUID)

    /**
     * Check if a token exists and is valid.
     */
    @Query(
        """
        SELECT COUNT(*) > 0 FROM share_tokens
        WHERE token = :token
        AND revoked_at IS NULL
        AND expires_at > NOW()
    """,
    )
    suspend fun isTokenValid(token: String): Boolean
}
