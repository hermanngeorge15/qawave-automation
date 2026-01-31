package com.qawave.presentation.controller

import com.qawave.application.service.WebhookNotFoundException
import com.qawave.application.service.WebhookService
import com.qawave.domain.model.WebhookEvent
import com.qawave.domain.model.WebhookId
import com.qawave.domain.model.WebhookType
import com.qawave.infrastructure.security.Roles
import com.qawave.infrastructure.security.SecurityUtils
import com.qawave.presentation.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * REST controller for webhook management.
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Manage webhook notifications for test run events")
class WebhookController(
    private val webhookService: WebhookService,
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping
    @PreAuthorize(Roles.CAN_CREATE)
    @Operation(
        summary = "Create a webhook",
        description = "Creates a new webhook configuration. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Webhook created"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun createWebhook(
        @Valid @RequestBody request: CreateWebhookRequest,
    ): ResponseEntity<WebhookConfigResponse> {
        val userId = SecurityUtils.getCurrentUserId() ?: "anonymous"
        logger.info("Creating webhook {} for user {}", request.name, userId)

        val webhook =
            webhookService.createWebhook(
                name = request.name,
                url = request.url,
                webhookType = WebhookType.valueOf(request.webhookType),
                events = request.events.map { WebhookEvent.valueOf(it) }.toSet(),
                headers = request.headers ?: emptyMap(),
                secret = request.secret,
                createdBy = userId,
            )

        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookConfigResponse.from(webhook))
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "List webhooks",
        description = "Lists all webhooks for the authenticated user.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of webhooks"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun listWebhooks(): ResponseEntity<List<WebhookConfigResponse>> {
        val userId = SecurityUtils.getCurrentUserId() ?: return ResponseEntity.ok(emptyList())
        val webhooks = webhookService.listWebhooks(userId)
        return ResponseEntity.ok(webhooks.map { WebhookConfigResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Get a webhook",
        description = "Gets a webhook configuration by ID.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Webhook found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Webhook not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun getWebhook(
        @Parameter(description = "Webhook ID") @PathVariable id: UUID,
    ): ResponseEntity<WebhookConfigResponse> {
        val webhook =
            webhookService.getWebhook(WebhookId(id))
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(WebhookConfigResponse.from(webhook))
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Roles.CAN_UPDATE)
    @Operation(
        summary = "Update a webhook",
        description = "Updates a webhook configuration. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Webhook updated"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Webhook not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun updateWebhook(
        @Parameter(description = "Webhook ID") @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateWebhookRequest,
    ): ResponseEntity<WebhookConfigResponse> {
        return try {
            val webhook =
                webhookService.updateWebhook(
                    id = WebhookId(id),
                    name = request.name,
                    url = request.url,
                    events = request.events?.map { WebhookEvent.valueOf(it) }?.toSet(),
                    headers = request.headers,
                    secret = request.secret,
                    isActive = request.isActive,
                )
            ResponseEntity.ok(WebhookConfigResponse.from(webhook))
        } catch (e: WebhookNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_DELETE)
    @Operation(
        summary = "Delete a webhook",
        description = "Deletes a webhook configuration and all delivery history. Requires ADMIN role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Webhook deleted"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Webhook not found"),
    )
    suspend fun deleteWebhook(
        @Parameter(description = "Webhook ID") @PathVariable id: UUID,
    ): ResponseEntity<Unit> {
        val deleted = webhookService.deleteWebhook(WebhookId(id))
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Get webhook deliveries",
        description = "Lists recent delivery attempts for a webhook.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of deliveries"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun getDeliveries(
        @Parameter(description = "Webhook ID") @PathVariable id: UUID,
    ): ResponseEntity<List<WebhookDeliveryResponse>> {
        val deliveries = webhookService.getRecentDeliveries(WebhookId(id))
        return ResponseEntity.ok(deliveries.map { WebhookDeliveryResponse.from(it) })
    }

    @PostMapping("/{id}/test")
    @PreAuthorize(Roles.CAN_CREATE)
    @Operation(
        summary = "Test a webhook",
        description = "Sends a test event to the webhook URL. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Test event queued"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Webhook not found"),
    )
    suspend fun testWebhook(
        @Parameter(description = "Webhook ID") @PathVariable id: UUID,
    ): ResponseEntity<TestWebhookResponse> {
        webhookService.getWebhook(WebhookId(id))
            ?: return ResponseEntity.notFound().build()

        webhookService.triggerEvent(
            event = WebhookEvent.RUN_COMPLETED,
            payload =
                TestWebhookPayload(
                    message = "This is a test webhook event from QAWave",
                    webhookId = id.toString(),
                    timestamp = Instant.now().toString(),
                ),
        )

        return ResponseEntity.accepted().body(TestWebhookResponse(message = "Test event queued"))
    }
}

/**
 * Request for creating a webhook.
 */
data class CreateWebhookRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,
    @field:NotBlank(message = "URL is required")
    @field:Size(max = 2048, message = "URL must be at most 2048 characters")
    val url: String,
    @field:NotBlank(message = "Webhook type is required")
    val webhookType: String,
    @field:NotEmpty(message = "At least one event is required")
    val events: List<String>,
    val headers: Map<String, String>? = null,
    val secret: String? = null,
)

/**
 * Request for updating a webhook.
 */
data class UpdateWebhookRequest(
    val name: String? = null,
    val url: String? = null,
    val events: List<String>? = null,
    val headers: Map<String, String>? = null,
    val secret: String? = null,
    val isActive: Boolean? = null,
)

/**
 * Response for webhook configuration.
 */
data class WebhookConfigResponse(
    val id: String,
    val name: String,
    val url: String,
    val webhookType: String,
    val events: List<String>,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(webhook: com.qawave.domain.model.WebhookConfig): WebhookConfigResponse {
            return WebhookConfigResponse(
                id = webhook.id.toString(),
                name = webhook.name,
                url = webhook.url,
                webhookType = webhook.webhookType.name,
                events = webhook.events.map { it.name },
                isActive = webhook.isActive,
                createdBy = webhook.createdBy,
                createdAt = webhook.createdAt,
                updatedAt = webhook.updatedAt,
            )
        }
    }
}

/**
 * Response for webhook delivery.
 */
data class WebhookDeliveryResponse(
    val id: String,
    val eventType: String,
    val status: String,
    val attemptCount: Int,
    val lastAttemptAt: Instant?,
    val responseStatus: Int?,
    val errorMessage: String?,
    val createdAt: Instant,
    val completedAt: Instant?,
) {
    companion object {
        fun from(delivery: com.qawave.domain.model.WebhookDelivery): WebhookDeliveryResponse {
            return WebhookDeliveryResponse(
                id = delivery.id.toString(),
                eventType = delivery.eventType.name,
                status = delivery.status.name,
                attemptCount = delivery.attemptCount,
                lastAttemptAt = delivery.lastAttemptAt,
                responseStatus = delivery.responseStatus,
                errorMessage = delivery.errorMessage,
                createdAt = delivery.createdAt,
                completedAt = delivery.completedAt,
            )
        }
    }
}

/**
 * Test webhook payload.
 */
data class TestWebhookPayload(
    val message: String,
    val webhookId: String,
    val timestamp: String,
)

/**
 * Response for test webhook.
 */
data class TestWebhookResponse(
    val message: String,
)
