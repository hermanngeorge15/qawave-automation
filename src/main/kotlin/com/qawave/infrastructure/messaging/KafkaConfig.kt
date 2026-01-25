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
 * Kafka topic names used by the application.
 */
object KafkaTopics {
    const val QA_PACKAGE_EVENTS = "qa-package-events"
    const val TEST_RUN_EVENTS = "test-run-events"
    const val SCENARIO_EVENTS = "scenario-events"
    const val AI_GENERATION_EVENTS = "ai-generation-events"
}

/**
 * Configuration for Apache Kafka.
 * Sets up producers, consumers, and topics.
 */
@Configuration
class KafkaConfig(
    private val objectMapper: ObjectMapper,
) {
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id:qawave}")
    private lateinit var groupId: String

    @Value("\${qawave.kafka.partitions:3}")
    private var partitions: Int = 3

    @Value("\${qawave.kafka.replication-factor:1}")
    private var replicationFactor: Short = 1

    // ==================== Topics ====================

    @Bean
    fun qaPackageEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.QA_PACKAGE_EVENTS)
            .partitions(partitions)
            .replicas(replicationFactor.toInt())
            .build()
    }

    @Bean
    fun testRunEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.TEST_RUN_EVENTS)
            .partitions(partitions)
            .replicas(replicationFactor.toInt())
            .build()
    }

    @Bean
    fun scenarioEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.SCENARIO_EVENTS)
            .partitions(partitions)
            .replicas(replicationFactor.toInt())
            .build()
    }

    @Bean
    fun aiGenerationEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.AI_GENERATION_EVENTS)
            .partitions(partitions)
            .replicas(replicationFactor.toInt())
            .build()
    }

    // ==================== Producer ====================

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 3,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    // ==================== Consumer ====================

    @Bean
    fun consumerFactory(): ConsumerFactory<String, DomainEvent> {
        val configProps =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                JsonDeserializer.TRUSTED_PACKAGES to "com.qawave.*",
            )
        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            JsonDeserializer(DomainEvent::class.java, objectMapper),
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
