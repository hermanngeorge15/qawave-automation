package com.qawave.infrastructure.logging

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * WebFilter that logs structured request/response information.
 *
 * Features:
 * - Structured JSON logging with request metadata
 * - Request timing measurement
 * - Sensitive header redaction
 * - Path-based exclusion
 * - MDC context with trace information
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After CorrelationIdFilter
@ConditionalOnProperty(
    name = ["qawave.logging.request-logging.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class RequestLoggingFilter(
    private val loggingConfig: LoggingConfig,
    private val sensitiveDataRedactor: SensitiveDataRedactor,
) : WebFilter {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()

        // Skip excluded paths
        if (isExcludedPath(path)) {
            return chain.filter(exchange)
        }

        val startTime = System.currentTimeMillis()
        val request = exchange.request

        // Set MDC values for structured logging
        setRequestMdc(request)

        // Log incoming request
        logRequest(request)

        return chain.filter(exchange)
            .doOnSuccess {
                logResponse(exchange, startTime)
            }
            .doOnError { error ->
                logError(exchange, startTime, error)
            }
            .doFinally {
                clearRequestMdc()
            }
    }

    private fun setRequestMdc(request: ServerHttpRequest) {
        MDC.put("http.method", request.method.name())
        MDC.put("http.path", request.path.value())
        MDC.put("http.query", request.uri.query ?: "")
        MDC.put("client.ip", request.remoteAddress?.address?.hostAddress ?: "unknown")
        MDC.put("http.userAgent", request.headers.getFirst("User-Agent") ?: "unknown")
    }

    private fun clearRequestMdc() {
        MDC.remove("http.method")
        MDC.remove("http.path")
        MDC.remove("http.query")
        MDC.remove("http.status")
        MDC.remove("http.duration")
        MDC.remove("client.ip")
        MDC.remove("http.userAgent")
    }

    private fun logRequest(request: ServerHttpRequest) {
        val headers = redactHeaders(request.headers.toSingleValueMap())

        logger.info(
            "Incoming request: {} {} from {}",
            request.method.name(),
            request.path.value(),
            request.remoteAddress?.address?.hostAddress ?: "unknown",
        )

        if (logger.isDebugEnabled) {
            logger.debug("Request headers: {}", headers)
        }
    }

    private fun logResponse(exchange: ServerWebExchange, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        val statusCode = exchange.response.statusCode?.value() ?: 0

        MDC.put("http.status", statusCode.toString())
        MDC.put("http.duration", duration.toString())

        if (statusCode >= 400) {
            logger.warn(
                "Request completed: {} {} - {} in {}ms",
                exchange.request.method.name(),
                exchange.request.path.value(),
                statusCode,
                duration,
            )
        } else {
            logger.info(
                "Request completed: {} {} - {} in {}ms",
                exchange.request.method.name(),
                exchange.request.path.value(),
                statusCode,
                duration,
            )
        }
    }

    private fun logError(exchange: ServerWebExchange, startTime: Long, error: Throwable) {
        val duration = System.currentTimeMillis() - startTime

        MDC.put("http.status", "500")
        MDC.put("http.duration", duration.toString())
        MDC.put("error.type", error.javaClass.simpleName)
        MDC.put("error.message", error.message ?: "")

        logger.error(
            "Request failed: {} {} - {} in {}ms",
            exchange.request.method.name(),
            exchange.request.path.value(),
            error.javaClass.simpleName,
            duration,
            error,
        )

        MDC.remove("error.type")
        MDC.remove("error.message")
    }

    private fun redactHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (sensitiveDataRedactor.isSensitiveHeader(key)) {
                "[REDACTED]"
            } else {
                value
            }
        }
    }

    private fun isExcludedPath(path: String): Boolean {
        return loggingConfig.requestLogging.excludedPaths.any { excluded ->
            path.startsWith(excluded)
        }
    }
}
