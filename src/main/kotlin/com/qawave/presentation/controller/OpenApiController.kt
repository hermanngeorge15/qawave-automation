package com.qawave.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Controller for OpenAPI documentation utilities.
 */
@RestController
@RequestMapping("/api/docs")
@Tag(name = "Documentation", description = "API documentation utilities")
class OpenApiController {

    @GetMapping("/info")
    @Operation(
        summary = "Get API documentation info",
        description = "Returns information about where to access API documentation"
    )
    @ApiResponse(responseCode = "200", description = "Documentation info returned")
    suspend fun getDocInfo(): ResponseEntity<ApiDocsInfo> {
        return ResponseEntity.ok(ApiDocsInfo(
            swaggerUi = "/swagger-ui.html",
            apiDocs = "/api-docs",
            apiDocsYaml = "/api-docs.yaml",
            version = "1.0.0"
        ))
    }
}

/**
 * Response DTO for API documentation info.
 */
data class ApiDocsInfo(
    val swaggerUi: String,
    val apiDocs: String,
    val apiDocsYaml: String,
    val version: String
)

/**
 * Configuration for API grouping in OpenAPI.
 */
@Configuration
class OpenApiGroupConfig {

    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("public")
            .packagesToScan("com.qawave.presentation.controller")
            .pathsToMatch("/api/**")
            .build()
    }

    @Bean
    fun actuatorApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("actuator")
            .pathsToMatch("/actuator/**")
            .build()
    }
}
