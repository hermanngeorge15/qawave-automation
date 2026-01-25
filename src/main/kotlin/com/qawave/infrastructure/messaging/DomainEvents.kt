package com.qawave.infrastructure.messaging

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.UUID

/**
 * Base class for all domain events.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = QaPackageCreatedEvent::class, name = "QA_PACKAGE_CREATED"),
    JsonSubTypes.Type(value = QaPackageStatusChangedEvent::class, name = "QA_PACKAGE_STATUS_CHANGED"),
    JsonSubTypes.Type(value = QaPackageCompletedEvent::class, name = "QA_PACKAGE_COMPLETED"),
    JsonSubTypes.Type(value = TestRunStartedEvent::class, name = "TEST_RUN_STARTED"),
    JsonSubTypes.Type(value = TestRunCompletedEvent::class, name = "TEST_RUN_COMPLETED"),
    JsonSubTypes.Type(value = ScenarioGeneratedEvent::class, name = "SCENARIO_GENERATED"),
    JsonSubTypes.Type(value = AiGenerationRequestedEvent::class, name = "AI_GENERATION_REQUESTED"),
    JsonSubTypes.Type(value = AiGenerationCompletedEvent::class, name = "AI_GENERATION_COMPLETED")
)
sealed class DomainEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
    abstract val aggregateId: String
    abstract val aggregateType: String
}

// ==================== QA Package Events ====================

data class QaPackageCreatedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "QaPackage",
    val packageName: String,
    val triggeredBy: String,
    val baseUrl: String,
    val hasSpecUrl: Boolean,
    val hasSpecContent: Boolean
) : DomainEvent()

data class QaPackageStatusChangedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "QaPackage",
    val previousStatus: String,
    val newStatus: String,
    val reason: String? = null
) : DomainEvent()

data class QaPackageCompletedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "QaPackage",
    val verdict: String,
    val totalScenarios: Int,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val durationMs: Long?,
    val coveragePercentage: Double?
) : DomainEvent()

// ==================== Test Run Events ====================

data class TestRunStartedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "TestRun",
    val packageId: String,
    val scenarioId: String,
    val runNumber: Int
) : DomainEvent()

data class TestRunCompletedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "TestRun",
    val packageId: String,
    val scenarioId: String,
    val success: Boolean,
    val durationMs: Long,
    val stepsExecuted: Int,
    val stepsPassed: Int,
    val errorMessage: String? = null
) : DomainEvent()

// ==================== Scenario Events ====================

data class ScenarioGeneratedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "TestScenario",
    val packageId: String,
    val scenarioName: String,
    val stepsCount: Int,
    val priority: String
) : DomainEvent()

// ==================== AI Generation Events ====================

data class AiGenerationRequestedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "QaPackage",
    val provider: String,
    val model: String,
    val promptTokensEstimate: Int
) : DomainEvent()

data class AiGenerationCompletedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "QaPackage",
    val provider: String,
    val model: String,
    val success: Boolean,
    val scenariosGenerated: Int,
    val tokensUsed: Int,
    val durationMs: Long,
    val errorMessage: String? = null
) : DomainEvent()
