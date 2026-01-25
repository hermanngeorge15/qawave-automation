package com.qawave.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for OpenAPI/Swagger documentation.
 */
@Configuration
class OpenApiConfig {

    @Value("\${server.port:8080}")
    private var serverPort: Int = 8080

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
            .servers(listOf(
                Server()
                    .url("http://localhost:$serverPort")
                    .description("Local development server"),
                Server()
                    .url("https://api.qawave.com")
                    .description("Production server")
            ))
            .tags(listOf(
                Tag()
                    .name("QA Packages")
                    .description("Operations for managing QA test packages"),
                Tag()
                    .name("Test Scenarios")
                    .description("Operations for managing test scenarios"),
                Tag()
                    .name("Test Runs")
                    .description("Operations for managing test execution runs"),
                Tag()
                    .name("Health")
                    .description("Health and status endpoints")
            ))
    }

    private fun apiInfo(): Info {
        return Info()
            .title("QAWave API")
            .description("""
                QAWave is an AI-powered QA automation platform that acts as a virtual QA engineering team for backend APIs.

                ## Features
                - Automated test scenario generation from OpenAPI specs
                - AI-powered test execution and validation
                - Comprehensive coverage reporting
                - Risk assessment and quality scoring

                ## Authentication
                API authentication is handled via API keys passed in the `X-API-Key` header.

                ## Rate Limiting
                The API implements rate limiting to ensure fair usage. Current limits:
                - 100 requests per minute per API key
                - 10 concurrent AI generation requests
            """.trimIndent())
            .version("1.0.0")
            .contact(Contact()
                .name("QAWave Team")
                .email("support@qawave.com")
                .url("https://qawave.com"))
            .license(License()
                .name("Proprietary")
                .url("https://qawave.com/terms"))
    }
}
