package com.qawave.infrastructure.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * R2DBC configuration for reactive PostgreSQL access.
 *
 * This configuration:
 * - Enables R2DBC repositories with coroutine support
 * - Enables R2DBC auditing for createdAt/updatedAt fields
 * - Configures reactive transaction management
 */
@Configuration
@EnableR2dbcRepositories(basePackages = ["com.qawave.infrastructure.persistence.repository"])
@EnableR2dbcAuditing
@EnableTransactionManagement
class R2dbcConfig {
    /**
     * Configures the reactive transaction manager for R2DBC.
     * This enables @Transactional support for suspend functions.
     */
    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }
}
