package com.qawave.unit.domain

import com.qawave.domain.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestScenarioTest {
    @Test
    fun `scenario requires non-blank name`() {
        assertThrows<IllegalArgumentException> {
            createScenario(name = "")
        }
    }

    @Test
    fun `scenario requires at least one step`() {
        assertThrows<IllegalArgumentException> {
            createScenario(steps = emptyList())
        }
    }

    @Test
    fun `scenario requires unique step indices`() {
        assertThrows<IllegalArgumentException> {
            createScenario(
                steps =
                    listOf(
                        createStep(index = 0),
                        createStep(index = 0), // Duplicate index
                    ),
            )
        }
    }

    @Test
    fun `stepCount returns correct number`() {
        val scenario =
            createScenario(
                steps =
                    listOf(
                        createStep(index = 0),
                        createStep(index = 1),
                        createStep(index = 2),
                    ),
            )
        assertEquals(3, scenario.stepCount)
    }

    @Test
    fun `orderedSteps returns steps sorted by index`() {
        val scenario =
            createScenario(
                steps =
                    listOf(
                        createStep(index = 2),
                        createStep(index = 0),
                        createStep(index = 1),
                    ),
            )
        val ordered = scenario.orderedSteps
        assertEquals(0, ordered[0].index)
        assertEquals(1, ordered[1].index)
        assertEquals(2, ordered[2].index)
    }

    @Test
    fun `coveredOperations returns unique method and endpoint combinations`() {
        val scenario =
            createScenario(
                steps =
                    listOf(
                        createStep(index = 0, method = HttpMethod.GET, endpoint = "/users"),
                        createStep(index = 1, method = HttpMethod.POST, endpoint = "/users"),
                        createStep(index = 2, method = HttpMethod.GET, endpoint = "/users"), // Duplicate
                    ),
            )
        val operations = scenario.coveredOperations
        assertEquals(2, operations.size)
        assertTrue(operations.contains("GET /users"))
        assertTrue(operations.contains("POST /users"))
    }

    private fun createScenario(
        name: String = "Test Scenario",
        steps: List<TestStep> = listOf(createStep()),
    ) = TestScenario(
        id = ScenarioId.generate(),
        suiteId = null,
        qaPackageId = null,
        name = name,
        description = "Test description",
        steps = steps,
        source = ScenarioSource.MANUAL,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun createStep(
        index: Int = 0,
        method: HttpMethod = HttpMethod.GET,
        endpoint: String = "/api/test",
    ) = TestStep(
        index = index,
        name = "Step $index",
        method = method,
        endpoint = endpoint,
        expected = ExpectedResult(status = 200),
    )
}
