package com.qawave.presentation.controller

import com.qawave.application.service.CreateScenarioCommand
import com.qawave.application.service.ScenarioNotFoundException
import com.qawave.application.service.ScenarioService
import com.qawave.application.service.ScenarioValidationService
import com.qawave.application.service.UpdateScenarioCommand
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.ScenarioId
import com.qawave.domain.model.ScenarioSource
import com.qawave.domain.model.ScenarioStatus
import com.qawave.domain.model.TestSuiteId
import com.qawave.infrastructure.security.Roles
import com.qawave.presentation.dto.request.CreateScenarioRequest
import com.qawave.presentation.dto.request.UpdateScenarioRequest
import com.qawave.presentation.dto.response.ErrorResponse
import com.qawave.presentation.dto.response.PageResponse
import com.qawave.presentation.dto.response.ScenarioResponse
import com.qawave.presentation.dto.response.ScenarioSummaryResponse
import com.qawave.presentation.dto.response.ValidationResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST controller for Test Scenario operations.
 * Provides CRUD operations and validation endpoints for test scenarios.
 */
@RestController
@RequestMapping("/api/scenarios")
@Tag(name = "Test Scenarios", description = "Operations for managing test scenarios")
class ScenarioController(
    private val scenarioService: ScenarioService,
    private val validationService: ScenarioValidationService,
) {
    private val logger = LoggerFactory.getLogger(ScenarioController::class.java)

    @PostMapping
    @PreAuthorize(Roles.CAN_CREATE)
    @Operation(
        summary = "Create a new scenario",
        description = "Creates a new test scenario with the given configuration. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Scenario created successfully"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request or validation failed",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun createScenario(
        @Valid @RequestBody request: CreateScenarioRequest,
    ): ResponseEntity<ScenarioResponse> {
        logger.info("Creating scenario: name={}", request.name)

        val command =
            CreateScenarioCommand(
                qaPackageId = request.qaPackageId?.let { QaPackageId(UUID.fromString(it)) },
                suiteId = request.suiteId?.let { TestSuiteId(UUID.fromString(it)) },
                name = request.name,
                description = request.description,
                steps = request.steps.map { it.toDomain() },
                tags = request.tags,
                source = request.source,
            )

        val created = scenarioService.create(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(ScenarioResponse.from(created))
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Get a scenario by ID",
        description = "Retrieves a test scenario by its unique identifier. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Scenario found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Scenario not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun getScenario(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
    ): ResponseEntity<ScenarioResponse> {
        val scenario =
            scenarioService.findById(ScenarioId(id))
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ScenarioResponse.from(scenario))
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "List all scenarios",
        description = "Retrieves a paginated list of scenarios with optional filters. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Scenarios retrieved successfully"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
    )
    suspend fun listScenarios(
        @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Filter by source") @RequestParam(required = false) source: String?,
        @Parameter(description = "Filter by tag") @RequestParam(required = false) tag: String?,
        @Parameter(description = "Filter by QA package ID") @RequestParam(required = false) packageId: UUID?,
    ): ResponseEntity<PageResponse<ScenarioSummaryResponse>> {
        val result =
            when {
                packageId != null -> {
                    val scenarios = scenarioService.findByPackageId(QaPackageId(packageId))
                    com.qawave.application.service.Page(
                        content = scenarios,
                        page = 0,
                        size = scenarios.size,
                        totalElements = scenarios.size.toLong(),
                        totalPages = 1,
                    )
                }
                status != null -> {
                    val statusEnum = ScenarioStatus.valueOf(status)
                    val scenarios = scenarioService.findByStatus(statusEnum)
                    com.qawave.application.service.Page(
                        content = scenarios,
                        page = 0,
                        size = scenarios.size,
                        totalElements = scenarios.size.toLong(),
                        totalPages = 1,
                    )
                }
                source != null -> {
                    val sourceEnum = ScenarioSource.valueOf(source)
                    val scenarios = scenarioService.findBySource(sourceEnum)
                    com.qawave.application.service.Page(
                        content = scenarios,
                        page = 0,
                        size = scenarios.size,
                        totalElements = scenarios.size.toLong(),
                        totalPages = 1,
                    )
                }
                tag != null -> {
                    val scenarios = scenarioService.findByTag(tag).toList()
                    com.qawave.application.service.Page(
                        content = scenarios,
                        page = 0,
                        size = scenarios.size,
                        totalElements = scenarios.size.toLong(),
                        totalPages = 1,
                    )
                }
                else -> scenarioService.findAll(page, size)
            }

        val response =
            PageResponse(
                content = result.content.map { ScenarioSummaryResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext,
                hasPrevious = result.hasPrevious,
            )

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_UPDATE)
    @Operation(
        summary = "Update a scenario",
        description = """
            Updates an existing test scenario. All provided fields will be validated.
            Returns the updated scenario with validation status.
            Requires ADMIN or TESTER role.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Scenario updated successfully"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request or validation failed",
            content = [Content(schema = Schema(implementation = ValidationResultResponse::class))],
        ),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Scenario not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun updateScenario(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateScenarioRequest,
    ): ResponseEntity<Any> {
        logger.info("Updating scenario: id={}", id)

        val scenarioId = ScenarioId(id)

        // First check if scenario exists
        val existingScenario =
            scenarioService.findById(scenarioId)
                ?: return ResponseEntity.notFound().build()

        // If steps are provided, validate them first
        if (request.steps != null) {
            val steps = request.steps.map { it.toDomain() }
            val validationResult = validationService.validateSteps(steps)

            if (!validationResult.valid) {
                logger.warn("Scenario update validation failed: id={}, errors={}", id, validationResult.errors.size)
                return ResponseEntity.badRequest().body(validationResult)
            }
        }

        val command =
            UpdateScenarioCommand(
                name = request.name,
                description = request.description,
                steps = request.steps?.map { it.toDomain() },
                tags = request.tags,
                status = request.status,
            )

        return try {
            val updated = scenarioService.update(scenarioId, command)
            ResponseEntity.ok(ScenarioResponse.from(updated))
        } catch (e: ScenarioNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                ErrorResponse(
                    message = e.message ?: "Invalid request",
                    code = "INVALID_REQUEST",
                ),
            )
        }
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Validate a scenario",
        description = """
            Performs dry-run validation on the provided scenario update.
            Does not persist any changes. Returns detailed validation errors and warnings.
            Useful for preview/validation before saving.
            Requires any authenticated role.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Validation completed (may contain errors)"),
        ApiResponse(
            responseCode = "404",
            description = "Scenario not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun validateScenario(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateScenarioRequest,
    ): ResponseEntity<ValidationResultResponse> {
        logger.debug("Validating scenario update: id={}", id)

        val scenarioId = ScenarioId(id)

        // Check if scenario exists
        val existingScenario =
            scenarioService.findById(scenarioId)
                ?: return ResponseEntity.notFound().build()

        // Create a merged scenario for validation
        val stepsToValidate = request.steps?.map { it.toDomain() } ?: existingScenario.steps

        // Perform validation
        val validationResult = validationService.validateSteps(stepsToValidate)

        return ResponseEntity.ok(validationResult)
    }

    @PostMapping("/validate")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Validate scenario steps",
        description = """
            Validates the provided scenario steps without requiring an existing scenario.
            Useful for validating new scenarios before creation.
            Returns detailed validation errors and warnings.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Validation completed (may contain errors)"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request format",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun validateSteps(
        @Valid @RequestBody request: CreateScenarioRequest,
    ): ResponseEntity<ValidationResultResponse> {
        logger.debug("Validating scenario steps: name={}, stepCount={}", request.name, request.steps.size)

        val steps = request.steps.map { it.toDomain() }
        val validationResult = validationService.validateSteps(steps)

        return ResponseEntity.ok(validationResult)
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.CAN_UPDATE)
    @Operation(
        summary = "Update scenario status",
        description = "Updates the status of a test scenario. Requires ADMIN or TESTER role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status updated successfully"),
        ApiResponse(
            responseCode = "400",
            description = "Invalid status",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Scenario not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun updateStatus(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
        @RequestBody request: UpdateScenarioStatusRequest,
    ): ResponseEntity<ScenarioResponse> {
        return try {
            val status = ScenarioStatus.valueOf(request.status)
            val updated = scenarioService.updateStatus(ScenarioId(id), status)
            ResponseEntity.ok(ScenarioResponse.from(updated))
        } catch (e: ScenarioNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_DELETE)
    @Operation(
        summary = "Delete a scenario",
        description = "Deletes a test scenario. Requires ADMIN role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Scenario deleted successfully"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Scenario not found"),
    )
    suspend fun deleteScenario(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
    ): ResponseEntity<Unit> {
        val deleted = scenarioService.delete(ScenarioId(id))
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/count")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Get scenario count",
        description = "Returns the total number of scenarios. Requires any authenticated role.",
    )
    suspend fun getCount(
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Filter by QA package ID") @RequestParam(required = false) packageId: UUID?,
    ): ResponseEntity<CountResponse> {
        val count =
            when {
                packageId != null -> scenarioService.countByPackageId(QaPackageId(packageId))
                status != null -> scenarioService.countByStatus(ScenarioStatus.valueOf(status))
                else -> scenarioService.count()
            }
        return ResponseEntity.ok(CountResponse(count))
    }
}

/**
 * Request for updating scenario status.
 */
data class UpdateScenarioStatusRequest(
    val status: String,
)
