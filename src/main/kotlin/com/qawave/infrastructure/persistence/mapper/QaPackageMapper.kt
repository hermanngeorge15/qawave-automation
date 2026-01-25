package com.qawave.infrastructure.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.QaPackageEntity
import org.springframework.stereotype.Component

/**
 * Mapper for converting between QaPackageEntity and QaPackage domain model.
 */
@Component
class QaPackageMapper(
    private val objectMapper: ObjectMapper,
) {
    /**
     * Converts an entity to a domain model.
     */
    fun toDomain(entity: QaPackageEntity): QaPackage {
        return QaPackage(
            id = QaPackageId(entity.id!!),
            name = entity.name,
            description = entity.description,
            specUrl = entity.specUrl,
            specContent = entity.specContent,
            specHash = entity.specHash,
            baseUrl = entity.baseUrl,
            requirements = entity.requirements,
            status = QaPackageStatus.valueOf(entity.status),
            config = entity.configJson?.let { parseConfig(it) } ?: QaPackageConfig(),
            coverage = entity.coverageJson?.let { parseCoverage(it) },
            qaSummary = entity.qaSummaryJson?.let { parseQaSummary(it) },
            triggeredBy = entity.triggeredBy,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    /**
     * Converts a domain model to an entity.
     */
    fun toEntity(domain: QaPackage): QaPackageEntity {
        return QaPackageEntity(
            id = domain.id.value,
            name = domain.name,
            description = domain.description,
            specUrl = domain.specUrl,
            specContent = domain.specContent,
            specHash = domain.specHash,
            baseUrl = domain.baseUrl,
            requirements = domain.requirements,
            status = domain.status.name,
            configJson = objectMapper.writeValueAsString(domain.config),
            coverageJson = domain.coverage?.let { objectMapper.writeValueAsString(it) },
            qaSummaryJson = domain.qaSummary?.let { objectMapper.writeValueAsString(it) },
            triggeredBy = domain.triggeredBy,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
    }

    /**
     * Creates a new entity from a domain model (for insert).
     */
    fun toNewEntity(domain: QaPackage): QaPackageEntity {
        return QaPackageEntity(
            id = null, // Let the database generate
            name = domain.name,
            description = domain.description,
            specUrl = domain.specUrl,
            specContent = domain.specContent,
            specHash = domain.specHash,
            baseUrl = domain.baseUrl,
            requirements = domain.requirements,
            status = domain.status.name,
            configJson = objectMapper.writeValueAsString(domain.config),
            coverageJson = domain.coverage?.let { objectMapper.writeValueAsString(it) },
            qaSummaryJson = domain.qaSummary?.let { objectMapper.writeValueAsString(it) },
            triggeredBy = domain.triggeredBy,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
    }

    private fun parseConfig(json: String): QaPackageConfig {
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            QaPackageConfig()
        }
    }

    private fun parseCoverage(json: String): CoverageReport? {
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseQaSummary(json: String): QaSummary? {
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            null
        }
    }
}
