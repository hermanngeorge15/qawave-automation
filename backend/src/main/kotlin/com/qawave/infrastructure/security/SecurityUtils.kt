package com.qawave.infrastructure.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Security utilities for accessing the current authenticated user.
 */
object SecurityUtils {
    /**
     * Get the current user ID from the security context.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUserId(): String? {
        val context =
            ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
                ?: return null

        val authentication =
            context.authentication
                ?: return null

        if (authentication !is JwtAuthenticationToken) {
            return authentication.name
        }

        return authentication.token.getClaimAsString("sub")
    }

    /**
     * Get the current username from the security context.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUsername(): String? {
        val context =
            ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
                ?: return null

        val authentication =
            context.authentication
                ?: return null

        if (authentication !is JwtAuthenticationToken) {
            return authentication.name
        }

        return authentication.token.getClaimAsString("preferred_username")
    }

    /**
     * Check if the current user has a specific role.
     * Returns false if not authenticated.
     */
    suspend fun hasRole(role: String): Boolean {
        val context =
            ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
                ?: return false

        val authentication =
            context.authentication
                ?: return false

        return authentication.authorities.any { it.authority == "ROLE_$role" }
    }

    /**
     * Check if there is an authenticated user.
     */
    suspend fun isAuthenticated(): Boolean {
        val context =
            ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
                ?: return false

        return context.authentication?.isAuthenticated == true
    }
}
