package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.TestStepResultEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * R2DBC repository for Test Step Results.
 * Uses Kotlin coroutines for non-blocking database access.
 */
@Repository
interface TestStepResultR2dbcRepository : CoroutineCrudRepository<TestStepResultEntity, UUID> {
    /**
     * Find all step results for a test run.
     */
    suspend fun findByRunId(runId: UUID): List<TestStepResultEntity>

    /**
     * Stream all step results for a test run.
     */
    fun findAllByRunId(runId: UUID): Flow<TestStepResultEntity>

    /**
     * Find step results ordered by step index.
     */
    @Query("SELECT * FROM test_step_results WHERE run_id = :runId ORDER BY step_index ASC")
    suspend fun findByRunIdOrderByStepIndex(runId: UUID): List<TestStepResultEntity>

    /**
     * Find a specific step result by run ID and step index.
     */
    suspend fun findByRunIdAndStepIndex(
        runId: UUID,
        stepIndex: Int,
    ): TestStepResultEntity?

    /**
     * Find passed step results for a run.
     */
    suspend fun findByRunIdAndPassed(
        runId: UUID,
        passed: Boolean,
    ): List<TestStepResultEntity>

    /**
     * Count step results by run ID.
     */
    suspend fun countByRunId(runId: UUID): Long

    /**
     * Count passed step results for a run.
     */
    @Query("SELECT COUNT(*) FROM test_step_results WHERE run_id = :runId AND passed = true")
    suspend fun countPassedByRunId(runId: UUID): Long

    /**
     * Count failed step results for a run.
     */
    @Query("SELECT COUNT(*) FROM test_step_results WHERE run_id = :runId AND passed = false")
    suspend fun countFailedByRunId(runId: UUID): Long

    /**
     * Find step results with errors.
     */
    @Query("SELECT * FROM test_step_results WHERE run_id = :runId AND error_message IS NOT NULL")
    suspend fun findWithErrorsByRunId(runId: UUID): List<TestStepResultEntity>

    /**
     * Delete all step results for a test run.
     */
    suspend fun deleteByRunId(runId: UUID)

    /**
     * Get average duration for passed steps in a run.
     */
    @Query("SELECT AVG(duration_ms) FROM test_step_results WHERE run_id = :runId AND passed = true")
    suspend fun getAverageDurationForPassedSteps(runId: UUID): Double?

    /**
     * Get max duration step in a run.
     */
    @Query("SELECT * FROM test_step_results WHERE run_id = :runId ORDER BY duration_ms DESC LIMIT 1")
    suspend fun findSlowestStepByRunId(runId: UUID): TestStepResultEntity?

    /**
     * Find all step results with pagination.
     */
    fun findAllBy(pageable: Pageable): Flow<TestStepResultEntity>

    /**
     * Find step results by run ID with pagination.
     */
    fun findByRunId(runId: UUID, pageable: Pageable): Flow<TestStepResultEntity>

    /**
     * Find step results by run ID ordered by step index with pagination.
     */
    @Query("SELECT * FROM test_step_results WHERE run_id = :runId ORDER BY step_index ASC LIMIT :limit OFFSET :offset")
    fun findByRunIdOrderByStepIndex(runId: UUID, limit: Int, offset: Int): Flow<TestStepResultEntity>
}
