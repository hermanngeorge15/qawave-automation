package com.qawave.domain.model

import java.util.UUID

/**
 * Value class for QA Package identifier.
 * Provides type safety to prevent mixing with other UUID types.
 */
@JvmInline
value class QaPackageId(val value: UUID) {
    companion object {
        fun generate(): QaPackageId = QaPackageId(UUID.randomUUID())

        fun from(string: String): QaPackageId = QaPackageId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Test Scenario identifier.
 */
@JvmInline
value class ScenarioId(val value: UUID) {
    companion object {
        fun generate(): ScenarioId = ScenarioId(UUID.randomUUID())

        fun from(string: String): ScenarioId = ScenarioId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Test Run identifier.
 */
@JvmInline
value class TestRunId(val value: UUID) {
    companion object {
        fun generate(): TestRunId = TestRunId(UUID.randomUUID())

        fun from(string: String): TestRunId = TestRunId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Test Suite identifier.
 */
@JvmInline
value class TestSuiteId(val value: UUID) {
    companion object {
        fun generate(): TestSuiteId = TestSuiteId(UUID.randomUUID())

        fun from(string: String): TestSuiteId = TestSuiteId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for API Specification identifier.
 */
@JvmInline
value class ApiSpecId(val value: UUID) {
    companion object {
        fun generate(): ApiSpecId = ApiSpecId(UUID.randomUUID())

        fun from(string: String): ApiSpecId = ApiSpecId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Requirement identifier.
 */
@JvmInline
value class RequirementId(val value: UUID) {
    companion object {
        fun generate(): RequirementId = RequirementId(UUID.randomUUID())

        fun from(string: String): RequirementId = RequirementId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Share Token identifier.
 */
@JvmInline
value class ShareTokenId(val value: UUID) {
    companion object {
        fun generate(): ShareTokenId = ShareTokenId(UUID.randomUUID())

        fun from(string: String): ShareTokenId = ShareTokenId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Webhook Configuration identifier.
 */
@JvmInline
value class WebhookId(val value: UUID) {
    companion object {
        fun generate(): WebhookId = WebhookId(UUID.randomUUID())

        fun from(string: String): WebhookId = WebhookId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class for Webhook Delivery identifier.
 */
@JvmInline
value class WebhookDeliveryId(val value: UUID) {
    companion object {
        fun generate(): WebhookDeliveryId = WebhookDeliveryId(UUID.randomUUID())

        fun from(string: String): WebhookDeliveryId = WebhookDeliveryId(UUID.fromString(string))
    }

    override fun toString(): String = value.toString()
}
