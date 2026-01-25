package com.qawave.domain.model

import java.time.Instant

/**
 * Represents an execution instance of a test scenario.
 * A TestRun captures the results of running a scenario against a target API.
 */
data class TestRun(
    val id: TestRunId,
    val scenarioId: ScenarioId,
    val qaPackageId: QaPackageId?,
    val triggeredBy: String,
    val baseUrl: String,
    val status: TestRunStatus,
    val stepResults: List<TestStepResult> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
    }

    /**
     * Total duration of the test run in milliseconds.
     */
    val durationMs: Long?
        get() = completedAt?.let { it.toEpochMilli() - startedAt.toEpochMilli() }

    /**
     * Whether the test run has completed (success or failure).
     */
    val isComplete: Boolean
        get() = status in listOf(TestRunStatus.PASSED, TestRunStatus.FAILED, TestRunStatus.ERROR, TestRunStatus.CANCELLED)

    /**
     * Number of passed steps.
     */
    val passedSteps: Int
        get() = stepResults.count { it.passed }

    /**
     * Number of failed steps.
     */
    val failedSteps: Int
        get() = stepResults.count { !it.passed }

    /**
     * Total number of steps executed.
     */
    val executedSteps: Int
        get() = stepResults.size

    /**
     * Pass rate as a percentage (0-100).
     */
    val passRate: Double
        get() = if (stepResults.isEmpty()) 0.0 else (passedSteps.toDouble() / stepResults.size) * 100
}

/**
 * Status of a test run.
 */
enum class TestRunStatus {
    /**
     * Run is queued and waiting to start.
     */
    QUEUED,

    /**
     * Run is currently executing.
     */
    RUNNING,

    /**
     * All steps passed.
     */
    PASSED,

    /**
     * One or more steps failed assertions.
     */
    FAILED,

    /**
     * Run encountered an error (e.g., network failure, timeout).
     */
    ERROR,

    /**
     * Run was cancelled before completion.
     */
    CANCELLED
}
