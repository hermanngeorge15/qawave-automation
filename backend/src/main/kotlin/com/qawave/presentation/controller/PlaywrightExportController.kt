package com.qawave.presentation.controller

import com.qawave.application.service.ExportNotFoundException
import com.qawave.application.service.PlaywrightExportService
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.ScenarioId
import com.qawave.domain.model.TestSuiteId
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
 * REST controller for exporting test scenarios as Playwright test code.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Playwright Export", description = "Export test scenarios as runnable Playwright test code")
class PlaywrightExportController(
    private val playwrightExportService: PlaywrightExportService,
) {
    private val logger = LoggerFactory.getLogger(PlaywrightExportController::class.java)

    @GetMapping("/scenarios/{id}/export")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Export scenario as test code",
        description = "Exports a test scenario as Playwright TypeScript test code. Requires any authenticated role.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export successful"),
        ApiResponse(responseCode = "400", description = "Invalid format parameter"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Scenario not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun exportScenario(
        @Parameter(description = "Scenario ID") @PathVariable id: UUID,
        @Parameter(
            description = "Export format (playwright for TypeScript test code)",
        ) @RequestParam(defaultValue = "playwright") format: String,
    ): ResponseEntity<*> {
        logger.info("Exporting scenario {} as {}", id, format)

        return try {
            when (format.lowercase()) {
                "playwright" -> {
                    val result = playwrightExportService.exportScenario(ScenarioId(id))
                    buildTypeScriptResponse(result.testCode, result.filename)
                }
                else -> {
                    ResponseEntity.badRequest()
                        .body("""{"error": "Invalid format. Use 'playwright'."}""")
                }
            }
        } catch (e: ExportNotFoundException) {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/suites/{id}/export")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Export test suite as Playwright project",
        description = """
            Exports all scenarios in a test suite as a complete Playwright project.
            Returns a ZIP file containing:
            - TypeScript test files for each scenario
            - package.json with dependencies
            - playwright.config.ts configuration
            - README.md with setup instructions
            Requires any authenticated role.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export successful"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Suite not found or empty",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun exportSuite(
        @Parameter(description = "Suite ID") @PathVariable id: UUID,
    ): ResponseEntity<*> {
        logger.info("Exporting suite {} as Playwright project", id)

        return try {
            val zipContent = playwrightExportService.exportSuite(TestSuiteId(id))
            buildZipResponse(zipContent, "suite-$id")
        } catch (e: ExportNotFoundException) {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/qa/packages/{id}/export/playwright")
    @PreAuthorize(Roles.CAN_READ)
    @Operation(
        summary = "Export QA package as Playwright project",
        description = """
            Exports all scenarios in a QA package as a complete Playwright project.
            Returns a ZIP file containing:
            - TypeScript test files for each scenario
            - package.json with dependencies
            - playwright.config.ts configuration
            - README.md with setup instructions
            Requires any authenticated role.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Export successful"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(
            responseCode = "404",
            description = "Package not found or no scenarios",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    suspend fun exportPackage(
        @Parameter(description = "Package ID") @PathVariable id: UUID,
    ): ResponseEntity<*> {
        logger.info("Exporting package {} as Playwright project", id)

        return try {
            val zipContent = playwrightExportService.exportPackage(QaPackageId(id))
            buildZipResponse(zipContent, "package-$id")
        } catch (e: ExportNotFoundException) {
            ResponseEntity.notFound().build<Any>()
        }
    }

    private fun buildTypeScriptResponse(
        content: String,
        filename: String,
    ): ResponseEntity<String> {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/typescript"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(content)
    }

    private fun buildZipResponse(
        content: ByteArray,
        filenamePrefix: String,
    ): ResponseEntity<ByteArray> {
        val timestamp =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(
                Instant.now().atZone(java.time.ZoneOffset.UTC),
            )
        val filename = "$filenamePrefix-$timestamp.zip"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(content)
    }
}
