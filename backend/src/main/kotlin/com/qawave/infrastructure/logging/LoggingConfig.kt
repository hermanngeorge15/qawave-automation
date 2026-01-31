package com.qawave.infrastructure.logging

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for logging and tracing.
 */
@ConfigurationProperties(prefix = "qawave.logging")
data class LoggingConfig(
    /** Enable request/response logging. */
    val requestLogging: RequestLoggingConfig = RequestLoggingConfig(),
    /** Headers to redact from logs. */
    val redactedHeaders: List<String> =
        listOf(
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-API-Key",
            "X-Auth-Token",
        ),
    /** JSON fields to redact from request/response bodies. */
    val redactedFields: List<String> =
        listOf(
            "password",
            "secret",
            "token",
            "apiKey",
            "api_key",
            "credentials",
            "credit_card",
            "creditCard",
            "ssn",
        ),
)

data class RequestLoggingConfig(
    /** Enable request logging. */
    val enabled: Boolean = true,
    /** Log request body. */
    val logRequestBody: Boolean = false,
    /** Log response body. */
    val logResponseBody: Boolean = false,
    /** Maximum body size to log (bytes). */
    val maxBodySize: Int = 4096,
    /** Paths to exclude from logging. */
    val excludedPaths: List<String> =
        listOf(
            "/actuator",
            "/health",
            "/ready",
            "/api-docs",
            "/swagger-ui",
        ),
)
