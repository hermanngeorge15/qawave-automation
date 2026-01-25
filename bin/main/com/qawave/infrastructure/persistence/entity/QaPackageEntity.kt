package com.qawave.infrastructure.persistence.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * R2DBC entity for QA Package.
 * Maps to the qa_packages table in PostgreSQL.
 */
@Table("qa_packages")
data class QaPackageEntity(
    @Id
    val id: UUID? = null,

    @Column("name")
    val name: String,

    @Column("description")
    val description: String? = null,

    @Column("spec_url")
    val specUrl: String? = null,

    @Column("spec_content")
    val specContent: String? = null,

    @Column("spec_hash")
    val specHash: String? = null,

    @Column("base_url")
    val baseUrl: String,

    @Column("requirements")
    val requirements: String? = null,

    @Column("status")
    val status: String,

    @Column("config_json")
    val configJson: String? = null,

    @Column("coverage_json")
    val coverageJson: String? = null,

    @Column("qa_summary_json")
    val qaSummaryJson: String? = null,

    @Column("triggered_by")
    val triggeredBy: String,

    @Column("started_at")
    val startedAt: Instant? = null,

    @Column("completed_at")
    val completedAt: Instant? = null,

    @CreatedDate
    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
