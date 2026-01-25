package com.qawave.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Configuration for reactive Redis with Lettuce client.
 * Provides connection pooling and JSON serialization.
 */
@Configuration
class RedisConfig(
    private val objectMapper: ObjectMapper,
) {
    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379

    @Value("\${spring.data.redis.password:}")
    private lateinit var redisPassword: String

    @Value("\${qawave.cache.pool.min-idle:2}")
    private var poolMinIdle: Int = 2

    @Value("\${qawave.cache.pool.max-idle:8}")
    private var poolMaxIdle: Int = 8

    @Value("\${qawave.cache.pool.max-active:16}")
    private var poolMaxActive: Int = 16

    @Value("\${qawave.cache.command-timeout:5000}")
    private var commandTimeout: Long = 5000

    /**
     * Creates a Lettuce connection factory with pooling configuration.
     */
    @Bean
    @Primary
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val serverConfig =
            RedisStandaloneConfiguration(redisHost, redisPort).apply {
                if (redisPassword.isNotBlank()) {
                    setPassword(redisPassword)
                }
            }

        val clientConfig =
            LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(commandTimeout))
                .poolConfig(buildPoolConfig())
                .build()

        return LettuceConnectionFactory(serverConfig, clientConfig)
    }

    /**
     * Creates a reactive Redis template with JSON serialization.
     */
    @Bean
    fun reactiveRedisTemplate(connectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)

        val serializationContext =
            RedisSerializationContext.newSerializationContext<String, Any>()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }

    /**
     * Creates a string-only reactive Redis template for simple caching.
     */
    @Bean
    fun reactiveStringRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()

        val serializationContext =
            RedisSerializationContext.newSerializationContext<String, String>()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }

    private fun buildPoolConfig(): org.apache.commons.pool2.impl.GenericObjectPoolConfig<Any> {
        return org.apache.commons.pool2.impl.GenericObjectPoolConfig<Any>().apply {
            minIdle = poolMinIdle
            maxIdle = poolMaxIdle
            maxTotal = poolMaxActive
            testOnBorrow = true
            testWhileIdle = true
        }
    }
}
