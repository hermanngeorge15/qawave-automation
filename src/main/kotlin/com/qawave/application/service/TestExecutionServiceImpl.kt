package com.qawave.application.service

import com.qawave.domain.model.*
import com.qawave.infrastructure.http.HttpStepExecutor
import com.qawave.infrastructure.persistence.mapper.TestRunMapper
import com.qawave.infrastructure.persistence.mapper.TestStepResultMapper
import com.qawave.infrastructure.persistence.repository.TestRunR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestStepResultR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Implementation of TestExecutionService.
 */
@Service
@Transactional
class TestExecutionServiceImpl(
    private val runRepository: TestRunR2dbcRepository,
    private val stepResultRepository: TestStepResultR2dbcRepository,
    private val runMapper: TestRunMapper,
    private val stepResultMapper: TestStepResultMapper,
    private val httpStepExecutor: HttpStepExecutor,
) : TestExecutionService {
    private val logger = LoggerFactory.getLogger(TestExecutionServiceImpl::class.java)

    override suspend fun startRun(command: StartRunCommand): TestRun {
        logger.debug("Starting test run for scenario: {}", command.scenarioId.value)

        val now = Instant.now()
        val run =
            TestRun(
                id = TestRunId.generate(),
                scenarioId = command.scenarioId,
                qaPackageId = command.qaPackageId,
                triggeredBy = command.triggeredBy,
                baseUrl = command.baseUrl,
                status = TestRunStatus.QUEUED,
                stepResults = emptyList(),
                environment = command.environment,
                startedAt = now,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
            )

        val entity = runMapper.toEntityWithId(run, run.id.value)
        val savedEntity = runRepository.save(entity)

        logger.info("Created test run: {} for scenario: {}", run.id.value, command.scenarioId.value)
        return runMapper.toDomain(savedEntity)
    }

    override suspend fun executeRun(
        runId: TestRunId,
        scenario: TestScenario,
    ): TestRun {
        logger.info("Executing test run: {} with {} steps", runId.value, scenario.stepCount)

        // Update status to RUNNING
        updateStatus(runId, TestRunStatus.RUNNING)

        val run = findById(runId) ?: throw TestRunNotFoundException(runId)
        val context =
            ExecutionContext(
                extractedValues = mutableMapOf(),
                environment = run.environment,
            )

        val stepResults = mutableListOf<TestStepResult>()
        var hasError = false
        var hasFailed = false

        for (step in scenario.orderedSteps) {
            try {
                val result = executeStep(runId, step, run.baseUrl, context)
                stepResults.add(result)

                // Add extracted values to context for next steps
                context.addExtracted(result.extractedValues)

                if (!result.passed) {
                    if (result.hasError) {
                        hasError = true
                    } else {
                        hasFailed = true
                    }
                }

                logger.debug("Step {} completed: passed={}", step.index, result.passed)
            } catch (e: Exception) {
                logger.error("Error executing step {}: {}", step.index, e.message)
                hasError = true

                stepResults.add(
                    TestStepResult.error(
                        runId = runId,
                        step = step,
                        error = e,
                        durationMs = 0,
                    ),
                )
            }
        }

        // Determine final status
        val finalStatus =
            when {
                hasError -> TestRunStatus.ERROR
                hasFailed -> TestRunStatus.FAILED
                else -> TestRunStatus.PASSED
            }

        // Mark run as completed
        val completedRun = markCompleted(runId, finalStatus)

        logger.info(
            "Test run {} completed with status: {} ({} passed, {} failed)",
            runId.value,
            finalStatus,
            stepResults.count { it.passed },
            stepResults.count { !it.passed },
        )

        return completedRun.copy(stepResults = stepResults)
    }

    override suspend fun executeStep(
        runId: TestRunId,
        step: TestStep,
        baseUrl: String,
        context: ExecutionContext,
    ): TestStepResult {
        logger.debug("Executing step {}: {} {}", step.index, step.method, step.endpoint)

        val result = httpStepExecutor.execute(runId, step, baseUrl, context)

        // Save result to database
        val entity = stepResultMapper.toEntity(result)
        stepResultRepository.save(entity)

        return result
    }

    override suspend fun findById(id: TestRunId): TestRun? {
        logger.debug("Finding test run by id: {}", id.value)
        return runRepository.findById(id.value)?.let { runMapper.toDomain(it) }
    }

    override suspend fun findByIdWithResults(id: TestRunId): TestRun? {
        logger.debug("Finding test run with results by id: {}", id.value)
        val runEntity = runRepository.findById(id.value) ?: return null
        val stepResults =
            stepResultRepository.findByRunIdOrderByStepIndex(id.value)
                .map { stepResultMapper.toDomain(it) }
        return runMapper.toDomain(runEntity, stepResults)
    }

    override suspend fun findAll(
        page: Int,
        size: Int,
    ): Page<TestRun> {
        logger.debug("Finding all test runs, page: {}, size: {}", page, size)

        val offset = page * size
        val entities = runRepository.findAll().toList()
        val totalElements = entities.size.toLong()
        val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0

        val pagedEntities =
            entities
                .drop(offset)
                .take(size)
                .map { runMapper.toDomain(it) }

        return Page(
            content = pagedEntities,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    override suspend fun findByScenarioId(scenarioId: ScenarioId): List<TestRun> {
        logger.debug("Finding test runs by scenario id: {}", scenarioId.value)
        return runRepository.findByScenarioId(scenarioId.value).map { runMapper.toDomain(it) }
    }

    override suspend fun findLatestByScenarioId(scenarioId: ScenarioId): TestRun? {
        logger.debug("Finding latest test run by scenario id: {}", scenarioId.value)
        return runRepository.findLatestByScenarioId(scenarioId.value)?.let { runMapper.toDomain(it) }
    }

    override suspend fun findByPackageId(packageId: QaPackageId): List<TestRun> {
        logger.debug("Finding test runs by package id: {}", packageId.value)
        return runRepository.findByQaPackageId(packageId.value).map { runMapper.toDomain(it) }
    }

    override fun findByPackageIdStream(packageId: QaPackageId): Flow<TestRun> {
        logger.debug("Streaming test runs by package id: {}", packageId.value)
        return runRepository.findAllByQaPackageId(packageId.value).map { runMapper.toDomain(it) }
    }

    override suspend fun findByStatus(status: TestRunStatus): List<TestRun> {
        logger.debug("Finding test runs by status: {}", status)
        return runRepository.findByStatus(status.name).map { runMapper.toDomain(it) }
    }

    override fun findByStatusStream(status: TestRunStatus): Flow<TestRun> {
        logger.debug("Streaming test runs by status: {}", status)
        return runRepository.findAllByStatus(status.name).map { runMapper.toDomain(it) }
    }

    override fun findIncomplete(): Flow<TestRun> {
        logger.debug("Finding incomplete test runs")
        return runRepository.findIncompleteRuns().map { runMapper.toDomain(it) }
    }

    override fun findRecent(since: Instant): Flow<TestRun> {
        logger.debug("Finding test runs created since: {}", since)
        return runRepository.findRecentRuns(since).map { runMapper.toDomain(it) }
    }

    override suspend fun updateStatus(
        id: TestRunId,
        status: TestRunStatus,
    ): TestRun {
        logger.debug("Updating test run status: {} to {}", id.value, status)

        val existing =
            runRepository.findById(id.value)
                ?: throw TestRunNotFoundException(id)

        val updated =
            existing.copy(
                status = status.name,
                updatedAt = Instant.now(),
            )

        val savedEntity = runRepository.save(updated)
        return runMapper.toDomain(savedEntity)
    }

    override suspend fun markCompleted(
        id: TestRunId,
        status: TestRunStatus,
    ): TestRun {
        logger.debug("Marking test run as completed: {} with status: {}", id.value, status)

        require(status in listOf(TestRunStatus.PASSED, TestRunStatus.FAILED, TestRunStatus.ERROR)) {
            "Completion status must be PASSED, FAILED, or ERROR"
        }

        val existing =
            runRepository.findById(id.value)
                ?: throw TestRunNotFoundException(id)

        val updated =
            existing.copy(
                status = status.name,
                completedAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val savedEntity = runRepository.save(updated)
        logger.info("Test run {} marked as {}", id.value, status)
        return runMapper.toDomain(savedEntity)
    }

    override suspend fun cancel(id: TestRunId): TestRun {
        logger.debug("Cancelling test run: {}", id.value)

        val existing =
            runRepository.findById(id.value)
                ?: throw TestRunNotFoundException(id)

        check(existing.status !in listOf("PASSED", "FAILED", "ERROR", "CANCELLED")) {
            "Cannot cancel a completed test run"
        }

        val updated =
            existing.copy(
                status = TestRunStatus.CANCELLED.name,
                completedAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val savedEntity = runRepository.save(updated)
        logger.info("Test run {} cancelled", id.value)
        return runMapper.toDomain(savedEntity)
    }

    override suspend fun getStepResults(runId: TestRunId): List<TestStepResult> {
        logger.debug("Getting step results for test run: {}", runId.value)
        return stepResultRepository.findByRunIdOrderByStepIndex(runId.value)
            .map { stepResultMapper.toDomain(it) }
    }

    override suspend fun delete(id: TestRunId): Boolean {
        logger.debug("Deleting test run: {}", id.value)

        val exists = runRepository.existsById(id.value)
        if (exists) {
            stepResultRepository.deleteByRunId(id.value)
            runRepository.deleteById(id.value)
            logger.info("Deleted test run: {}", id.value)
        }
        return exists
    }

    override suspend fun countByStatus(status: TestRunStatus): Long {
        return runRepository.countByStatus(status.name)
    }

    override suspend fun countByPackageId(packageId: QaPackageId): Long {
        return runRepository.countByQaPackageId(packageId.value)
    }

    override suspend fun countPassedByPackageId(packageId: QaPackageId): Long {
        return runRepository.countPassedByQaPackageId(packageId.value)
    }

    override suspend fun countFailedByPackageId(packageId: QaPackageId): Long {
        return runRepository.countFailedByQaPackageId(packageId.value)
    }
}

/**
 * Exception thrown when a test run is not found.
 */
class TestRunNotFoundException(id: TestRunId) :
    RuntimeException("Test run not found: ${id.value}")
