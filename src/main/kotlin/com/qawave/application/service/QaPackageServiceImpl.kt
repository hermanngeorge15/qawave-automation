package com.qawave.application.service

import com.qawave.domain.model.*
import com.qawave.domain.repository.QaPackageRepository
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
    private val repository: QaPackageRepository
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
        val total = allPackages.size.toLong()
        val totalPages = ceil(total.toDouble() / size).toInt().coerceAtLeast(1)

        val content = allPackages
            .drop(page * size)
            .take(size)

        return Page(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages
        )
    }

    override fun findAllStream(): Flow<QaPackage> {
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
            coverage = coverage,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Set coverage for package: id={}, coverage={}%", id, coverage.coveragePercentage)

        return saved
    }

    override suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            qaSummary = summary,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Set QA summary for package: id={}, verdict={}", id, summary.overallVerdict)

        return saved
    }

    override suspend fun markStarted(id: QaPackageId): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            startedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Marked package as started: id={}", id)

        return saved
    }

    override suspend fun markCompleted(id: QaPackageId): QaPackage {
        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            status = QaPackageStatus.COMPLETE,
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Marked package as completed: id={}", id)

        return saved
    }

    override suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage {
        require(status.name.startsWith("FAILED_") || status == QaPackageStatus.CANCELLED) {
            "Status must be a failure status"
        }

        val existing = repository.findById(id)
            ?: throw PackageNotFoundException(id)

        val updated = existing.copy(
            status = status,
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Marked package as failed: id={}, status={}", id, status)

        return saved
    }

    override suspend fun delete(id: QaPackageId): Boolean {
        val exists = repository.existsById(id)
        if (!exists) return false

        repository.delete(id)
        logger.info("Deleted package: id={}", id)
        return true
    }

    override suspend fun countByStatus(status: QaPackageStatus): Long {
        return repository.countByStatus(status)
    }

    override suspend fun count(): Long {
        return repository.count()
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
