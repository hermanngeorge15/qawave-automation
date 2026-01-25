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
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Kafka event publishing and consuming.
 */
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            // Disable R2DBC for this test
            registry.add("spring.r2dbc.url") { "r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1" }
            registry.add("spring.r2dbc.username") { "sa" }
            registry.add("spring.r2dbc.password") { "" }
            // Disable Redis for this test
            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { "6379" }
            registry.add("spring.autoconfigure.exclude") {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
            }
        }
    }

    @Autowired
    private lateinit var eventPublisher: EventPublisher

    @Test
    fun `Kafka container is running`() {
        assertTrue(kafka.isRunning, "Kafka container should be running")
    }

    @Test
    fun `should publish QaPackageCreatedEvent`() = runBlocking {
        val event = QaPackageCreatedEvent(
            aggregateId = "test-package-123",
            packageName = "Test Package",
            baseUrl = "https://api.example.com",
            triggeredBy = "test-user"
        )

        val result = eventPublisher.publishQaPackageEvent(event)

        assertNotNull(result)
        assertEquals(KafkaConfig.QA_PACKAGE_EVENTS_TOPIC, result.recordMetadata.topic())
    }

    @Test
    fun `should publish QaPackageStatusChangedEvent`() = runBlocking {
        val event = QaPackageStatusChangedEvent(
            aggregateId = "test-package-456",
            previousStatus = "REQUESTED",
            newStatus = "SPEC_FETCHED"
        )

        val result = eventPublisher.publishQaPackageEvent(event)

        assertNotNull(result)
        assertEquals(KafkaConfig.QA_PACKAGE_EVENTS_TOPIC, result.recordMetadata.topic())
    }

    @Test
    fun `should publish TestRunStartedEvent`() = runBlocking {
        val event = TestRunStartedEvent(
            aggregateId = "test-run-789",
            packageId = "package-123",
            scenarioCount = 5
        )

        val result = eventPublisher.publishTestRunEvent(event)

        assertNotNull(result)
        assertEquals(KafkaConfig.TEST_RUN_EVENTS_TOPIC, result.recordMetadata.topic())
    }

    @Test
    fun `should publish ScenarioGeneratedEvent`() = runBlocking {
        val event = ScenarioGeneratedEvent(
            aggregateId = "scenario-111",
            packageId = "package-123",
            scenarioName = "Test login flow",
            stepCount = 3
        )

        val result = eventPublisher.publishScenarioEvent(event)

        assertNotNull(result)
        assertEquals(KafkaConfig.SCENARIO_EVENTS_TOPIC, result.recordMetadata.topic())
    }

    @Test
    fun `should publish and consume event`() = runBlocking {
        val event = QaPackageCreatedEvent(
            aggregateId = "consume-test-${UUID.randomUUID()}",
            packageName = "Consume Test Package",
            baseUrl = "https://api.consume.test",
            triggeredBy = "consumer-test"
        )

        // Publish event
        eventPublisher.publishQaPackageEvent(event)

        // Wait for event to be processed
        delay(2000)

        // Create a consumer to verify the event was published
        val consumer = createTestConsumer()
        consumer.subscribe(listOf(KafkaConfig.QA_PACKAGE_EVENTS_TOPIC))

        val records = consumer.poll(Duration.ofSeconds(10))
        consumer.close()

        assertTrue(records.count() > 0, "Should have consumed at least one record")
    }

    private fun createTestConsumer(): KafkaConsumer<String, DomainEvent> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-${UUID.randomUUID()}")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "com.qawave.infrastructure.messaging")
        }

        return KafkaConsumer(props)
    }
}
