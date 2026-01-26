package com.qawave.infrastructure.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.TestStepResultEntity
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Mapper for converting between TestStepResultEntity and TestStepResult domain model.
 */
@Component
class TestStepResultMapper(
    private val objectMapper: ObjectMapper,
) {
    /**
     * Converts an entity to a domain model.
     */
    fun toDomain(entity: TestStepResultEntity): TestStepResult {
        return TestStepResult(
            runId = TestRunId(entity.runId),
            stepIndex = entity.stepIndex,
            stepName = entity.stepName,
            actualStatus = entity.actualStatus,
            actualHeaders = parseHeaders(entity.actualHeadersJson),
            actualBody = entity.actualBody,
            passed = entity.passed,
            assertions = parseAssertions(entity.assertionsJson),
            extractedValues = parseExtractedValues(entity.extractedValuesJson),
            errorMessage = entity.errorMessage,
            durationMs = entity.durationMs,
            executedAt = entity.executedAt,
        )
    }

    /**
     * Converts a domain model to an entity.
     */
    fun toEntity(domain: TestStepResult): TestStepResultEntity {
        return TestStepResultEntity(
            id = null,
            runId = domain.runId.value,
            stepIndex = domain.stepIndex,
            stepName = domain.stepName,
            actualStatus = domain.actualStatus,
            actualHeadersJson = serializeHeaders(domain.actualHeaders),
            actualBody = domain.actualBody,
            passed = domain.passed,
            assertionsJson = serializeAssertions(domain.assertions),
            extractedValuesJson = serializeExtractedValues(domain.extractedValues),
            errorMessage = domain.errorMessage,
            durationMs = domain.durationMs,
            executedAt = domain.executedAt,
        )
    }

    /**
     * Creates a new entity with a predetermined ID.
     */
    fun toEntityWithId(
        domain: TestStepResult,
        id: UUID,
    ): TestStepResultEntity {
        return TestStepResultEntity(
            id = id,
            runId = domain.runId.value,
            stepIndex = domain.stepIndex,
            stepName = domain.stepName,
            actualStatus = domain.actualStatus,
            actualHeadersJson = serializeHeaders(domain.actualHeaders),
            actualBody = domain.actualBody,
            passed = domain.passed,
            assertionsJson = serializeAssertions(domain.assertions),
            extractedValuesJson = serializeExtractedValues(domain.extractedValues),
            errorMessage = domain.errorMessage,
            durationMs = domain.durationMs,
            executedAt = domain.executedAt,
        )
    }

    private fun parseHeaders(json: String?): Map<String, String> {
        return try {
            json?.let { objectMapper.readValue<Map<String, String>>(it) } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeHeaders(headers: Map<String, String>): String? {
        return if (headers.isEmpty()) null else objectMapper.writeValueAsString(headers)
    }

    private fun parseAssertions(json: String?): List<AssertionResult> {
        return try {
            json?.let { objectMapper.readValue<List<AssertionResult>>(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeAssertions(assertions: List<AssertionResult>): String? {
        return if (assertions.isEmpty()) null else objectMapper.writeValueAsString(assertions)
    }

    private fun parseExtractedValues(json: String?): Map<String, String> {
        return try {
            json?.let { objectMapper.readValue<Map<String, String>>(it) } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeExtractedValues(values: Map<String, String>): String? {
        return if (values.isEmpty()) null else objectMapper.writeValueAsString(values)
    }
}
