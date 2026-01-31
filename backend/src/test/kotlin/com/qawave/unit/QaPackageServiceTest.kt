package com.qawave.unit

import com.qawave.application.service.*
import com.qawave.domain.model.*
import com.qawave.infrastructure.persistence.entity.QaPackageEntity
import com.qawave.infrastructure.persistence.mapper.QaPackageMapper
import com.qawave.infrastructure.persistence.repository.QaPackageR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestRunR2dbcRepository
import com.qawave.infrastructure.persistence.repository.TestScenarioR2dbcRepository
import io.mockk.*
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

/**
 * Comprehensive unit tests for QaPackageService.
 */
class QaPackageServiceTest {
    private lateinit var packageRepository: QaPackageR2dbcRepository
    private lateinit var scenarioRepository: TestScenarioR2dbcRepository
    private lateinit var runRepository: TestRunR2dbcRepository
    private lateinit var mapper: QaPackageMapper
    private lateinit var service: QaPackageService

    @BeforeEach
    fun setup() {
        packageRepository = mockk()
        scenarioRepository = mockk()
        runRepository = mockk()
        mapper = mockk()
        service = QaPackageServiceImpl(packageRepository, scenarioRepository, runRepository, mapper)
    }

    @Nested
    inner class CreateTests {
        @Test
        fun `should create package with valid command`() =
            runTest {
                val command =
                    CreateQaPackageCommand(
                        name = "Test Package",
                        description = "A test package",
                        specUrl = "https://api.example.com/openapi.yaml",
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                    )

                val savedEntity = createTestEntity(UUID.randomUUID())
                val expectedPackage = createTestPackage(QaPackageId(savedEntity.id!!))

                coEvery { packageRepository.save(any()) } returns savedEntity
                every { mapper.toNewEntity(any()) } returns savedEntity.copy(id = null)
                every { mapper.toDomain(savedEntity) } returns expectedPackage

                val result = service.create(command)

                assertNotNull(result.id)
                assertEquals(command.name, result.name)
                assertEquals(command.triggeredBy, result.triggeredBy)

                coVerify { packageRepository.save(any()) }
            }

        @Test
        fun `should compute spec hash when specContent provided`() =
            runTest {
                val command =
                    CreateQaPackageCommand(
                        name = "Test Package",
                        baseUrl = "https://api.example.com",
                        specContent = "openapi: 3.0.0\ninfo:\n  title: Test API",
                        triggeredBy = "test-user",
                    )

                val savedEntity = createTestEntity(UUID.randomUUID())
                val expectedPackage =
                    createTestPackage(
                        QaPackageId(savedEntity.id!!),
                        specContent = command.specContent,
                        specHash = "somehash",
                    )

                coEvery { packageRepository.save(any()) } returns savedEntity
                every { mapper.toNewEntity(any()) } returns savedEntity.copy(id = null)
                every { mapper.toDomain(savedEntity) } returns expectedPackage

                val result = service.create(command)

                assertNotNull(result.specHash)
            }

        @Test
        fun `should use default config when not provided`() =
            runTest {
                val command =
                    CreateQaPackageCommand(
                        name = "Test Package",
                        specUrl = "https://api.example.com/openapi.yaml",
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                    )

                val savedEntity = createTestEntity(UUID.randomUUID())
                val expectedPackage = createTestPackage(QaPackageId(savedEntity.id!!))

                coEvery { packageRepository.save(any()) } returns savedEntity
                every { mapper.toNewEntity(any()) } returns savedEntity.copy(id = null)
                every { mapper.toDomain(savedEntity) } returns expectedPackage

                val result = service.create(command)

                assertEquals(10, result.config.maxScenarios)
                assertEquals(10, result.config.maxStepsPerScenario)
                assertTrue(result.config.parallelExecution)
            }

        @Test
        fun `should use custom config when provided`() =
            runTest {
                val customConfig =
                    QaPackageConfig(
                        maxScenarios = 20,
                        maxStepsPerScenario = 15,
                        parallelExecution = false,
                        stopOnFirstFailure = true,
                    )

                val command =
                    CreateQaPackageCommand(
                        name = "Test Package",
                        specUrl = "https://api.example.com/openapi.yaml",
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                        config = customConfig,
                    )

                val savedEntity = createTestEntity(UUID.randomUUID())
                val expectedPackage = createTestPackage(QaPackageId(savedEntity.id!!), config = customConfig)

                coEvery { packageRepository.save(any()) } returns savedEntity
                every { mapper.toNewEntity(any()) } returns savedEntity.copy(id = null)
                every { mapper.toDomain(savedEntity) } returns expectedPackage

                val result = service.create(command)

                assertEquals(20, result.config.maxScenarios)
                assertEquals(15, result.config.maxStepsPerScenario)
                assertFalse(result.config.parallelExecution)
                assertTrue(result.config.stopOnFirstFailure)
            }
    }

    @Nested
    inner class FindByIdTests {
        @Test
        fun `should return package when found`() =
            runTest {
                val id = QaPackageId.generate()
                val entity = createTestEntity(id.value)
                val expected = createTestPackage(id)

                coEvery { packageRepository.findById(id.value) } returns entity
                every { mapper.toDomain(entity) } returns expected

                val result = service.findById(id)

                assertEquals(expected, result)
            }

        @Test
        fun `should return null when not found`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { packageRepository.findById(id.value) } returns null

                val result = service.findById(id)

                assertNull(result)
            }
    }

    @Nested
    inner class FindAllTests {
        @Test
        fun `should return paginated results`() =
            runTest {
                val entities = (1..25).map { createTestEntity(UUID.randomUUID(), name = "Package $it") }
                val packages = entities.map { createTestPackage(QaPackageId(it.id!!), name = it.name) }

                coEvery { packageRepository.findAll() } returns flowOf(*entities.toTypedArray())
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val page0 = service.findAll(page = 0, size = 10)

                assertEquals(10, page0.content.size)
                assertEquals(0, page0.page)
                assertEquals(10, page0.size)
                assertEquals(25, page0.totalElements)
                assertEquals(3, page0.totalPages)
                assertTrue(page0.hasNext)
                assertFalse(page0.hasPrevious)
            }

        @Test
        fun `should return last page correctly`() =
            runTest {
                val entities = (1..25).map { createTestEntity(UUID.randomUUID(), name = "Package $it") }
                val packages = entities.map { createTestPackage(QaPackageId(it.id!!), name = it.name) }

                coEvery { packageRepository.findAll() } returns flowOf(*entities.toTypedArray())
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val lastPage = service.findAll(page = 2, size = 10)

                assertEquals(5, lastPage.content.size)
                assertEquals(2, lastPage.page)
                assertFalse(lastPage.hasNext)
                assertTrue(lastPage.hasPrevious)
            }

        @Test
        fun `should return empty page when no packages`() =
            runTest {
                coEvery { packageRepository.findAll() } returns flowOf()

                val result = service.findAll()

                assertTrue(result.content.isEmpty())
                assertEquals(0, result.totalElements)
                assertEquals(1, result.totalPages)
            }
    }

    @Nested
    inner class FindByStatusTests {
        @Test
        fun `should return packages with matching status`() =
            runTest {
                val status = QaPackageStatus.EXECUTION_IN_PROGRESS
                val entities =
                    listOf(
                        createTestEntity(UUID.randomUUID(), status = status.name),
                        createTestEntity(UUID.randomUUID(), status = status.name),
                    )
                val packages = entities.map { createTestPackage(QaPackageId(it.id!!), status = status) }

                coEvery { packageRepository.findByStatus(status.name) } returns entities
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val result = service.findByStatus(status)

                assertEquals(2, result.size)
                assertTrue(result.all { it.status == status })
            }
    }

    @Nested
    inner class UpdateStatusTests {
        @Test
        fun `should update status when transition is valid`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value, status = QaPackageStatus.REQUESTED.name)
                val updatedEntity = existingEntity.copy(status = QaPackageStatus.SPEC_FETCHED.name)
                val updatedPackage = createTestPackage(id, status = QaPackageStatus.SPEC_FETCHED)

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.updateStatus(id, QaPackageStatus.SPEC_FETCHED)

                assertEquals(QaPackageStatus.SPEC_FETCHED, result.status)
                coVerify { packageRepository.save(match { it.status == QaPackageStatus.SPEC_FETCHED.name }) }
            }

        @Test
        fun `should throw exception when package not found`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { packageRepository.findById(id.value) } returns null

                assertThrows<PackageNotFoundException> {
                    service.updateStatus(id, QaPackageStatus.SPEC_FETCHED)
                }
            }

        @Test
        fun `should throw exception for invalid status transition`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value, status = QaPackageStatus.REQUESTED.name)

                coEvery { packageRepository.findById(id.value) } returns existingEntity

                assertThrows<InvalidStatusTransitionException> {
                    service.updateStatus(id, QaPackageStatus.COMPLETE)
                }
            }

        @Test
        fun `should allow transition to CANCELLED from any state`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value, status = QaPackageStatus.EXECUTION_IN_PROGRESS.name)
                val updatedEntity = existingEntity.copy(status = QaPackageStatus.CANCELLED.name)
                val updatedPackage = createTestPackage(id, status = QaPackageStatus.CANCELLED)

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.updateStatus(id, QaPackageStatus.CANCELLED)

                assertEquals(QaPackageStatus.CANCELLED, result.status)
            }
    }

    @Nested
    inner class UpdateTests {
        @Test
        fun `should update package with provided fields`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value, name = "Original Name")
                val updatedEntity = existingEntity.copy(name = "New Name", description = "New Description")
                val updatedPackage = createTestPackage(id, name = "New Name", description = "New Description")

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val command =
                    UpdateQaPackageCommand(
                        name = "New Name",
                        description = "New Description",
                    )

                val result = service.update(id, command)

                assertEquals("New Name", result.name)
                assertEquals("New Description", result.description)
            }

        @Test
        fun `should not change fields not in command`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity =
                    createTestEntity(
                        id.value,
                        name = "Original Name",
                        description = "Original Description",
                        baseUrl = "https://original.com",
                    )
                val updatedEntity = existingEntity.copy(name = "New Name")
                val updatedPackage =
                    createTestPackage(
                        id,
                        name = "New Name",
                        description = "Original Description",
                        baseUrl = "https://original.com",
                    )

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val command = UpdateQaPackageCommand(name = "New Name")

                val result = service.update(id, command)

                assertEquals("New Name", result.name)
                assertEquals("Original Description", result.description)
                assertEquals("https://original.com", result.baseUrl)
            }

        @Test
        fun `should throw exception when package not found`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { packageRepository.findById(id.value) } returns null

                assertThrows<PackageNotFoundException> {
                    service.update(id, UpdateQaPackageCommand(name = "New Name"))
                }
            }
    }

    @Nested
    inner class SetCoverageTests {
        @Test
        fun `should set coverage report`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)
                val coverage =
                    CoverageReport(
                        totalOperations = 10,
                        coveredOperations = 8,
                        coveragePercentage = 80.0,
                        generatedAt = Instant.now(),
                    )
                val existingPackage = createTestPackage(id)
                val updatedPackage = existingPackage.copy(coverage = coverage)
                val updatedEntity = existingEntity.copy(coverageJson = "{}")

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                every { mapper.toDomain(existingEntity) } returns existingPackage
                every { mapper.toEntity(any()) } returns updatedEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.setCoverage(id, coverage)

                assertNotNull(result.coverage)
                assertEquals(80.0, result.coverage!!.coveragePercentage)
            }
    }

    @Nested
    inner class SetQaSummaryTests {
        @Test
        fun `should set QA summary`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)
                val summary =
                    QaSummary(
                        overallVerdict = QaVerdict.PASS,
                        summary = "All tests passed",
                        passedScenarios = 10,
                        failedScenarios = 0,
                        erroredScenarios = 0,
                        generatedAt = Instant.now(),
                    )
                val existingPackage = createTestPackage(id)
                val updatedPackage = existingPackage.copy(qaSummary = summary)
                val updatedEntity = existingEntity.copy(qaSummaryJson = "{}")

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                every { mapper.toDomain(existingEntity) } returns existingPackage
                every { mapper.toEntity(any()) } returns updatedEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.setQaSummary(id, summary)

                assertNotNull(result.qaSummary)
                assertEquals(QaVerdict.PASS, result.qaSummary!!.overallVerdict)
            }
    }

    @Nested
    inner class MarkStartedTests {
        @Test
        fun `should set startedAt timestamp`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)
                val updatedEntity = existingEntity.copy(startedAt = Instant.now())
                val updatedPackage = createTestPackage(id, startedAt = Instant.now())

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.markStarted(id)

                assertNotNull(result.startedAt)
            }
    }

    @Nested
    inner class MarkCompletedTests {
        @Test
        fun `should set completedAt timestamp and COMPLETE status`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value, status = QaPackageStatus.QA_EVAL_DONE.name)
                val updatedEntity =
                    existingEntity.copy(
                        status = QaPackageStatus.COMPLETE.name,
                        completedAt = Instant.now(),
                    )
                val updatedPackage =
                    createTestPackage(
                        id,
                        status = QaPackageStatus.COMPLETE,
                        completedAt = Instant.now(),
                    )

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.markCompleted(id)

                assertNotNull(result.completedAt)
                assertEquals(QaPackageStatus.COMPLETE, result.status)
            }
    }

    @Nested
    inner class MarkFailedTests {
        @Test
        fun `should set failed status`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)
                val updatedEntity =
                    existingEntity.copy(
                        status = QaPackageStatus.FAILED_EXECUTION.name,
                        completedAt = Instant.now(),
                    )
                val updatedPackage =
                    createTestPackage(
                        id,
                        status = QaPackageStatus.FAILED_EXECUTION,
                        completedAt = Instant.now(),
                    )

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.markFailed(id, QaPackageStatus.FAILED_EXECUTION)

                assertEquals(QaPackageStatus.FAILED_EXECUTION, result.status)
                assertNotNull(result.completedAt)
            }

        @Test
        fun `should throw exception for non-failure status`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)

                coEvery { packageRepository.findById(id.value) } returns existingEntity

                assertThrows<IllegalArgumentException> {
                    service.markFailed(id, QaPackageStatus.COMPLETE)
                }
            }

        @Test
        fun `should allow CANCELLED as failure status`() =
            runTest {
                val id = QaPackageId.generate()
                val existingEntity = createTestEntity(id.value)
                val updatedEntity =
                    existingEntity.copy(
                        status = QaPackageStatus.CANCELLED.name,
                        completedAt = Instant.now(),
                    )
                val updatedPackage =
                    createTestPackage(
                        id,
                        status = QaPackageStatus.CANCELLED,
                        completedAt = Instant.now(),
                    )

                coEvery { packageRepository.findById(id.value) } returns existingEntity
                coEvery { packageRepository.save(any()) } returns updatedEntity
                every { mapper.toDomain(updatedEntity) } returns updatedPackage

                val result = service.markFailed(id, QaPackageStatus.CANCELLED)

                assertEquals(QaPackageStatus.CANCELLED, result.status)
            }
    }

    @Nested
    inner class DeleteTests {
        @Test
        fun `should delete package and return true when exists`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { packageRepository.existsById(id.value) } returns true
                coEvery { runRepository.deleteByQaPackageId(id.value) } just Runs
                coEvery { scenarioRepository.deleteByQaPackageId(id.value) } just Runs
                coEvery { packageRepository.deleteById(id.value) } just Runs

                val result = service.delete(id)

                assertTrue(result)
                coVerify { packageRepository.deleteById(id.value) }
            }

        @Test
        fun `should return false when package does not exist`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { packageRepository.existsById(id.value) } returns false

                val result = service.delete(id)

                assertFalse(result)
                coVerify(exactly = 0) { packageRepository.deleteById(any()) }
            }
    }

    @Nested
    inner class CountTests {
        @Test
        fun `should return total count`() =
            runTest {
                coEvery { packageRepository.count() } returns 42

                val result = service.count()

                assertEquals(42, result)
            }

        @Test
        fun `should return count by status`() =
            runTest {
                val status = QaPackageStatus.COMPLETE

                coEvery { packageRepository.countByStatus(status.name) } returns 15

                val result = service.countByStatus(status)

                assertEquals(15, result)
            }
    }

    @Nested
    inner class FindStreamTests {
        @Test
        fun `findAllStream should return flow of packages`() =
            runTest {
                val entities =
                    listOf(
                        createTestEntity(UUID.randomUUID()),
                        createTestEntity(UUID.randomUUID()),
                    )
                val packages = entities.map { createTestPackage(QaPackageId(it.id!!)) }

                coEvery { packageRepository.findAll() } returns flowOf(*entities.toTypedArray())
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val result = service.findAllStream().toList()

                assertEquals(2, result.size)
            }

        @Test
        fun `findIncomplete should return flow of incomplete packages`() =
            runTest {
                val entities =
                    listOf(
                        createTestEntity(UUID.randomUUID(), status = QaPackageStatus.EXECUTION_IN_PROGRESS.name),
                    )
                val packages =
                    entities.map {
                        createTestPackage(QaPackageId(it.id!!), status = QaPackageStatus.EXECUTION_IN_PROGRESS)
                    }

                every { packageRepository.findIncompletePackages() } returns flowOf(*entities.toTypedArray())
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val result = service.findIncomplete().toList()

                assertEquals(1, result.size)
                assertTrue(result.all { !it.isComplete })
            }

        @Test
        fun `findRecent should return packages since given time`() =
            runTest {
                val since = Instant.now().minusSeconds(3600)
                val entities =
                    listOf(
                        createTestEntity(UUID.randomUUID()),
                    )
                val packages = entities.map { createTestPackage(QaPackageId(it.id!!)) }

                every { packageRepository.findRecentPackages(since) } returns flowOf(*entities.toTypedArray())
                entities.forEachIndexed { index, entity ->
                    every { mapper.toDomain(entity) } returns packages[index]
                }

                val result = service.findRecent(since).toList()

                assertEquals(1, result.size)
            }
    }

    // Helper function to create test entities
    private fun createTestEntity(
        id: UUID,
        name: String = "Test Package",
        description: String? = "Test description",
        baseUrl: String = "https://api.example.com",
        status: String = QaPackageStatus.REQUESTED.name,
    ): QaPackageEntity {
        return QaPackageEntity(
            id = id,
            name = name,
            description = description,
            specUrl = "https://api.example.com/openapi.yaml",
            specContent = null,
            specHash = null,
            baseUrl = baseUrl,
            requirements = null,
            status = status,
            configJson = null,
            coverageJson = null,
            qaSummaryJson = null,
            triggeredBy = "test-user",
            startedAt = null,
            completedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }

    // Helper function to create test packages
    @Suppress("LongParameterList")
    private fun createTestPackage(
        id: QaPackageId,
        name: String = "Test Package",
        description: String? = "Test description",
        baseUrl: String = "https://api.example.com",
        status: QaPackageStatus = QaPackageStatus.REQUESTED,
        config: QaPackageConfig = QaPackageConfig(),
        specUrl: String? = "https://api.example.com/openapi.yaml",
        specContent: String? = null,
        specHash: String? = null,
        startedAt: Instant? = null,
        completedAt: Instant? = null,
    ): QaPackage {
        return QaPackage(
            id = id,
            name = name,
            description = description,
            specUrl = specUrl,
            specContent = specContent,
            specHash = specHash,
            baseUrl = baseUrl,
            requirements = null,
            status = status,
            config = config,
            triggeredBy = "test-user",
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }
}
