package com.qawave.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qawave.application.service.*
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.TestScenarioEntity
import com.qawave.infrastructure.persistence.mapper.TestScenarioMapper
import com.qawave.infrastructure.persistence.repository.TestScenarioR2dbcRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
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

class ScenarioServiceTest {

    @MockK
    private lateinit var repository: TestScenarioR2dbcRepository

    private lateinit var objectMapper: ObjectMapper
    private lateinit var mapper: TestScenarioMapper
    private lateinit var service: ScenarioServiceImpl

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        objectMapper = jacksonObjectMapper().findAndRegisterModules()
        mapper = TestScenarioMapper(objectMapper)
        service = ScenarioServiceImpl(repository, mapper)
    }

    @Nested
    inner class CreateTests {
        @Test
        fun `create saves and returns scenario`() = runTest {
            val command = createCommand()
            val entitySlot = slot<TestScenarioEntity>()

            coEvery { repository.save(capture(entitySlot)) } coAnswers {
                entitySlot.captured.copy(id = entitySlot.captured.id ?: UUID.randomUUID())
            }

            val result = service.create(command)

            assertNotNull(result)
            assertEquals(command.name, result.name)
            assertEquals(command.source, result.source)
            assertEquals(command.steps.size, result.steps.size)

            coVerify { repository.save(any()) }
        }

        @Test
        fun `createBatch saves multiple scenarios`() = runTest {
            val commands = listOf(
                createCommand(name = "Scenario 1"),
                createCommand(name = "Scenario 2")
            )

            coEvery { repository.saveAll(any<List<TestScenarioEntity>>()) } returns flowOf(
                createEntity(name = "Scenario 1"),
                createEntity(name = "Scenario 2")
            )

            val results = service.createBatch(commands)

            assertEquals(2, results.size)
            assertEquals("Scenario 1", results[0].name)
            assertEquals("Scenario 2", results[1].name)
        }
    }

    @Nested
    inner class FindTests {
        @Test
        fun `findById returns scenario when found`() = runTest {
            val id = UUID.randomUUID()
            val entity = createEntity(id = id)

            coEvery { repository.findById(id) } returns entity

            val result = service.findById(ScenarioId(id))

            assertNotNull(result)
            assertEquals(id, result.id.value)
        }

        @Test
        fun `findById returns null when not found`() = runTest {
            val id = UUID.randomUUID()

            coEvery { repository.findById(id) } returns null

            val result = service.findById(ScenarioId(id))

            assertNull(result)
        }

        @Test
        fun `findByPackageId returns scenarios for package`() = runTest {
            val packageId = UUID.randomUUID()
            val entities = listOf(
                createEntity(qaPackageId = packageId),
                createEntity(qaPackageId = packageId)
            )

            coEvery { repository.findByQaPackageId(packageId) } returns entities

            val results = service.findByPackageId(QaPackageId(packageId))

            assertEquals(2, results.size)
        }

        @Test
        fun `findBySuiteId returns scenarios for suite`() = runTest {
            val suiteId = UUID.randomUUID()
            val entities = listOf(createEntity(suiteId = suiteId))

            coEvery { repository.findBySuiteId(suiteId) } returns entities

            val results = service.findBySuiteId(TestSuiteId(suiteId))

            assertEquals(1, results.size)
        }

        @Test
        fun `findByStatus returns scenarios with status`() = runTest {
            val entities = listOf(createEntity(status = "READY"))

            coEvery { repository.findByStatus("READY") } returns entities

            val results = service.findByStatus(ScenarioStatus.READY)

            assertEquals(1, results.size)
            assertEquals(ScenarioStatus.READY, results[0].status)
        }

        @Test
        fun `findByStatusStream returns flow of scenarios`() = runTest {
            val entities = listOf(createEntity(status = "PENDING"))

            every { repository.findAllByStatus("PENDING") } returns flowOf(*entities.toTypedArray())

            val results = service.findByStatusStream(ScenarioStatus.PENDING).toList()

            assertEquals(1, results.size)
        }

        @Test
        fun `findBySource returns scenarios by source`() = runTest {
            val entities = listOf(createEntity(source = "AI_GENERATED"))

            coEvery { repository.findBySource("AI_GENERATED") } returns entities

            val results = service.findBySource(ScenarioSource.AI_GENERATED)

            assertEquals(1, results.size)
        }
    }

    @Nested
    inner class UpdateTests {
        @Test
        fun `update modifies scenario fields`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id, name = "Original")
            val command = UpdateScenarioCommand(name = "Updated")

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.update(ScenarioId(id), command)

            assertEquals("Updated", result.name)
        }

        @Test
        fun `update throws when scenario not found`() = runTest {
            val id = UUID.randomUUID()

            coEvery { repository.findById(id) } returns null

            assertThrows<ScenarioNotFoundException> {
                service.update(ScenarioId(id), UpdateScenarioCommand(name = "Test"))
            }
        }

        @Test
        fun `updateStatus changes scenario status`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id, status = "PENDING")

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.updateStatus(ScenarioId(id), ScenarioStatus.READY)

            assertEquals(ScenarioStatus.READY, result.status)
        }

        @Test
        fun `markReady sets status to READY`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id, status = "PENDING")

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.markReady(ScenarioId(id))

            assertEquals(ScenarioStatus.READY, result.status)
        }

        @Test
        fun `markInvalid sets status to INVALID`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id)

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.markInvalid(ScenarioId(id))

            assertEquals(ScenarioStatus.INVALID, result.status)
        }

        @Test
        fun `disable sets status to DISABLED`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id)

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.disable(ScenarioId(id))

            assertEquals(ScenarioStatus.DISABLED, result.status)
        }

        @Test
        fun `enable sets status to PENDING`() = runTest {
            val id = UUID.randomUUID()
            val existing = createEntity(id = id, status = "DISABLED")

            coEvery { repository.findById(id) } returns existing
            coEvery { repository.save(any()) } coAnswers { firstArg() }

            val result = service.enable(ScenarioId(id))

            assertEquals(ScenarioStatus.PENDING, result.status)
        }
    }

    @Nested
    inner class DeleteTests {
        @Test
        fun `delete removes scenario when exists`() = runTest {
            val id = UUID.randomUUID()

            coEvery { repository.existsById(id) } returns true
            coEvery { repository.deleteById(id) } just runs

            val result = service.delete(ScenarioId(id))

            assertTrue(result)
            coVerify { repository.deleteById(id) }
        }

        @Test
        fun `delete returns false when not exists`() = runTest {
            val id = UUID.randomUUID()

            coEvery { repository.existsById(id) } returns false

            val result = service.delete(ScenarioId(id))

            assertFalse(result)
            coVerify(exactly = 0) { repository.deleteById(any()) }
        }

        @Test
        fun `deleteByPackageId removes all scenarios for package`() = runTest {
            val packageId = UUID.randomUUID()

            coEvery { repository.deleteByQaPackageId(packageId) } just runs

            service.deleteByPackageId(QaPackageId(packageId))

            coVerify { repository.deleteByQaPackageId(packageId) }
        }
    }

    @Nested
    inner class CountTests {
        @Test
        fun `countByPackageId returns count`() = runTest {
            val packageId = UUID.randomUUID()

            coEvery { repository.countByQaPackageId(packageId) } returns 5

            val result = service.countByPackageId(QaPackageId(packageId))

            assertEquals(5, result)
        }

        @Test
        fun `countByStatus returns count`() = runTest {
            coEvery { repository.countByStatus("READY") } returns 10

            val result = service.countByStatus(ScenarioStatus.READY)

            assertEquals(10, result)
        }

        @Test
        fun `count returns total count`() = runTest {
            coEvery { repository.count() } returns 100

            val result = service.count()

            assertEquals(100, result)
        }
    }

    private fun createCommand(
        name: String = "Test Scenario",
        packageId: QaPackageId? = null,
        suiteId: TestSuiteId? = null
    ) = CreateScenarioCommand(
        qaPackageId = packageId,
        suiteId = suiteId,
        name = name,
        description = "Test description",
        steps = listOf(createStep()),
        tags = setOf("test"),
        source = ScenarioSource.MANUAL
    )

    private fun createStep(
        index: Int = 0,
        method: HttpMethod = HttpMethod.GET,
        endpoint: String = "/api/test"
    ) = TestStep(
        index = index,
        name = "Step $index",
        method = method,
        endpoint = endpoint,
        expected = ExpectedResult(status = 200)
    )

    private fun createEntity(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Scenario",
        qaPackageId: UUID? = null,
        suiteId: UUID? = null,
        status: String = "PENDING",
        source: String = "MANUAL"
    ): TestScenarioEntity {
        val steps = listOf(createStep())
        return TestScenarioEntity(
            id = id,
            qaPackageId = qaPackageId,
            suiteId = suiteId,
            name = name,
            description = "Test description",
            stepsJson = objectMapper.writeValueAsString(steps),
            tags = """["test"]""",
            source = source,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
