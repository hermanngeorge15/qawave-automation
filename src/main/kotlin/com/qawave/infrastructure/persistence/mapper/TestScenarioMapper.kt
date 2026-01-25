package com.qawave.infrastructure.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.TestScenarioEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Mapper for converting between TestScenarioEntity and TestScenario domain model.
 */
@Component
class TestScenarioMapper(
    private val objectMapper: ObjectMapper
) {

    /**
     * Converts an entity to a domain model.
     */
    fun toDomain(entity: TestScenarioEntity): TestScenario {
        return TestScenario(
            id = ScenarioId(entity.id!!),
            suiteId = entity.suiteId?.let { TestSuiteId(it) },
            qaPackageId = entity.qaPackageId?.let { QaPackageId(it) },
            name = entity.name,
            description = entity.description,
            steps = parseSteps(entity.stepsJson),
            tags = parseTags(entity.tags),
            source = ScenarioSource.valueOf(entity.source),
            status = ScenarioStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a domain model to an entity.
     */
    fun toEntity(domain: TestScenario): TestScenarioEntity {
        return TestScenarioEntity(
            id = domain.id.value,
            suiteId = domain.suiteId?.value,
            qaPackageId = domain.qaPackageId?.value,
            name = domain.name,
            description = domain.description,
            stepsJson = objectMapper.writeValueAsString(domain.steps),
            tags = serializeTags(domain.tags),
            source = domain.source.name,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Creates a new entity from a domain model (for insert).
     */
    fun toNewEntity(domain: TestScenario): TestScenarioEntity {
        return TestScenarioEntity(
            id = null,
            suiteId = domain.suiteId?.value,
            qaPackageId = domain.qaPackageId?.value,
            name = domain.name,
            description = domain.description,
            stepsJson = objectMapper.writeValueAsString(domain.steps),
            tags = serializeTags(domain.tags),
            source = domain.source.name,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Creates an entity with a specified ID (for insert with predetermined ID).
     */
    fun toEntityWithId(domain: TestScenario, id: UUID): TestScenarioEntity {
        return TestScenarioEntity(
            id = id,
            suiteId = domain.suiteId?.value,
            qaPackageId = domain.qaPackageId?.value,
            name = domain.name,
            description = domain.description,
            stepsJson = objectMapper.writeValueAsString(domain.steps),
            tags = serializeTags(domain.tags),
            source = domain.source.name,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = Instant.now()
        )
    }

    private fun parseSteps(json: String): List<TestStep> {
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTags(json: String?): Set<String> {
        return try {
            json?.let { objectMapper.readValue<Set<String>>(it) } ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun serializeTags(tags: Set<String>): String? {
        return if (tags.isEmpty()) null else objectMapper.writeValueAsString(tags)
    }
}
