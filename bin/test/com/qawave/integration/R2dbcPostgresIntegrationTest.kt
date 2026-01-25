package com.qawave.integration

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test verifying R2DBC PostgreSQL connectivity using Testcontainers.
 * This test ensures that:
 * - R2DBC driver connects to PostgreSQL successfully
 * - Basic queries execute correctly
 * - Connection pooling works as expected
 */
@SpringBootTest
@Testcontainers
@EnableAutoConfiguration(exclude = [
    RedisAutoConfiguration::class,
    RedisReactiveAutoConfiguration::class,
    KafkaAutoConfiguration::class
])
class R2dbcPostgresIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("qawave_test")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "false" }
        }
    }

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Test
    fun `PostgreSQL container is running`() {
        assertTrue(postgres.isRunning, "PostgreSQL container should be running")
    }

    @Test
    fun `can execute simple query`() = runBlocking {
        val result = databaseClient.sql("SELECT 1::integer as value")
            .map { row, _ -> row.get("value", Integer::class.java) }
            .first()
            .awaitFirst()

        assertEquals(1, result?.toInt())
    }

    @Test
    fun `can get database version`() = runBlocking {
        val version = databaseClient.sql("SELECT version()")
            .map { row, _ -> row.get(0, String::class.java) }
            .first()
            .awaitFirst()

        assertNotNull(version)
        assertTrue(version!!.contains("PostgreSQL"), "Should return PostgreSQL version")
    }

    @Test
    fun `can create and query temporary table`() = runBlocking {
        // Create a temporary table
        databaseClient.sql("""
            CREATE TEMPORARY TABLE test_table (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            )
        """).fetch().rowsUpdated().awaitFirst()

        // Insert a row
        databaseClient.sql("INSERT INTO test_table (name) VALUES ('test_value')")
            .fetch()
            .rowsUpdated()
            .awaitFirst()

        // Query the row
        val name = databaseClient.sql("SELECT name FROM test_table WHERE id = 1")
            .map { row, _ -> row.get("name", String::class.java) }
            .first()
            .awaitFirst()

        assertEquals("test_value", name)
    }

    @Test
    fun `connection pool is configured`() = runBlocking {
        // Execute multiple queries to verify pooling works
        repeat(10) { index ->
            val result = databaseClient.sql("SELECT $index::integer as value")
                .map { row, _ -> row.get("value", Integer::class.java) }
                .first()
                .awaitFirst()
            assertEquals(index, result?.toInt())
        }
    }
}
