package com.qawave.infrastructure.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.qawave.application.service.ExecutionContext
import com.qawave.domain.model.*
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.Instant
import org.springframework.http.HttpMethod as SpringHttpMethod

/**
 * Executes HTTP requests for test steps using WebClient.
 */
@Component
class HttpStepExecutor(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(HttpStepExecutor::class.java)

    /**
     * Executes a test step and returns the result.
     */
    suspend fun execute(
        runId: TestRunId,
        step: TestStep,
        baseUrl: String,
        context: ExecutionContext,
    ): TestStepResult {
        val startTime = System.currentTimeMillis()
        val executedAt = Instant.now()

        try {
            val url = context.resolve("$baseUrl${step.endpoint}")
            val body = step.body?.let { context.resolve(it) }
            val headers = step.headers.mapValues { context.resolve(it.value) }

            logger.debug("Executing step {}: {} {}", step.index, step.method, url)

            val result =
                withTimeoutOrNull(step.timeoutMs) {
                    executeRequest(runId, step, url, body, headers, startTime, executedAt)
                }

            return result ?: TestStepResult.timeout(
                runId = runId,
                step = step,
                durationMs = System.currentTimeMillis() - startTime,
                executedAt = executedAt,
            )
        } catch (e: Exception) {
            logger.error("Error executing step {}: {}", step.index, e.message)
            return TestStepResult.error(
                runId = runId,
                step = step,
                error = e,
                durationMs = System.currentTimeMillis() - startTime,
                executedAt = executedAt,
            )
        }
    }

    private suspend fun executeRequest(
        runId: TestRunId,
        step: TestStep,
        url: String,
        body: String?,
        headers: Map<String, String>,
        startTime: Long,
        executedAt: Instant,
    ): TestStepResult {
        val method = toSpringHttpMethod(step.method)

        val response =
            webClient
                .method(method)
                .uri(url)
                .apply {
                    headers.forEach { (k, v) -> header(k, v) }
                    if (body != null) {
                        bodyValue(body)
                    }
                }
                .awaitExchange { response ->
                    val statusCode = response.statusCode().value()
                    val responseHeaders =
                        response.headers().asHttpHeaders()
                            .toSingleValueMap()
                    val responseBody = response.awaitBodyOrNull<String>()

                    Triple(statusCode, responseHeaders, responseBody)
                }

        val (statusCode, responseHeaders, responseBody) = response
        val durationMs = System.currentTimeMillis() - startTime

        // Run assertions
        val assertions = runAssertions(step.expected, statusCode, responseHeaders, responseBody)
        val passed = assertions.all { it.passed }

        // Extract values
        val extractedValues = extractValues(step.extractions, responseBody)

        return TestStepResult(
            runId = runId,
            stepIndex = step.index,
            stepName = step.name,
            actualStatus = statusCode,
            actualHeaders = responseHeaders,
            actualBody = responseBody,
            passed = passed,
            assertions = assertions,
            extractedValues = extractedValues,
            errorMessage = null,
            durationMs = durationMs,
            executedAt = executedAt,
        )
    }

    private fun runAssertions(
        expected: ExpectedResult,
        statusCode: Int,
        headers: Map<String, String>,
        body: String?,
    ): List<AssertionResult> {
        val assertions = mutableListOf<AssertionResult>()

        // Status code assertion
        expected.status?.let { expectedStatus ->
            assertions.add(
                AssertionResult(
                    type = AssertionType.STATUS_CODE,
                    field = null,
                    expected = expectedStatus.toString(),
                    actual = statusCode.toString(),
                    passed = statusCode == expectedStatus,
                    message = if (statusCode == expectedStatus) null else "Expected status $expectedStatus but got $statusCode",
                ),
            )
        }

        // Status range assertion
        expected.statusRange?.let { range ->
            assertions.add(
                AssertionResult(
                    type = AssertionType.STATUS_RANGE,
                    field = null,
                    expected = "${range.first}-${range.last}",
                    actual = statusCode.toString(),
                    passed = statusCode in range,
                    message = if (statusCode in range) null else "Expected status in range ${range.first}-${range.last} but got $statusCode",
                ),
            )
        }

        // Body contains assertions
        expected.bodyContains.forEach { expectedString ->
            val contains = body?.contains(expectedString) == true
            assertions.add(
                AssertionResult(
                    type = AssertionType.BODY_CONTAINS,
                    field = null,
                    expected = expectedString,
                    actual = if (contains) "found" else "not found",
                    passed = contains,
                    message = if (contains) null else "Body does not contain '$expectedString'",
                ),
            )
        }

        // Body field assertions
        val jsonBody = parseJson(body)
        expected.bodyFields.forEach { (field, matcher) ->
            val assertion = assertField(jsonBody, field, matcher)
            assertions.add(assertion)
        }

        // Header assertions
        expected.headers.forEach { (headerName, expectedValue) ->
            val actualValue = headers[headerName]
            val matches = actualValue == expectedValue
            assertions.add(
                AssertionResult(
                    type = AssertionType.HEADER_VALUE,
                    field = headerName,
                    expected = expectedValue,
                    actual = actualValue,
                    passed = matches,
                    message = if (matches) null else "Header '$headerName' expected '$expectedValue' but got '$actualValue'",
                ),
            )
        }

        return assertions
    }

    private fun assertField(
        json: JsonNode?,
        field: String,
        matcher: FieldMatcher,
    ): AssertionResult {
        val value = getJsonValue(json, field)
        val valueStr = value?.asText()

        return when (matcher) {
            is FieldMatcher.Exact -> {
                val matches = valueStr == matcher.value.toString()
                AssertionResult(
                    type = AssertionType.BODY_FIELD_EXACT,
                    field = field,
                    expected = matcher.value.toString(),
                    actual = valueStr,
                    passed = matches,
                    message = if (matches) null else "Field '$field' expected '${matcher.value}' but got '$valueStr'",
                )
            }
            is FieldMatcher.Any -> {
                val exists = value != null && !value.isNull
                AssertionResult(
                    type = AssertionType.BODY_FIELD_EXISTS,
                    field = field,
                    expected = "exists",
                    actual = if (exists) "exists" else "missing",
                    passed = exists,
                    message = if (exists) null else "Field '$field' does not exist",
                )
            }
            is FieldMatcher.Regex -> {
                val matches = valueStr?.matches(Regex(matcher.pattern)) == true
                AssertionResult(
                    type = AssertionType.BODY_FIELD_REGEX,
                    field = field,
                    expected = matcher.pattern,
                    actual = valueStr,
                    passed = matches,
                    message = if (matches) null else "Field '$field' does not match pattern '${matcher.pattern}'",
                )
            }
            is FieldMatcher.GreaterThan -> {
                val numValue = value?.asDouble()
                val matches = numValue != null && numValue > matcher.value.toDouble()
                AssertionResult(
                    type = AssertionType.BODY_FIELD_GREATER_THAN,
                    field = field,
                    expected = "> ${matcher.value}",
                    actual = valueStr,
                    passed = matches,
                    message = if (matches) null else "Field '$field' expected > ${matcher.value} but got '$valueStr'",
                )
            }
            is FieldMatcher.LessThan -> {
                val numValue = value?.asDouble()
                val matches = numValue != null && numValue < matcher.value.toDouble()
                AssertionResult(
                    type = AssertionType.BODY_FIELD_LESS_THAN,
                    field = field,
                    expected = "< ${matcher.value}",
                    actual = valueStr,
                    passed = matches,
                    message = if (matches) null else "Field '$field' expected < ${matcher.value} but got '$valueStr'",
                )
            }
            is FieldMatcher.OneOf -> {
                val matches = matcher.values.map { it.toString() }.contains(valueStr)
                AssertionResult(
                    type = AssertionType.BODY_FIELD_ONE_OF,
                    field = field,
                    expected = matcher.values.joinToString(", "),
                    actual = valueStr,
                    passed = matches,
                    message =
                        if (matches) {
                            null
                        } else {
                            "Field '$field' expected one of [${matcher.values.joinToString(
                                ", ",
                            )}] but got '$valueStr'"
                        },
                )
            }
            is FieldMatcher.NotNull -> {
                val notNull = value != null && !value.isNull
                AssertionResult(
                    type = AssertionType.BODY_FIELD_NOT_NULL,
                    field = field,
                    expected = "not null",
                    actual = if (notNull) "not null" else "null",
                    passed = notNull,
                    message = if (notNull) null else "Field '$field' is null",
                )
            }
            is FieldMatcher.IsNull -> {
                val isNull = value == null || value.isNull
                AssertionResult(
                    type = AssertionType.BODY_FIELD_NULL,
                    field = field,
                    expected = "null",
                    actual = if (isNull) "null" else "not null",
                    passed = isNull,
                    message = if (isNull) null else "Field '$field' is not null",
                )
            }
        }
    }

    private fun extractValues(
        extractions: Map<String, String>,
        body: String?,
    ): Map<String, String> {
        val json = parseJson(body) ?: return emptyMap()
        val extracted = mutableMapOf<String, String>()

        extractions.forEach { (name, path) ->
            val value = getJsonValue(json, path)
            if (value != null && !value.isNull) {
                extracted[name] = value.asText()
            }
        }

        return extracted
    }

    private fun parseJson(body: String?): JsonNode? {
        return try {
            body?.let { objectMapper.readTree(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getJsonValue(
        json: JsonNode?,
        path: String,
    ): JsonNode? {
        if (json == null) return null

        val parts = path.split(".")
        var current: JsonNode? = json

        for (part in parts) {
            current =
                when {
                    current == null -> null
                    part.contains("[") && part.contains("]") -> {
                        val name = part.substringBefore("[")
                        val index = part.substringAfter("[").substringBefore("]").toIntOrNull() ?: return null
                        current.get(name)?.get(index)
                    }
                    else -> current.get(part)
                }
        }

        return current
    }

    private fun toSpringHttpMethod(method: HttpMethod): SpringHttpMethod {
        return when (method) {
            HttpMethod.GET -> SpringHttpMethod.GET
            HttpMethod.POST -> SpringHttpMethod.POST
            HttpMethod.PUT -> SpringHttpMethod.PUT
            HttpMethod.PATCH -> SpringHttpMethod.PATCH
            HttpMethod.DELETE -> SpringHttpMethod.DELETE
            HttpMethod.HEAD -> SpringHttpMethod.HEAD
            HttpMethod.OPTIONS -> SpringHttpMethod.OPTIONS
        }
    }
}
