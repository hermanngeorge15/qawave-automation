package com.qawave.unit.domain

import com.qawave.domain.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestRunTest {

    @Test
    fun `test run requires non-blank baseUrl`() {
        assertThrows<IllegalArgumentException> {
            createTestRun(baseUrl = "")
        }
    }

    @Test
    fun `durationMs returns null when not completed`() {
        val run = createTestRun(completedAt = null)
        assertEquals(null, run.durationMs)
    }

    @Test
    fun `durationMs calculates correctly when completed`() {
        val startedAt = Instant.parse("2026-01-25T10:00:00Z")
        val completedAt = Instant.parse("2026-01-25T10:00:05Z")
        val run = createTestRun(startedAt = startedAt, completedAt = completedAt)
        assertEquals(5000, run.durationMs)
    }

    @Test
    fun `isComplete returns true for terminal statuses`() {
        assertTrue(createTestRun(status = TestRunStatus.PASSED).isComplete)
        assertTrue(createTestRun(status = TestRunStatus.FAILED).isComplete)
        assertTrue(createTestRun(status = TestRunStatus.ERROR).isComplete)
        assertTrue(createTestRun(status = TestRunStatus.CANCELLED).isComplete)
    }

    @Test
    fun `isComplete returns false for non-terminal statuses`() {
        assertFalse(createTestRun(status = TestRunStatus.QUEUED).isComplete)
        assertFalse(createTestRun(status = TestRunStatus.RUNNING).isComplete)
    }

    @Test
    fun `passedSteps and failedSteps count correctly`() {
        val run = createTestRun(
            stepResults = listOf(
                createStepResult(passed = true),
                createStepResult(passed = true),
                createStepResult(passed = false)
            )
        )
        assertEquals(2, run.passedSteps)
        assertEquals(1, run.failedSteps)
        assertEquals(3, run.executedSteps)
    }

    @Test
    fun `passRate calculates correctly`() {
        val run = createTestRun(
            stepResults = listOf(
                createStepResult(passed = true),
                createStepResult(passed = true),
                createStepResult(passed = false),
                createStepResult(passed = false)
            )
        )
        assertEquals(50.0, run.passRate)
    }

    @Test
    fun `passRate returns 0 for empty results`() {
        val run = createTestRun(stepResults = emptyList())
        assertEquals(0.0, run.passRate)
    }

    private fun createTestRun(
        baseUrl: String = "https://api.example.com",
        status: TestRunStatus = TestRunStatus.RUNNING,
        stepResults: List<TestStepResult> = emptyList(),
        startedAt: Instant = Instant.now(),
        completedAt: Instant? = null
    ): TestRun {
        val runId = TestRunId.generate()
        return TestRun(
            id = runId,
            scenarioId = ScenarioId.generate(),
            qaPackageId = null,
            triggeredBy = "test",
            baseUrl = baseUrl,
            status = status,
            stepResults = stepResults,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun createStepResult(passed: Boolean): TestStepResult {
        return TestStepResult(
            runId = TestRunId.generate(),
            stepIndex = 0,
            stepName = "Test step",
            actualStatus = if (passed) 200 else 500,
            passed = passed,
            durationMs = 100,
            executedAt = Instant.now()
        )
    }
}
