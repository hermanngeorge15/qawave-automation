package com.qawave.domain.model

import java.time.Instant

/**
 * Represents a QA Package - a container for a complete test run with AI evaluation.
 * This is the top-level entity that orchestrates scenario generation, execution,
 * and result evaluation.
 */
data class QaPackage(
    val id: QaPackageId,
    val name: String,
    val description: String?,
    val specUrl: String?,
    val specContent: String?,
    val specHash: String?,
    val baseUrl: String,
    val requirements: String?,
    val status: QaPackageStatus,
    val scenarios: List<TestScenario> = emptyList(),
    val runs: List<TestRun> = emptyList(),
    val coverage: CoverageReport? = null,
    val qaSummary: QaSummary? = null,
    val config: QaPackageConfig = QaPackageConfig(),
    val triggeredBy: String,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Package name cannot be blank" }
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(specUrl != null || specContent != null) { "Either specUrl or specContent must be provided" }
    }

    /**
     * Total duration of the package run in milliseconds.
     */
    val durationMs: Long?
        get() =
            if (startedAt != null && completedAt != null) {
                completedAt.toEpochMilli() - startedAt.toEpochMilli()
            } else {
                null
            }

    /**
     * Whether the package run has completed.
     */
    val isComplete: Boolean
        get() =
            status in
                listOf(
                    QaPackageStatus.COMPLETE,
                    QaPackageStatus.FAILED_SPEC_FETCH,
                    QaPackageStatus.FAILED_GENERATION,
                    QaPackageStatus.FAILED_EXECUTION,
                    QaPackageStatus.CANCELLED,
                )

    /**
     * Total number of scenarios in this package.
     */
    val scenarioCount: Int
        get() = scenarios.size

    /**
     * Total number of test steps across all scenarios.
     */
    val totalSteps: Int
        get() = scenarios.sumOf { it.stepCount }

    /**
     * Number of passed runs.
     */
    val passedRuns: Int
        get() = runs.count { it.status == TestRunStatus.PASSED }

    /**
     * Number of failed runs.
     */
    val failedRuns: Int
        get() = runs.count { it.status == TestRunStatus.FAILED }

    /**
     * Overall pass rate as a percentage (0-100).
     */
    val overallPassRate: Double
        get() = if (runs.isEmpty()) 0.0 else (passedRuns.toDouble() / runs.size) * 100
}

/**
 * Status of a QA Package run.
 * Follows a state machine: REQUESTED -> SPEC_FETCHED -> AI_SUCCESS -> EXECUTION_* -> QA_EVAL_DONE -> COMPLETE
 */
enum class QaPackageStatus {
    /**
     * Package has been created and is waiting to start.
     */
    REQUESTED,

    /**
     * OpenAPI spec has been fetched and parsed.
     */
    SPEC_FETCHED,

    /**
     * AI has successfully generated test scenarios.
     */
    AI_SUCCESS,

    /**
     * Test execution is in progress.
     */
    EXECUTION_IN_PROGRESS,

    /**
     * Test execution has completed.
     */
    EXECUTION_COMPLETE,

    /**
     * QA evaluation (AI review) is in progress.
     */
    QA_EVAL_IN_PROGRESS,

    /**
     * QA evaluation has completed.
     */
    QA_EVAL_DONE,

    /**
     * Package run is fully complete.
     */
    COMPLETE,

    /**
     * Failed to fetch the OpenAPI spec.
     */
    FAILED_SPEC_FETCH,

    /**
     * Failed during scenario generation.
     */
    FAILED_GENERATION,

    /**
     * Failed during test execution.
     */
    FAILED_EXECUTION,

    /**
     * Package run was cancelled.
     */
    CANCELLED,
}

/**
 * Configuration options for a QA Package run.
 */
data class QaPackageConfig(
    val maxScenarios: Int = 10,
    val maxStepsPerScenario: Int = 10,
    val timeoutMs: Long = 300_000,
    val parallelExecution: Boolean = true,
    val stopOnFirstFailure: Boolean = false,
    val includeSecurityTests: Boolean = false,
    val aiProvider: String = "openai",
    val aiModel: String = "gpt-4o-mini",
)

/**
 * Coverage report for a QA Package.
 */
data class CoverageReport(
    val totalOperations: Int,
    val coveredOperations: Int,
    val coveragePercentage: Double,
    val operationDetails: List<OperationCoverage> = emptyList(),
    val gaps: List<CoverageGap> = emptyList(),
    val generatedAt: Instant,
) {
    /**
     * Number of uncovered operations.
     */
    val uncoveredOperations: Int
        get() = totalOperations - coveredOperations
}

/**
 * Coverage details for a single API operation.
 */
data class OperationCoverage(
    val operationId: String,
    val method: HttpMethod,
    val path: String,
    val status: OperationCoverageStatus,
    val scenarioIds: List<ScenarioId> = emptyList(),
    val lastTestedAt: Instant? = null,
)

/**
 * Status of coverage for an operation.
 */
enum class OperationCoverageStatus {
    /**
     * Operation is tested and passing.
     */
    COVERED,

    /**
     * Operation is tested but failing.
     */
    FAILING,

    /**
     * Operation has no test scenarios.
     */
    UNTESTED,
}

/**
 * Represents a gap in test coverage.
 */
data class CoverageGap(
    val type: CoverageGapType,
    val operationId: String?,
    val description: String,
    val severity: GapSeverity,
)

/**
 * Types of coverage gaps.
 */
enum class CoverageGapType {
    UNCOVERED_OPERATION,
    FAILING_OPERATION,
    UNRESOLVED_PLACEHOLDER,
    WEAK_ASSERTIONS,
    MISSING_ERROR_CASES,
}

/**
 * Severity of a coverage gap.
 */
enum class GapSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * AI-generated summary and recommendations for a QA Package.
 */
data class QaSummary(
    val overallVerdict: QaVerdict,
    val summary: String,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val erroredScenarios: Int,
    val keyFindings: List<Finding> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val riskAssessment: RiskAssessment? = null,
    val generatedAt: Instant,
)

/**
 * Overall verdict for the QA Package.
 */
enum class QaVerdict {
    PASS,
    PASS_WITH_WARNINGS,
    FAIL,
    ERROR,
    INCONCLUSIVE,
}

/**
 * A key finding from the QA evaluation.
 */
data class Finding(
    val type: FindingType,
    val severity: FindingSeverity,
    val title: String,
    val description: String,
    val affectedScenarios: List<ScenarioId> = emptyList(),
)

/**
 * Types of findings.
 */
enum class FindingType {
    BUG,
    REGRESSION,
    PERFORMANCE_ISSUE,
    SECURITY_CONCERN,
    DATA_INCONSISTENCY,
    API_CONTRACT_VIOLATION,
}

/**
 * Severity of a finding.
 */
enum class FindingSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * A recommendation from the QA evaluation.
 */
data class Recommendation(
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val actionItems: List<String> = emptyList(),
)

/**
 * Priority of a recommendation.
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    IMMEDIATE,
}

/**
 * Risk assessment for the tested API.
 */
data class RiskAssessment(
    val overallRisk: RiskLevel,
    val qualityScore: Int,
    val stabilityScore: Int,
    val securityScore: Int?,
    val riskFactors: List<String> = emptyList(),
)

/**
 * Risk levels.
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
