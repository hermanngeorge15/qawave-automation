package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for Test Run.
 * Maps to the test_runs table in PostgreSQL.
 */
@Table("test_runs")
data class TestRunEntity(
    @Id
    val id: UUID? = null,
    @Column("scenario_id")
    val scenarioId: UUID,
    @Column("qa_package_id")
    val qaPackageId: UUID? = null,
    @Column("triggered_by")
    val triggeredBy: String,
    @Column("base_url")
    val baseUrl: String,
    @Column("status")
    val status: String,
    @Column("environment_json")
    val environmentJson: String? = null,
    @Column("started_at")
    val startedAt: Instant,
    @Column("completed_at")
    val completedAt: Instant? = null,
    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),
)
