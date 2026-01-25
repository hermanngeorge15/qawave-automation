package com.qawave

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@EnableAutoConfiguration(exclude = [
    RedisAutoConfiguration::class,
    RedisReactiveAutoConfiguration::class,
    KafkaAutoConfiguration::class
])
class QaWaveApplicationTests {

    @Test
    fun contextLoads() {
        // Basic smoke test to verify application context loads
    }
}
