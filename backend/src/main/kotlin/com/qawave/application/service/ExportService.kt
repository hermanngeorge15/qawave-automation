package com.qawave.application.service

import com.qawave.domain.model.CoverageReport
import com.qawave.domain.model.OperationCoverage
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.TestRun
import com.qawave.domain.model.TestRunId
import com.qawave.domain.model.TestStepResult
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * Service for exporting test runs and coverage data in various formats.
 */
interface ExportService {
    /**
     * Export a test run as JSON.
     */
    suspend fun exportRunAsJson(runId: TestRunId): String

    /**
     * Export a test run as CSV.
     */
    suspend fun exportRunAsCsv(runId: TestRunId): String

    /**
     * Export coverage report as JSON.
     */
    suspend fun exportCoverageAsJson(packageId: QaPackageId): String

    /**
     * Export coverage report as CSV.
     */
    suspend fun exportCoverageAsCsv(packageId: QaPackageId): String
}

@Service
class ExportServiceImpl(
    private val testExecutionService: TestExecutionService,
    private val qaPackageService: QaPackageService,
) : ExportService {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    override suspend fun exportRunAsJson(runId: TestRunId): String {
        val run =
            testExecutionService.findById(runId)
                ?: throw ExportNotFoundException("Test run not found: $runId")

        return buildJsonExport(run)
    }

    override suspend fun exportRunAsCsv(runId: TestRunId): String {
        val run =
            testExecutionService.findById(runId)
                ?: throw ExportNotFoundException("Test run not found: $runId")

        return buildCsvExport(run)
    }

    override suspend fun exportCoverageAsJson(packageId: QaPackageId): String {
        val qaPackage =
            qaPackageService.findById(packageId)
                ?: throw ExportNotFoundException("QA Package not found: $packageId")

        val coverage =
            qaPackage.coverage
                ?: throw ExportNotFoundException("No coverage data available for package: $packageId")

        return buildCoverageJsonExport(coverage)
    }

    override suspend fun exportCoverageAsCsv(packageId: QaPackageId): String {
        val qaPackage =
            qaPackageService.findById(packageId)
                ?: throw ExportNotFoundException("QA Package not found: $packageId")

        val coverage =
            qaPackage.coverage
                ?: throw ExportNotFoundException("No coverage data available for package: $packageId")

        return buildCoverageCsvExport(coverage)
    }

    private fun buildJsonExport(run: TestRun): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "runId": "${run.id}",""")
        sb.appendLine("""  "scenarioId": "${run.scenarioId}",""")
        run.qaPackageId?.let { sb.appendLine("""  "qaPackageId": "$it",""") }
        sb.appendLine("""  "triggeredBy": "${escapeJson(run.triggeredBy)}",""")
        sb.appendLine("""  "baseUrl": "${escapeJson(run.baseUrl)}",""")
        sb.appendLine("""  "status": "${run.status}",""")
        sb.appendLine("""  "startedAt": "${DATE_FORMATTER.format(run.startedAt)}",""")
        run.completedAt?.let { sb.appendLine("""  "completedAt": "${DATE_FORMATTER.format(it)}",""") }
        run.durationMs?.let { sb.appendLine("""  "durationMs": $it,""") }
        sb.appendLine("""  "passedSteps": ${run.passedSteps},""")
        sb.appendLine("""  "failedSteps": ${run.failedSteps},""")
        sb.appendLine("""  "executedSteps": ${run.executedSteps},""")
        sb.appendLine("""  "passRate": ${run.passRate},""")
        sb.appendLine("""  "stepResults": [""")

        run.stepResults.forEachIndexed { index, result ->
            sb.append(buildStepResultJson(result))
            if (index < run.stepResults.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    private fun buildStepResultJson(result: TestStepResult): String {
        val sb = StringBuilder()
        sb.appendLine("    {")
        sb.appendLine("""      "stepIndex": ${result.stepIndex},""")
        sb.appendLine("""      "stepName": "${escapeJson(result.stepName)}",""")
        result.actualStatus?.let { sb.appendLine("""      "actualStatus": $it,""") }
        sb.appendLine("""      "passed": ${result.passed},""")
        result.errorMessage?.let { sb.appendLine("""      "errorMessage": "${escapeJson(it)}",""") }
        sb.appendLine("""      "durationMs": ${result.durationMs},""")
        sb.appendLine("""      "executedAt": "${DATE_FORMATTER.format(result.executedAt)}",""")
        sb.appendLine("""      "assertions": [""")

        result.assertions.forEachIndexed { index, assertion ->
            sb.append("        {")
            sb.append(""""type": "${assertion.type}"""")
            assertion.field?.let { sb.append(""", "field": "${escapeJson(it)}"""") }
            assertion.expected?.let { sb.append(""", "expected": "${escapeJson(it)}"""") }
            assertion.actual?.let { sb.append(""", "actual": "${escapeJson(it)}"""") }
            sb.append(""", "passed": ${assertion.passed}""")
            assertion.message?.let { sb.append(""", "message": "${escapeJson(it)}"""") }
            sb.append("}")
            if (index < result.assertions.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("      ]")
        sb.append("    }")
        return sb.toString()
    }

    private fun buildCsvExport(run: TestRun): String {
        val sb = StringBuilder()

        // Header row
        sb.appendLine(
            "Run ID,Scenario ID,Step Index,Step Name,Status,Passed,Duration (ms)," +
                "Actual Status,Error Message,Executed At,Assertion Type,Assertion Field," +
                "Expected,Actual,Assertion Passed,Assertion Message",
        )

        // Data rows - one per assertion, or one per step if no assertions
        run.stepResults.forEach { result ->
            if (result.assertions.isEmpty()) {
                sb.appendLine(buildCsvRow(run, result, null))
            } else {
                result.assertions.forEach { assertion ->
                    sb.appendLine(buildCsvRow(run, result, assertion))
                }
            }
        }

        return sb.toString()
    }

    private fun buildCsvRow(
        run: TestRun,
        result: TestStepResult,
        assertion: com.qawave.domain.model.AssertionResult?,
    ): String {
        return listOf(
            run.id.toString(),
            run.scenarioId.toString(),
            result.stepIndex.toString(),
            escapeCsv(result.stepName),
            run.status.name,
            result.passed.toString(),
            result.durationMs.toString(),
            result.actualStatus?.toString() ?: "",
            escapeCsv(result.errorMessage ?: ""),
            DATE_FORMATTER.format(result.executedAt),
            assertion?.type?.name ?: "",
            escapeCsv(assertion?.field ?: ""),
            escapeCsv(assertion?.expected ?: ""),
            escapeCsv(assertion?.actual ?: ""),
            assertion?.passed?.toString() ?: "",
            escapeCsv(assertion?.message ?: ""),
        ).joinToString(",")
    }

    private fun buildCoverageJsonExport(coverage: CoverageReport): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "totalOperations": ${coverage.totalOperations},""")
        sb.appendLine("""  "coveredOperations": ${coverage.coveredOperations},""")
        sb.appendLine("""  "uncoveredOperations": ${coverage.uncoveredOperations},""")
        sb.appendLine("""  "coveragePercentage": ${coverage.coveragePercentage},""")
        sb.appendLine("""  "generatedAt": "${DATE_FORMATTER.format(coverage.generatedAt)}",""")
        sb.appendLine("""  "operationDetails": [""")

        coverage.operationDetails.forEachIndexed { index, op ->
            sb.append(buildOperationCoverageJson(op))
            if (index < coverage.operationDetails.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("  ],")
        sb.appendLine("""  "gaps": [""")

        coverage.gaps.forEachIndexed { index, gap ->
            sb.append("    {")
            sb.append(""""type": "${gap.type}"""")
            gap.operationId?.let { sb.append(""", "operationId": "${escapeJson(it)}"""") }
            sb.append(""", "description": "${escapeJson(gap.description)}"""")
            sb.append(""", "severity": "${gap.severity}"""")
            sb.append("}")
            if (index < coverage.gaps.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    private fun buildOperationCoverageJson(op: OperationCoverage): String {
        val sb = StringBuilder()
        sb.append("    {")
        sb.append(""""operationId": "${escapeJson(op.operationId)}"""")
        sb.append(""", "method": "${op.method}"""")
        sb.append(""", "path": "${escapeJson(op.path)}"""")
        sb.append(""", "status": "${op.status}"""")
        sb.append(""", "scenarioIds": [${op.scenarioIds.joinToString(", ") { "\"$it\"" }}]""")
        op.lastTestedAt?.let { sb.append(""", "lastTestedAt": "${DATE_FORMATTER.format(it)}"""") }
        sb.append("}")
        return sb.toString()
    }

    private fun buildCoverageCsvExport(coverage: CoverageReport): String {
        val sb = StringBuilder()

        // Summary section
        sb.appendLine("Coverage Summary")
        sb.appendLine("Total Operations,Covered,Uncovered,Coverage %,Generated At")
        sb.appendLine(
            "${coverage.totalOperations},${coverage.coveredOperations}," +
                "${coverage.uncoveredOperations},${coverage.coveragePercentage}," +
                "${DATE_FORMATTER.format(coverage.generatedAt)}",
        )
        sb.appendLine()

        // Operation details
        sb.appendLine("Operation Details")
        sb.appendLine("Operation ID,Method,Path,Status,Scenario Count,Last Tested At")
        coverage.operationDetails.forEach { op ->
            sb.appendLine(
                "${escapeCsv(op.operationId)},${op.method},${escapeCsv(op.path)}," +
                    "${op.status},${op.scenarioIds.size}," +
                    "${op.lastTestedAt?.let { DATE_FORMATTER.format(it) } ?: ""}",
            )
        }
        sb.appendLine()

        // Coverage gaps
        sb.appendLine("Coverage Gaps")
        sb.appendLine("Type,Operation ID,Description,Severity")
        coverage.gaps.forEach { gap ->
            sb.appendLine(
                "${gap.type},${escapeCsv(gap.operationId ?: "")}," +
                    "${escapeCsv(gap.description)},${gap.severity}",
            )
        }

        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}

/**
 * Exception thrown when export data is not found.
 */
class ExportNotFoundException(message: String) : RuntimeException(message)
