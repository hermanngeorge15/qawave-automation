package com.qawave.domain.model

/**
 * Represents a single step in a test scenario.
 * Contains the HTTP action to perform and assertions to verify.
 */
data class TestStep(
    val index: Int,
    val name: String,
    val method: HttpMethod,
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val expected: ExpectedResult,
    val extractions: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 30_000
)

/**
 * HTTP methods supported for test steps.
 */
enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
}

/**
 * Expected results for a test step.
 * Defines what the response should look like.
 */
data class ExpectedResult(
    val status: Int? = null,
    val statusRange: IntRange? = null,
    val bodyContains: List<String> = emptyList(),
    val bodyFields: Map<String, FieldMatcher> = emptyMap(),
    val headers: Map<String, String> = emptyMap()
)

/**
 * Matcher for verifying a field value in the response body.
 */
sealed class FieldMatcher {
    /**
     * Field should have exactly this value.
     */
    data class Exact(val value: Any) : FieldMatcher()

    /**
     * Field can have any value (just needs to exist).
     */
    data object Any : FieldMatcher()

    /**
     * Field value should match this regex pattern.
     */
    data class Regex(val pattern: String) : FieldMatcher()

    /**
     * Field value should be greater than this number.
     */
    data class GreaterThan(val value: Number) : FieldMatcher()

    /**
     * Field value should be less than this number.
     */
    data class LessThan(val value: Number) : FieldMatcher()

    /**
     * Field value should be one of these values.
     */
    data class OneOf(val values: List<Any>) : FieldMatcher()

    /**
     * Field should not be null.
     */
    data object NotNull : FieldMatcher()

    /**
     * Field should be null.
     */
    data object IsNull : FieldMatcher()
}
