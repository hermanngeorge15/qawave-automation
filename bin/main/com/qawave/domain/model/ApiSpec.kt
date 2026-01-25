package com.qawave.domain.model

import java.time.Instant

/**
 * Represents an API specification (OpenAPI/Swagger).
 * Contains the parsed specification content and metadata.
 */
data class ApiSpec(
    val id: ApiSpecId,
    val name: String,
    val description: String?,
    val version: String?,
    val url: String?,
    val rawContent: String,
    val contentHash: String,
    val format: ApiSpecFormat,
    val operations: List<ApiOperation> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "API spec name cannot be blank" }
        require(rawContent.isNotBlank()) { "API spec content cannot be blank" }
    }

    /**
     * Total number of operations in this spec.
     */
    val operationCount: Int
        get() = operations.size

    /**
     * All unique HTTP methods used in this spec.
     */
    val usedMethods: Set<HttpMethod>
        get() = operations.map { it.method }.toSet()

    /**
     * All unique paths in this spec.
     */
    val paths: Set<String>
        get() = operations.map { it.path }.toSet()

    /**
     * All unique tags from operations.
     */
    val allTags: Set<String>
        get() = operations.flatMap { it.tags }.toSet()
}

/**
 * Format of the API specification.
 */
enum class ApiSpecFormat {
    OPENAPI_3_0,
    OPENAPI_3_1,
    SWAGGER_2_0,
    UNKNOWN
}

/**
 * Represents a single operation (endpoint) in an API spec.
 */
data class ApiOperation(
    val operationId: String,
    val method: HttpMethod,
    val path: String,
    val summary: String?,
    val description: String?,
    val tags: Set<String> = emptySet(),
    val parameters: List<ApiParameter> = emptyList(),
    val requestBody: ApiRequestBody? = null,
    val responses: Map<Int, ApiResponse> = emptyMap(),
    val security: List<String> = emptyList(),
    val deprecated: Boolean = false
)

/**
 * Represents a parameter for an API operation.
 */
data class ApiParameter(
    val name: String,
    val location: ParameterLocation,
    val description: String?,
    val required: Boolean,
    val schema: ParameterSchema?,
    val example: Any? = null
)

/**
 * Location of a parameter.
 */
enum class ParameterLocation {
    QUERY, PATH, HEADER, COOKIE
}

/**
 * Schema information for a parameter.
 */
data class ParameterSchema(
    val type: String,
    val format: String? = null,
    val enum: List<Any>? = null,
    val default: Any? = null,
    val minimum: Number? = null,
    val maximum: Number? = null,
    val pattern: String? = null
)

/**
 * Request body specification.
 */
data class ApiRequestBody(
    val description: String?,
    val required: Boolean,
    val contentTypes: Map<String, RequestBodySchema> = emptyMap()
)

/**
 * Schema for a request body content type.
 */
data class RequestBodySchema(
    val schema: String?,
    val example: Any? = null
)

/**
 * Response specification.
 */
data class ApiResponse(
    val description: String?,
    val contentTypes: Map<String, ResponseSchema> = emptyMap(),
    val headers: Map<String, String> = emptyMap()
)

/**
 * Schema for a response content type.
 */
data class ResponseSchema(
    val schema: String?,
    val example: Any? = null
)
