package com.qawave

import com.qawave.application.service.*
import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.security.MessageDigest
import java.time.Instant

/**
 * Test configuration providing mock beans for integration tests.
 */
@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun testQaPackageService(): QaPackageService = InMemoryQaPackageService()
}

/**
 * In-memory implementation of QaPackageService for testing.
 */
class InMemoryQaPackageService : QaPackageService {
    private val packages = mutableMapOf<QaPackageId, QaPackage>()

    private fun computeHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override suspend fun create(command: CreateQaPackageCommand): QaPackage {
        val id = QaPackageId.generate()
        val specContent = command.specContent ?: "openapi: 3.0.0" // Default spec for tests
        val specHash = computeHash(specContent)

        val qaPackage = QaPackage(
            id = id,
            name = command.name,
            description = command.description,
            specUrl = command.specUrl,
            specContent = specContent,
            specHash = specHash,
            baseUrl = command.baseUrl,
            requirements = command.requirements,
            status = QaPackageStatus.REQUESTED,
            config = command.config,
            triggeredBy = command.triggeredBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        packages[id] = qaPackage
        return qaPackage
    }

    override suspend fun findById(id: QaPackageId): QaPackage? {
        return packages[id]
    }

    override suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage? {
        return packages[id]
    }

    override suspend fun findByIdFull(id: QaPackageId): QaPackage? {
        return packages[id]
    }

    override suspend fun findAll(page: Int, size: Int): Page<QaPackage> {
        val allPackages = packages.values.toList()
        val total = allPackages.size.toLong()
        val totalPages = ((total + size - 1) / size).toInt().coerceAtLeast(1)
        val content = allPackages.drop(page * size).take(size)

        return Page(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages
        )
    }

    override fun findAllStream(): Flow<QaPackage> {
        return flow {
            packages.values.forEach { emit(it) }
        }
    }

    override suspend fun findByStatus(status: QaPackageStatus): List<QaPackage> {
        return packages.values.filter { it.status == status }
    }

    override fun findIncomplete(): Flow<QaPackage> {
        return flow {
            packages.values.filter { !it.isComplete }.forEach { emit(it) }
        }
    }

    override fun findRecent(since: Instant): Flow<QaPackage> {
        return flow {
            packages.values.filter { it.createdAt.isAfter(since) }.forEach { emit(it) }
        }
    }

    override suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)

        // Validate status transition
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

        val allowed = validTransitions[existing.status] ?: emptySet()
        if (status !in allowed && existing.status != status) {
            throw InvalidStatusTransitionException(existing.status, status)
        }

        val updated = existing.copy(status = status, updatedAt = Instant.now())
        packages[id] = updated
        return updated
    }

    override suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val newSpecContent = command.specContent ?: existing.specContent
        val newSpecHash = if (command.specContent != null && newSpecContent != null) {
            computeHash(newSpecContent)
        } else {
            existing.specHash
        }

        val updated = existing.copy(
            name = command.name ?: existing.name,
            description = command.description ?: existing.description,
            specUrl = command.specUrl ?: existing.specUrl,
            specContent = newSpecContent,
            specHash = newSpecHash,
            baseUrl = command.baseUrl ?: existing.baseUrl,
            requirements = command.requirements ?: existing.requirements,
            config = command.config ?: existing.config,
            updatedAt = Instant.now()
        )
        packages[id] = updated
        return updated
    }

    override suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val updated = existing.copy(coverage = coverage, updatedAt = Instant.now())
        packages[id] = updated
        return updated
    }

    override suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val updated = existing.copy(qaSummary = summary, updatedAt = Instant.now())
        packages[id] = updated
        return updated
    }

    override suspend fun markStarted(id: QaPackageId): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val updated = existing.copy(startedAt = Instant.now(), updatedAt = Instant.now())
        packages[id] = updated
        return updated
    }

    override suspend fun markCompleted(id: QaPackageId): QaPackage {
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val updated = existing.copy(
            status = QaPackageStatus.COMPLETE,
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        packages[id] = updated
        return updated
    }

    override suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage {
        require(status.name.startsWith("FAILED_") || status == QaPackageStatus.CANCELLED) {
            "Status must be a failure status"
        }
        val existing = packages[id] ?: throw PackageNotFoundException(id)
        val updated = existing.copy(
            status = status,
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        packages[id] = updated
        return updated
    }

    override suspend fun delete(id: QaPackageId): Boolean {
        return packages.remove(id) != null
    }

    override suspend fun countByStatus(status: QaPackageStatus): Long {
        return packages.values.count { it.status == status }.toLong()
    }

    override suspend fun count(): Long {
        return packages.size.toLong()
    }

    fun clear() {
        packages.clear()
    }
}
