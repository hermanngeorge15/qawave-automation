package com.qawave.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Kafka configuration for domain event streaming.
 * Configures producers, consumers, and topic management.
 */
@Configuration
class KafkaConfig(
    private val objectMapper: ObjectMapper
) {

    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id:qawave-group}")
    private lateinit var consumerGroupId: String

    companion object {
        const val QA_PACKAGE_EVENTS_TOPIC = "qa-package-events"
        const val TEST_RUN_EVENTS_TOPIC = "test-run-events"
        const val SCENARIO_EVENTS_TOPIC = "scenario-events"
        const val DLQ_TOPIC = "qawave-dlq"
    }

    // ==================== Topics ====================

    @Bean
    fun qaPackageEventsTopic(): NewTopic {
        return TopicBuilder.name(QA_PACKAGE_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun testRunEventsTopic(): NewTopic {
        return TopicBuilder.name(TEST_RUN_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun scenarioEventsTopic(): NewTopic {
        return TopicBuilder.name(SCENARIO_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun deadLetterQueueTopic(): NewTopic {
        return TopicBuilder.name(DLQ_TOPIC)
            .partitions(1)
            .replicas(1)
            .build()
    }

    // ==================== Producer ====================

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = mutableMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        val factory = DefaultKafkaProducerFactory<String, Any>(configProps)
        factory.setValueSerializer(JsonSerializer(objectMapper))
        return factory
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    // ==================== Consumer ====================

    @Bean
    fun consumerFactory(): ConsumerFactory<String, DomainEvent> {
        val configProps = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to consumerGroupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            JsonDeserializer.TRUSTED_PACKAGES to "com.qawave.infrastructure.messaging"
        )
        val deserializer = JsonDeserializer(DomainEvent::class.java, objectMapper, false)
        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            deserializer
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, DomainEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, DomainEvent>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setConcurrency(3)
        return factory
    }
}
