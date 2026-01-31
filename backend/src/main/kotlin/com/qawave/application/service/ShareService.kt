package com.qawave.application.service

import com.qawave.domain.model.ShareToken
import com.qawave.domain.model.ShareTokenId
import com.qawave.domain.model.TestRun
import com.qawave.domain.model.TestRunId
import com.qawave.infrastructure.persistence.entity.ShareTokenEntity
import com.qawave.infrastructure.persistence.repository.ShareTokenR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for managing shareable run links.
 */
interface ShareService {
    /**
     * Create a shareable link for a test run.
     */
    suspend fun createShare(
        runId: TestRunId,
        createdBy: String,
        expirationDays: Long = 7,
    ): ShareToken

    /**
     * Get shared run data by token.
     * Returns null if token is invalid, expired, or revoked.
     */
    suspend fun getSharedRun(token: String): SharedRunResult?

    /**
     * Revoke a share token.
     */
    suspend fun revokeShare(
        tokenId: ShareTokenId,
        userId: String,
    ): Boolean

    /**
     * Get all active shares for a run.
     */
    suspend fun getActiveSharesForRun(runId: TestRunId): List<ShareToken>

    /**
     * Get all shares created by a user.
     */
    fun getSharesByUser(userId: String): Flow<ShareToken>

    /**
     * Count active shares for a run.
     */
    suspend fun countActiveSharesForRun(runId: TestRunId): Long
}

/**
 * Result of fetching a shared run.
 */
data class SharedRunResult(
    val run: TestRun,
    val shareToken: ShareToken,
)

@Service
class ShareServiceImpl(
    private val shareTokenRepository: ShareTokenR2dbcRepository,
    private val testExecutionService: TestExecutionService,
) : ShareService {
    private val logger = LoggerFactory.getLogger(ShareServiceImpl::class.java)

    override suspend fun createShare(
        runId: TestRunId,
        createdBy: String,
        expirationDays: Long,
    ): ShareToken {
        // Verify run exists
        testExecutionService.findById(runId)
            ?: throw ShareNotFoundException("Test run not found: $runId")

        // Create the share token
        val shareToken =
            ShareToken.create(
                runId = runId,
                createdBy = createdBy,
                expirationDays = expirationDays,
            )

        // Save to database
        val entity = shareToken.toEntity()
        val saved = shareTokenRepository.save(entity)

        logger.info("Created share token for run {} by user {}", runId, createdBy)

        return saved.toDomain()
    }

    override suspend fun getSharedRun(token: String): SharedRunResult? {
        val entity = shareTokenRepository.findByToken(token) ?: return null
        val shareToken = entity.toDomain()

        // Check if token is valid
        if (!shareToken.isValid()) {
            logger.debug("Share token {} is invalid (expired or revoked)", token)
            return null
        }

        // Get the run
        val run =
            testExecutionService.findByIdWithResults(shareToken.runId)
                ?: return null

        // Increment view count
        val updatedToken = shareToken.incrementViewCount()
        shareTokenRepository.save(updatedToken.toEntity())

        return SharedRunResult(run = run, shareToken = updatedToken)
    }

    override suspend fun revokeShare(
        tokenId: ShareTokenId,
        userId: String,
    ): Boolean {
        val entity = shareTokenRepository.findById(tokenId.value) ?: return false

        // Verify ownership
        if (entity.createdBy != userId) {
            logger.warn("User {} attempted to revoke share token {} owned by {}", userId, tokenId, entity.createdBy)
            return false
        }

        // Revoke the token
        val revokedEntity = entity.copy(revokedAt = java.time.Instant.now())
        shareTokenRepository.save(revokedEntity)

        logger.info("Share token {} revoked by user {}", tokenId, userId)
        return true
    }

    override suspend fun getActiveSharesForRun(runId: TestRunId): List<ShareToken> {
        return shareTokenRepository.findActiveByRunId(runId.value)
            .map { it.toDomain() }
            .toList()
    }

    override fun getSharesByUser(userId: String): Flow<ShareToken> {
        return shareTokenRepository.findByCreatedBy(userId)
            .map { it.toDomain() }
    }

    override suspend fun countActiveSharesForRun(runId: TestRunId): Long {
        return shareTokenRepository.countActiveByRunId(runId.value)
    }

    private fun ShareToken.toEntity(): ShareTokenEntity {
        return ShareTokenEntity(
            id = id.value,
            runId = runId.value,
            token = token,
            expiresAt = expiresAt,
            viewCount = viewCount,
            createdBy = createdBy,
            createdAt = createdAt,
            revokedAt = revokedAt,
        )
    }

    private fun ShareTokenEntity.toDomain(): ShareToken {
        return ShareToken(
            id = ShareTokenId(id!!),
            runId = TestRunId(runId),
            token = token,
            expiresAt = expiresAt,
            viewCount = viewCount,
            createdBy = createdBy,
            createdAt = createdAt,
            revokedAt = revokedAt,
        )
    }
}

/**
 * Exception thrown when a share resource is not found.
 */
class ShareNotFoundException(message: String) : RuntimeException(message)
