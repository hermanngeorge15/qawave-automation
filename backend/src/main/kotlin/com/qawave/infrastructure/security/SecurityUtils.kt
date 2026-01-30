package com.qawave.infrastructure.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Security utilities for accessing the current authenticated user.
 */
object SecurityUtils {
    /**
     * Get the current authenticated user details from the security context.
     * Returns null if not authenticated or if authentication is not JWT-based.
     */
    suspend fun getCurrentUser(): JwtUserDetails? {
        val context =
            ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
                ?: return null

        val authentication =
            context.authentication
                ?: return null

        if (authentication !is JwtAuthenticationToken) {
            return null
        }

        return JwtUserDetails.fromJwt(authentication.token, authentication.authorities)
    }

    /**
     * Get the current user ID from the security context.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUserId(): String? {
        return getCurrentUser()?.userId
    }

    /**
     * Get the current username from the security context.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUsername(): String? {
        return getCurrentUser()?.username
    }

    /**
     * Check if the current user has a specific role.
     * Returns false if not authenticated.
     */
    suspend fun hasRole(role: String): Boolean {
        return getCurrentUser()?.hasRole(role) ?: false
    }

    /**
     * Check if the current user has any of the specified roles.
     * Returns false if not authenticated.
     */
    suspend fun hasAnyRole(vararg roles: String): Boolean {
        return getCurrentUser()?.hasAnyRole(*roles) ?: false
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
