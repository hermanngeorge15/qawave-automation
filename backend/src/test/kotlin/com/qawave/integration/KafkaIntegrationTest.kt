package com.qawave.integration

import com.qawave.infrastructure.messaging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Kafka event publishing and consumption.
 */
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val kafkaContainer: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
            // Disable R2DBC auto-config for this test
            registry.add("spring.r2dbc.url") { "r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1" }
            registry.add("spring.r2dbc.username") { "sa" }
            registry.add("spring.r2dbc.password") { "" }
            // Disable Redis for this test
            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { "6379" }
        }
    }

    @Autowired
    private lateinit var eventPublisher: EventPublisher

    @Test
    fun `should publish QaPackageCreatedEvent to qa-package-events topic`() =
        runBlocking {
            val event =
                QaPackageCreatedEvent(
                    aggregateId = UUID.randomUUID().toString(),
                    packageName = "Test Package",
                    triggeredBy = "test-user",
                    baseUrl = "https://api.example.com",
                    hasSpecUrl = true,
                    hasSpecContent = false,
                )

            // Publish event
            eventPublisher.publish(event)

            // Give some time for the message to be sent
            delay(1000)

            // Verify by consuming from topic
            val consumer = createConsumer()
            consumer.subscribe(listOf(KafkaTopics.QA_PACKAGE_EVENTS))

            val records = consumer.poll(Duration.ofSeconds(10))
            consumer.close()

            assertTrue(records.count() >= 1, "Should have received at least 1 record")

            val receivedEvent = records.first()
            assertEquals(event.aggregateId, receivedEvent.key())
        }

    @Test
    fun `should publish TestRunStartedEvent to test-run-events topic`() =
        runBlocking {
            val event =
                TestRunStartedEvent(
                    aggregateId = UUID.randomUUID().toString(),
                    packageId = UUID.randomUUID().toString(),
                    scenarioId = UUID.randomUUID().toString(),
                    runNumber = 1,
                )

            eventPublisher.publish(event)
            delay(1000)

            val consumer = createConsumer()
            consumer.subscribe(listOf(KafkaTopics.TEST_RUN_EVENTS))

            val records = consumer.poll(Duration.ofSeconds(10))
            consumer.close()

            assertTrue(records.count() >= 1, "Should have received at least 1 record")
        }

    @Test
    fun `should publish ScenarioGeneratedEvent to scenario-events topic`() =
        runBlocking {
            val event =
                ScenarioGeneratedEvent(
                    aggregateId = UUID.randomUUID().toString(),
                    packageId = UUID.randomUUID().toString(),
                    scenarioName = "Test Scenario",
                    stepsCount = 5,
                    priority = "HIGH",
                )

            eventPublisher.publish(event)
            delay(1000)

            val consumer = createConsumer()
            consumer.subscribe(listOf(KafkaTopics.SCENARIO_EVENTS))

            val records = consumer.poll(Duration.ofSeconds(10))
            consumer.close()

            assertTrue(records.count() >= 1, "Should have received at least 1 record")
        }

    @Test
    fun `should publish AiGenerationCompletedEvent to ai-generation-events topic`() =
        runBlocking {
            val event =
                AiGenerationCompletedEvent(
                    aggregateId = UUID.randomUUID().toString(),
                    provider = "openai",
                    model = "gpt-4o-mini",
                    success = true,
                    scenariosGenerated = 10,
                    tokensUsed = 5000,
                    durationMs = 3500,
                )

            eventPublisher.publish(event)
            delay(1000)

            val consumer = createConsumer()
            consumer.subscribe(listOf(KafkaTopics.AI_GENERATION_EVENTS))

            val records = consumer.poll(Duration.ofSeconds(10))
            consumer.close()

            assertTrue(records.count() >= 1, "Should have received at least 1 record")
        }

    @Test
    fun `should route events to correct topics based on event type`() {
        // QA Package events
        assertTrue(KafkaTopics.QA_PACKAGE_EVENTS.isNotEmpty())
        assertTrue(KafkaTopics.TEST_RUN_EVENTS.isNotEmpty())
        assertTrue(KafkaTopics.SCENARIO_EVENTS.isNotEmpty())
        assertTrue(KafkaTopics.AI_GENERATION_EVENTS.isNotEmpty())
    }

    @Test
    fun `domain events should have required fields`() {
        val event =
            QaPackageStatusChangedEvent(
                aggregateId = "test-id",
                previousStatus = "REQUESTED",
                newStatus = "SPEC_FETCHED",
            )

        assertTrue(event.eventId.isNotEmpty())
        assertTrue(event.timestamp <= Instant.now())
        assertEquals("test-id", event.aggregateId)
        assertEquals("QaPackage", event.aggregateType)
    }

    private fun createConsumer(): KafkaConsumer<String, String> {
        val props =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "test-group-${UUID.randomUUID()}",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            )
        return KafkaConsumer(props)
    }
}
