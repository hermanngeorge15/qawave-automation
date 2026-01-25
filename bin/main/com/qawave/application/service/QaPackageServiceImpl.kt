package com.qawave.application.service

import com.qawave.domain.model.*
<<<<<<< HEAD
import com.qawave.domain.repository.QaPackageRepository
=======
import com.qawave.infrastructure.persistence.mapper.QaPackageMapper
import com.qawave.infrastructure.persistence.repository.QaPackageR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestRunR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestScenarioR2dbcRepository
>>>>>>> origin/main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import kotlin.math.ceil

/**
 * Implementation of QaPackageService.
 * Handles all business logic for QA package management.
 */
@Service
@Transactional
class QaPackageServiceImpl(
<<<<<<< HEAD
    private val repository: QaPackageRepository
=======
    private val packageRepository: QaPackageR2dbcRepository,
    private val scenarioRepository: TestScenarioR2dbcRepository,
    private val runRepository: TestRunR2dbcRepository,
    private val mapper: QaPackageMapper
>>>>>>> origin/main
) : QaPackageService {

    private val logger = LoggerFactory.getLogger(QaPackageServiceImpl::class.java)

    override suspend fun create(command: CreateQaPackageCommand): QaPackage {
        logger.info("Creating QA package: name={}, triggeredBy={}", command.name, command.triggeredBy)

        val specHash = command.specContent?.let { computeHash(it) }

        val qaPackage = QaPackage(
            id = QaPackageId.generate(),
            name = command.name,
            description = command.description,
            specUrl = command.specUrl,
            specContent = command.specContent,
            specHash = specHash,
            baseUrl = command.baseUrl,
            requirements = command.requirements,
            status = QaPackageStatus.REQUESTED,
            config = command.config,
            triggeredBy = command.triggeredBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(qaPackage)
        logger.info("Created QA package: id={}", saved.id)

        return saved
    }

    override suspend fun findById(id: QaPackageId): QaPackage? {
        return repository.findById(id)
    }

    override suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage? {
        return repository.findById(id)
    }

    override suspend fun findByIdFull(id: QaPackageId): QaPackage? {
        return repository.findById(id)
    }

    override suspend fun findAll(page: Int, size: Int): Page<QaPackage> {
        val allPackages = repository.findAll().toList()
=======
        val entity = mapper.toNewEntity(qaPackage)
        val savedEntity = packageRepository.save(entity)

        logger.info("Created QA package: id={}", savedEntity.id)

        return mapper.toDomain(savedEntity)
    }

    override suspend fun findById(id: QaPackageId): QaPackage? {
        return packageRepository.findById(id.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage? {
        val entity = packageRepository.findById(id.value) ?: return null
        val qaPackage = mapper.toDomain(entity)

        // Load scenarios (simplified - would need scenario mapper)
        val scenarioCount = scenarioRepository.countByQaPackageId(id.value)

        logger.debug("Found package {} with {} scenarios", id, scenarioCount)

        return qaPackage
    }

    override suspend fun findByIdFull(id: QaPackageId): QaPackage? {
        val entity = packageRepository.findById(id.value) ?: return null
        val qaPackage = mapper.toDomain(entity)

        // This would load all related data - scenarios, runs, results
        // For now, return the package without nested data
        // Full loading would be implemented with proper mappers

        return qaPackage
    }

    override suspend fun findAll(page: Int, size: Int): Page<QaPackage> {
        val allPackages = packageRepository.findAll().toList()
>>>>>>> origin/main
        val total = allPackages.size.toLong()
        val totalPages = ceil(total.toDouble() / size).toInt().coerceAtLeast(1)

        val content = allPackages
            .drop(page * size)
            .take(size)
<<<<<<< HEAD
=======
            .map { mapper.toDomain(it) }
>>>>>>> origin/main

        return Page(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages
        )
    }

    override fun findAllStream(): Flow<QaPackage> {
<<<<<<< HEAD
        return repository.findAll()
    }

    override suspend fun findByStatus(status: QaPackageStatus): List<QaPackage> {
        return repository.findByStatus(status)
    }

    override fun findIncomplete(): Flow<QaPackage> {
        return repository.findIncomplete()
    }

    override fun findRecent(since: Instant): Flow<QaPackage> {
        return repository.findRecent(since)
    }

    override suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        validateStatusTransition(existing.status, status)

        val updated = existing.copy(
            status = status,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Updated package {} status: {} -> {}", id, existing.status, status)

        return saved
    }

    override suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            name = command.name ?: existing.name,
            description = command.description ?: existing.description,
            specUrl = command.specUrl ?: existing.specUrl,
            specContent = command.specContent ?: existing.specContent,
            specHash = command.specContent?.let { computeHash(it) } ?: existing.specHash,
            baseUrl = command.baseUrl ?: existing.baseUrl,
            requirements = command.requirements ?: existing.requirements,
            config = command.config ?: existing.config,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Updated package: id={}", id)

        return saved
    }

    override suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
=======
        return packageRepository.findAll().map { mapper.toDomain(it) }
    }

    override suspend fun findByStatus(status: QaPackageStatus): List<QaPackage> {
        return packageRepository.findByStatus(status.name).map { mapper.toDomain(it) }
    }

    override fun findIncomplete(): Flow<QaPackage> {
        return packageRepository.findIncompletePackages().map { mapper.toDomain(it) }
    }

    override fun findRecent(since: Instant): Flow<QaPackage> {
        return packageRepository.findRecentPackages(since).map { mapper.toDomain(it) }
    }

    override suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val currentStatus = QaPackageStatus.valueOf(entity.status)
        validateStatusTransition(currentStatus, status)

        val updatedEntity = entity.copy(
            status = status.name,
            updatedAt = Instant.now()
        )

        val saved = packageRepository.save(updatedEntity)
        logger.info("Updated package {} status: {} -> {}", id, currentStatus, status)

        return mapper.toDomain(saved)
    }

    override suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val updatedEntity = entity.copy(
            name = command.name ?: entity.name,
            description = command.description ?: entity.description,
            specUrl = command.specUrl ?: entity.specUrl,
            specContent = command.specContent ?: entity.specContent,
            specHash = command.specContent?.let { computeHash(it) } ?: entity.specHash,
            baseUrl = command.baseUrl ?: entity.baseUrl,
            requirements = command.requirements ?: entity.requirements,
            updatedAt = Instant.now()
        )

        val saved = packageRepository.save(updatedEntity)
        logger.info("Updated package: id={}", id)

        return mapper.toDomain(saved)
    }

    override suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val qaPackage = mapper.toDomain(entity).copy(
>>>>>>> origin/main
            coverage = coverage,
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(updated)
        logger.info("Set coverage for package: id={}, coverage={}%", id, coverage.coveragePercentage)

        return saved
    }

    override suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
=======
        val saved = packageRepository.save(mapper.toEntity(qaPackage))
        logger.info("Set coverage for package: id={}, coverage={}%", id, coverage.coveragePercentage)

        return mapper.toDomain(saved)
    }

    override suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val qaPackage = mapper.toDomain(entity).copy(
>>>>>>> origin/main
            qaSummary = summary,
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(updated)
        logger.info("Set QA summary for package: id={}, verdict={}", id, summary.overallVerdict)

        return saved
    }

    override suspend fun markStarted(id: QaPackageId): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
=======
        val saved = packageRepository.save(mapper.toEntity(qaPackage))
        logger.info("Set QA summary for package: id={}, verdict={}", id, summary.overallVerdict)

        return mapper.toDomain(saved)
    }

    override suspend fun markStarted(id: QaPackageId): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val updatedEntity = entity.copy(
>>>>>>> origin/main
            startedAt = Instant.now(),
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(updated)
        logger.info("Marked package as started: id={}", id)

        return saved
    }

    override suspend fun markCompleted(id: QaPackageId): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            status = QaPackageStatus.COMPLETE,
=======
        val saved = packageRepository.save(updatedEntity)
        logger.info("Marked package as started: id={}", id)

        return mapper.toDomain(saved)
    }

    override suspend fun markCompleted(id: QaPackageId): QaPackage {
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val updatedEntity = entity.copy(
            status = QaPackageStatus.COMPLETE.name,
>>>>>>> origin/main
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(updated)
        logger.info("Marked package as completed: id={}", id)

        return saved
=======
        val saved = packageRepository.save(updatedEntity)
        logger.info("Marked package as completed: id={}", id)

        return mapper.toDomain(saved)
>>>>>>> origin/main
    }

    override suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage {
        require(status.name.startsWith("FAILED_") || status == QaPackageStatus.CANCELLED) {
            "Status must be a failure status"
        }

<<<<<<< HEAD
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            status = status,
=======
        val entity = packageRepository.findById(id.value)
            ?: throw PackageNotFoundException(id)

        val updatedEntity = entity.copy(
            status = status.name,
>>>>>>> origin/main
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )

<<<<<<< HEAD
        val saved = repository.save(updated)
        logger.info("Marked package as failed: id={}, status={}", id, status)

        return saved
    }

    override suspend fun delete(id: QaPackageId): Boolean {
        val exists = repository.existsById(id)
        if (!exists) return false

        repository.delete(id)
=======
        val saved = packageRepository.save(updatedEntity)
        logger.info("Marked package as failed: id={}, status={}", id, status)

        return mapper.toDomain(saved)
    }

    override suspend fun delete(id: QaPackageId): Boolean {
        val exists = packageRepository.existsById(id.value)
        if (!exists) return false

        // Delete related data first
        runRepository.deleteByQaPackageId(id.value)
        scenarioRepository.deleteByQaPackageId(id.value)
        packageRepository.deleteById(id.value)

>>>>>>> origin/main
        logger.info("Deleted package: id={}", id)
        return true
    }

    override suspend fun countByStatus(status: QaPackageStatus): Long {
<<<<<<< HEAD
        return repository.countByStatus(status)
    }

    override suspend fun count(): Long {
        return repository.count()
=======
        return packageRepository.countByStatus(status.name)
    }

    override suspend fun count(): Long {
        return packageRepository.count()
>>>>>>> origin/main
    }

    private fun validateStatusTransition(current: QaPackageStatus, target: QaPackageStatus) {
        val validTransitions = mapOf(
            QaPackageStatus.REQUESTED to setOf(
                QaPackageStatus.SPEC_FETCHED,
                QaPackageStatus.FAILED_SPEC_FETCH,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.SPEC_FETCHED to setOf(
                QaPackageStatus.AI_SUCCESS,
                QaPackageStatus.FAILED_GENERATION,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.AI_SUCCESS to setOf(
                QaPackageStatus.EXECUTION_IN_PROGRESS,
                QaPackageStatus.FAILED_EXECUTION,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.EXECUTION_IN_PROGRESS to setOf(
                QaPackageStatus.EXECUTION_COMPLETE,
                QaPackageStatus.FAILED_EXECUTION,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.EXECUTION_COMPLETE to setOf(
                QaPackageStatus.QA_EVAL_IN_PROGRESS,
                QaPackageStatus.COMPLETE,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.QA_EVAL_IN_PROGRESS to setOf(
                QaPackageStatus.QA_EVAL_DONE,
                QaPackageStatus.COMPLETE,
                QaPackageStatus.CANCELLED
            ),
            QaPackageStatus.QA_EVAL_DONE to setOf(
                QaPackageStatus.COMPLETE
            )
        )

        val allowed = validTransitions[current] ?: emptySet()
        if (target !in allowed && current != target) {
            throw InvalidStatusTransitionException(current, target)
        }
    }

    private fun computeHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Exception thrown when a package is not found.
 */
class PackageNotFoundException(id: QaPackageId) : RuntimeException("Package not found: $id")

/**
 * Exception thrown when an invalid status transition is attempted.
 */
class InvalidStatusTransitionException(
    current: QaPackageStatus,
    target: QaPackageStatus
) : RuntimeException("Invalid status transition: $current -> $target")
