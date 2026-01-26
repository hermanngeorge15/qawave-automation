package com.qawave.integration

import com.qawave.infrastructure.persistence.entity.QaPackageEntity
import com.qawave.infrastructure.persistence.entity.TestRunEntity
import com.qawave.infrastructure.persistence.entity.TestScenarioEntity
import com.qawave.infrastructure.persistence.entity.TestStepResultEntity
import com.qawave.infrastructure.persistence.repository.QaPackageR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestRunR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestScenarioR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestStepResultR2dbcRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.domain.PageRequest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for R2DBC repositories.
 *
 * Uses Testcontainers for local development and CI-provided service containers
 * when SPRING_R2DBC_URL environment variable is set (in GitHub Actions).
 */
@DataR2dbcTest
@Testcontainers
class R2dbcRepositoryTest {
    companion object {
        // Check if running in CI with service containers
        private val useTestcontainers = System.getenv("SPRING_R2DBC_URL") == null

        @Container
        @JvmStatic
        val postgresContainer: PostgreSQLContainer<*>? =
            if (useTestcontainers) {
                PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("qawave_test")
                    .withUsername("test")
                    .withPassword("test")
            } else {
                null
            }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Only configure from Testcontainers if not using CI service containers
            if (useTestcontainers && postgresContainer != null) {
                registry.add("spring.r2dbc.url") {
                    "r2dbc:postgresql://${postgresContainer.host}:${postgresContainer.firstMappedPort}/${postgresContainer.databaseName}"
                }
                registry.add("spring.r2dbc.username") { postgresContainer.username }
                registry.add("spring.r2dbc.password") { postgresContainer.password }
            }
            // When in CI, SPRING_R2DBC_URL is already set via environment variables
        }
    }

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Autowired
    private lateinit var qaPackageRepository: QaPackageR2dbcRepository

    @Autowired
    private lateinit var testScenarioRepository: TestScenarioR2dbcRepository

    @Autowired
    private lateinit var testRunRepository: TestRunR2dbcRepository

    @Autowired
    private lateinit var testStepResultRepository: TestStepResultR2dbcRepository

    @BeforeEach
    fun setup() =
        runTest {
            // Create tables for testing (matching actual entity structure)
            // Using gen_random_uuid() for auto-generated UUIDs (available in PostgreSQL 13+)
            databaseClient.sql(
                """
            CREATE TABLE IF NOT EXISTS qa_packages (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                name VARCHAR(255) NOT NULL,
                description TEXT,
                spec_url VARCHAR(2048),
                spec_content TEXT,
                spec_hash VARCHAR(64),
                base_url VARCHAR(2048) NOT NULL,
                requirements TEXT,
                status VARCHAR(50) NOT NULL,
                config_json TEXT,
                coverage_json TEXT,
                qa_summary_json TEXT,
                triggered_by VARCHAR(255) NOT NULL,
                started_at TIMESTAMP WITH TIME ZONE,
                completed_at TIMESTAMP WITH TIME ZONE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
        """,
            ).then().block()

            databaseClient.sql(
                """
            CREATE TABLE IF NOT EXISTS test_scenarios (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                suite_id UUID,
                qa_package_id UUID,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                steps_json TEXT NOT NULL,
                tags TEXT,
                source VARCHAR(50) NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
        """,
            ).then().block()

            databaseClient.sql(
                """
            CREATE TABLE IF NOT EXISTS test_runs (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                scenario_id UUID NOT NULL,
                qa_package_id UUID,
                triggered_by VARCHAR(255) NOT NULL,
                base_url VARCHAR(2048) NOT NULL,
                status VARCHAR(50) NOT NULL,
                environment_json TEXT,
                started_at TIMESTAMP WITH TIME ZONE NOT NULL,
                completed_at TIMESTAMP WITH TIME ZONE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
        """,
            ).then().block()

            databaseClient.sql(
                """
            CREATE TABLE IF NOT EXISTS test_step_results (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                run_id UUID NOT NULL,
                step_index INT NOT NULL,
                step_name VARCHAR(255) NOT NULL,
                actual_status INT,
                actual_headers_json TEXT,
                actual_body TEXT,
                passed BOOLEAN NOT NULL,
                assertions_json TEXT,
                extracted_values_json TEXT,
                error_message TEXT,
                duration_ms BIGINT NOT NULL,
                executed_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
        """,
            ).then().block()

            // Clean up before each test
            databaseClient.sql("DELETE FROM test_step_results").then().block()
            databaseClient.sql("DELETE FROM test_runs").then().block()
            databaseClient.sql("DELETE FROM test_scenarios").then().block()
            databaseClient.sql("DELETE FROM qa_packages").then().block()
        }

    @Nested
    inner class QaPackageRepositoryTests {
        @Test
        fun `save and find by id`() =
            runTest {
                val entity = createQaPackageEntity()

                val saved = qaPackageRepository.save(entity)
                val found = qaPackageRepository.findById(saved.id!!)

                assertNotNull(found)
                assertEquals(entity.name, found.name)
                assertEquals(entity.status, found.status)
            }

        @Test
        fun `find by status`() =
            runTest {
                val pending = createQaPackageEntity(status = "PENDING")
                val running = createQaPackageEntity(status = "RUNNING")
                qaPackageRepository.save(pending)
                qaPackageRepository.save(running)

                val result = qaPackageRepository.findByStatus("PENDING")

                assertEquals(1, result.size)
                assertEquals("PENDING", result[0].status)
            }

        @Test
        fun `find all by status with Flow`() =
            runTest {
                repeat(3) { qaPackageRepository.save(createQaPackageEntity(status = "PENDING")) }
                repeat(2) { qaPackageRepository.save(createQaPackageEntity(status = "RUNNING")) }

                val result = qaPackageRepository.findAllByStatus("PENDING").toList()

                assertEquals(3, result.size)
            }

        @Test
        fun `count by status`() =
            runTest {
                repeat(3) { qaPackageRepository.save(createQaPackageEntity(status = "COMPLETE")) }
                repeat(2) { qaPackageRepository.save(createQaPackageEntity(status = "FAILED_EXECUTION")) }

                assertEquals(3, qaPackageRepository.countByStatus("COMPLETE"))
                assertEquals(2, qaPackageRepository.countByStatus("FAILED_EXECUTION"))
            }

        @Test
        fun `find by spec hash`() =
            runTest {
                val entity = createQaPackageEntity(specHash = "abc123hash")
                qaPackageRepository.save(entity)

                val found = qaPackageRepository.findBySpecHash("abc123hash")

                assertNotNull(found)
                assertEquals("abc123hash", found.specHash)
            }

        @Test
        fun `pagination works`() =
            runTest {
                repeat(10) { i ->
                    qaPackageRepository.save(createQaPackageEntity(name = "Package $i"))
                }

                val page1 = qaPackageRepository.findAllBy(PageRequest.of(0, 5)).toList()
                val page2 = qaPackageRepository.findAllBy(PageRequest.of(1, 5)).toList()

                assertEquals(5, page1.size)
                assertEquals(5, page2.size)
            }

        private fun createQaPackageEntity(
            name: String = "Test Package",
            status: String = "PENDING",
            specHash: String? = null,
        ) = QaPackageEntity(
            id = null, // Let the database generate the ID (INSERT instead of UPDATE)
            name = name,
            description = "Test description",
            specUrl = "https://api.example.com/openapi.yaml",
            specContent = null,
            specHash = specHash,
            baseUrl = "https://api.example.com",
            requirements = null,
            status = status,
            configJson = "{}",
            coverageJson = null,
            qaSummaryJson = null,
            triggeredBy = "test-user",
            startedAt = null,
            completedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }

    @Nested
    inner class TestScenarioRepositoryTests {
        @Test
        fun `save and find by QA package ID`() =
            runTest {
                val packageId = UUID.randomUUID()
                val scenario1 = createTestScenarioEntity(qaPackageId = packageId)
                val scenario2 = createTestScenarioEntity(qaPackageId = packageId)

                testScenarioRepository.save(scenario1)
                testScenarioRepository.save(scenario2)

                val result = testScenarioRepository.findByQaPackageId(packageId)

                assertEquals(2, result.size)
            }

        @Test
        fun `count by QA package ID`() =
            runTest {
                val packageId = UUID.randomUUID()
                repeat(5) { testScenarioRepository.save(createTestScenarioEntity(qaPackageId = packageId)) }

                assertEquals(5, testScenarioRepository.countByQaPackageId(packageId))
            }

        @Test
        fun `delete by QA package ID`() =
            runTest {
                val packageId = UUID.randomUUID()
                repeat(3) { testScenarioRepository.save(createTestScenarioEntity(qaPackageId = packageId)) }

                testScenarioRepository.deleteByQaPackageId(packageId)

                assertEquals(0, testScenarioRepository.countByQaPackageId(packageId))
            }

        private fun createTestScenarioEntity(
            qaPackageId: UUID = UUID.randomUUID(),
            status: String = "PENDING",
        ) = TestScenarioEntity(
            id = null, // Let the database generate the ID (INSERT instead of UPDATE)
            qaPackageId = qaPackageId,
            suiteId = null,
            name = "Test Scenario",
            description = "Test description",
            stepsJson = "[]",
            tags = "[]",
            source = "AI_GENERATED",
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }

    @Nested
    inner class TestRunRepositoryTests {
        @Test
        fun `save and find by scenario ID`() =
            runTest {
                val scenarioId = UUID.randomUUID()
                val run1 = createTestRunEntity(scenarioId = scenarioId)
                val run2 = createTestRunEntity(scenarioId = scenarioId)

                testRunRepository.save(run1)
                testRunRepository.save(run2)

                val result = testRunRepository.findByScenarioId(scenarioId)

                assertEquals(2, result.size)
            }

        @Test
        fun `find latest by scenario ID`() =
            runTest {
                val scenarioId = UUID.randomUUID()
                val oldRun = createTestRunEntity(scenarioId = scenarioId, createdAt = Instant.now().minusSeconds(60))
                val newRun = createTestRunEntity(scenarioId = scenarioId, createdAt = Instant.now())

                testRunRepository.save(oldRun)
                val savedNewRun = testRunRepository.save(newRun)

                val latest = testRunRepository.findLatestByScenarioId(scenarioId)

                assertNotNull(latest)
                assertEquals(savedNewRun.id, latest.id)
            }

        @Test
        fun `count passed and failed`() =
            runTest {
                val packageId = UUID.randomUUID()
                repeat(3) { testRunRepository.save(createTestRunEntity(qaPackageId = packageId, status = "PASSED")) }
                repeat(2) { testRunRepository.save(createTestRunEntity(qaPackageId = packageId, status = "FAILED")) }

                assertEquals(3, testRunRepository.countPassedByQaPackageId(packageId))
                assertEquals(2, testRunRepository.countFailedByQaPackageId(packageId))
            }

        private fun createTestRunEntity(
            scenarioId: UUID = UUID.randomUUID(),
            qaPackageId: UUID = UUID.randomUUID(),
            status: String = "PENDING",
            createdAt: Instant = Instant.now(),
        ) = TestRunEntity(
            id = null, // Let the database generate the ID (INSERT instead of UPDATE)
            scenarioId = scenarioId,
            qaPackageId = qaPackageId,
            triggeredBy = "test-user",
            baseUrl = "https://api.example.com",
            status = status,
            environmentJson = null,
            startedAt = Instant.now(),
            completedAt = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    @Nested
    inner class TestStepResultRepositoryTests {
        @Test
        fun `save and find by run ID`() =
            runTest {
                val runId = UUID.randomUUID()
                val result1 = createTestStepResultEntity(runId = runId, stepIndex = 0)
                val result2 = createTestStepResultEntity(runId = runId, stepIndex = 1)

                testStepResultRepository.save(result1)
                testStepResultRepository.save(result2)

                val result = testStepResultRepository.findByRunId(runId)

                assertEquals(2, result.size)
            }

        @Test
        fun `find by run ID and step index`() =
            runTest {
                val runId = UUID.randomUUID()
                val step0 = createTestStepResultEntity(runId = runId, stepIndex = 0, stepName = "Step 0")
                val step1 = createTestStepResultEntity(runId = runId, stepIndex = 1, stepName = "Step 1")

                testStepResultRepository.save(step0)
                testStepResultRepository.save(step1)

                val found = testStepResultRepository.findByRunIdAndStepIndex(runId, 1)

                assertNotNull(found)
                assertEquals("Step 1", found.stepName)
            }

        @Test
        fun `count passed and failed`() =
            runTest {
                val runId = UUID.randomUUID()
                repeat(3) { i ->
                    testStepResultRepository.save(
                        createTestStepResultEntity(runId = runId, stepIndex = i, passed = true),
                    )
                }
                repeat(2) { i ->
                    testStepResultRepository.save(
                        createTestStepResultEntity(runId = runId, stepIndex = i + 3, passed = false),
                    )
                }

                assertEquals(3, testStepResultRepository.countPassedByRunId(runId))
                assertEquals(2, testStepResultRepository.countFailedByRunId(runId))
            }

        @Test
        fun `find slowest step`() =
            runTest {
                val runId = UUID.randomUUID()
                val fastStep = createTestStepResultEntity(runId = runId, stepIndex = 0, durationMs = 100)
                val slowStep = createTestStepResultEntity(runId = runId, stepIndex = 1, durationMs = 500)

                testStepResultRepository.save(fastStep)
                testStepResultRepository.save(slowStep)

                val slowest = testStepResultRepository.findSlowestStepByRunId(runId)

                assertNotNull(slowest)
                assertEquals(500L, slowest.durationMs)
            }

        private fun createTestStepResultEntity(
            runId: UUID = UUID.randomUUID(),
            stepIndex: Int = 0,
            stepName: String = "Test Step",
            passed: Boolean = true,
            durationMs: Long = 100,
        ) = TestStepResultEntity(
            id = null, // Let the database generate the ID (INSERT instead of UPDATE)
            runId = runId,
            stepIndex = stepIndex,
            stepName = stepName,
            actualStatus = 200,
            actualHeadersJson = "{}",
            actualBody = "{}",
            passed = passed,
            assertionsJson = "[]",
            extractedValuesJson = "{}",
            errorMessage = null,
            durationMs = durationMs,
            executedAt = Instant.now(),
        )
    }
}
