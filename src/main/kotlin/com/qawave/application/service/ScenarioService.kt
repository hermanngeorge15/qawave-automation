package com.qawave.application.service

import com.qawave.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service interface for Test Scenario operations.
 * Provides business logic for creating, managing, and querying test scenarios.
 */
interface ScenarioService {
    /**
     * Creates a new test scenario.
     *
     * @param command The creation command with all required data
     * @return The created scenario
     */
    suspend fun create(command: CreateScenarioCommand): TestScenario

    /**
     * Creates multiple scenarios in batch.
     *
     * @param commands List of creation commands
     * @return List of created scenarios
     */
    suspend fun createBatch(commands: List<CreateScenarioCommand>): List<TestScenario>

    /**
     * Finds a scenario by its ID.
     *
     * @param id The scenario ID
     * @return The scenario if found, null otherwise
     */
    suspend fun findById(id: ScenarioId): TestScenario?

    /**
     * Lists all scenarios with pagination.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return A page of scenarios
     */
    suspend fun findAll(
        page: Int = 0,
        size: Int = 20,
    ): Page<TestScenario>

    /**
     * Streams all scenarios.
     *
     * @return A Flow of scenarios
     */
    fun findAllStream(): Flow<TestScenario>

    /**
     * Finds scenarios belonging to a QA package.
     *
     * @param packageId The QA package ID
     * @return List of scenarios for the package
     */
    suspend fun findByPackageId(packageId: QaPackageId): List<TestScenario>

    /**
     * Streams scenarios belonging to a QA package.
     *
     * @param packageId The QA package ID
     * @return Flow of scenarios for the package
     */
    fun findByPackageIdStream(packageId: QaPackageId): Flow<TestScenario>

    /**
     * Finds scenarios belonging to a test suite.
     *
     * @param suiteId The test suite ID
     * @return List of scenarios for the suite
     */
    suspend fun findBySuiteId(suiteId: TestSuiteId): List<TestScenario>

    /**
     * Finds scenarios by status.
     *
     * @param status The status to filter by
     * @return List of scenarios with the given status
     */
    suspend fun findByStatus(status: ScenarioStatus): List<TestScenario>

    /**
     * Streams scenarios by status.
     *
     * @param status The status to filter by
     * @return Flow of scenarios with the given status
     */
    fun findByStatusStream(status: ScenarioStatus): Flow<TestScenario>

    /**
     * Finds scenarios by source type.
     *
     * @param source The source type
     * @return List of scenarios from the given source
     */
    suspend fun findBySource(source: ScenarioSource): List<TestScenario>

    /**
     * Finds scenarios created after a specific time.
     *
     * @param since The cutoff time
     * @return Flow of recent scenarios
     */
    fun findRecent(since: Instant): Flow<TestScenario>

    /**
     * Finds scenarios by tag.
     *
     * @param tag The tag to search for
     * @return Flow of scenarios with the tag
     */
    fun findByTag(tag: String): Flow<TestScenario>

    /**
     * Updates a scenario.
     *
     * @param id The scenario ID
     * @param command The update command
     * @return The updated scenario
     */
    suspend fun update(
        id: ScenarioId,
        command: UpdateScenarioCommand,
    ): TestScenario

    /**
     * Updates the status of a scenario.
     *
     * @param id The scenario ID
     * @param status The new status
     * @return The updated scenario
     */
    suspend fun updateStatus(
        id: ScenarioId,
        status: ScenarioStatus,
    ): TestScenario

    /**
     * Marks a scenario as ready for execution.
     *
     * @param id The scenario ID
     * @return The updated scenario
     */
    suspend fun markReady(id: ScenarioId): TestScenario

    /**
     * Marks a scenario as invalid.
     *
     * @param id The scenario ID
     * @return The updated scenario
     */
    suspend fun markInvalid(id: ScenarioId): TestScenario

    /**
     * Disables a scenario.
     *
     * @param id The scenario ID
     * @return The updated scenario
     */
    suspend fun disable(id: ScenarioId): TestScenario

    /**
     * Enables a disabled scenario.
     *
     * @param id The scenario ID
     * @return The updated scenario
     */
    suspend fun enable(id: ScenarioId): TestScenario

    /**
     * Deletes a scenario.
     *
     * @param id The scenario ID
     * @return true if deleted, false if not found
     */
    suspend fun delete(id: ScenarioId): Boolean

    /**
     * Deletes all scenarios for a QA package.
     *
     * @param packageId The QA package ID
     */
    suspend fun deleteByPackageId(packageId: QaPackageId)

    /**
     * Counts scenarios by QA package.
     *
     * @param packageId The QA package ID
     * @return The count
     */
    suspend fun countByPackageId(packageId: QaPackageId): Long

    /**
     * Counts scenarios by status.
     *
     * @param status The status to count
     * @return The count
     */
    suspend fun countByStatus(status: ScenarioStatus): Long

    /**
     * Counts all scenarios.
     *
     * @return The total count
     */
    suspend fun count(): Long
}

/**
 * Command for creating a new test scenario.
 */
data class CreateScenarioCommand(
    val qaPackageId: QaPackageId? = null,
    val suiteId: TestSuiteId? = null,
    val name: String,
    val description: String? = null,
    val steps: List<TestStep>,
    val tags: Set<String> = emptySet(),
    val source: ScenarioSource,
    val status: ScenarioStatus = ScenarioStatus.PENDING,
) {
    init {
        require(name.isNotBlank()) { "Scenario name cannot be blank" }
        require(steps.isNotEmpty()) { "Scenario must have at least one step" }
        require(steps.map { it.index }.toSet().size == steps.size) { "Step indices must be unique" }
    }
}

/**
 * Command for updating a test scenario.
 */
data class UpdateScenarioCommand(
    val name: String? = null,
    val description: String? = null,
    val steps: List<TestStep>? = null,
    val tags: Set<String>? = null,
    val status: ScenarioStatus? = null,
) {
    init {
        steps?.let {
            require(it.isNotEmpty()) { "Scenario must have at least one step" }
            require(it.map { step -> step.index }.toSet().size == it.size) { "Step indices must be unique" }
        }
    }
}
