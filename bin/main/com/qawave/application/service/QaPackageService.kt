package com.qawave.application.service

import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service interface for QA Package operations.
<<<<<<< HEAD
 * All methods use suspend functions for non-blocking execution.
 */
interface QaPackageService {
    /**
     * Creates a new QA package.
=======
 * Provides business logic for creating, managing, and executing QA packages.
 */
interface QaPackageService {

    /**
     * Creates a new QA package.
     *
     * @param command The creation command with all required data
     * @return The created QA package
>>>>>>> origin/main
     */
    suspend fun create(command: CreateQaPackageCommand): QaPackage

    /**
<<<<<<< HEAD
     * Finds a QA package by ID.
=======
     * Finds a QA package by its ID.
     *
     * @param id The package ID
     * @return The package if found, null otherwise
>>>>>>> origin/main
     */
    suspend fun findById(id: QaPackageId): QaPackage?

    /**
<<<<<<< HEAD
     * Finds a QA package by ID with scenarios loaded.
=======
     * Finds a QA package by ID with its scenarios loaded.
     *
     * @param id The package ID
     * @return The package with scenarios if found, null otherwise
>>>>>>> origin/main
     */
    suspend fun findByIdWithScenarios(id: QaPackageId): QaPackage?

    /**
<<<<<<< HEAD
     * Finds a QA package by ID with all related data.
=======
     * Finds a QA package by ID with all related data (scenarios, runs, results).
     *
     * @param id The package ID
     * @return The fully loaded package if found, null otherwise
>>>>>>> origin/main
     */
    suspend fun findByIdFull(id: QaPackageId): QaPackage?

    /**
<<<<<<< HEAD
     * Finds all QA packages with pagination.
=======
     * Lists all QA packages with pagination.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return A page of packages
>>>>>>> origin/main
     */
    suspend fun findAll(page: Int = 0, size: Int = 20): Page<QaPackage>

    /**
<<<<<<< HEAD
     * Returns a Flow of all QA packages.
=======
     * Streams all QA packages.
     *
     * @return A Flow of packages
>>>>>>> origin/main
     */
    fun findAllStream(): Flow<QaPackage>

    /**
<<<<<<< HEAD
     * Finds QA packages by status.
=======
     * Finds packages by status.
     *
     * @param status The status to filter by
     * @return List of packages with the given status
>>>>>>> origin/main
     */
    suspend fun findByStatus(status: QaPackageStatus): List<QaPackage>

    /**
<<<<<<< HEAD
     * Finds incomplete (in-progress) packages.
=======
     * Finds incomplete packages (not in terminal status).
     *
     * @return Flow of incomplete packages
>>>>>>> origin/main
     */
    fun findIncomplete(): Flow<QaPackage>

    /**
<<<<<<< HEAD
     * Finds packages created since a given time.
=======
     * Finds packages created after a specific time.
     *
     * @param since The cutoff time
     * @return Flow of recent packages
>>>>>>> origin/main
     */
    fun findRecent(since: Instant): Flow<QaPackage>

    /**
     * Updates the status of a QA package.
<<<<<<< HEAD
=======
     *
     * @param id The package ID
     * @param status The new status
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun updateStatus(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
     * Updates a QA package.
<<<<<<< HEAD
=======
     *
     * @param id The package ID
     * @param command The update command
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun update(id: QaPackageId, command: UpdateQaPackageCommand): QaPackage

    /**
<<<<<<< HEAD
     * Sets the coverage report for a package.
=======
     * Sets the coverage report for a QA package.
     *
     * @param id The package ID
     * @param coverage The coverage report
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun setCoverage(id: QaPackageId, coverage: CoverageReport): QaPackage

    /**
     * Sets the QA summary for a package.
<<<<<<< HEAD
=======
     *
     * @param id The package ID
     * @param summary The QA summary
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun setQaSummary(id: QaPackageId, summary: QaSummary): QaPackage

    /**
     * Marks a package as started.
<<<<<<< HEAD
=======
     *
     * @param id The package ID
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun markStarted(id: QaPackageId): QaPackage

    /**
     * Marks a package as completed.
<<<<<<< HEAD
=======
     *
     * @param id The package ID
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun markCompleted(id: QaPackageId): QaPackage

    /**
<<<<<<< HEAD
     * Marks a package as failed with a specific status.
=======
     * Marks a package as failed.
     *
     * @param id The package ID
     * @param status The failure status
     * @return The updated package
>>>>>>> origin/main
     */
    suspend fun markFailed(id: QaPackageId, status: QaPackageStatus): QaPackage

    /**
<<<<<<< HEAD
     * Deletes a QA package and all related data.
=======
     * Deletes a QA package (soft delete).
     *
     * @param id The package ID
     * @return true if deleted, false if not found
>>>>>>> origin/main
     */
    suspend fun delete(id: QaPackageId): Boolean

    /**
     * Counts packages by status.
<<<<<<< HEAD
=======
     *
     * @param status The status to count
     * @return The count
>>>>>>> origin/main
     */
    suspend fun countByStatus(status: QaPackageStatus): Long

    /**
     * Counts all packages.
<<<<<<< HEAD
=======
     *
     * @return The total count
>>>>>>> origin/main
     */
    suspend fun count(): Long
}

/**
<<<<<<< HEAD
 * Command for creating a QA package.
=======
 * Command for creating a new QA package.
>>>>>>> origin/main
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
<<<<<<< HEAD
)
=======
) {
    init {
        require(name.isNotBlank()) { "Package name cannot be blank" }
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(specUrl != null || specContent != null) { "Either specUrl or specContent must be provided" }
    }
}
>>>>>>> origin/main

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
<<<<<<< HEAD
 * Page of results.
=======
 * Represents a page of results.
>>>>>>> origin/main
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
