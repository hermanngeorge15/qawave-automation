package com.qawave.presentation.dto.response

import com.qawave.domain.model.*
import java.time.Instant

/**
 * Response DTO for a QA package.
 */
data class QaPackageResponse(
    val id: String,
    val name: String,
    val description: String?,
    val specUrl: String?,
    val specHash: String?,
    val baseUrl: String,
    val requirements: String?,
    val status: String,
    val config: QaPackageConfigResponse,
    val coverage: CoverageReportResponse?,
    val qaSummary: QaSummaryResponse?,
    val scenarioCount: Int,
    val triggeredBy: String,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val durationMs: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(qaPackage: QaPackage): QaPackageResponse {
            return QaPackageResponse(
                id = qaPackage.id.toString(),
                name = qaPackage.name,
                description = qaPackage.description,
                specUrl = qaPackage.specUrl,
                specHash = qaPackage.specHash,
                baseUrl = qaPackage.baseUrl,
                requirements = qaPackage.requirements,
                status = qaPackage.status.name,
                config = QaPackageConfigResponse.from(qaPackage.config),
                coverage = qaPackage.coverage?.let { CoverageReportResponse.from(it) },
                qaSummary = qaPackage.qaSummary?.let { QaSummaryResponse.from(it) },
                scenarioCount = qaPackage.scenarioCount,
                triggeredBy = qaPackage.triggeredBy,
                startedAt = qaPackage.startedAt,
                completedAt = qaPackage.completedAt,
                durationMs = qaPackage.durationMs,
                createdAt = qaPackage.createdAt,
                updatedAt = qaPackage.updatedAt
            )
        }
    }
}

/**
 * Response DTO for QA package configuration.
 */
data class QaPackageConfigResponse(
    val maxScenarios: Int,
    val maxStepsPerScenario: Int,
    val timeoutMs: Long,
    val parallelExecution: Boolean,
    val stopOnFirstFailure: Boolean,
    val includeSecurityTests: Boolean,
    val aiProvider: String,
    val aiModel: String
) {
    companion object {
        fun from(config: QaPackageConfig): QaPackageConfigResponse {
            return QaPackageConfigResponse(
                maxScenarios = config.maxScenarios,
                maxStepsPerScenario = config.maxStepsPerScenario,
                timeoutMs = config.timeoutMs,
                parallelExecution = config.parallelExecution,
                stopOnFirstFailure = config.stopOnFirstFailure,
                includeSecurityTests = config.includeSecurityTests,
                aiProvider = config.aiProvider,
                aiModel = config.aiModel
            )
        }
    }
}

/**
 * Response DTO for coverage report.
 */
data class CoverageReportResponse(
    val totalOperations: Int,
    val coveredOperations: Int,
    val coveragePercentage: Double,
    val uncoveredOperations: Int,
    val generatedAt: Instant
) {
    companion object {
        fun from(coverage: CoverageReport): CoverageReportResponse {
            return CoverageReportResponse(
                totalOperations = coverage.totalOperations,
                coveredOperations = coverage.coveredOperations,
                coveragePercentage = coverage.coveragePercentage,
                uncoveredOperations = coverage.uncoveredOperations,
                generatedAt = coverage.generatedAt
            )
        }
    }
}

/**
 * Response DTO for QA summary.
 */
data class QaSummaryResponse(
    val overallVerdict: String,
    val summary: String,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val erroredScenarios: Int,
    val qualityScore: Int?,
    val stabilityScore: Int?,
    val generatedAt: Instant
) {
    companion object {
        fun from(summary: QaSummary): QaSummaryResponse {
            return QaSummaryResponse(
                overallVerdict = summary.overallVerdict.name,
                summary = summary.summary,
                passedScenarios = summary.passedScenarios,
                failedScenarios = summary.failedScenarios,
                erroredScenarios = summary.erroredScenarios,
                qualityScore = summary.riskAssessment?.qualityScore,
                stabilityScore = summary.riskAssessment?.stabilityScore,
                generatedAt = summary.generatedAt
            )
        }
    }
}

/**
 * Response DTO for paginated results.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Standard error response.
 */
data class ErrorResponse(
    val message: String,
    val code: String? = null,
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now()
)
