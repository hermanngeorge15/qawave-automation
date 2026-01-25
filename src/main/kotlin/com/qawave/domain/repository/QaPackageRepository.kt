package com.qawave.domain.repository

import com.qawave.domain.model.QaPackage
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.QaPackageStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface for QA Package persistence.
 */
interface QaPackageRepository {
    suspend fun save(qaPackage: QaPackage): QaPackage
    suspend fun findById(id: QaPackageId): QaPackage?
    fun findAll(): Flow<QaPackage>
    suspend fun findByStatus(status: QaPackageStatus): List<QaPackage>
    fun findIncomplete(): Flow<QaPackage>
    fun findRecent(since: Instant): Flow<QaPackage>
    suspend fun delete(id: QaPackageId): Boolean
    suspend fun existsById(id: QaPackageId): Boolean
    suspend fun count(): Long
    suspend fun countByStatus(status: QaPackageStatus): Long
}
