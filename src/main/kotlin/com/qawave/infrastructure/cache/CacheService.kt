package com.qawave.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Cache key prefixes for different entity types.
 */
object CacheKeys {
    const val QA_PACKAGE = "qawave:package:"
    const val SCENARIO = "qawave:scenario:"
    const val TEST_RUN = "qawave:run:"
    const val API_SPEC = "qawave:spec:"
    const val AI_RESPONSE = "qawave:ai:"

    fun qaPackage(id: String) = "$QA_PACKAGE$id"
    fun scenario(id: String) = "$SCENARIO$id"
    fun testRun(id: String) = "$TEST_RUN$id"
    fun apiSpec(hash: String) = "$API_SPEC$hash"
    fun aiResponse(hash: String) = "$AI_RESPONSE$hash"
}

/**
 * Default TTL values for different cache types.
 */
object CacheTtl {
    val QA_PACKAGE: Duration = Duration.ofMinutes(30)
    val SCENARIO: Duration = Duration.ofMinutes(60)
    val TEST_RUN: Duration = Duration.ofMinutes(15)
    val API_SPEC: Duration = Duration.ofHours(24)
    val AI_RESPONSE: Duration = Duration.ofHours(12)
}

/**
 * Service for caching operations using reactive Redis.
 * Provides type-safe caching with automatic JSON serialization.
 */
@Service
class CacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val stringRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Gets a cached value by key with type conversion.
     * @param key The cache key
     * @param type The class type to deserialize to
     */
    suspend fun <T> get(key: String, type: Class<T>): T? {
        return try {
            val value = stringRedisTemplate.opsForValue()
                .get(key)
                .awaitFirstOrNull()

            value?.let { objectMapper.readValue(it, type) }
        } catch (e: Exception) {
            logger.warn("Cache get failed for key '{}': {}", key, e.message)
            null
        }
    }

    /**
     * Sets a cached value with the specified TTL.
     */
    suspend fun <T : Any> set(key: String, value: T, ttl: Duration): Boolean {
        return try {
            val json = objectMapper.writeValueAsString(value)
            stringRedisTemplate.opsForValue()
                .set(key, json, ttl)
                .awaitSingle()
        } catch (e: Exception) {
            logger.warn("Cache set failed for key '{}': {}", key, e.message)
            false
        }
    }

    /**
     * Gets a cached value or computes it if not present.
     * @param key The cache key
     * @param type The class type to deserialize to
     * @param ttl Time to live for the cached value
     * @param compute Function to compute the value if not cached
     */
    suspend fun <T : Any> getOrSet(
        key: String,
        type: Class<T>,
        ttl: Duration,
        compute: suspend () -> T?
    ): T? {
        val cached = get(key, type)
        if (cached != null) {
            logger.debug("Cache hit for key '{}'", key)
            return cached
        }

        logger.debug("Cache miss for key '{}'", key)
        val computed = compute()
        if (computed != null) {
            set(key, computed, ttl)
        }
        return computed
    }

    /**
     * Deletes a cached value by key.
     */
    suspend fun delete(key: String): Boolean {
        return try {
            stringRedisTemplate.delete(key).awaitSingle() > 0
        } catch (e: Exception) {
            logger.warn("Cache delete failed for key '{}': {}", key, e.message)
            false
        }
    }

    /**
     * Deletes all cached values matching a pattern.
     */
    suspend fun deleteByPattern(pattern: String): Long {
        return try {
            val keys = stringRedisTemplate.keys(pattern)
            stringRedisTemplate.delete(keys).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Cache delete by pattern failed for '{}': {}", pattern, e.message)
            0
        }
    }

    /**
     * Checks if a key exists in the cache.
     */
    suspend fun exists(key: String): Boolean {
        return try {
            stringRedisTemplate.hasKey(key).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Cache exists check failed for key '{}': {}", key, e.message)
            false
        }
    }

    /**
     * Gets the TTL for a key.
     */
    suspend fun getTtl(key: String): Duration? {
        return try {
            val ttl = stringRedisTemplate.getExpire(key).awaitFirstOrNull()
            ttl?.let { Duration.ofSeconds(it.seconds) }
        } catch (e: Exception) {
            logger.warn("Cache TTL check failed for key '{}': {}", key, e.message)
            null
        }
    }

    /**
     * Extends the TTL for a key.
     */
    suspend fun expire(key: String, ttl: Duration): Boolean {
        return try {
            stringRedisTemplate.expire(key, ttl).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Cache expire failed for key '{}': {}", key, e.message)
            false
        }
    }

    /**
     * Increments a counter value.
     */
    suspend fun increment(key: String, delta: Long = 1): Long? {
        return try {
            redisTemplate.opsForValue()
                .increment(key, delta)
                .awaitFirstOrNull()
        } catch (e: Exception) {
            logger.warn("Cache increment failed for key '{}': {}", key, e.message)
            null
        }
    }

    /**
     * Health check for Redis connection.
     */
    suspend fun ping(): Boolean {
        return try {
            val result = stringRedisTemplate.connectionFactory
                .reactiveConnection
                .ping()
                .awaitFirstOrNull()
            result == "PONG"
        } catch (e: Exception) {
            logger.error("Redis health check failed: {}", e.message)
            false
        }
    }
}
