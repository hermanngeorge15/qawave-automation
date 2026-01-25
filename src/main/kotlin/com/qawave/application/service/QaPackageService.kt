package com.qawave.application.service

import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service interface for QA Package operations.
 * All methods use suspend functions for non-blocking execution.
 */
interface QaPackageService {
    /**
     * Creates a new QA package.
     */
    suspend fun create(command: CreateQaPackageCommand): QaPackage

    /**
     * Finds a QA package by ID.
     */
    suspend fun findById(id: QaPackageId): QaPackage?

    /**
     * Finds a QA package by ID with scenarios loaded.
     */
    suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage?

    /**
     * Finds a QA package by ID with all related data.
     */
    suspend fun findByIdFull(id: QaPackageId): QaPackage?

    /**
     * Finds all QA packages with pagination.
     */
    suspend fun findAll(page: Int = 0, size: Int = 20): Page<QaPackage>

    /**
     * Returns a Flow of all QA packages.
     */
    fun findAllStream(): Flow<QaPackage>

    /**
     * Finds QA packages by status.
     */
    suspend fun findByStatus(status: QaPackageStatus): List<QaPackage>

    /**
     * Finds incomplete (in-progress) packages.
     */
    fun findIncomplete(): Flow<QaPackage>

    /**
     * Finds packages created since a given time.
     */
    fun findRecent(since: Instant): Flow<QaPackage>

    /**
     * Updates the status of a QA package.
     */
    suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
     * Updates a QA package.
     */
    suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage

    /**
     * Sets the coverage report for a package.
     */
    suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage

    /**
     * Sets the QA summary for a package.
     */
    suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage

    /**
     * Marks a package as started.
     */
    suspend fun markStarted(id: QaPackageId): QaPackage

    /**
     * Marks a package as completed.
     */
    suspend fun markCompleted(id: QaPackageId): QaPackage

    /**
     * Marks a package as failed with a specific status.
     */
    suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
     * Deletes a QA package and all related data.
     */
    suspend fun delete(id: QaPackageId): Boolean

    /**
     * Counts packages by status.
     */
    suspend fun countByStatus(status: QaPackageStatus): Long

    /**
     * Counts all packages.
     */
    suspend fun count(): Long
}

/**
 * Command for creating a QA package.
 */
data class CreateQaPackageCommand(
    val name: String,
    val description: String? = null,
    val specUrl: String? = null,
    val specContent: String? = null,
    val baseUrl: String,
    val requirements: String? = null,
    val triggeredBy: String,
    val config: QaPackageConfig = QaPackageConfig()
)

/**
 * Command for updating a QA package.
 */
data class UpdateQaPackageCommand(
    val name: String? = null,
    val description: String? = null,
    val specUrl: String? = null,
    val specContent: String? = null,
    val baseUrl: String? = null,
    val requirements: String? = null,
    val config: QaPackageConfig? = null
)

/**
 * Page of results.
 */
data class Page<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    val hasNext: Boolean get() = page < totalPages - 1
    val hasPrevious: Boolean get() = page > 0
}
