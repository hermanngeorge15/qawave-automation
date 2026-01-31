package com.qawave.domain.model

import java.security.SecureRandom
import java.time.Instant

/**
 * Domain model for a share token that enables public access to test run results.
 */
data class ShareToken(
    val id: ShareTokenId,
    val runId: TestRunId,
    val token: String,
    val expiresAt: Instant,
    val viewCount: Int = 0,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
    val revokedAt: Instant? = null,
) {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val secureRandom = SecureRandom()
        private const val TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        /**
         * Generate a secure random token.
         */
        fun generateToken(): String {
            return (1..TOKEN_LENGTH)
                .map { TOKEN_CHARS[secureRandom.nextInt(TOKEN_CHARS.length)] }
                .joinToString("")
        }

        /**
         * Create a new share token with default expiration (7 days).
         */
        fun create(
            runId: TestRunId,
            createdBy: String,
            expirationDays: Long = 7,
        ): ShareToken {
            return ShareToken(
                id = ShareTokenId.generate(),
                runId = runId,
                token = generateToken(),
                expiresAt = Instant.now().plusSeconds(expirationDays * 24 * 60 * 60),
                createdBy = createdBy,
            )
        }
    }

    /**
     * Check if this token is still valid (not expired and not revoked).
     */
    fun isValid(): Boolean = revokedAt == null && expiresAt.isAfter(Instant.now())

    /**
     * Check if this token has been revoked.
     */
    fun isRevoked(): Boolean = revokedAt != null

    /**
     * Check if this token has expired.
     */
    fun isExpired(): Boolean = expiresAt.isBefore(Instant.now())

    /**
     * Create a copy with incremented view count.
     */
    fun incrementViewCount(): ShareToken = copy(viewCount = viewCount + 1)

    /**
     * Create a copy marked as revoked.
     */
    fun revoke(): ShareToken = copy(revokedAt = Instant.now())
}
