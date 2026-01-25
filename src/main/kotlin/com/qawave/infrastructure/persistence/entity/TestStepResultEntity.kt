package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for Test Step Result.
 * Maps to the test_step_results table in PostgreSQL.
 */
@Table("test_step_results")
data class TestStepResultEntity(
    @Id
    val id: UUID? = null,

    @Column("run_id")
    val runId: UUID,

    @Column("step_index")
    val stepIndex: Int,

    @Column("step_name")
    val stepName: String,

    @Column("actual_status")
    val actualStatus: Int? = null,

    @Column("actual_headers_json")
    val actualHeadersJson: String? = null,

    @Column("actual_body")
    val actualBody: String? = null,

    @Column("passed")
    val passed: Boolean,

    @Column("assertions_json")
    val assertionsJson: String? = null,

    @Column("extracted_values_json")
    val extractedValuesJson: String? = null,

    @Column("error_message")
    val errorMessage: String? = null,

    @Column("duration_ms")
    val durationMs: Long,

    @Column("executed_at")
    val executedAt: Instant
)
