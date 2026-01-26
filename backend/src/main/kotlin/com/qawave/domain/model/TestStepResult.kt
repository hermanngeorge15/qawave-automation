package com.qawave.domain.model

import java.time.Instant

/**
 * Represents the result of executing a single test step.
 * Captures the actual HTTP response and whether assertions passed.
 */
data class TestStepResult(
    val runId: TestRunId,
    val stepIndex: Int,
    val stepName: String,
    val actualStatus: Int?,
    val actualHeaders: Map<String, String> = emptyMap(),
    val actualBody: String? = null,
    val passed: Boolean,
    val assertions: List<AssertionResult> = emptyList(),
    val extractedValues: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val durationMs: Long,
    val executedAt: Instant,
) {
    /**
     * Whether this step resulted in an error (not a failed assertion).
     */
    val hasError: Boolean
        get() = errorMessage != null

    /**
     * Number of passed assertions.
     */
    val passedAssertions: Int
        get() = assertions.count { it.passed }

    /**
     * Number of failed assertions.
     */
    val failedAssertions: Int
        get() = assertions.count { !it.passed }

    /**
     * Summary of the step result.
     */
    val summary: String
        get() =
            when {
                hasError -> "Error: $errorMessage"
                passed -> "Passed ($passedAssertions assertions)"
                else -> "Failed ($failedAssertions of ${assertions.size} assertions failed)"
            }

    companion object {
        /**
         * Creates a result for a step that timed out.
         */
        fun timeout(
            runId: TestRunId,
            step: TestStep,
            durationMs: Long,
            executedAt: Instant = Instant.now(),
        ): TestStepResult =
            TestStepResult(
                runId = runId,
                stepIndex = step.index,
                stepName = step.name,
                actualStatus = null,
                passed = false,
                errorMessage = "Request timed out after ${step.timeoutMs}ms",
                durationMs = durationMs,
                executedAt = executedAt,
            )

        /**
         * Creates a result for a step that encountered an error.
         */
        fun error(
            runId: TestRunId,
            step: TestStep,
            error: Throwable,
            durationMs: Long,
            executedAt: Instant = Instant.now(),
        ): TestStepResult =
            TestStepResult(
                runId = runId,
                stepIndex = step.index,
                stepName = step.name,
                actualStatus = null,
                passed = false,
                errorMessage = error.message ?: "Unknown error: ${error::class.simpleName}",
                durationMs = durationMs,
                executedAt = executedAt,
            )
    }
}

/**
 * Result of a single assertion check.
 */
data class AssertionResult(
    val type: AssertionType,
    val field: String?,
    val expected: String?,
    val actual: String?,
    val passed: Boolean,
    val message: String?,
)

/**
 * Types of assertions that can be performed.
 */
enum class AssertionType {
    STATUS_CODE,
    STATUS_RANGE,
    BODY_CONTAINS,
    BODY_FIELD_EXACT,
    BODY_FIELD_REGEX,
    BODY_FIELD_EXISTS,
    BODY_FIELD_NOT_NULL,
    BODY_FIELD_NULL,
    BODY_FIELD_GREATER_THAN,
    BODY_FIELD_LESS_THAN,
    BODY_FIELD_ONE_OF,
    HEADER_VALUE,
}
