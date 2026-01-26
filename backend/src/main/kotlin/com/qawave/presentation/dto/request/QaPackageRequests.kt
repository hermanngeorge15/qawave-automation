package com.qawave.presentation.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request DTO for creating a QA package.
 */
data class CreateQaPackageRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,
    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,
    val specUrl: String? = null,
    val specContent: String? = null,
    @field:NotBlank(message = "Base URL is required")
    val baseUrl: String,
    val requirements: String? = null,
    val config: QaPackageConfigRequest? = null,
)

/**
 * Configuration options for creating a QA package.
 */
data class QaPackageConfigRequest(
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
 * Request DTO for updating a QA package.
 */
data class UpdateQaPackageRequest(
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String? = null,
    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,
    val specUrl: String? = null,
    val specContent: String? = null,
    val baseUrl: String? = null,
    val requirements: String? = null,
    val config: QaPackageConfigRequest? = null,
)
