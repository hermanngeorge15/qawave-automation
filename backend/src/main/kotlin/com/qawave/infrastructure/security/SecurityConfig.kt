package com.qawave.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

/**
 * Security configuration for Spring WebFlux with OAuth2/OIDC (Keycloak).
 *
 * This configuration:
 * - Enables OAuth2 Resource Server with JWT validation
 * - Configures public and protected endpoints
 * - Extracts roles from Keycloak JWT tokens
 * - Configures CORS for frontend access
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    @Value("\${qawave.security.enabled:true}")
    private val securityEnabled: Boolean,
    private val authenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val accessDeniedHandler: JwtAccessDeniedHandler,
) {
    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    companion object {
        /**
         * Public endpoints that don't require authentication.
         */
        val PUBLIC_ENDPOINTS =
            arrayOf(
                "/api/health",
                "/health",
                "/ready",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info",
                "/actuator/prometheus",
                "/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**",
                "/v3/api-docs/**",
            )

        /**
         * Keycloak realm roles claim path in JWT.
         */
        const val KEYCLOAK_REALM_ROLES_CLAIM = "realm_access"
        const val KEYCLOAK_RESOURCE_ROLES_CLAIM = "resource_access"
        const val ROLES_KEY = "roles"
    }

    /**
     * Configures the security filter chain for WebFlux.
     */
    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        logger.info("Configuring security filter chain, security enabled: {}", securityEnabled)

        if (!securityEnabled) {
            logger.warn("Security is DISABLED - all endpoints are publicly accessible")
            return http
                .csrf { it.disable() }
                .authorizeExchange { exchanges ->
                    exchanges.anyExchange().permitAll()
                }
                .build()
        }

        return http
            .csrf { it.disable() } // Disable CSRF for API (stateless JWT auth)
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers(*PUBLIC_ENDPOINTS).permitAll()
                    // Allow OPTIONS for CORS preflight
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // All other endpoints require authentication
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
                oauth2.authenticationEntryPoint(authenticationEntryPoint)
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(authenticationEntryPoint)
                exceptions.accessDeniedHandler(accessDeniedHandler)
            }
            .build()
    }

    /**
     * Configures JWT authentication converter to extract Keycloak roles.
     */
    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val jwtConverter = JwtAuthenticationConverter()
        jwtConverter.setJwtGrantedAuthoritiesConverter(KeycloakGrantedAuthoritiesConverter())
        return ReactiveJwtAuthenticationConverterAdapter(jwtConverter)
    }
}

/**
 * Converter that extracts granted authorities from Keycloak JWT tokens.
 *
 * Keycloak stores roles in:
 * - realm_access.roles: realm-level roles
 * - resource_access.{client-id}.roles: client-level roles
 */
class KeycloakGrantedAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    private val logger = LoggerFactory.getLogger(KeycloakGrantedAuthoritiesConverter::class.java)

    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()

        // Extract realm roles
        val realmAccess = jwt.getClaim<Map<String, Any>>(SecurityConfig.KEYCLOAK_REALM_ROLES_CLAIM)
        realmAccess?.let { realm ->
            @Suppress("UNCHECKED_CAST")
            val roles = realm[SecurityConfig.ROLES_KEY] as? Collection<String> ?: emptyList()
            roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_$role"))
            }
            logger.debug("Extracted {} realm roles from JWT", roles.size)
        }

        // Extract resource roles (client-specific roles)
        val resourceAccess = jwt.getClaim<Map<String, Any>>(SecurityConfig.KEYCLOAK_RESOURCE_ROLES_CLAIM)
        resourceAccess?.forEach { (clientId, clientRoles) ->
            @Suppress("UNCHECKED_CAST")
            val roles = (clientRoles as? Map<String, Any>)?.get(SecurityConfig.ROLES_KEY) as? Collection<String>
            roles?.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_${clientId}_$role"))
            }
        }

        logger.debug("Total authorities extracted from JWT: {}", authorities.size)
        return authorities
    }
}
