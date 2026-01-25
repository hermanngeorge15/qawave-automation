package com.qawave.unit

import com.qawave.application.service.*
import com.qawave.domain.model.*
import com.qawave.domain.repository.QaPackageRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for QaPackageService.
 */
class QaPackageServiceTest {
    private lateinit var repository: QaPackageRepository
    private lateinit var service: QaPackageService

    @BeforeEach
    fun setup() {
        repository = mockk()
        service = QaPackageServiceImpl(repository)
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
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                    )

                coEvery { repository.save(any()) } answers { firstArg() }

                val result = service.create(command)

                assertNotNull(result.id)
                assertEquals(command.name, result.name)
                assertEquals(command.description, result.description)
                assertEquals(command.baseUrl, result.baseUrl)
                assertEquals(command.triggeredBy, result.triggeredBy)
                assertEquals(QaPackageStatus.REQUESTED, result.status)

                coVerify { repository.save(any()) }
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

                coEvery { repository.save(any()) } answers { firstArg() }

                val result = service.create(command)

                assertNotNull(result.specHash)
                assertTrue(result.specHash!!.length == 64) // SHA-256 hex length
            }

        @Test
        fun `should use default config when not provided`() =
            runTest {
                val command =
                    CreateQaPackageCommand(
                        name = "Test Package",
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                    )

                coEvery { repository.save(any()) } answers { firstArg() }

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
                        baseUrl = "https://api.example.com",
                        triggeredBy = "test-user",
                        config = customConfig,
                    )

                coEvery { repository.save(any()) } answers { firstArg() }

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
                val expected = createTestPackage(id)

                coEvery { repository.findById(id) } returns expected

                val result = service.findById(id)

                assertEquals(expected, result)
            }

        @Test
        fun `should return null when not found`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { repository.findById(id) } returns null

                val result = service.findById(id)

                assertNull(result)
            }
    }

    @Nested
    inner class FindAllTests {
        @Test
        fun `should return paginated results`() =
            runTest {
                val packages = (1..25).map { createTestPackage(QaPackageId.generate(), name = "Package $it") }

                coEvery { repository.findAll() } returns flowOf(*packages.toTypedArray())

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
                val packages = (1..25).map { createTestPackage(QaPackageId.generate(), name = "Package $it") }

                coEvery { repository.findAll() } returns flowOf(*packages.toTypedArray())

                val lastPage = service.findAll(page = 2, size = 10)

                assertEquals(5, lastPage.content.size)
                assertEquals(2, lastPage.page)
                assertFalse(lastPage.hasNext)
                assertTrue(lastPage.hasPrevious)
            }

        @Test
        fun `should return empty page when no packages`() =
            runTest {
                coEvery { repository.findAll() } returns flowOf()

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
                val packages =
                    listOf(
                        createTestPackage(QaPackageId.generate(), status = status),
                        createTestPackage(QaPackageId.generate(), status = status),
                    )

                coEvery { repository.findByStatus(status) } returns packages

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
                val existing = createTestPackage(id, status = QaPackageStatus.REQUESTED)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

                val result = service.updateStatus(id, QaPackageStatus.SPEC_FETCHED)

                assertEquals(QaPackageStatus.SPEC_FETCHED, result.status)
                coVerify { repository.save(match { it.status == QaPackageStatus.SPEC_FETCHED }) }
            }

        @Test
        fun `should throw exception when package not found`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { repository.findById(id) } returns null

                assertThrows<PackageNotFoundException> {
                    service.updateStatus(id, QaPackageStatus.SPEC_FETCHED)
                }
            }

        @Test
        fun `should throw exception for invalid status transition`() =
            runTest {
                val id = QaPackageId.generate()
                val existing = createTestPackage(id, status = QaPackageStatus.REQUESTED)

                coEvery { repository.findById(id) } returns existing

                assertThrows<InvalidStatusTransitionException> {
                    service.updateStatus(id, QaPackageStatus.COMPLETE)
                }
            }

        @Test
        fun `should allow transition to CANCELLED from any state`() =
            runTest {
                val id = QaPackageId.generate()
                val existing = createTestPackage(id, status = QaPackageStatus.EXECUTION_IN_PROGRESS)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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
                val existing = createTestPackage(id, name = "Original Name")

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

                val command =
                    UpdateQaPackageCommand(
                        name = "New Name",
                        description = "New Description",
                    )

                val result = service.update(id, command)

                assertEquals("New Name", result.name)
                assertEquals("New Description", result.description)
                assertEquals(existing.baseUrl, result.baseUrl) // Unchanged
            }

        @Test
        fun `should not change fields not in command`() =
            runTest {
                val id = QaPackageId.generate()
                val existing =
                    createTestPackage(
                        id,
                        name = "Original Name",
                        description = "Original Description",
                        baseUrl = "https://original.com",
                    )

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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

                coEvery { repository.findById(id) } returns null

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
                val existing = createTestPackage(id)
                val coverage =
                    CoverageReport(
                        totalOperations = 10,
                        coveredOperations = 8,
                        coveragePercentage = 80.0,
                        uncoveredOperations = 2,
                    )

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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
                val existing = createTestPackage(id)
                val summary =
                    QaSummary(
                        overallVerdict = QaVerdict.PASS,
                        summary = "All tests passed",
                        passedScenarios = 10,
                        failedScenarios = 0,
                        erroredScenarios = 0,
                    )

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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
                val existing = createTestPackage(id)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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
                val existing = createTestPackage(id, status = QaPackageStatus.QA_EVAL_DONE)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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
                val existing = createTestPackage(id)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

                val result = service.markFailed(id, QaPackageStatus.FAILED_EXECUTION)

                assertEquals(QaPackageStatus.FAILED_EXECUTION, result.status)
                assertNotNull(result.completedAt)
            }

        @Test
        fun `should throw exception for non-failure status`() =
            runTest {
                val id = QaPackageId.generate()
                val existing = createTestPackage(id)

                coEvery { repository.findById(id) } returns existing

                assertThrows<IllegalArgumentException> {
                    service.markFailed(id, QaPackageStatus.COMPLETE)
                }
            }

        @Test
        fun `should allow CANCELLED as failure status`() =
            runTest {
                val id = QaPackageId.generate()
                val existing = createTestPackage(id)

                coEvery { repository.findById(id) } returns existing
                coEvery { repository.save(any()) } answers { firstArg() }

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

                coEvery { repository.existsById(id) } returns true
                coEvery { repository.delete(id) } returns true

                val result = service.delete(id)

                assertTrue(result)
                coVerify { repository.delete(id) }
            }

        @Test
        fun `should return false when package does not exist`() =
            runTest {
                val id = QaPackageId.generate()

                coEvery { repository.existsById(id) } returns false

                val result = service.delete(id)

                assertFalse(result)
                coVerify(exactly = 0) { repository.delete(any()) }
            }
    }

    @Nested
    inner class CountTests {
        @Test
        fun `should return total count`() =
            runTest {
                coEvery { repository.count() } returns 42

                val result = service.count()

                assertEquals(42, result)
            }

        @Test
        fun `should return count by status`() =
            runTest {
                val status = QaPackageStatus.COMPLETE

                coEvery { repository.countByStatus(status) } returns 15

                val result = service.countByStatus(status)

                assertEquals(15, result)
            }
    }

    @Nested
    inner class FindStreamTests {
        @Test
        fun `findAllStream should return flow of packages`() =
            runTest {
                val packages =
                    listOf(
                        createTestPackage(QaPackageId.generate()),
                        createTestPackage(QaPackageId.generate()),
                    )

                coEvery { repository.findAll() } returns flowOf(*packages.toTypedArray())

                val result = service.findAllStream().toList()

                assertEquals(2, result.size)
            }

        @Test
        fun `findIncomplete should return flow of incomplete packages`() =
            runTest {
                val packages =
                    listOf(
                        createTestPackage(QaPackageId.generate(), status = QaPackageStatus.EXECUTION_IN_PROGRESS),
                    )

                every { repository.findIncomplete() } returns flowOf(*packages.toTypedArray())

                val result = service.findIncomplete().toList()

                assertEquals(1, result.size)
                assertTrue(result.all { it.isInProgress })
            }

        @Test
        fun `findRecent should return packages since given time`() =
            runTest {
                val since = Instant.now().minusSeconds(3600)
                val packages =
                    listOf(
                        createTestPackage(QaPackageId.generate()),
                    )

                every { repository.findRecent(since) } returns flowOf(*packages.toTypedArray())

                val result = service.findRecent(since).toList()

                assertEquals(1, result.size)
            }
    }

    // Helper function to create test packages
    private fun createTestPackage(
        id: QaPackageId,
        name: String = "Test Package",
        description: String? = "Test description",
        baseUrl: String = "https://api.example.com",
        status: QaPackageStatus = QaPackageStatus.REQUESTED
    ): QaPackage {
        return QaPackage(
            id = id,
            name = name,
            description = description,
            baseUrl = baseUrl,
            status = status,
            triggeredBy = "test-user",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }
}
