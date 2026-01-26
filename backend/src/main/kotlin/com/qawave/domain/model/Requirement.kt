package com.qawave.domain.model

import java.time.Instant

/**
 * Represents a business requirement that drives test scenario generation.
 * Requirements can be linked to external systems (Jira, Linear, etc.).
 */
data class Requirement(
    val id: RequirementId,
    val title: String,
    val description: String,
    val externalReference: ExternalReference? = null,
    val priority: RequirementPriority = RequirementPriority.MEDIUM,
    val status: RequirementStatus = RequirementStatus.ACTIVE,
    val acceptanceCriteria: List<AcceptanceCriterion> = emptyList(),
    val userFlows: List<UserFlow> = emptyList(),
    val tags: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(title.isNotBlank()) { "Requirement title cannot be blank" }
        require(description.isNotBlank()) { "Requirement description cannot be blank" }
    }

    /**
     * Total number of acceptance criteria.
     */
    val criteriaCount: Int
        get() = acceptanceCriteria.size

    /**
     * Total number of user flows.
     */
    val flowCount: Int
        get() = userFlows.size

    /**
     * Whether this requirement has any acceptance criteria.
     */
    val hasCriteria: Boolean
        get() = acceptanceCriteria.isNotEmpty()

    /**
     * Whether this requirement has any user flows defined.
     */
    val hasFlows: Boolean
        get() = userFlows.isNotEmpty()
}

/**
 * Reference to an external system (Jira, Linear, etc.).
 */
data class ExternalReference(
    val system: ExternalSystem,
    val id: String,
    val url: String? = null,
)

/**
 * Supported external systems for requirement tracking.
 */
enum class ExternalSystem {
    JIRA,
    LINEAR,
    GITHUB,
    GITLAB,
    AZURE_DEVOPS,
    NOTION,
    OTHER,
}

/**
 * Priority of a requirement.
 */
enum class RequirementPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Status of a requirement.
 */
enum class RequirementStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED,
}

/**
 * Represents an acceptance criterion for a requirement.
 */
data class AcceptanceCriterion(
    val id: String,
    val description: String,
    val testable: Boolean = true,
)

/**
 * Represents a user flow extracted from requirements.
 * User flows are used by the AI to generate test scenarios.
 */
data class UserFlow(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<UserFlowStep> = emptyList(),
    val preconditions: List<String> = emptyList(),
    val expectedOutcomes: List<String> = emptyList(),
)

/**
 * A single step in a user flow.
 */
data class UserFlowStep(
    val index: Int,
    val action: String,
    val expectedResult: String? = null,
)
