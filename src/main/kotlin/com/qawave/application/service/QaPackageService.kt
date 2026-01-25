package com.qawave.application.service

import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service interface for QA Package operations.
 * Provides business logic for creating, managing, and executing QA packages.
 */
interface QaPackageService {

    /**
     * Creates a new QA package.
     *
     * @param command The creation command with all required data
     * @return The created QA package
     */
    suspend fun create(command: CreateQaPackageCommand): QaPackage

    /**
     * Finds a QA package by its ID.
     *
     * @param id The package ID
     * @return The package if found, null otherwise
     */
    suspend fun findById(id: QaPackageId): QaPackage?

    /**
     * Finds a QA package by ID with its scenarios loaded.
     *
     * @param id The package ID
     * @return The package with scenarios if found, null otherwise
     */
    suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage?

    /**
     * Finds a QA package by ID with all related data (scenarios, runs, results).
     *
     * @param id The package ID
     * @return The fully loaded package if found, null otherwise
     */
    suspend fun findByIdFull(id: QaPackageId): QaPackage?

    /**
     * Lists all QA packages with pagination.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return A page of packages
     */
    suspend fun findAll(page: Int = 0, size: Int = 20): Page<QaPackage>

    /**
     * Streams all QA packages.
     *
     * @return A Flow of packages
     */
    fun findAllStream(): Flow<QaPackage>

    /**
     * Finds packages by status.
     *
     * @param status The status to filter by
     * @return List of packages with the given status
     */
    suspend fun findByStatus(status: QaPackageStatus): List<QaPackage>

    /**
     * Finds incomplete packages (not in terminal status).
     *
     * @return Flow of incomplete packages
     */
    fun findIncomplete(): Flow<QaPackage>

    /**
     * Finds packages created after a specific time.
     *
     * @param since The cutoff time
     * @return Flow of recent packages
     */
    fun findRecent(since: Instant): Flow<QaPackage>

    /**
     * Updates the status of a QA package.
     *
     * @param id The package ID
     * @param status The new status
     * @return The updated package
     */
    suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
     * Updates a QA package.
     *
     * @param id The package ID
     * @param command The update command
     * @return The updated package
     */
    suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage

    /**
     * Sets the coverage report for a QA package.
     *
     * @param id The package ID
     * @param coverage The coverage report
     * @return The updated package
     */
    suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage

    /**
     * Sets the QA summary for a package.
     *
     * @param id The package ID
     * @param summary The QA summary
     * @return The updated package
     */
    suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage

    /**
     * Marks a package as started.
     *
     * @param id The package ID
     * @return The updated package
     */
    suspend fun markStarted(id: QaPackageId): QaPackage

    /**
     * Marks a package as completed.
     *
     * @param id The package ID
     * @return The updated package
     */
    suspend fun markCompleted(id: QaPackageId): QaPackage

    /**
     * Marks a package as failed.
     *
     * @param id The package ID
     * @param status The failure status
     * @return The updated package
     */
    suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
     * Deletes a QA package (soft delete).
     *
     * @param id The package ID
     * @return true if deleted, false if not found
     */
    suspend fun delete(id: QaPackageId): Boolean

    /**
     * Counts packages by status.
     *
     * @param status The status to count
     * @return The count
     */
    suspend fun countByStatus(status: QaPackageStatus): Long

    /**
     * Counts all packages.
     *
     * @return The total count
     */
    suspend fun count(): Long
}

/**
 * Command for creating a new QA package.
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
) {
    init {
        require(name.isNotBlank()) { "Package name cannot be blank" }
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(specUrl != null || specContent != null) { "Either specUrl or specContent must be provided" }
    }
}

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
 * Represents a page of results.
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
