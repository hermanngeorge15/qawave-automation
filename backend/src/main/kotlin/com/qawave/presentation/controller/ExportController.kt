package com.qawave.presentation.controller

import com.qawave.application.service.ExportNotFoundException
import com.qawave.application.service.ExportService
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.TestRunId
import com.qawave.infrastructure.security.Roles
import com.qawave.presentation.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * REST controller for exporting test run and coverage data.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Export", description = "Export test runs and coverage data in various formats")
class ExportController(
    private val exportService: ExportService,
) {
    private val logger = LoggerFactory.getLogger(ExportController::class.java)

    @GetMapping("/runs/{id}/export")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Export test run results",
        description = "Exports a test run's results in JSON or CSV format. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export successful"),
        ApiResponse(responseCode = "400", description = "Invalid format parameter"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Run not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun exportRun(
        @Parameter(description = "Run ID") @PathVariable id: UUID,
        @Parameter(description = "Export format (json or csv)") @RequestParam(defaultValue = "json") format: String,
    ): ResponseEntity<String> {
        logger.info("Exporting run {} as {}", id, format)

        val runId = TestRunId(id)

        return try {
            when (format.lowercase()) {
                "json" -> {
                    val content = exportService.exportRunAsJson(runId)
                    buildJsonResponse(content, "run-$id")
                }
                "csv" -> {
                    val content = exportService.exportRunAsCsv(runId)
                    buildCsvResponse(content, "run-$id")
                }
                else -> {
                    ResponseEntity.badRequest().body("""{"error": "Invalid format. Use 'json' or 'csv'."}""")
                }
            }
        } catch (e: ExportNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/coverage/{packageId}/export")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Export coverage report",
        description = "Exports a QA package's coverage report in JSON or CSV format. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export successful"),
        ApiResponse(responseCode = "400", description = "Invalid format parameter"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Package or coverage not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun exportCoverage(
        @Parameter(description = "QA Package ID") @PathVariable packageId: UUID,
        @Parameter(description = "Export format (json or csv)") @RequestParam(defaultValue = "json") format: String,
    ): ResponseEntity<String> {
        logger.info("Exporting coverage for package {} as {}", packageId, format)

        val pkgId = QaPackageId(packageId)

        return try {
            when (format.lowercase()) {
                "json" -> {
                    val content = exportService.exportCoverageAsJson(pkgId)
                    buildJsonResponse(content, "coverage-$packageId")
                }
                "csv" -> {
                    val content = exportService.exportCoverageAsCsv(pkgId)
                    buildCsvResponse(content, "coverage-$packageId")
                }
                else -> {
                    ResponseEntity.badRequest().body("""{"error": "Invalid format. Use 'json' or 'csv'."}""")
                }
            }
        } catch (e: ExportNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    private fun buildJsonResponse(content: String, filenamePrefix: String): ResponseEntity<String> {
        val timestamp =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(
                Instant.now().atZone(java.time.ZoneOffset.UTC),
            )
        val filename = "$filenamePrefix-$timestamp.json"

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(content)
    }

    private fun buildCsvResponse(content: String, filenamePrefix: String): ResponseEntity<String> {
        val timestamp =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(
                Instant.now().atZone(java.time.ZoneOffset.UTC),
            )
        val filename = "$filenamePrefix-$timestamp.csv"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(content)
    }
}
