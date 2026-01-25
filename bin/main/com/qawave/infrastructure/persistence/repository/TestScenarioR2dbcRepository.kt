package com.qawave.infrastructure.persistence.repository

import com.qawave.infrastructure.persistence.entity.TestScenarioEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * R2DBC repository for Test Scenarios.
 * Uses Kotlin coroutines for non-blocking database access.
 */
@Repository
interface TestScenarioR2dbcRepository : CoroutineCrudRepository<TestScenarioEntity, UUID> {

    /**
     * Find scenarios belonging to a QA package.
     */
    suspend fun findByQaPackageId(qaPackageId: UUID): List<TestScenarioEntity>

    /**
     * Stream scenarios belonging to a QA package.
     */
    fun findAllByQaPackageId(qaPackageId: UUID): Flow<TestScenarioEntity>

    /**
     * Find scenarios belonging to a test suite.
     */
    suspend fun findBySuiteId(suiteId: UUID): List<TestScenarioEntity>

    /**
     * Find scenarios by status.
     */
    suspend fun findByStatus(status: String): List<TestScenarioEntity>

    /**
     * Stream scenarios by status.
     */
    fun findAllByStatus(status: String): Flow<TestScenarioEntity>

    /**
     * Find scenarios created after a specific time.
     */
    @Query("SELECT * FROM test_scenarios WHERE created_at > :since ORDER BY created_at DESC")
    fun findRecentScenarios(since: Instant): Flow<TestScenarioEntity>

    /**
     * Find scenarios by source type.
     */
    suspend fun findBySource(source: String): List<TestScenarioEntity>

    /**
     * Count scenarios by QA package.
     */
    suspend fun countByQaPackageId(qaPackageId: UUID): Long

    /**
     * Count scenarios by status.
     */
    suspend fun countByStatus(status: String): Long

    /**
     * Find scenarios with specific tags (JSON contains).
     */
    @Query("SELECT * FROM test_scenarios WHERE tags::jsonb @> :tag::jsonb")
    fun findByTag(tag: String): Flow<TestScenarioEntity>

    /**
     * Delete all scenarios for a QA package.
     */
    suspend fun deleteByQaPackageId(qaPackageId: UUID)
}
