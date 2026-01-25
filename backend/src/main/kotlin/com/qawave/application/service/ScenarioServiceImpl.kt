package com.qawave.application.service

import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.mapper.TestScenarioMapper
import com.qawave.infrastructure.persistence.repository.TestScenarioR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Implementation of ScenarioService using R2DBC repository.
 */
@Service
@Transactional
class ScenarioServiceImpl(
    private val repository: TestScenarioR2dbcRepository,
    private val mapper: TestScenarioMapper,
) : ScenarioService {
    private val logger = LoggerFactory.getLogger(ScenarioServiceImpl::class.java)

    override suspend fun create(command: CreateScenarioCommand): TestScenario {
        logger.debug("Creating scenario: {}", command.name)

        val now = Instant.now()
        val scenario =
            TestScenario(
                id = ScenarioId.generate(),
                suiteId = command.suiteId,
                qaPackageId = command.qaPackageId,
                name = command.name,
                description = command.description,
                steps = command.steps,
                tags = command.tags,
                source = command.source,
                status = command.status,
                createdAt = now,
                updatedAt = now,
            )

        val entity = mapper.toEntityWithId(scenario, scenario.id.value)
        val savedEntity = repository.save(entity)

        logger.info("Created scenario: {} with id: {}", scenario.name, scenario.id.value)
        return mapper.toDomain(savedEntity)
    }

    override suspend fun createBatch(commands: List<CreateScenarioCommand>): List<TestScenario> {
        logger.debug("Creating {} scenarios in batch", commands.size)

        val scenarios =
            commands.map { command ->
                val now = Instant.now()
                TestScenario(
                    id = ScenarioId.generate(),
                    suiteId = command.suiteId,
                    qaPackageId = command.qaPackageId,
                    name = command.name,
                    description = command.description,
                    steps = command.steps,
                    tags = command.tags,
                    source = command.source,
                    status = command.status,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        val entities = scenarios.map { mapper.toEntityWithId(it, it.id.value) }
        val savedEntities = repository.saveAll(entities).toList()

        logger.info("Created {} scenarios in batch", savedEntities.size)
        return savedEntities.map { mapper.toDomain(it) }
    }

    override suspend fun findById(id: ScenarioId): TestScenario? {
        logger.debug("Finding scenario by id: {}", id.value)
        return repository.findById(id.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun findAll(
        page: Int,
        size: Int,
    ): Page<TestScenario> {
        logger.debug("Finding all scenarios, page: {}, size: {}", page, size)

        val offset = page * size
        val entities = repository.findAll().toList()
        val totalElements = entities.size.toLong()
        val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0

        val pagedEntities =
            entities
                .drop(offset)
                .take(size)
                .map { mapper.toDomain(it) }

        return Page(
            content = pagedEntities,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    override fun findAllStream(): Flow<TestScenario> {
        logger.debug("Streaming all scenarios")
        return repository.findAll().map { mapper.toDomain(it) }
    }

    override suspend fun findByPackageId(packageId: QaPackageId): List<TestScenario> {
        logger.debug("Finding scenarios by package id: {}", packageId.value)
        return repository.findByQaPackageId(packageId.value).map { mapper.toDomain(it) }
    }

    override fun findByPackageIdStream(packageId: QaPackageId): Flow<TestScenario> {
        logger.debug("Streaming scenarios by package id: {}", packageId.value)
        return repository.findAllByQaPackageId(packageId.value).map { mapper.toDomain(it) }
    }

    override suspend fun findBySuiteId(suiteId: TestSuiteId): List<TestScenario> {
        logger.debug("Finding scenarios by suite id: {}", suiteId.value)
        return repository.findBySuiteId(suiteId.value).map { mapper.toDomain(it) }
    }

    override suspend fun findByStatus(status: ScenarioStatus): List<TestScenario> {
        logger.debug("Finding scenarios by status: {}", status)
        return repository.findByStatus(status.name).map { mapper.toDomain(it) }
    }

    override fun findByStatusStream(status: ScenarioStatus): Flow<TestScenario> {
        logger.debug("Streaming scenarios by status: {}", status)
        return repository.findAllByStatus(status.name).map { mapper.toDomain(it) }
    }

    override suspend fun findBySource(source: ScenarioSource): List<TestScenario> {
        logger.debug("Finding scenarios by source: {}", source)
        return repository.findBySource(source.name).map { mapper.toDomain(it) }
    }

    override fun findRecent(since: Instant): Flow<TestScenario> {
        logger.debug("Finding scenarios created since: {}", since)
        return repository.findRecentScenarios(since).map { mapper.toDomain(it) }
    }

    override fun findByTag(tag: String): Flow<TestScenario> {
        logger.debug("Finding scenarios by tag: {}", tag)
        return repository.findByTag("[\"$tag\"]").map { mapper.toDomain(it) }
    }

    override suspend fun update(
        id: ScenarioId,
        command: UpdateScenarioCommand,
    ): TestScenario {
        logger.debug("Updating scenario: {}", id.value)

        val existing =
            repository.findById(id.value)
                ?: throw ScenarioNotFoundException(id)

        val current = mapper.toDomain(existing)
        val updated =
            TestScenario(
                id = current.id,
                suiteId = current.suiteId,
                qaPackageId = current.qaPackageId,
                name = command.name ?: current.name,
                description = command.description ?: current.description,
                steps = command.steps ?: current.steps,
                tags = command.tags ?: current.tags,
                source = current.source,
                status = command.status ?: current.status,
                createdAt = current.createdAt,
                updatedAt = Instant.now(),
            )

        val entity = mapper.toEntity(updated)
        val savedEntity = repository.save(entity)

        logger.info("Updated scenario: {}", id.value)
        return mapper.toDomain(savedEntity)
    }

    override suspend fun updateStatus(
        id: ScenarioId,
        status: ScenarioStatus,
    ): TestScenario {
        logger.debug("Updating scenario status: {} to {}", id.value, status)
        return update(id, UpdateScenarioCommand(status = status))
    }

    override suspend fun markReady(id: ScenarioId): TestScenario {
        return updateStatus(id, ScenarioStatus.READY)
    }

    override suspend fun markInvalid(id: ScenarioId): TestScenario {
        return updateStatus(id, ScenarioStatus.INVALID)
    }

    override suspend fun disable(id: ScenarioId): TestScenario {
        return updateStatus(id, ScenarioStatus.DISABLED)
    }

    override suspend fun enable(id: ScenarioId): TestScenario {
        return updateStatus(id, ScenarioStatus.PENDING)
    }

    override suspend fun delete(id: ScenarioId): Boolean {
        logger.debug("Deleting scenario: {}", id.value)

        val exists = repository.existsById(id.value)
        if (exists) {
            repository.deleteById(id.value)
            logger.info("Deleted scenario: {}", id.value)
        }
        return exists
    }

    override suspend fun deleteByPackageId(packageId: QaPackageId) {
        logger.debug("Deleting scenarios for package: {}", packageId.value)
        repository.deleteByQaPackageId(packageId.value)
        logger.info("Deleted scenarios for package: {}", packageId.value)
    }

    override suspend fun countByPackageId(packageId: QaPackageId): Long {
        return repository.countByQaPackageId(packageId.value)
    }

    override suspend fun countByStatus(status: ScenarioStatus): Long {
        return repository.countByStatus(status.name)
    }

    override suspend fun count(): Long {
        return repository.count()
    }
}

/**
 * Exception thrown when a scenario is not found.
 */
class ScenarioNotFoundException(id: ScenarioId) :
    RuntimeException("Scenario not found: ${id.value}")
