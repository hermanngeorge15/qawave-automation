package com.qawave.presentation.controller

import com.qawave.application.service.ShareNotFoundException
import com.qawave.application.service.ShareService
import com.qawave.domain.model.ShareTokenId
import com.qawave.domain.model.TestRunId
import com.qawave.infrastructure.security.Roles
import com.qawave.infrastructure.security.SecurityUtils
import com.qawave.presentation.dto.request.CreateShareRequest
import com.qawave.presentation.dto.response.ErrorResponse
import com.qawave.presentation.dto.response.ShareInfoResponse
import com.qawave.presentation.dto.response.ShareTokenResponse
import com.qawave.presentation.dto.response.SharedRunResponse
import com.qawave.presentation.dto.response.StepResultResponse
import com.qawave.presentation.dto.response.TestRunResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST controller for shareable run links.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Share", description = "Manage shareable links for test runs")
class ShareController(
    private val shareService: ShareService,
) {
    private val logger = LoggerFactory.getLogger(ShareController::class.java)

    @PostMapping("/runs/{id}/share")
    @PreAuthorize(Roles.CAN_CREATE)
    @Operation(
        summary = "Create a shareable link",
        description = "Generates a unique shareable link for a test run. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Share link created"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Run not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun createShare(
        @Parameter(description = "Run ID") @PathVariable id: UUID,
        @Valid @RequestBody(required = false) request: CreateShareRequest?,
    ): ResponseEntity<ShareTokenResponse> {
        val userId = SecurityUtils.getCurrentUserId() ?: "anonymous"
        logger.info("Creating share link for run {} by user {}", id, userId)

        return try {
            val shareToken =
                shareService.createShare(
                    runId = TestRunId(id),
                    createdBy = userId,
                    expirationDays = request?.expirationDays ?: 7,
                )
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ShareTokenResponse.from(shareToken))
        } catch (e: ShareNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/runs/{id}/shares")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "List active shares for a run",
        description = "Lists all active share tokens for a test run. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of active shares"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun listSharesForRun(
        @Parameter(description = "Run ID") @PathVariable id: UUID,
    ): ResponseEntity<List<ShareTokenResponse>> {
        val shares = shareService.getActiveSharesForRun(TestRunId(id))
        return ResponseEntity.ok(shares.map { ShareTokenResponse.from(it) })
    }

    @DeleteMapping("/runs/{runId}/shares/{tokenId}")
    @PreAuthorize(Roles.CAN_CREATE)
    @Operation(
        summary = "Revoke a share token",
        description = "Revokes a share token, making the link invalid. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Share revoked"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions or not owner"),
        ApiResponse(responseCode = "404", description = "Share token not found"),
    )
    suspend fun revokeShare(
        @Parameter(description = "Run ID") @PathVariable runId: UUID,
        @Parameter(description = "Share Token ID") @PathVariable tokenId: UUID,
    ): ResponseEntity<Unit> {
        val userId = SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val revoked = shareService.revokeShare(ShareTokenId(tokenId), userId)
        return if (revoked) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/shares")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "List shares created by current user",
        description = "Lists all share tokens created by the authenticated user.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of user's shares"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun listUserShares(): ResponseEntity<List<ShareTokenResponse>> {
        val userId = SecurityUtils.getCurrentUserId() ?: return ResponseEntity.ok(emptyList())
        val shares = shareService.getSharesByUser(userId).map { ShareTokenResponse.from(it) }.toList()
        return ResponseEntity.ok(shares)
    }

    @GetMapping("/shared/{token}")
    @Operation(
        summary = "Access shared run data",
        description = "Retrieves test run data via a shared token. No authentication required.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Shared run data"),
        ApiResponse(
            responseCode = "404",
            description = "Invalid or expired token",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun getSharedRun(
        @Parameter(description = "Share token") @PathVariable token: String,
    ): ResponseEntity<SharedRunResponse> {
        logger.debug("Accessing shared run with token: {}", token.take(8) + "...")

        val result = shareService.getSharedRun(token) ?: return ResponseEntity.notFound().build()

        val runResponse =
            TestRunResponse(
                id = result.run.id.toString(),
                scenarioId = result.run.scenarioId.toString(),
                status = result.run.status.name,
                startedAt = result.run.startedAt,
                completedAt = result.run.completedAt,
                durationMs = result.run.durationMs,
                passedSteps = result.run.passedSteps,
                failedSteps = result.run.failedSteps,
                executedSteps = result.run.executedSteps,
                passRate = result.run.passRate,
                stepResults =
                    result.run.stepResults.map { step ->
                        StepResultResponse(
                            stepIndex = step.stepIndex,
                            stepName = step.stepName,
                            passed = step.passed,
                            durationMs = step.durationMs,
                            errorMessage = step.errorMessage,
                            executedAt = step.executedAt,
                        )
                    },
            )

        val shareInfo =
            ShareInfoResponse(
                token = result.shareToken.token,
                expiresAt = result.shareToken.expiresAt,
                viewCount = result.shareToken.viewCount,
            )

        return ResponseEntity.ok(SharedRunResponse(run = runResponse, shareInfo = shareInfo))
    }
}
