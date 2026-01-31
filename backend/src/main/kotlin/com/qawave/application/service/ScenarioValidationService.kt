package com.qawave.application.service

import com.qawave.domain.model.ApiOperation
import com.qawave.domain.model.ApiSpec
import com.qawave.domain.model.ExpectedResult
import com.qawave.domain.model.HttpMethod
import com.qawave.domain.model.TestScenario
import com.qawave.domain.model.TestStep
import com.qawave.presentation.dto.response.ValidationErrorResponse
import com.qawave.presentation.dto.response.ValidationResultResponse
import com.qawave.presentation.dto.response.ValidationWarningResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for validating test scenarios.
 * Provides schema validation, endpoint validation against OpenAPI specs,
 * and placeholder detection.
 */
interface ScenarioValidationService {
    /**
     * Validate a scenario without persisting changes.
     * Returns detailed validation errors and warnings.
     */
    suspend fun validate(scenario: TestScenario, apiSpec: ApiSpec? = null): ValidationResultResponse

    /**
     * Validate scenario steps independently.
     */
    suspend fun validateSteps(steps: List<TestStep>, apiSpec: ApiSpec? = null): ValidationResultResponse

    /**
     * Validate a single step.
     */
    fun validateStep(step: TestStep, stepIndex: Int, apiSpec: ApiSpec? = null): List<ValidationErrorResponse>
}

/**
 * Error codes for scenario validation.
 */
object ScenarioValidationErrors {
    const val EMPTY_NAME = "SCENARIO_EMPTY_NAME"
    const val NAME_TOO_LONG = "SCENARIO_NAME_TOO_LONG"
    const val NO_STEPS = "SCENARIO_NO_STEPS"
    const val DUPLICATE_STEP_INDEX = "SCENARIO_DUPLICATE_STEP_INDEX"
    const val INVALID_STEP_INDEX = "STEP_INVALID_INDEX"
    const val EMPTY_STEP_NAME = "STEP_EMPTY_NAME"
    const val EMPTY_ENDPOINT = "STEP_EMPTY_ENDPOINT"
    const val INVALID_METHOD = "STEP_INVALID_METHOD"
    const val ENDPOINT_NOT_IN_SPEC = "STEP_ENDPOINT_NOT_IN_SPEC"
    const val METHOD_MISMATCH = "STEP_METHOD_MISMATCH"
    const val UNRESOLVED_PLACEHOLDER = "STEP_UNRESOLVED_PLACEHOLDER"
    const val INVALID_TIMEOUT = "STEP_INVALID_TIMEOUT"
    const val NO_EXPECTED_RESULT = "STEP_NO_EXPECTED_RESULT"
    const val INVALID_STATUS_CODE = "STEP_INVALID_STATUS_CODE"
    const val INVALID_REGEX_PATTERN = "STEP_INVALID_REGEX_PATTERN"
    const val BODY_TOO_LARGE = "STEP_BODY_TOO_LARGE"
}

/**
 * Warning codes for scenario validation.
 */
object ScenarioValidationWarnings {
    const val WEAK_ASSERTIONS = "STEP_WEAK_ASSERTIONS"
    const val LONG_TIMEOUT = "STEP_LONG_TIMEOUT"
    const val DEPRECATED_ENDPOINT = "STEP_DEPRECATED_ENDPOINT"
    const val MISSING_AUTH_HEADER = "STEP_MISSING_AUTH_HEADER"
}

@Service
class ScenarioValidationServiceImpl : ScenarioValidationService {
    private val log = LoggerFactory.getLogger(ScenarioValidationServiceImpl::class.java)

    companion object {
        private const val MAX_NAME_LENGTH = 255
        private const val MAX_BODY_SIZE = 1_000_000 // 1MB
        private const val MAX_TIMEOUT_MS = 300_000L // 5 minutes
        private const val MIN_TIMEOUT_MS = 100L

        // Regex to find unresolved placeholders like {variable} or {{variable}}
        private val PLACEHOLDER_REGEX = Regex("\\{\\{?[^}]+\\}\\}?")

        // Regex for path parameters like /users/{id}
        private val PATH_PARAM_REGEX = Regex("\\{([^}]+)\\}")
    }

    override suspend fun validate(scenario: TestScenario, apiSpec: ApiSpec?): ValidationResultResponse {
        log.debug("Validating scenario: ${scenario.id}")

        val errors = mutableListOf<ValidationErrorResponse>()
        val warnings = mutableListOf<ValidationWarningResponse>()

        // Validate scenario-level properties
        errors.addAll(validateScenarioProperties(scenario))

        // Validate each step
        scenario.steps.forEachIndexed { index, step ->
            val stepErrors = validateStep(step, index, apiSpec)
            errors.addAll(stepErrors)

            val stepWarnings = generateStepWarnings(step, index, apiSpec)
            warnings.addAll(stepWarnings)
        }

        // Validate step indices are unique and sequential
        errors.addAll(validateStepIndices(scenario.steps))

        return if (errors.isEmpty()) {
            log.debug("Scenario ${scenario.id} is valid")
            ValidationResultResponse.valid().copy(warnings = warnings)
        } else {
            log.debug("Scenario ${scenario.id} has ${errors.size} validation errors")
            ValidationResultResponse.invalid(errors).copy(warnings = warnings)
        }
    }

    override suspend fun validateSteps(steps: List<TestStep>, apiSpec: ApiSpec?): ValidationResultResponse {
        val errors = mutableListOf<ValidationErrorResponse>()
        val warnings = mutableListOf<ValidationWarningResponse>()

        if (steps.isEmpty()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.NO_STEPS,
                    message = "Scenario must have at least one step",
                ),
            )
        }

        steps.forEachIndexed { index, step ->
            errors.addAll(validateStep(step, index, apiSpec))
            warnings.addAll(generateStepWarnings(step, index, apiSpec))
        }

        errors.addAll(validateStepIndices(steps))

        return if (errors.isEmpty()) {
            ValidationResultResponse.valid().copy(warnings = warnings)
        } else {
            ValidationResultResponse.invalid(errors).copy(warnings = warnings)
        }
    }

    override fun validateStep(step: TestStep, stepIndex: Int, apiSpec: ApiSpec?): List<ValidationErrorResponse> {
        val errors = mutableListOf<ValidationErrorResponse>()

        // Validate step name
        if (step.name.isBlank()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.EMPTY_STEP_NAME,
                    message = "Step name cannot be blank",
                    field = "steps[$stepIndex].name",
                    stepIndex = stepIndex,
                ),
            )
        }

        // Validate endpoint
        if (step.endpoint.isBlank()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.EMPTY_ENDPOINT,
                    message = "Step endpoint cannot be blank",
                    field = "steps[$stepIndex].endpoint",
                    stepIndex = stepIndex,
                ),
            )
        }

        // Validate timeout
        if (step.timeoutMs < MIN_TIMEOUT_MS || step.timeoutMs > MAX_TIMEOUT_MS) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.INVALID_TIMEOUT,
                    message = "Timeout must be between ${MIN_TIMEOUT_MS}ms and ${MAX_TIMEOUT_MS}ms",
                    field = "steps[$stepIndex].timeoutMs",
                    stepIndex = stepIndex,
                    details = mapOf("value" to step.timeoutMs, "min" to MIN_TIMEOUT_MS, "max" to MAX_TIMEOUT_MS),
                ),
            )
        }

        // Validate body size
        if (step.body != null && step.body.length > MAX_BODY_SIZE) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.BODY_TOO_LARGE,
                    message = "Request body exceeds maximum size of ${MAX_BODY_SIZE} bytes",
                    field = "steps[$stepIndex].body",
                    stepIndex = stepIndex,
                    details = mapOf("size" to step.body.length, "max" to MAX_BODY_SIZE),
                ),
            )
        }

        // Check for unresolved placeholders in endpoint
        val endpointPlaceholders = findUnresolvedPlaceholders(step.endpoint)
        if (endpointPlaceholders.isNotEmpty()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.UNRESOLVED_PLACEHOLDER,
                    message = "Endpoint contains unresolved placeholders: ${endpointPlaceholders.joinToString(", ")}",
                    field = "steps[$stepIndex].endpoint",
                    stepIndex = stepIndex,
                    details = mapOf("placeholders" to endpointPlaceholders),
                ),
            )
        }

        // Check for unresolved placeholders in body
        if (step.body != null) {
            val bodyPlaceholders = findUnresolvedPlaceholders(step.body)
            if (bodyPlaceholders.isNotEmpty()) {
                errors.add(
                    ValidationErrorResponse(
                        code = ScenarioValidationErrors.UNRESOLVED_PLACEHOLDER,
                        message = "Body contains unresolved placeholders: ${bodyPlaceholders.joinToString(", ")}",
                        field = "steps[$stepIndex].body",
                        stepIndex = stepIndex,
                        details = mapOf("placeholders" to bodyPlaceholders),
                    ),
                )
            }
        }

        // Check for unresolved placeholders in headers
        step.headers.forEach { (key, value) ->
            val headerPlaceholders = findUnresolvedPlaceholders(value)
            if (headerPlaceholders.isNotEmpty()) {
                errors.add(
                    ValidationErrorResponse(
                        code = ScenarioValidationErrors.UNRESOLVED_PLACEHOLDER,
                        message = "Header '$key' contains unresolved placeholders",
                        field = "steps[$stepIndex].headers.$key",
                        stepIndex = stepIndex,
                        details = mapOf("placeholders" to headerPlaceholders),
                    ),
                )
            }
        }

        // Validate expected result
        errors.addAll(validateExpectedResult(step, stepIndex))

        // Validate against OpenAPI spec if provided
        if (apiSpec != null) {
            errors.addAll(validateAgainstSpec(step, stepIndex, apiSpec))
        }

        return errors
    }

    private fun validateScenarioProperties(scenario: TestScenario): List<ValidationErrorResponse> {
        val errors = mutableListOf<ValidationErrorResponse>()

        if (scenario.name.isBlank()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.EMPTY_NAME,
                    message = "Scenario name cannot be blank",
                    field = "name",
                ),
            )
        }

        if (scenario.name.length > MAX_NAME_LENGTH) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.NAME_TOO_LONG,
                    message = "Scenario name must be at most $MAX_NAME_LENGTH characters",
                    field = "name",
                    details = mapOf("length" to scenario.name.length, "max" to MAX_NAME_LENGTH),
                ),
            )
        }

        if (scenario.steps.isEmpty()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.NO_STEPS,
                    message = "Scenario must have at least one step",
                    field = "steps",
                ),
            )
        }

        return errors
    }

    private fun validateStepIndices(steps: List<TestStep>): List<ValidationErrorResponse> {
        val errors = mutableListOf<ValidationErrorResponse>()
        val indices = steps.map { it.index }

        // Check for duplicates
        val duplicates = indices.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.DUPLICATE_STEP_INDEX,
                    message = "Duplicate step indices found: ${duplicates.keys.joinToString(", ")}",
                    field = "steps",
                    details = mapOf("duplicates" to duplicates.keys.toList()),
                ),
            )
        }

        // Check for negative indices
        val negativeIndices = indices.filter { it < 0 }
        if (negativeIndices.isNotEmpty()) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.INVALID_STEP_INDEX,
                    message = "Step indices must be non-negative",
                    field = "steps",
                    details = mapOf("invalidIndices" to negativeIndices),
                ),
            )
        }

        return errors
    }

    private fun validateExpectedResult(step: TestStep, stepIndex: Int): List<ValidationErrorResponse> {
        val errors = mutableListOf<ValidationErrorResponse>()
        val expected = step.expected

        // Check that at least some assertion is defined
        if (!hasAnyAssertion(expected)) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.NO_EXPECTED_RESULT,
                    message = "Step must have at least one expected result assertion",
                    field = "steps[$stepIndex].expected",
                    stepIndex = stepIndex,
                ),
            )
        }

        // Validate status code range
        if (expected.status != null && (expected.status < 100 || expected.status > 599)) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.INVALID_STATUS_CODE,
                    message = "Status code must be between 100 and 599",
                    field = "steps[$stepIndex].expected.status",
                    stepIndex = stepIndex,
                    details = mapOf("value" to expected.status),
                ),
            )
        }

        // Validate regex patterns in bodyFields
        expected.bodyFields.forEach { (fieldPath, matcher) ->
            if (matcher is com.qawave.domain.model.FieldMatcher.Regex) {
                try {
                    Regex(matcher.pattern)
                } catch (e: Exception) {
                    errors.add(
                        ValidationErrorResponse(
                            code = ScenarioValidationErrors.INVALID_REGEX_PATTERN,
                            message = "Invalid regex pattern for field '$fieldPath': ${e.message}",
                            field = "steps[$stepIndex].expected.bodyFields.$fieldPath",
                            stepIndex = stepIndex,
                            details = mapOf("pattern" to matcher.pattern),
                        ),
                    )
                }
            }
        }

        return errors
    }

    private fun validateAgainstSpec(step: TestStep, stepIndex: Int, apiSpec: ApiSpec): List<ValidationErrorResponse> {
        val errors = mutableListOf<ValidationErrorResponse>()

        // Normalize the endpoint for comparison (remove path params)
        val normalizedEndpoint = normalizeEndpoint(step.endpoint)

        // Find matching operation in spec
        val matchingOperation = findMatchingOperation(step.method, normalizedEndpoint, apiSpec)

        if (matchingOperation == null) {
            errors.add(
                ValidationErrorResponse(
                    code = ScenarioValidationErrors.ENDPOINT_NOT_IN_SPEC,
                    message = "Endpoint '${step.method} ${step.endpoint}' not found in OpenAPI specification",
                    field = "steps[$stepIndex].endpoint",
                    stepIndex = stepIndex,
                    details =
                        mapOf(
                            "method" to step.method.name,
                            "endpoint" to step.endpoint,
                            "availableOperations" to
                                apiSpec.operations.take(10).map {
                                    "${it.method} ${it.path}"
                                },
                        ),
                ),
            )
        }

        return errors
    }

    private fun findMatchingOperation(method: HttpMethod, endpoint: String, apiSpec: ApiSpec): ApiOperation? {
        return apiSpec.operations.find { operation ->
            operation.method == method && pathMatches(endpoint, operation.path)
        }
    }

    private fun pathMatches(requestPath: String, specPath: String): Boolean {
        // Convert spec path params like /users/{id} to regex
        val regexPattern = specPath.replace(PATH_PARAM_REGEX, "[^/]+")
        val regex = Regex("^$regexPattern$")
        return regex.matches(requestPath)
    }

    private fun normalizeEndpoint(endpoint: String): String {
        // Remove query parameters for comparison
        return endpoint.substringBefore("?")
    }

    private fun findUnresolvedPlaceholders(text: String): List<String> {
        // Find placeholders like {variable} or {{variable}}
        // But exclude path parameters that look like /users/{id} if they match extraction patterns
        val matches = PLACEHOLDER_REGEX.findAll(text)
        return matches.map { it.value }.toList()
    }

    private fun generateStepWarnings(step: TestStep, stepIndex: Int, apiSpec: ApiSpec?): List<ValidationWarningResponse> {
        val warnings = mutableListOf<ValidationWarningResponse>()

        // Warn about weak assertions (only status code check)
        if (step.expected.status != null &&
            step.expected.bodyContains.isEmpty() &&
            step.expected.bodyFields.isEmpty()
        ) {
            warnings.add(
                ValidationWarningResponse(
                    code = ScenarioValidationWarnings.WEAK_ASSERTIONS,
                    message = "Step only checks status code. Consider adding body assertions.",
                    field = "steps[$stepIndex].expected",
                    stepIndex = stepIndex,
                ),
            )
        }

        // Warn about long timeouts
        if (step.timeoutMs > 60_000) {
            warnings.add(
                ValidationWarningResponse(
                    code = ScenarioValidationWarnings.LONG_TIMEOUT,
                    message = "Step has a timeout longer than 60 seconds",
                    field = "steps[$stepIndex].timeoutMs",
                    stepIndex = stepIndex,
                ),
            )
        }

        // Check for deprecated endpoint
        if (apiSpec != null) {
            val normalizedEndpoint = normalizeEndpoint(step.endpoint)
            val operation = findMatchingOperation(step.method, normalizedEndpoint, apiSpec)
            if (operation?.deprecated == true) {
                warnings.add(
                    ValidationWarningResponse(
                        code = ScenarioValidationWarnings.DEPRECATED_ENDPOINT,
                        message = "Endpoint '${step.method} ${step.endpoint}' is marked as deprecated in the spec",
                        field = "steps[$stepIndex].endpoint",
                        stepIndex = stepIndex,
                    ),
                )
            }
        }

        // Warn if POST/PUT/PATCH without Authorization header
        if (step.method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            if (!step.headers.any { it.key.equals("Authorization", ignoreCase = true) }) {
                warnings.add(
                    ValidationWarningResponse(
                        code = ScenarioValidationWarnings.MISSING_AUTH_HEADER,
                        message = "Modifying request without Authorization header",
                        field = "steps[$stepIndex].headers",
                        stepIndex = stepIndex,
                    ),
                )
            }
        }

        return warnings
    }

    /**
     * Checks if the expected result has at least one assertion defined.
     */
    private fun hasAnyAssertion(expected: ExpectedResult): Boolean {
        return expected.status != null ||
            expected.statusRange != null ||
            expected.bodyContains.isNotEmpty() ||
            expected.bodyFields.isNotEmpty() ||
            expected.headers.isNotEmpty()
    }
}

/**
 * Exception thrown when scenario validation fails.
 */
class ScenarioValidationException(
    val errors: List<ValidationErrorResponse>,
) : RuntimeException("Scenario validation failed with ${errors.size} errors")
