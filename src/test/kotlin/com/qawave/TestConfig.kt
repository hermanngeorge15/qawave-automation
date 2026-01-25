package com.qawave

import com.qawave.domain.model.QaPackage
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.QaPackageStatus
import com.qawave.domain.repository.QaPackageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Instant

/**
 * Test configuration providing mock beans for integration tests.
 */
@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun testQaPackageRepository(): QaPackageRepository = InMemoryQaPackageRepository()
}

/**
 * In-memory implementation of QaPackageRepository for testing.
 */
class InMemoryQaPackageRepository : QaPackageRepository {
    private val packages = mutableMapOf<QaPackageId, QaPackage>()

    override suspend fun save(qaPackage: QaPackage): QaPackage {
        packages[qaPackage.id] = qaPackage
        return qaPackage
    }

    override suspend fun findById(id: QaPackageId): QaPackage? {
        return packages[id]
    }

    override fun findAll(): Flow<QaPackage> {
        return kotlinx.coroutines.flow.flow {
            packages.values.forEach { emit(it) }
        }
    }

    override suspend fun findByStatus(status: QaPackageStatus): List<QaPackage> {
        return packages.values.filter { it.status == status }
    }

    override fun findIncomplete(): Flow<QaPackage> {
        return kotlinx.coroutines.flow.flow {
            packages.values.filter { it.isInProgress }.forEach { emit(it) }
        }
    }

    override fun findRecent(since: Instant): Flow<QaPackage> {
        return kotlinx.coroutines.flow.flow {
            packages.values.filter { it.createdAt.isAfter(since) }.forEach { emit(it) }
        }
    }

    override suspend fun delete(id: QaPackageId): Boolean {
        return packages.remove(id) != null
    }

    override suspend fun existsById(id: QaPackageId): Boolean {
        return packages.containsKey(id)
    }

    override suspend fun count(): Long {
        return packages.size.toLong()
    }

    override suspend fun countByStatus(status: QaPackageStatus): Long {
        return packages.values.count { it.status == status }.toLong()
    }

    fun clear() {
        packages.clear()
    }
}
