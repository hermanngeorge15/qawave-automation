package com.qawave.infrastructure.messaging

import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

/**
 * Service for publishing domain events to Kafka.
 * Provides suspend functions for non-blocking event publishing.
 */
@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    /**
     * Publishes a QA package event to the qa-package-events topic.
     */
    suspend fun publishQaPackageEvent(event: DomainEvent): SendResult<String, Any> {
        return publish(KafkaConfig.QA_PACKAGE_EVENTS_TOPIC, event.aggregateId, event)
    }

    /**
     * Publishes a test run event to the test-run-events topic.
     */
    suspend fun publishTestRunEvent(event: DomainEvent): SendResult<String, Any> {
        return publish(KafkaConfig.TEST_RUN_EVENTS_TOPIC, event.aggregateId, event)
    }

    /**
     * Publishes a scenario event to the scenario-events topic.
     */
    suspend fun publishScenarioEvent(event: DomainEvent): SendResult<String, Any> {
        return publish(KafkaConfig.SCENARIO_EVENTS_TOPIC, event.aggregateId, event)
    }

    /**
     * Publishes an event to the specified topic.
     */
    private suspend fun publish(
        topic: String,
        key: String,
        event: DomainEvent
    ): SendResult<String, Any> {
        logger.debug("Publishing event to {}: eventId={}, aggregateId={}",
            topic, event.eventId, event.aggregateId)

        return try {
            val result = kafkaTemplate.send(topic, key, event).await()
            logger.info("Published event to {}: eventId={}, partition={}, offset={}",
                topic, event.eventId,
                result.recordMetadata.partition(),
                result.recordMetadata.offset())
            result
        } catch (e: Exception) {
            logger.error("Failed to publish event to {}: eventId={}, error={}",
                topic, event.eventId, e.message)
            throw EventPublishException("Failed to publish event: ${event.eventId}", e)
        }
    }

    /**
     * Publishes a message to the dead letter queue.
     */
    suspend fun publishToDlq(
        originalTopic: String,
        key: String,
        event: DomainEvent,
        error: String
    ) {
        val dlqMessage = DeadLetterMessage(
            originalTopic = originalTopic,
            key = key,
            event = event,
            error = error
        )

        try {
            kafkaTemplate.send(KafkaConfig.DLQ_TOPIC, key, dlqMessage).await()
            logger.warn("Published to DLQ: originalTopic={}, eventId={}",
                originalTopic, event.eventId)
        } catch (e: Exception) {
            logger.error("Failed to publish to DLQ: eventId={}", event.eventId, e)
        }
    }
}

/**
 * Wrapper for messages sent to the dead letter queue.
 */
data class DeadLetterMessage(
    val originalTopic: String,
    val key: String,
    val event: DomainEvent,
    val error: String,
    val timestamp: java.time.Instant = java.time.Instant.now()
)

/**
 * Exception thrown when event publishing fails.
 */
class EventPublishException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
