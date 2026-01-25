package com.qawave.application.service

import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service interface for Test Execution operations.
 * Provides business logic for running test scenarios and recording results.
 */
interface TestExecutionService {

    /**
     * Starts a new test run for a scenario.
     *
     * @param command The execution command
     * @return The created test run in QUEUED status
     */
    suspend fun startRun(command: StartRunCommand): TestRun

    /**
     * Executes a test run (runs all steps).
     *
     * @param runId The test run ID
     * @param scenario The scenario to execute
     * @return The completed test run with results
     */
    suspend fun executeRun(runId: TestRunId, scenario: TestScenario): TestRun

    /**
     * Executes a single test step.
     *
     * @param runId The test run ID
     * @param step The step to execute
     * @param baseUrl The base URL for the API
     * @param context Execution context with extracted values
     * @return The step result
     */
    suspend fun executeStep(
        runId: TestRunId,
        step: TestStep,
        baseUrl: String,
        context: ExecutionContext
    ): TestStepResult

    /**
     * Finds a test run by its ID.
     *
     * @param id The run ID
     * @return The test run if found, null otherwise
     */
    suspend fun findById(id: TestRunId): TestRun?

    /**
     * Finds a test run by ID with all step results.
     *
     * @param id The run ID
     * @return The test run with step results if found, null otherwise
     */
    suspend fun findByIdWithResults(id: TestRunId): TestRun?

    /**
     * Lists all test runs with pagination.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return A page of test runs
     */
    suspend fun findAll(page: Int = 0, size: Int = 20): Page<TestRun>

    /**
     * Finds test runs for a scenario.
     *
     * @param scenarioId The scenario ID
     * @return List of test runs
     */
    suspend fun findByScenarioId(scenarioId: ScenarioId): List<TestRun>

    /**
     * Finds the latest test run for a scenario.
     *
     * @param scenarioId The scenario ID
     * @return The latest test run if found
     */
    suspend fun findLatestByScenarioId(scenarioId: ScenarioId): TestRun?

    /**
     * Finds test runs for a QA package.
     *
     * @param packageId The QA package ID
     * @return List of test runs
     */
    suspend fun findByPackageId(packageId: QaPackageId): List<TestRun>

    /**
     * Streams test runs for a QA package.
     *
     * @param packageId The QA package ID
     * @return Flow of test runs
     */
    fun findByPackageIdStream(packageId: QaPackageId): Flow<TestRun>

    /**
     * Finds test runs by status.
     *
     * @param status The status to filter by
     * @return List of test runs
     */
    suspend fun findByStatus(status: TestRunStatus): List<TestRun>

    /**
     * Streams test runs by status.
     *
     * @param status The status to filter by
     * @return Flow of test runs
     */
    fun findByStatusStream(status: TestRunStatus): Flow<TestRun>

    /**
     * Finds incomplete (still running) test runs.
     *
     * @return Flow of incomplete test runs
     */
    fun findIncomplete(): Flow<TestRun>

    /**
     * Finds test runs created after a specific time.
     *
     * @param since The cutoff time
     * @return Flow of recent test runs
     */
    fun findRecent(since: Instant): Flow<TestRun>

    /**
     * Updates the status of a test run.
     *
     * @param id The run ID
     * @param status The new status
     * @return The updated test run
     */
    suspend fun updateStatus(id: TestRunId, status: TestRunStatus): TestRun

    /**
     * Marks a test run as completed.
     *
     * @param id The run ID
     * @param status The final status (PASSED, FAILED, or ERROR)
     * @return The updated test run
     */
    suspend fun markCompleted(id: TestRunId, status: TestRunStatus): TestRun

    /**
     * Cancels a running test.
     *
     * @param id The run ID
     * @return The cancelled test run
     */
    suspend fun cancel(id: TestRunId): TestRun

    /**
     * Gets step results for a test run.
     *
     * @param runId The run ID
     * @return List of step results
     */
    suspend fun getStepResults(runId: TestRunId): List<TestStepResult>

    /**
     * Deletes a test run and its results.
     *
     * @param id The run ID
     * @return true if deleted, false if not found
     */
    suspend fun delete(id: TestRunId): Boolean

    /**
     * Counts test runs by status.
     *
     * @param status The status to count
     * @return The count
     */
    suspend fun countByStatus(status: TestRunStatus): Long

    /**
     * Counts test runs for a QA package.
     *
     * @param packageId The QA package ID
     * @return The count
     */
    suspend fun countByPackageId(packageId: QaPackageId): Long

    /**
     * Counts passed runs for a QA package.
     *
     * @param packageId The QA package ID
     * @return The count
     */
    suspend fun countPassedByPackageId(packageId: QaPackageId): Long

    /**
     * Counts failed runs for a QA package.
     *
     * @param packageId The QA package ID
     * @return The count
     */
    suspend fun countFailedByPackageId(packageId: QaPackageId): Long
}

/**
 * Command for starting a new test run.
 */
data class StartRunCommand(
    val scenarioId: ScenarioId,
    val qaPackageId: QaPackageId? = null,
    val triggeredBy: String,
    val baseUrl: String,
    val environment: Map<String, String> = emptyMap()
) {
    init {
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(triggeredBy.isNotBlank()) { "Triggered by cannot be blank" }
    }
}

/**
 * Execution context for test steps.
 * Holds extracted values from previous steps for variable substitution.
 */
data class ExecutionContext(
    val extractedValues: MutableMap<String, String> = mutableMapOf(),
    val environment: Map<String, String> = emptyMap()
) {
    /**
     * Resolves a value, replacing variables with extracted/environment values.
     */
    fun resolve(value: String): String {
        var result = value
        extractedValues.forEach { (key, v) ->
            result = result.replace("\${$key}", v)
        }
        environment.forEach { (key, v) ->
            result = result.replace("\${env.$key}", v)
        }
        return result
    }

    /**
     * Adds extracted values from a step result.
     */
    fun addExtracted(values: Map<String, String>) {
        extractedValues.putAll(values)
    }
}
