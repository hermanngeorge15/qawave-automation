package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.TestRunEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * R2DBC repository for Test Runs.
 * Uses Kotlin coroutines for non-blocking database access.
 */
@Repository
interface TestRunR2dbcRepository : CoroutineCrudRepository<TestRunEntity, UUID> {

    /**
     * Find runs for a specific scenario.
     */
    suspend fun findByScenarioId(scenarioId: UUID): List<TestRunEntity>

    /**
     * Stream runs for a specific scenario.
     */
    fun findAllByScenarioId(scenarioId: UUID): Flow<TestRunEntity>

    /**
     * Find runs belonging to a QA package.
     */
    suspend fun findByQaPackageId(qaPackageId: UUID): List<TestRunEntity>

    /**
     * Stream runs belonging to a QA package.
     */
    fun findAllByQaPackageId(qaPackageId: UUID): Flow<TestRunEntity>

    /**
     * Find runs by status.
     */
    suspend fun findByStatus(status: String): List<TestRunEntity>

    /**
     * Stream runs by status.
     */
    fun findAllByStatus(status: String): Flow<TestRunEntity>

    /**
     * Find the latest run for a scenario.
     */
    @Query("SELECT * FROM test_runs WHERE scenario_id = :scenarioId ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestByScenarioId(scenarioId: UUID): TestRunEntity?

    /**
     * Find runs created after a specific time.
     */
    @Query("SELECT * FROM test_runs WHERE created_at > :since ORDER BY created_at DESC")
    fun findRecentRuns(since: Instant): Flow<TestRunEntity>

    /**
     * Find incomplete runs (not in terminal status).
     */
    @Query("""
        SELECT * FROM test_runs
        WHERE status NOT IN ('PASSED', 'FAILED', 'ERROR', 'CANCELLED')
        ORDER BY created_at DESC
    """)
    fun findIncompleteRuns(): Flow<TestRunEntity>

    /**
     * Count runs by status.
     */
    suspend fun countByStatus(status: String): Long

    /**
     * Count runs for a QA package.
     */
    suspend fun countByQaPackageId(qaPackageId: UUID): Long

    /**
     * Count passed runs for a QA package.
     */
    @Query("SELECT COUNT(*) FROM test_runs WHERE qa_package_id = :qaPackageId AND status = 'PASSED'")
    suspend fun countPassedByQaPackageId(qaPackageId: UUID): Long

    /**
     * Count failed runs for a QA package.
     */
    @Query("SELECT COUNT(*) FROM test_runs WHERE qa_package_id = :qaPackageId AND status = 'FAILED'")
    suspend fun countFailedByQaPackageId(qaPackageId: UUID): Long

    /**
     * Delete all runs for a scenario.
     */
    suspend fun deleteByScenarioId(scenarioId: UUID)

    /**
     * Delete all runs for a QA package.
     */
    suspend fun deleteByQaPackageId(qaPackageId: UUID)
}
