package com.qawave.infrastructure.messaging

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Interface for handling domain events.
 * Implementations can be registered to process specific event types.
 */
interface EventHandler<T : DomainEvent> {
    suspend fun handle(event: T)

    fun eventType(): Class<T>
}

/**
 * Consumer for QA Package events from Kafka.
 * Routes events to registered handlers.
 */
@Component
class QaPackageEventConsumer(
    private val handlers: List<EventHandler<*>>,
) {
    private val logger = LoggerFactory.getLogger(QaPackageEventConsumer::class.java)

    @KafkaListener(
        topics = [KafkaTopics.QA_PACKAGE_EVENTS],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeQaPackageEvents(
        @Payload event: DomainEvent,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        processEvent(event, topic, partition, offset, acknowledgment)
    }

    @KafkaListener(
        topics = [KafkaTopics.TEST_RUN_EVENTS],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeTestRunEvents(
        @Payload event: DomainEvent,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        processEvent(event, topic, partition, offset, acknowledgment)
    }

    @KafkaListener(
        topics = [KafkaTopics.SCENARIO_EVENTS],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeScenarioEvents(
        @Payload event: DomainEvent,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        processEvent(event, topic, partition, offset, acknowledgment)
    }

    @KafkaListener(
        topics = [KafkaTopics.AI_GENERATION_EVENTS],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeAiGenerationEvents(
        @Payload event: DomainEvent,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        processEvent(event, topic, partition, offset, acknowledgment)
    }

    private fun processEvent(
        event: DomainEvent,
        topic: String,
        partition: Int,
        offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        try {
            logger.debug(
                "Received event: type={}, eventId={}, topic={}, partition={}, offset={}",
                event::class.simpleName,
                event.eventId,
                topic,
                partition,
                offset,
            )

            // Find and invoke appropriate handlers
            val applicableHandlers =
                handlers.filter { handler ->
                    handler.eventType().isInstance(event)
                }

            if (applicableHandlers.isEmpty()) {
                logger.debug("No handlers registered for event type: {}", event::class.simpleName)
            } else {
                logger.debug(
                    "Found {} handlers for event type: {}",
                    applicableHandlers.size,
                    event::class.simpleName,
                )
            }

            // Note: In production, handlers would be invoked asynchronously with proper coroutine context
            // For now, we just log the event

            acknowledgment.acknowledge()
            logger.info("Processed event: type={}, eventId={}", event::class.simpleName, event.eventId)
        } catch (e: Exception) {
            logger.error(
                "Failed to process event: type={}, eventId={}, error={}",
                event::class.simpleName,
                event.eventId,
                e.message,
                e,
            )
            // Don't acknowledge - message will be redelivered
            throw e
        }
    }
}
