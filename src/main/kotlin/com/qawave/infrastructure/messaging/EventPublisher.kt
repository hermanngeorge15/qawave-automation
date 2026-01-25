package com.qawave.infrastructure.messaging

import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/**
 * Service for publishing domain events to Kafka.
 * Provides type-safe event publishing with automatic topic routing.
 */
@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    /**
     * Publishes a domain event to the appropriate Kafka topic.
     * The topic is determined by the event type.
     */
    suspend fun publish(event: DomainEvent) {
        val topic = getTopicForEvent(event)
        val key = event.aggregateId

        try {
            logger.debug("Publishing event to topic '{}': eventId={}, aggregateId={}",
                topic, event.eventId, event.aggregateId)

            kafkaTemplate.send(topic, key, event).await()

            logger.info("Published event: type={}, eventId={}, aggregateId={}",
                event::class.simpleName, event.eventId, event.aggregateId)
        } catch (e: Exception) {
            logger.error("Failed to publish event: type={}, eventId={}, error={}",
                event::class.simpleName, event.eventId, e.message)
            throw EventPublishException("Failed to publish event ${event.eventId}", e)
        }
    }

    /**
     * Publishes multiple events in order.
     */
    suspend fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }

    /**
     * Determines the Kafka topic for a given event type.
     */
    private fun getTopicForEvent(event: DomainEvent): String {
        return when (event) {
            is QaPackageCreatedEvent,
            is QaPackageStatusChangedEvent,
            is QaPackageCompletedEvent -> KafkaTopics.QA_PACKAGE_EVENTS

            is TestRunStartedEvent,
            is TestRunCompletedEvent -> KafkaTopics.TEST_RUN_EVENTS

            is ScenarioGeneratedEvent -> KafkaTopics.SCENARIO_EVENTS

            is AiGenerationRequestedEvent,
            is AiGenerationCompletedEvent -> KafkaTopics.AI_GENERATION_EVENTS
        }
    }
}

/**
 * Exception thrown when event publishing fails.
 */
class EventPublishException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
