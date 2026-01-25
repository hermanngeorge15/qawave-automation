package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.QaPackageEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * R2DBC repository for QA Packages.
 * Uses Kotlin coroutines for non-blocking database access.
 */
@Repository
interface QaPackageR2dbcRepository : CoroutineCrudRepository<QaPackageEntity, UUID> {
    /**
     * Find all packages with a specific status.
     */
    suspend fun findByStatus(status: String): List<QaPackageEntity>

    /**
     * Stream packages with a specific status.
     */
    fun findAllByStatus(status: String): Flow<QaPackageEntity>

    /**
     * Find packages triggered by a specific user.
     */
    suspend fun findByTriggeredBy(triggeredBy: String): List<QaPackageEntity>

    /**
     * Find packages created after a specific time.
     */
    @Query("SELECT * FROM qa_packages WHERE created_at > :since ORDER BY created_at DESC")
    fun findRecentPackages(since: Instant): Flow<QaPackageEntity>

    /**
     * Find packages by spec hash for deduplication.
     */
    suspend fun findBySpecHash(specHash: String): QaPackageEntity?

    /**
     * Find incomplete packages (not in terminal status).
     */
    @Query(
        """
        SELECT * FROM qa_packages
        WHERE status NOT IN ('COMPLETE', 'FAILED_SPEC_FETCH', 'FAILED_GENERATION', 'FAILED_EXECUTION', 'CANCELLED')
        ORDER BY created_at DESC
    """,
    )
    fun findIncompletePackages(): Flow<QaPackageEntity>

    /**
     * Count packages by status.
     */
    suspend fun countByStatus(status: String): Long

    /**
     * Find all packages with pagination.
     */
    fun findAllBy(pageable: Pageable): Flow<QaPackageEntity>

    /**
     * Find packages by status with pagination.
     */
    fun findByStatus(status: String, pageable: Pageable): Flow<QaPackageEntity>

    /**
     * Find packages triggered by a user with pagination.
     */
    fun findByTriggeredBy(triggeredBy: String, pageable: Pageable): Flow<QaPackageEntity>

    /**
     * Find all packages ordered by created_at descending with pagination.
     */
    @Query("SELECT * FROM qa_packages ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllOrderByCreatedAtDesc(limit: Int, offset: Int): Flow<QaPackageEntity>
}
