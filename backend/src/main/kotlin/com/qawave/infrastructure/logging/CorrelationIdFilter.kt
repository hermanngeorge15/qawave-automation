package com.qawave.infrastructure.logging

import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * WebFilter that manages correlation IDs for request tracing.
 *
 * Features:
 * - Extracts correlation ID from X-Correlation-ID header
 * - Generates new correlation ID if not present
 * - Propagates correlation ID to response headers
 * - Sets correlation ID in MDC for logging
 * - Sets correlation ID in Reactor context for reactive chains
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : WebFilter {
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val MDC_CORRELATION_ID = "correlationId"
        const val MDC_REQUEST_ID = "requestId"
        const val CONTEXT_CORRELATION_ID = "correlationId"
        const val CONTEXT_REQUEST_ID = "requestId"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // Extract or generate correlation ID
        val correlationId =
            exchange.request.headers.getFirst(CORRELATION_ID_HEADER)
                ?: UUID.randomUUID().toString()

        // Generate unique request ID for this specific request
        val requestId =
            exchange.request.headers.getFirst(REQUEST_ID_HEADER)
                ?: UUID.randomUUID().toString()

        // Add IDs to response headers
        exchange.response.headers.add(CORRELATION_ID_HEADER, correlationId)
        exchange.response.headers.add(REQUEST_ID_HEADER, requestId)

        // Store in exchange attributes for access in other filters
        exchange.attributes[CONTEXT_CORRELATION_ID] = correlationId
        exchange.attributes[CONTEXT_REQUEST_ID] = requestId

        return chain.filter(exchange)
            .contextWrite { context ->
                context
                    .put(CONTEXT_CORRELATION_ID, correlationId)
                    .put(CONTEXT_REQUEST_ID, requestId)
            }
            .doFirst {
                MDC.put(MDC_CORRELATION_ID, correlationId)
                MDC.put(MDC_REQUEST_ID, requestId)
            }
            .doFinally {
                MDC.remove(MDC_CORRELATION_ID)
                MDC.remove(MDC_REQUEST_ID)
            }
    }
}

/**
 * Utility object for accessing correlation IDs in reactive chains.
 */
object CorrelationContext {
    /**
     * Gets the correlation ID from the Reactor context.
     */
    fun getCorrelationId(): Mono<String> =
        Mono.deferContextual { ctx ->
            Mono.justOrEmpty(ctx.getOrDefault<String?>(CorrelationIdFilter.CONTEXT_CORRELATION_ID, null))
        }

    /**
     * Gets the request ID from the Reactor context.
     */
    fun getRequestId(): Mono<String> =
        Mono.deferContextual { ctx ->
            Mono.justOrEmpty(ctx.getOrDefault<String?>(CorrelationIdFilter.CONTEXT_REQUEST_ID, null))
        }
}
