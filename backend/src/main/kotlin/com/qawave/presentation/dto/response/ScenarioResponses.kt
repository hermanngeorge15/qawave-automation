package com.qawave.presentation.dto.response

import com.qawave.domain.model.ExpectedResult
import com.qawave.domain.model.FieldMatcher
import com.qawave.domain.model.TestScenario
import com.qawave.domain.model.TestStep
import java.time.Instant

/**
 * Response DTO for a test scenario.
 */
data class ScenarioResponse(
    val id: String,
    val suiteId: String?,
    val qaPackageId: String?,
    val name: String,
    val description: String?,
    val steps: List<TestStepResponse>,
    val tags: Set<String>,
    val source: String,
    val status: String,
    val stepCount: Int,
    val coveredOperations: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(scenario: TestScenario): ScenarioResponse {
            return ScenarioResponse(
                id = scenario.id.toString(),
                suiteId = scenario.suiteId?.toString(),
                qaPackageId = scenario.qaPackageId?.toString(),
                name = scenario.name,
                description = scenario.description,
                steps = scenario.orderedSteps.map { TestStepResponse.from(it) },
                tags = scenario.tags,
                source = scenario.source.name,
                status = scenario.status.name,
                stepCount = scenario.stepCount,
                coveredOperations = scenario.coveredOperations,
                createdAt = scenario.createdAt,
                updatedAt = scenario.updatedAt,
            )
        }
    }
}

/**
 * Response DTO for a test step.
 */
data class TestStepResponse(
    val index: Int,
    val name: String,
    val method: String,
    val endpoint: String,
    val headers: Map<String, String>,
    val body: String?,
    val expected: ExpectedResultResponse,
    val extractions: Map<String, String>,
    val timeoutMs: Long,
) {
    companion object {
        fun from(step: TestStep): TestStepResponse {
            return TestStepResponse(
                index = step.index,
                name = step.name,
                method = step.method.name,
                endpoint = step.endpoint,
                headers = step.headers,
                body = step.body,
                expected = ExpectedResultResponse.from(step.expected),
                extractions = step.extractions,
                timeoutMs = step.timeoutMs,
            )
        }
    }
}

/**
 * Response DTO for expected results.
 */
data class ExpectedResultResponse(
    val status: Int?,
    val statusRangeStart: Int?,
    val statusRangeEnd: Int?,
    val bodyContains: List<String>,
    val bodyFields: Map<String, FieldMatcherResponse>,
    val headers: Map<String, String>,
) {
    companion object {
        fun from(expected: ExpectedResult): ExpectedResultResponse {
            return ExpectedResultResponse(
                status = expected.status,
                statusRangeStart = expected.statusRange?.first,
                statusRangeEnd = expected.statusRange?.last,
                bodyContains = expected.bodyContains,
                bodyFields = expected.bodyFields.mapValues { FieldMatcherResponse.from(it.value) },
                headers = expected.headers,
            )
        }
    }
}

/**
 * Response DTO for field matchers.
 */
data class FieldMatcherResponse(
    val type: String,
    val value: Any? = null,
    val pattern: String? = null,
    val values: List<Any>? = null,
) {
    companion object {
        fun from(matcher: FieldMatcher): FieldMatcherResponse {
            return when (matcher) {
                is FieldMatcher.Exact -> FieldMatcherResponse(type = "exact", value = matcher.value)
                is FieldMatcher.Any -> FieldMatcherResponse(type = "any")
                is FieldMatcher.Regex -> FieldMatcherResponse(type = "regex", pattern = matcher.pattern)
                is FieldMatcher.GreaterThan -> FieldMatcherResponse(type = "greaterThan", value = matcher.value)
                is FieldMatcher.LessThan -> FieldMatcherResponse(type = "lessThan", value = matcher.value)
                is FieldMatcher.OneOf -> FieldMatcherResponse(type = "oneOf", values = matcher.values)
                is FieldMatcher.NotNull -> FieldMatcherResponse(type = "notNull")
                is FieldMatcher.IsNull -> FieldMatcherResponse(type = "isNull")
            }
        }
    }
}

/**
 * Response DTO for scenario validation results.
 */
data class ValidationResultResponse(
    val valid: Boolean,
    val errors: List<ValidationErrorResponse>,
    val warnings: List<ValidationWarningResponse> = emptyList(),
    val validatedAt: Instant = Instant.now(),
) {
    companion object {
        fun valid(): ValidationResultResponse {
            return ValidationResultResponse(valid = true, errors = emptyList())
        }

        fun invalid(errors: List<ValidationErrorResponse>): ValidationResultResponse {
            return ValidationResultResponse(valid = false, errors = errors)
        }
    }
}

/**
 * Individual validation error.
 */
data class ValidationErrorResponse(
    val code: String,
    val message: String,
    val field: String? = null,
    val stepIndex: Int? = null,
    val details: Map<String, Any>? = null,
)

/**
 * Validation warning (non-blocking issue).
 */
data class ValidationWarningResponse(
    val code: String,
    val message: String,
    val field: String? = null,
    val stepIndex: Int? = null,
)

/**
 * Summary response for scenario lists.
 */
data class ScenarioSummaryResponse(
    val id: String,
    val name: String,
    val description: String?,
    val stepCount: Int,
    val tags: Set<String>,
    val source: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(scenario: TestScenario): ScenarioSummaryResponse {
            return ScenarioSummaryResponse(
                id = scenario.id.toString(),
                name = scenario.name,
                description = scenario.description,
                stepCount = scenario.stepCount,
                tags = scenario.tags,
                source = scenario.source.name,
                status = scenario.status.name,
                createdAt = scenario.createdAt,
                updatedAt = scenario.updatedAt,
            )
        }
    }
}
