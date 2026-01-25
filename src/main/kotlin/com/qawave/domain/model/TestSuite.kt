package com.qawave.domain.model

import java.time.Instant

/**
 * Represents a test suite - a logical grouping of test scenarios.
 * Suites provide organization and allow batch execution of related scenarios.
 */
data class TestSuite(
    val id: TestSuiteId,
    val name: String,
    val description: String?,
    val requirementId: RequirementId?,
    val apiSpecId: ApiSpecId?,
    val defaultBaseUrl: String?,
    val tags: Set<String> = emptySet(),
    val scenarios: List<TestScenario> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Suite name cannot be blank" }
    }

    /**
     * Total number of scenarios in this suite.
     */
    val scenarioCount: Int
        get() = scenarios.size

    /**
     * Total number of steps across all scenarios.
     */
    val totalSteps: Int
        get() = scenarios.sumOf { it.stepCount }

    /**
     * All unique tags from scenarios in this suite.
     */
    val allTags: Set<String>
        get() = tags + scenarios.flatMap { it.tags }.toSet()

    /**
     * All operations covered by scenarios in this suite.
     */
    val coveredOperations: Set<String>
        get() = scenarios.flatMap { it.coveredOperations }.toSet()
}
