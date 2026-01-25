package com.qawave.infrastructure.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.TestRunEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Mapper for converting between TestRunEntity and TestRun domain model.
 */
@Component
class TestRunMapper(
    private val objectMapper: ObjectMapper
) {

    /**
     * Converts an entity to a domain model (without step results).
     */
    fun toDomain(entity: TestRunEntity): TestRun {
        return TestRun(
            id = TestRunId(entity.id!!),
            scenarioId = ScenarioId(entity.scenarioId),
            qaPackageId = entity.qaPackageId?.let { QaPackageId(it) },
            triggeredBy = entity.triggeredBy,
            baseUrl = entity.baseUrl,
            status = TestRunStatus.valueOf(entity.status),
            stepResults = emptyList(),
            environment = parseEnvironment(entity.environmentJson),
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts an entity to a domain model with step results.
     */
    fun toDomain(entity: TestRunEntity, stepResults: List<TestStepResult>): TestRun {
        return TestRun(
            id = TestRunId(entity.id!!),
            scenarioId = ScenarioId(entity.scenarioId),
            qaPackageId = entity.qaPackageId?.let { QaPackageId(it) },
            triggeredBy = entity.triggeredBy,
            baseUrl = entity.baseUrl,
            status = TestRunStatus.valueOf(entity.status),
            stepResults = stepResults,
            environment = parseEnvironment(entity.environmentJson),
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a domain model to an entity.
     */
    fun toEntity(domain: TestRun): TestRunEntity {
        return TestRunEntity(
            id = domain.id.value,
            scenarioId = domain.scenarioId.value,
            qaPackageId = domain.qaPackageId?.value,
            triggeredBy = domain.triggeredBy,
            baseUrl = domain.baseUrl,
            status = domain.status.name,
            environmentJson = serializeEnvironment(domain.environment),
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Creates a new entity from a domain model (for insert with predetermined ID).
     */
    fun toEntityWithId(domain: TestRun, id: UUID): TestRunEntity {
        return TestRunEntity(
            id = id,
            scenarioId = domain.scenarioId.value,
            qaPackageId = domain.qaPackageId?.value,
            triggeredBy = domain.triggeredBy,
            baseUrl = domain.baseUrl,
            status = domain.status.name,
            environmentJson = serializeEnvironment(domain.environment),
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt,
            updatedAt = Instant.now()
        )
    }

    private fun parseEnvironment(json: String?): Map<String, String> {
        return try {
            json?.let { objectMapper.readValue<Map<String, String>>(it) } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeEnvironment(env: Map<String, String>): String? {
        return if (env.isEmpty()) null else objectMapper.writeValueAsString(env)
    }
}
