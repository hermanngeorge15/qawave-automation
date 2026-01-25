package com.qawave.infrastructure.cache

import kotlinx.coroutines.reactor.mono
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Health indicator for Redis connection.
 * Reports Redis status in the actuator health endpoint.
 */
@Component
class RedisHealthIndicator(
    private val cacheService: CacheService,
) : ReactiveHealthIndicator {
    override fun health(): Mono<Health> =
        mono {
            try {
                val isHealthy = cacheService.ping()
                if (isHealthy) {
                    Health.up()
                        .withDetail("status", "Connected")
                        .build()
                } else {
                    Health.down()
                        .withDetail("status", "Ping failed")
                        .build()
                }
            } catch (e: Exception) {
                Health.down()
                    .withDetail("status", "Error")
                    .withDetail("error", e.message ?: "Unknown error")
                    .build()
            }
        }
}
