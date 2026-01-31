package com.qawave.infrastructure.logging

import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for distributed tracing with OpenTelemetry.
 *
 * Features:
 * - Micrometer Observation integration
 * - Trace context propagation
 * - Custom spans for key operations
 */
@Configuration
@ConditionalOnProperty(
    name = ["qawave.tracing.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class TracingConfig {

    /**
     * Enables @Observed annotation support for creating custom spans.
     */
    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect {
        return ObservedAspect(observationRegistry)
    }
}
