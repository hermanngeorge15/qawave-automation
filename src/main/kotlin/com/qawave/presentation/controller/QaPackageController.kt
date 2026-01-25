package com.qawave.presentation.controller

import com.qawave.application.service.*
import com.qawave.domain.model.QaPackageConfig
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.QaPackageStatus
import com.qawave.presentation.dto.request.CreateQaPackageRequest
import com.qawave.presentation.dto.request.UpdateQaPackageRequest
import com.qawave.presentation.dto.response.ErrorResponse
import com.qawave.presentation.dto.response.PageResponse
import com.qawave.presentation.dto.response.QaPackageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST controller for QA Package operations.
 * All endpoints use suspend functions for non-blocking execution.
 */
@RestController
@RequestMapping("/api/qa/packages")
@Tag(name = "QA Packages", description = "Operations for managing QA test packages")
class QaPackageController(
    private val qaPackageService: QaPackageService
) {

    private val logger = LoggerFactory.getLogger(QaPackageController::class.java)

    @PostMapping
    @Operation(summary = "Create a new QA package", description = "Creates a new QA package with the given configuration")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Package created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    suspend fun createPackage(
        @Valid @RequestBody request: CreateQaPackageRequest,
        @RequestHeader("X-User-Id", required = false) userId: String?
    ): ResponseEntity<QaPackageResponse> {
        logger.info("Creating QA package: name={}", request.name)

        val command = CreateQaPackageCommand(
            name = request.name,
            description = request.description,
            specUrl = request.specUrl,
            specContent = request.specContent,
            baseUrl = request.baseUrl,
            requirements = request.requirements,
            triggeredBy = userId ?: "anonymous",
            config = request.config?.let {
                QaPackageConfig(
                    maxScenarios = it.maxScenarios,
                    maxStepsPerScenario = it.maxStepsPerScenario,
                    timeoutMs = it.timeoutMs,
                    parallelExecution = it.parallelExecution,
                    stopOnFirstFailure = it.stopOnFirstFailure,
                    includeSecurityTests = it.includeSecurityTests,
                    aiProvider = it.aiProvider,
                    aiModel = it.aiModel
                )
            } ?: QaPackageConfig()
        )

        val created = qaPackageService.create(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(QaPackageResponse.from(created))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a QA package by ID", description = "Retrieves a QA package by its unique identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Package found"),
        ApiResponse(responseCode = "404", description = "Package not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    suspend fun getPackage(
        @Parameter(description = "Package ID") @PathVariable id: UUID
    ): ResponseEntity<QaPackageResponse> {
        val qaPackage = qaPackageService.findById(QaPackageId(id))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(QaPackageResponse.from(qaPackage))
    }

    @GetMapping
    @Operation(summary = "List all QA packages", description = "Retrieves a paginated list of all QA packages")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Packages retrieved successfully")
    )
    suspend fun listPackages(
        @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: String?
    ): ResponseEntity<PageResponse<QaPackageResponse>> {
        val result = if (status != null) {
            val statusEnum = QaPackageStatus.valueOf(status)
            val packages = qaPackageService.findByStatus(statusEnum)
            Page(
                content = packages,
                page = 0,
                size = packages.size,
                totalElements = packages.size.toLong(),
                totalPages = 1
            )
        } else {
            qaPackageService.findAll(page, size)
        }

        val response = PageResponse(
            content = result.content.map { QaPackageResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext,
            hasPrevious = result.hasPrevious
        )

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a QA package", description = "Updates an existing QA package")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Package updated successfully"),
        ApiResponse(responseCode = "404", description = "Package not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    suspend fun updatePackage(
        @Parameter(description = "Package ID") @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateQaPackageRequest
    ): ResponseEntity<QaPackageResponse> {
        val command = UpdateQaPackageCommand(
            name = request.name,
            description = request.description,
            specUrl = request.specUrl,
            specContent = request.specContent,
            baseUrl = request.baseUrl,
            requirements = request.requirements,
            config = request.config?.let {
                QaPackageConfig(
                    maxScenarios = it.maxScenarios,
                    maxStepsPerScenario = it.maxStepsPerScenario,
                    timeoutMs = it.timeoutMs,
                    parallelExecution = it.parallelExecution,
                    stopOnFirstFailure = it.stopOnFirstFailure,
                    includeSecurityTests = it.includeSecurityTests,
                    aiProvider = it.aiProvider,
                    aiModel = it.aiModel
                )
            }
        )

        return try {
            val updated = qaPackageService.update(QaPackageId(id), command)
            ResponseEntity.ok(QaPackageResponse.from(updated))
        } catch (e: PackageNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update package status", description = "Updates the status of a QA package")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid status transition", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "Package not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    suspend fun updateStatus(
        @Parameter(description = "Package ID") @PathVariable id: UUID,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<QaPackageResponse> {
        return try {
            val status = QaPackageStatus.valueOf(request.status)
            val updated = qaPackageService.updateStatus(QaPackageId(id), status)
            ResponseEntity.ok(QaPackageResponse.from(updated))
        } catch (e: PackageNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: InvalidStatusTransitionException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a QA package", description = "Deletes a QA package and all related data")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Package deleted successfully"),
        ApiResponse(responseCode = "404", description = "Package not found")
    )
    suspend fun deletePackage(
        @Parameter(description = "Package ID") @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        val deleted = qaPackageService.delete(QaPackageId(id))
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/count")
    @Operation(summary = "Get package count", description = "Returns the total number of packages")
    suspend fun getCount(
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: String?
    ): ResponseEntity<CountResponse> {
        val count = if (status != null) {
            qaPackageService.countByStatus(QaPackageStatus.valueOf(status))
        } else {
            qaPackageService.count()
        }
        return ResponseEntity.ok(CountResponse(count))
    }
}

/**
 * Request for updating package status.
 */
data class UpdateStatusRequest(
    val status: String
)

/**
 * Response for count operations.
 */
data class CountResponse(
    val count: Long
)
