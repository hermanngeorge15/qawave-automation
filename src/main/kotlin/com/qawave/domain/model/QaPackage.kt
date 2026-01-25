package com.qawave.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Type-safe wrapper for QA Package IDs.
 */
@JvmInline
value class QaPackageId(val value: UUID) {
    companion object {
        fun generate(): QaPackageId = QaPackageId(UUID.randomUUID())
        fun from(string: String): QaPackageId = QaPackageId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Status of a QA package through its lifecycle.
 */
enum class QaPackageStatus {
    REQUESTED,
    SPEC_FETCHED,
    AI_SUCCESS,
    EXECUTION_IN_PROGRESS,
    EXECUTION_COMPLETE,
    QA_EVAL_IN_PROGRESS,
    QA_EVAL_DONE,
    COMPLETE,
    FAILED_SPEC_FETCH,
    FAILED_GENERATION,
    FAILED_EXECUTION,
    CANCELLED
}

/**
 * Core domain model for a QA test package.
 */
data class QaPackage(
    val id: QaPackageId,
    val name: String,
    val description: String? = null,
    val specUrl: String? = null,
    val specContent: String? = null,
    val specHash: String? = null,
    val baseUrl: String,
    val requirements: String? = null,
    val status: QaPackageStatus = QaPackageStatus.REQUESTED,
    val config: QaPackageConfig = QaPackageConfig(),
    val coverage: CoverageReport? = null,
    val qaSummary: QaSummary? = null,
    val triggeredBy: String,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val scenarioCount: Int get() = 0 // Would be loaded from scenarios
    val durationMs: Long? get() = if (startedAt != null && completedAt != null) {
        completedAt.toEpochMilli() - startedAt.toEpochMilli()
    } else null

    val isComplete: Boolean get() = status == QaPackageStatus.COMPLETE
    val isFailed: Boolean get() = status.name.startsWith("FAILED_") || status == QaPackageStatus.CANCELLED
    val isInProgress: Boolean get() = !isComplete && !isFailed
}

/**
 * Configuration for a QA package.
 */
data class QaPackageConfig(
    val maxScenarios: Int = 10,
    val maxStepsPerScenario: Int = 10,
    val timeoutMs: Long = 300_000,
    val parallelExecution: Boolean = true,
    val stopOnFirstFailure: Boolean = false,
    val includeSecurityTests: Boolean = false,
    val aiProvider: String = "openai",
    val aiModel: String = "gpt-4o-mini"
)

/**
 * Coverage report for a QA package.
 */
data class CoverageReport(
    val totalOperations: Int,
    val coveredOperations: Int,
    val coveragePercentage: Double,
    val uncoveredOperations: Int,
    val generatedAt: Instant = Instant.now()
)

/**
 * Overall verdict for QA summary.
 */
enum class QaVerdict {
    PASS,
    FAIL,
    PARTIAL,
    ERROR
}

/**
 * Risk assessment data.
 */
data class RiskAssessment(
    val qualityScore: Int,
    val stabilityScore: Int,
    val securityScore: Int? = null,
    val recommendations: List<String> = emptyList()
)

/**
 * QA summary for a completed package.
 */
data class QaSummary(
    val overallVerdict: QaVerdict,
    val summary: String,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val erroredScenarios: Int,
    val riskAssessment: RiskAssessment? = null,
    val generatedAt: Instant = Instant.now()
)
