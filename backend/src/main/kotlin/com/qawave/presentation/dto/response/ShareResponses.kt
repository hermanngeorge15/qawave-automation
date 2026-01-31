package com.qawave.presentation.dto.response

import com.qawave.domain.model.ShareToken
import java.time.Instant

/**
 * Response for a share token.
 */
data class ShareTokenResponse(
    val id: String,
    val runId: String,
    val token: String,
    val shareUrl: String,
    val expiresAt: Instant,
    val viewCount: Int,
    val createdBy: String,
    val createdAt: Instant,
    val isActive: Boolean,
) {
    companion object {
        fun from(shareToken: ShareToken, baseShareUrl: String = "/api/shared"): ShareTokenResponse {
            return ShareTokenResponse(
                id = shareToken.id.toString(),
                runId = shareToken.runId.toString(),
                token = shareToken.token,
                shareUrl = "$baseShareUrl/${shareToken.token}",
                expiresAt = shareToken.expiresAt,
                viewCount = shareToken.viewCount,
                createdBy = shareToken.createdBy,
                createdAt = shareToken.createdAt,
                isActive = shareToken.isValid(),
            )
        }
    }
}

/**
 * Response for accessing a shared run.
 */
data class SharedRunResponse(
    val run: TestRunResponse,
    val shareInfo: ShareInfoResponse,
)

/**
 * Share info included in shared run response.
 */
data class ShareInfoResponse(
    val token: String,
    val expiresAt: Instant,
    val viewCount: Int,
)

/**
 * Simplified test run response for shared access.
 */
data class TestRunResponse(
    val id: String,
    val scenarioId: String,
    val status: String,
    val startedAt: Instant,
    val completedAt: Instant?,
    val durationMs: Long?,
    val passedSteps: Int,
    val failedSteps: Int,
    val executedSteps: Int,
    val passRate: Double,
    val stepResults: List<StepResultResponse>,
)

/**
 * Simplified step result for shared access.
 */
data class StepResultResponse(
    val stepIndex: Int,
    val stepName: String,
    val passed: Boolean,
    val durationMs: Long,
    val errorMessage: String?,
    val executedAt: Instant,
)
