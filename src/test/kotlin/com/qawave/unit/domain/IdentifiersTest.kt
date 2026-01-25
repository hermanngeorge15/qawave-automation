package com.qawave.unit.domain

import com.qawave.domain.model.*
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdentifiersTest {
    @Test
    fun `QaPackageId generates unique IDs`() {
        val id1 = QaPackageId.generate()
        val id2 = QaPackageId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `QaPackageId can be created from UUID`() {
        val uuid = UUID.randomUUID()
        val id = QaPackageId(uuid)
        assertEquals(uuid, id.value)
    }

    @Test
    fun `QaPackageId can be created from string`() {
        val uuidString = "123e4567-e89b-12d3-a456-426614174000"
        val id = QaPackageId.from(uuidString)
        assertEquals(uuidString, id.toString())
    }

    @Test
    fun `ScenarioId generates unique IDs`() {
        val id1 = ScenarioId.generate()
        val id2 = ScenarioId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `TestRunId generates unique IDs`() {
        val id1 = TestRunId.generate()
        val id2 = TestRunId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `TestSuiteId generates unique IDs`() {
        val id1 = TestSuiteId.generate()
        val id2 = TestSuiteId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `ApiSpecId generates unique IDs`() {
        val id1 = ApiSpecId.generate()
        val id2 = ApiSpecId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `RequirementId generates unique IDs`() {
        val id1 = RequirementId.generate()
        val id2 = RequirementId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `value classes with same UUID are equal`() {
        val uuid = UUID.randomUUID()
        val id1 = QaPackageId(uuid)
        val id2 = QaPackageId(uuid)
        assertEquals(id1, id2)
    }

    @Test
    fun `different ID types with same UUID have equal underlying values`() {
        val uuid = UUID.randomUUID()
        val qaPackageId = QaPackageId(uuid)
        val scenarioId = ScenarioId(uuid)
        // They have the same underlying UUID value
        assertEquals(qaPackageId.value, scenarioId.value)
        // But they are different types - this provides compile-time type safety
        // You cannot accidentally use a ScenarioId where a QaPackageId is expected
    }
}
