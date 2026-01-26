package com.qawave.integration

import com.qawave.infrastructure.cache.CacheKeys
import com.qawave.infrastructure.cache.CacheService
import com.qawave.infrastructure.cache.CacheTtl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Redis caching with Testcontainers.
 */
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val redisContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            // Disable R2DBC auto-config for this test
            registry.add("spring.r2dbc.url") { "r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1" }
            registry.add("spring.r2dbc.username") { "sa" }
            registry.add("spring.r2dbc.password") { "" }
        }
    }

    @Autowired
    private lateinit var cacheService: CacheService

    @BeforeEach
    fun setup() {
        runBlocking {
            // Clear test keys before each test
            cacheService.deleteByPattern("qawave:test:*")
        }
    }

    @Test
    fun `should connect to Redis and respond to ping`() =
        runBlocking {
            val isHealthy = cacheService.ping()
            assertTrue(isHealthy, "Redis should be healthy")
        }

    @Test
    fun `should set and get string value`() =
        runBlocking {
            val key = "qawave:test:string"
            val value = "hello-world"

            val setResult = cacheService.set(key, value, Duration.ofMinutes(5))
            assertTrue(setResult, "Set should succeed")

            val retrieved = cacheService.get(key, String::class.java)
            assertEquals(value, retrieved, "Retrieved value should match")
        }

    @Test
    fun `should set and get complex object`() =
        runBlocking {
            val key = "qawave:test:object"
            val value =
                TestCacheObject(
                    id = "test-123",
                    name = "Test Object",
                    count = 42,
                )

            val setResult = cacheService.set(key, value, Duration.ofMinutes(5))
            assertTrue(setResult, "Set should succeed")

            val retrieved = cacheService.get(key, TestCacheObject::class.java)
            assertNotNull(retrieved, "Retrieved value should not be null")
            assertEquals(value.id, retrieved.id)
            assertEquals(value.name, retrieved.name)
            assertEquals(value.count, retrieved.count)
        }

    @Test
    fun `should return null for non-existent key`() =
        runBlocking {
            val key = "qawave:test:non-existent"
            val retrieved = cacheService.get(key, String::class.java)
            assertNull(retrieved, "Non-existent key should return null")
        }

    @Test
    fun `should delete key`() =
        runBlocking {
            val key = "qawave:test:delete"
            cacheService.set(key, "to-be-deleted", Duration.ofMinutes(5))

            assertTrue(cacheService.exists(key), "Key should exist before delete")

            val deleted = cacheService.delete(key)
            assertTrue(deleted, "Delete should succeed")

            assertFalse(cacheService.exists(key), "Key should not exist after delete")
        }

    @Test
    fun `should delete keys by pattern`() =
        runBlocking {
            cacheService.set("qawave:test:pattern:1", "value1", Duration.ofMinutes(5))
            cacheService.set("qawave:test:pattern:2", "value2", Duration.ofMinutes(5))
            cacheService.set("qawave:test:other:1", "other", Duration.ofMinutes(5))

            val deleted = cacheService.deleteByPattern("qawave:test:pattern:*")
            assertEquals(2, deleted, "Should delete 2 keys matching pattern")

            assertFalse(cacheService.exists("qawave:test:pattern:1"))
            assertFalse(cacheService.exists("qawave:test:pattern:2"))
            assertTrue(cacheService.exists("qawave:test:other:1"))
        }

    @Test
    fun `should use getOrSet for cache-aside pattern`() =
        runBlocking {
            val key = "qawave:test:cache-aside"
            var computeCount = 0

            val result1 =
                cacheService.getOrSet(key, String::class.java, Duration.ofMinutes(5)) {
                    computeCount++
                    "computed-value"
                }

            assertEquals("computed-value", result1)
            assertEquals(1, computeCount, "Should compute once on cache miss")

            val result2 =
                cacheService.getOrSet(key, String::class.java, Duration.ofMinutes(5)) {
                    computeCount++
                    "computed-value-2"
                }

            assertEquals("computed-value", result2, "Should return cached value")
            assertEquals(1, computeCount, "Should not compute again on cache hit")
        }

    @Test
    fun `should increment counter`() =
        runBlocking {
            val key = "qawave:test:counter"

            val first = cacheService.increment(key)
            assertEquals(1, first)

            val second = cacheService.increment(key)
            assertEquals(2, second)

            val third = cacheService.increment(key, 5)
            assertEquals(7, third)
        }

    @Test
    fun `should expire key`() =
        runBlocking {
            val key = "qawave:test:expire"
            cacheService.set(key, "value", Duration.ofMinutes(5))

            val newTtl = Duration.ofSeconds(10)
            val expired = cacheService.expire(key, newTtl)
            assertTrue(expired, "Expire should succeed")

            val ttl = cacheService.getTtl(key)
            assertNotNull(ttl, "TTL should be returned")
            assertTrue(ttl.seconds <= 10, "TTL should be updated")
        }

    @Test
    fun `cache keys should generate correct prefixes`() {
        assertEquals("qawave:package:123", CacheKeys.qaPackage("123"))
        assertEquals("qawave:scenario:456", CacheKeys.scenario("456"))
        assertEquals("qawave:run:789", CacheKeys.testRun("789"))
        assertEquals("qawave:spec:abc", CacheKeys.apiSpec("abc"))
        assertEquals("qawave:ai:def", CacheKeys.aiResponse("def"))
    }

    @Test
    fun `cache TTL constants should have sensible values`() {
        assertTrue(CacheTtl.QA_PACKAGE.toMinutes() > 0)
        assertTrue(CacheTtl.SCENARIO.toMinutes() > 0)
        assertTrue(CacheTtl.TEST_RUN.toMinutes() > 0)
        assertTrue(CacheTtl.API_SPEC.toHours() > 0)
        assertTrue(CacheTtl.AI_RESPONSE.toHours() > 0)
    }

    /**
     * Test data class for object caching tests.
     */
    data class TestCacheObject(
        val id: String,
        val name: String,
        val count: Int,
    )
}
