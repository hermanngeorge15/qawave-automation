package com.qawave.infrastructure.messaging

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Consumer for processing domain events from Kafka.
 * Handles events with proper acknowledgment and error handling.
 */
@Component
class EventConsumer(
    private val eventPublisher: EventPublisher,
    private val eventHandlers: List<EventHandler>
) {

    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    @KafkaListener(
        topics = [KafkaConfig.QA_PACKAGE_EVENTS_TOPIC],
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleQaPackageEvent(
        record: ConsumerRecord<String, DomainEvent>,
        acknowledgment: Acknowledgment
    ) {
        handleEvent(record, acknowledgment, KafkaConfig.QA_PACKAGE_EVENTS_TOPIC)
    }

    @KafkaListener(
        topics = [KafkaConfig.TEST_RUN_EVENTS_TOPIC],
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleTestRunEvent(
        record: ConsumerRecord<String, DomainEvent>,
        acknowledgment: Acknowledgment
    ) {
        handleEvent(record, acknowledgment, KafkaConfig.TEST_RUN_EVENTS_TOPIC)
    }

    @KafkaListener(
        topics = [KafkaConfig.SCENARIO_EVENTS_TOPIC],
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleScenarioEvent(
        record: ConsumerRecord<String, DomainEvent>,
        acknowledgment: Acknowledgment
    ) {
        handleEvent(record, acknowledgment, KafkaConfig.SCENARIO_EVENTS_TOPIC)
    }

    private fun handleEvent(
        record: ConsumerRecord<String, DomainEvent>,
        acknowledgment: Acknowledgment,
        topic: String
    ) {
        val event = record.value()
        logger.info("Received event from {}: eventId={}, type={}, partition={}, offset={}",
            topic, event.eventId, event::class.simpleName,
            record.partition(), record.offset())

        try {
            // Find and execute handlers for this event type
            val handlers = eventHandlers.filter { it.canHandle(event) }

            if (handlers.isEmpty()) {
                logger.debug("No handlers registered for event type: {}", event::class.simpleName)
            } else {
                handlers.forEach { handler ->
                    try {
                        handler.handle(event)
                        logger.debug("Handler {} processed event: {}",
                            handler::class.simpleName, event.eventId)
                    } catch (e: Exception) {
                        logger.error("Handler {} failed for event {}: {}",
                            handler::class.simpleName, event.eventId, e.message)
                        throw e
                    }
                }
            }

            acknowledgment.acknowledge()
            logger.debug("Acknowledged event: {}", event.eventId)
        } catch (e: Exception) {
            logger.error("Failed to process event: eventId={}, error={}",
                event.eventId, e.message, e)

            // Send to DLQ
            try {
                kotlinx.coroutines.runBlocking {
                    eventPublisher.publishToDlq(
                        originalTopic = topic,
                        key = record.key(),
                        event = event,
                        error = e.message ?: "Unknown error"
                    )
                }
            } catch (dlqError: Exception) {
                logger.error("Failed to send to DLQ: {}", dlqError.message)
            }

            // Acknowledge to prevent reprocessing (DLQ handles retries)
            acknowledgment.acknowledge()
        }
    }
}

/**
 * Interface for event handlers.
 * Implement this interface to process specific event types.
 */
interface EventHandler {
    /**
     * Returns true if this handler can process the given event.
     */
    fun canHandle(event: DomainEvent): Boolean

    /**
     * Processes the event.
     */
    fun handle(event: DomainEvent)
}

/**
 * Default event handler that logs events.
 * Can be replaced with specific business logic handlers.
 */
@Component
class LoggingEventHandler : EventHandler {

    private val logger = LoggerFactory.getLogger(LoggingEventHandler::class.java)

    override fun canHandle(event: DomainEvent): Boolean = true

    override fun handle(event: DomainEvent) {
        when (event) {
            is QaPackageCreatedEvent -> logger.info(
                "QA Package created: id={}, name={}, triggeredBy={}",
                event.aggregateId, event.packageName, event.triggeredBy
            )
            is QaPackageStatusChangedEvent -> logger.info(
                "QA Package status changed: id={}, {} -> {}",
                event.aggregateId, event.previousStatus, event.newStatus
            )
            is QaPackageCompletedEvent -> logger.info(
                "QA Package completed: id={}, passed={}, failed={}, coverage={}%",
                event.aggregateId, event.passedScenarios, event.failedScenarios, event.coveragePercentage
            )
            is QaPackageFailedEvent -> logger.warn(
                "QA Package failed: id={}, status={}, error={}",
                event.aggregateId, event.failureStatus, event.errorMessage
            )
            is TestRunStartedEvent -> logger.info(
                "Test run started: id={}, packageId={}, scenarios={}",
                event.aggregateId, event.packageId, event.scenarioCount
            )
            is TestRunCompletedEvent -> logger.info(
                "Test run completed: id={}, passed={}, failed={}, duration={}ms",
                event.aggregateId, event.passedSteps, event.failedSteps, event.durationMs
            )
            is ScenarioGeneratedEvent -> logger.info(
                "Scenario generated: id={}, name={}, steps={}",
                event.aggregateId, event.scenarioName, event.stepCount
            )
            is ScenarioExecutedEvent -> logger.info(
                "Scenario executed: id={}, name={}, passed={}, duration={}ms",
                event.aggregateId, event.scenarioName, event.passed, event.durationMs
            )
        }
    }
}
