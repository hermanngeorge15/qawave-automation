package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for Test Scenario.
 * Maps to the test_scenarios table in PostgreSQL.
 */
@Table("test_scenarios")
data class TestScenarioEntity(
    @Id
    val id: UUID? = null,
    @Column("suite_id")
    val suiteId: UUID? = null,
    @Column("qa_package_id")
    val qaPackageId: UUID? = null,
    @Column("name")
    val name: String,
    @Column("description")
    val description: String? = null,
    @Column("steps_json")
    val stepsJson: String,
    @Column("tags")
    val tags: String? = null, // Stored as JSON array
    @Column("source")
    val source: String,
    @Column("status")
    val status: String,
    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),
)
