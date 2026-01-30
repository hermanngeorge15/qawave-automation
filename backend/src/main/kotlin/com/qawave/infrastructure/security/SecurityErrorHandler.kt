package com.qawave.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Custom authentication entry point for handling authentication errors.
 * Returns JSON error responses instead of default HTML.
 */
@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : ServerAuthenticationEntryPoint {
    private val logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)

    override fun commence(
        exchange: ServerWebExchange,
        ex: AuthenticationException,
    ): Mono<Void> {
        logger.warn("Authentication failed: {} - {}", ex.javaClass.simpleName, ex.message)

        val (status, errorCode, message) =
            when (ex) {
                is InvalidBearerTokenException ->
                    Triple(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_TOKEN",
                        "The provided token is invalid or expired",
                    )
                else ->
                    when (val cause = ex.cause) {
                        is JwtException ->
                            Triple(
                                HttpStatus.UNAUTHORIZED,
                                "JWT_ERROR",
                                "JWT validation failed: ${cause.message}",
                            )
                        else ->
                            Triple(
                                HttpStatus.UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED",
                                "Authentication is required to access this resource",
                            )
                    }
            }

        // Log authentication event for audit
        logAuthEvent(exchange, status, errorCode, ex)

        return writeErrorResponse(exchange, status, errorCode, message)
    }

    private fun logAuthEvent(
        exchange: ServerWebExchange,
        status: HttpStatus,
        errorCode: String,
        ex: AuthenticationException,
    ) {
        val request = exchange.request
        logger.info(
            "AUTH_EVENT: status={}, code={}, method={}, path={}, remoteAddr={}, error={}",
            status.value(),
            errorCode,
            request.method,
            request.path,
            request.remoteAddress?.address?.hostAddress ?: "unknown",
            ex.message,
        )
    }

    private fun writeErrorResponse(
        exchange: ServerWebExchange,
        status: HttpStatus,
        errorCode: String,
        message: String,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON

        val errorBody =
            SecurityErrorResponse(
                error = errorCode,
                message = message,
                status = status.value(),
            )

        val bytes = objectMapper.writeValueAsBytes(errorBody)
        val buffer: DataBuffer = response.bufferFactory().wrap(bytes)
        return response.writeWith(Mono.just(buffer))
    }
}

/**
 * Custom access denied handler for authorization errors.
 * Returns JSON error responses when user lacks required permissions.
 */
@Component
class JwtAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : ServerAccessDeniedHandler {
    private val logger = LoggerFactory.getLogger(JwtAccessDeniedHandler::class.java)

    override fun handle(
        exchange: ServerWebExchange,
        denied: AccessDeniedException,
    ): Mono<Void> {
        logger.warn("Access denied: {} - {}", denied.javaClass.simpleName, denied.message)

        // Log authorization event for audit
        logAuthorizationEvent(exchange, denied)

        return writeErrorResponse(exchange, denied.message ?: "Access denied")
    }

    private fun logAuthorizationEvent(
        exchange: ServerWebExchange,
        denied: AccessDeniedException,
    ) {
        val request = exchange.request
        val principal = exchange.getPrincipal<java.security.Principal>()

        principal.subscribe { p ->
            logger.info(
                "AUTHZ_EVENT: status=403, method={}, path={}, principal={}, error={}",
                request.method,
                request.path,
                p?.name ?: "anonymous",
                denied.message,
            )
        }
    }

    private fun writeErrorResponse(
        exchange: ServerWebExchange,
        message: String,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.contentType = MediaType.APPLICATION_JSON

        val errorBody =
            SecurityErrorResponse(
                error = "ACCESS_DENIED",
                message = message,
                status = HttpStatus.FORBIDDEN.value(),
            )

        val bytes = objectMapper.writeValueAsBytes(errorBody)
        val buffer: DataBuffer = response.bufferFactory().wrap(bytes)
        return response.writeWith(Mono.just(buffer))
    }
}

/**
 * Standard error response for security errors.
 */
data class SecurityErrorResponse(
    val error: String,
    val message: String,
    val status: Int,
)
