package com.qawave.presentation.dto.request

import com.qawave.domain.model.ExpectedResult
import com.qawave.domain.model.HttpMethod
import com.qawave.domain.model.ScenarioSource
import com.qawave.domain.model.ScenarioStatus
import com.qawave.domain.model.TestStep
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for updating a scenario.
 * All fields are optional for partial updates.
 */
data class UpdateScenarioRequest(
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String? = null,
    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,
    @field:Valid
    val steps: List<TestStepRequest>? = null,
    @field:Size(max = 20, message = "Maximum 20 tags allowed")
    val tags: Set<
        @Size(max = 50, message = "Tag must be at most 50 characters")
        String,
    >? = null,
    val status: ScenarioStatus? = null,
)

/**
 * Request DTO for creating a scenario.
 */
data class CreateScenarioRequest(
    val qaPackageId: String? = null,
    val suiteId: String? = null,
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,
    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,
    @field:NotEmpty(message = "At least one step is required")
    @field:Valid
    val steps: List<TestStepRequest>,
    @field:Size(max = 20, message = "Maximum 20 tags allowed")
    val tags: Set<
        @Size(max = 50, message = "Tag must be at most 50 characters")
        String,
    > = emptySet(),
    val source: ScenarioSource = ScenarioSource.MANUAL,
)

/**
 * Request DTO for a test step.
 */
data class TestStepRequest(
    @field:Min(0, message = "Index must be non-negative")
    val index: Int,
    @field:NotBlank(message = "Step name is required")
    @field:Size(max = 255, message = "Step name must be at most 255 characters")
    val name: String,
    @field:NotBlank(message = "HTTP method is required")
    @field:Pattern(
        regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS",
        message = "Invalid HTTP method",
    )
    val method: String,
    @field:NotBlank(message = "Endpoint is required")
    @field:Size(max = 2000, message = "Endpoint must be at most 2000 characters")
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    @field:Size(max = 1_000_000, message = "Body must be at most 1MB")
    val body: String? = null,
    @field:Valid
    val expected: ExpectedResultRequest,
    val extractions: Map<String, String> = emptyMap(),
    @field:Min(100, message = "Timeout must be at least 100ms")
    @field:Max(300_000, message = "Timeout must be at most 5 minutes")
    val timeoutMs: Long = 30_000,
) {
    /**
     * Convert to domain model TestStep.
     */
    fun toDomain(): TestStep {
        return TestStep(
            index = index,
            name = name,
            method = HttpMethod.valueOf(method),
            endpoint = endpoint,
            headers = headers,
            body = body,
            expected = expected.toDomain(),
            extractions = extractions,
            timeoutMs = timeoutMs,
        )
    }
}

/**
 * Request DTO for expected results.
 */
data class ExpectedResultRequest(
    @field:Min(100, message = "Status must be at least 100")
    @field:Max(599, message = "Status must be at most 599")
    val status: Int? = null,
    val statusRangeStart: Int? = null,
    val statusRangeEnd: Int? = null,
    val bodyContains: List<String> = emptyList(),
    @field:Valid
    val bodyFields: Map<String, FieldMatcherRequest> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
) {
    /**
     * Convert to domain model ExpectedResult.
     */
    fun toDomain(): ExpectedResult {
        return ExpectedResult(
            status = status,
            statusRange =
                if (statusRangeStart != null && statusRangeEnd != null) {
                    statusRangeStart..statusRangeEnd
                } else {
                    null
                },
            bodyContains = bodyContains,
            bodyFields = bodyFields.mapValues { it.value.toDomain() },
            headers = headers,
        )
    }
}

/**
 * Request DTO for field matchers.
 * Uses a type discriminator to handle the sealed class.
 * Note: Using fully qualified names for FieldMatcher due to kotlin.Any vs FieldMatcher.Any confusion
 */
data class FieldMatcherRequest(
    @field:NotBlank(message = "Matcher type is required")
    @field:Pattern(
        regexp = "exact|any|regex|greaterThan|lessThan|oneOf|notNull|isNull",
        message = "Invalid matcher type",
    )
    val type: String,
    val value: kotlin.Any? = null,
    val pattern: String? = null,
    val values: List<kotlin.Any>? = null,
) {
    /**
     * Convert to domain model FieldMatcher.
     */
    @Suppress("ThrowsCount")
    fun toDomain(): com.qawave.domain.model.FieldMatcher {
        return when (type) {
            "exact" -> {
                val v = value ?: throw IllegalArgumentException("Value required for exact matcher")
                com.qawave.domain.model.FieldMatcher.Exact(v)
            }
            "any" -> com.qawave.domain.model.FieldMatcher.Any
            "regex" ->
                com.qawave.domain.model.FieldMatcher.Regex(
                    pattern ?: throw IllegalArgumentException("Pattern required for regex matcher"),
                )
            "greaterThan" ->
                com.qawave.domain.model.FieldMatcher.GreaterThan(
                    (value as? Number) ?: throw IllegalArgumentException("Numeric value required for greaterThan matcher"),
                )
            "lessThan" ->
                com.qawave.domain.model.FieldMatcher.LessThan(
                    (value as? Number) ?: throw IllegalArgumentException("Numeric value required for lessThan matcher"),
                )
            "oneOf" -> {
                @Suppress("UNCHECKED_CAST")
                val v =
                    values as? List<kotlin.Any>
                        ?: throw IllegalArgumentException("Values required for oneOf matcher")
                com.qawave.domain.model.FieldMatcher.OneOf(v)
            }
            "notNull" -> com.qawave.domain.model.FieldMatcher.NotNull
            "isNull" -> com.qawave.domain.model.FieldMatcher.IsNull
            else -> throw IllegalArgumentException("Unknown matcher type: $type")
        }
    }
}
