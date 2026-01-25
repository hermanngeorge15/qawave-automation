package com.qawave.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qawave.application.service.*
import com.qawave.domain.model.*
import com.qawave.infrastructure.http.HttpStepExecutor
import com.qawave.infrastructure.persistence.entity.TestRunEntity
import com.qawave.infrastructure.persistence.entity.TestStepResultEntity
import com.qawave.infrastructure.persistence.mapper.TestRunMapper
import com.qawave.infrastructure.persistence.mapper.TestStepResultMapper
import com.qawave.infrastructure.persistence.repository.TestRunR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestStepResultR2dbcRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestExecutionServiceTest {

    @MockK
    private lateinit var runRepository: TestRunR2dbcRepository

    @MockK
    private lateinit var stepResultRepository: TestStepResultR2dbcRepository

    @MockK
    private lateinit var httpStepExecutor: HttpStepExecutor

    private lateinit var objectMapper: ObjectMapper
    private lateinit var runMapper: TestRunMapper
    private lateinit var stepResultMapper: TestStepResultMapper
    private lateinit var service: TestExecutionServiceImpl

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        objectMapper = jacksonObjectMapper().findAndRegisterModules()
        runMapper = TestRunMapper(objectMapper)
        stepResultMapper = TestStepResultMapper(objectMapper)
        service = TestExecutionServiceImpl(
            runRepository,
            stepResultRepository,
            runMapper,
            stepResultMapper,
            httpStepExecutor
        )
    }

    @Nested
    inner class StartRunTests {
        @Test
        fun `startRun creates a queued test run`() = runTest {
            val command = StartRunCommand(
                scenarioId = ScenarioId.generate(),
                triggeredBy = "test-user",
                baseUrl = "https://api.example.com"
            )
            val entitySlot = slot<TestRunEntity>()

            coEvery { runRepository.save(capture(entitySlot)) } coAnswers {
                entitySlot.captured.copy(id = entitySlot.captured.id ?: UUID.randomUUID())
            }

            val result = service.startRun(command)

            assertNotNull(result)
            assertEquals(TestRunStatus.QUEUED, result.status)
            assertEquals(command.baseUrl, result.baseUrl)
            assertEquals(command.triggeredBy, result.triggeredBy)

            coVerify { runRepository.save(any()) }
        }

        @Test
        fun `startRun sets environment variables`() = runTest {
            val command = StartRunCommand(
                scenarioId = ScenarioId.generate(),
                triggeredBy = "test-user",
                baseUrl = "https://api.example.com",
                environment = mapOf("API_KEY" to "secret")
            )
            val entitySlot = slot<TestRunEntity>()

            coEvery { runRepository.save(capture(entitySlot)) } coAnswers {
                entitySlot.captured.copy(id = entitySlot.captured.id ?: UUID.randomUUID())
            }

            val result = service.startRun(command)

            assertEquals(mapOf("API_KEY" to "secret"), result.environment)
        }
    }

    @Nested
    inner class FindTests {
        @Test
        fun `findById returns test run when found`() = runTest {
            val id = UUID.randomUUID()
            val entity = createRunEntity(id = id)

            coEvery { runRepository.findById(id) } returns entity

            val result = service.findById(TestRunId(id))

            assertNotNull(result)
            assertEquals(id, result.id.value)
        }

        @Test
        fun `findById returns null when not found`() = runTest {
            val id = UUID.randomUUID()

            coEvery { runRepository.findById(id) } returns null

            val result = service.findById(TestRunId(id))

            assertNull(result)
        }

        @Test
        fun `findByIdWithResults includes step results`() = runTest {
            val runId = UUID.randomUUID()
            val runEntity = createRunEntity(id = runId)
            val stepResultEntities = listOf(
                createStepResultEntity(runId = runId, stepIndex = 0),
                createStepResultEntity(runId = runId, stepIndex = 1)
            )

            coEvery { runRepository.findById(runId) } returns runEntity
            coEvery { stepResultRepository.findByRunIdOrderByStepIndex(runId) } returns stepResultEntities

            val result = service.findByIdWithResults(TestRunId(runId))

            assertNotNull(result)
            assertEquals(2, result.stepResults.size)
        }

        @Test
        fun `findByScenarioId returns runs for scenario`() = runTest {
            val scenarioId = UUID.randomUUID()
            val entities = listOf(createRunEntity(scenarioId = scenarioId))

            coEvery { runRepository.findByScenarioId(scenarioId) } returns entities

            val results = service.findByScenarioId(ScenarioId(scenarioId))

            assertEquals(1, results.size)
        }

        @Test
        fun `findLatestByScenarioId returns most recent run`() = runTest {
            val scenarioId = UUID.randomUUID()
            val entity = createRunEntity(scenarioId = scenarioId)

            coEvery { runRepository.findLatestByScenarioId(scenarioId) } returns entity

            val result = service.findLatestByScenarioId(ScenarioId(scenarioId))

            assertNotNull(result)
        }

        @Test
        fun `findByPackageId returns runs for package`() = runTest {
            val packageId = UUID.randomUUID()
            val entities = listOf(createRunEntity(qaPackageId = packageId))

            coEvery { runRepository.findByQaPackageId(packageId) } returns entities

            val results = service.findByPackageId(QaPackageId(packageId))

            assertEquals(1, results.size)
        }

        @Test
        fun `findByStatus returns runs with status`() = runTest {
            val entities = listOf(createRunEntity(status = "PASSED"))

            coEvery { runRepository.findByStatus("PASSED") } returns entities

            val results = service.findByStatus(TestRunStatus.PASSED)

            assertEquals(1, results.size)
        }

        @Test
        fun `findIncomplete returns running runs`() = runTest {
            val entities = listOf(createRunEntity(status = "RUNNING"))

            every { runRepository.findIncompleteRuns() } returns flowOf(*entities.toTypedArray())

            val results = service.findIncomplete().toList()

            assertEquals(1, results.size)
        }
    }

    @Nested
    inner class UpdateTests {
        @Test
        fun `updateStatus changes run status`() = runTest {
            val id = UUID.randomUUID()
            val existing = createRunEntity(id = id, status = "QUEUED")

            coEvery { runRepository.findById(id) } returns existing
            coEvery { runRepository.save(any()) } coAnswers { firstArg() }

            val result = service.updateStatus(TestRunId(id), TestRunStatus.RUNNING)

            assertEquals(TestRunStatus.RUNNING, result.status)
        }

        @Test
        fun `updateStatus throws when run not found`() = runTest {
            val id = UUID.randomUUID()

            coEvery { runRepository.findById(id) } returns null

            assertThrows<TestRunNotFoundException> {
                service.updateStatus(TestRunId(id), TestRunStatus.RUNNING)
            }
        }

        @Test
        fun `markCompleted sets status and completedAt`() = runTest {
            val id = UUID.randomUUID()
            val existing = createRunEntity(id = id, status = "RUNNING")

            coEvery { runRepository.findById(id) } returns existing
            coEvery { runRepository.save(any()) } coAnswers {
                val saved = firstArg<TestRunEntity>()
                saved
            }

            val result = service.markCompleted(TestRunId(id), TestRunStatus.PASSED)

            assertEquals(TestRunStatus.PASSED, result.status)
            assertNotNull(result.completedAt)
        }

        @Test
        fun `markCompleted rejects invalid status`() = runTest {
            val id = UUID.randomUUID()

            assertThrows<IllegalArgumentException> {
                service.markCompleted(TestRunId(id), TestRunStatus.RUNNING)
            }
        }

        @Test
        fun `cancel marks run as cancelled`() = runTest {
            val id = UUID.randomUUID()
            val existing = createRunEntity(id = id, status = "RUNNING")

            coEvery { runRepository.findById(id) } returns existing
            coEvery { runRepository.save(any()) } coAnswers { firstArg() }

            val result = service.cancel(TestRunId(id))

            assertEquals(TestRunStatus.CANCELLED, result.status)
        }

        @Test
        fun `cancel throws for already completed run`() = runTest {
            val id = UUID.randomUUID()
            val existing = createRunEntity(id = id, status = "PASSED")

            coEvery { runRepository.findById(id) } returns existing

            assertThrows<IllegalStateException> {
                service.cancel(TestRunId(id))
            }
        }
    }

    @Nested
    inner class DeleteTests {
        @Test
        fun `delete removes run and results when exists`() = runTest {
            val id = UUID.randomUUID()

            coEvery { runRepository.existsById(id) } returns true
            coEvery { stepResultRepository.deleteByRunId(id) } just runs
            coEvery { runRepository.deleteById(id) } just runs

            val result = service.delete(TestRunId(id))

            assertTrue(result)
            coVerify { stepResultRepository.deleteByRunId(id) }
            coVerify { runRepository.deleteById(id) }
        }

        @Test
        fun `delete returns false when not exists`() = runTest {
            val id = UUID.randomUUID()

            coEvery { runRepository.existsById(id) } returns false

            val result = service.delete(TestRunId(id))

            assertFalse(result)
            coVerify(exactly = 0) { runRepository.deleteById(any()) }
        }
    }

    @Nested
    inner class CountTests {
        @Test
        fun `countByStatus returns count`() = runTest {
            coEvery { runRepository.countByStatus("PASSED") } returns 10

            val result = service.countByStatus(TestRunStatus.PASSED)

            assertEquals(10, result)
        }

        @Test
        fun `countByPackageId returns count`() = runTest {
            val packageId = UUID.randomUUID()

            coEvery { runRepository.countByQaPackageId(packageId) } returns 5

            val result = service.countByPackageId(QaPackageId(packageId))

            assertEquals(5, result)
        }

        @Test
        fun `countPassedByPackageId returns passed count`() = runTest {
            val packageId = UUID.randomUUID()

            coEvery { runRepository.countPassedByQaPackageId(packageId) } returns 3

            val result = service.countPassedByPackageId(QaPackageId(packageId))

            assertEquals(3, result)
        }

        @Test
        fun `countFailedByPackageId returns failed count`() = runTest {
            val packageId = UUID.randomUUID()

            coEvery { runRepository.countFailedByQaPackageId(packageId) } returns 2

            val result = service.countFailedByPackageId(QaPackageId(packageId))

            assertEquals(2, result)
        }
    }

    @Nested
    inner class ExecutionContextTests {
        @Test
        fun `resolve replaces variables with extracted values`() {
            val context = ExecutionContext(
                extractedValues = mutableMapOf("userId" to "123"),
                environment = mapOf("API_KEY" to "secret")
            )

            val result = context.resolve("/users/\${userId}")

            assertEquals("/users/123", result)
        }

        @Test
        fun `resolve replaces environment variables`() {
            val context = ExecutionContext(
                extractedValues = mutableMapOf(),
                environment = mapOf("API_KEY" to "secret")
            )

            val result = context.resolve("Bearer \${env.API_KEY}")

            assertEquals("Bearer secret", result)
        }

        @Test
        fun `addExtracted updates extracted values`() {
            val context = ExecutionContext()

            context.addExtracted(mapOf("token" to "abc"))

            assertEquals("abc", context.extractedValues["token"])
        }
    }

    private fun createRunEntity(
        id: UUID = UUID.randomUUID(),
        scenarioId: UUID = UUID.randomUUID(),
        qaPackageId: UUID? = null,
        status: String = "QUEUED"
    ) = TestRunEntity(
        id = id,
        scenarioId = scenarioId,
        qaPackageId = qaPackageId,
        triggeredBy = "test-user",
        baseUrl = "https://api.example.com",
        status = status,
        environmentJson = null,
        startedAt = Instant.now(),
        completedAt = if (status in listOf("PASSED", "FAILED", "ERROR", "CANCELLED")) Instant.now() else null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createStepResultEntity(
        runId: UUID,
        stepIndex: Int = 0,
        passed: Boolean = true
    ) = TestStepResultEntity(
        id = UUID.randomUUID(),
        runId = runId,
        stepIndex = stepIndex,
        stepName = "Step $stepIndex",
        actualStatus = 200,
        actualHeadersJson = null,
        actualBody = """{"status": "ok"}""",
        passed = passed,
        assertionsJson = null,
        extractedValuesJson = null,
        errorMessage = null,
        durationMs = 100,
        executedAt = Instant.now()
    )
}
