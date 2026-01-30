package com.qawave.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Represents the authenticated user extracted from a JWT token.
 * Provides convenient access to user details from Keycloak JWT claims.
 */
data class JwtUserDetails(
    val userId: String,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val roles: Set<String>,
    val authorities: Collection<GrantedAuthority>,
) {
    /**
     * Full name combining first and last name.
     */
    val fullName: String?
        get() = listOfNotNull(firstName, lastName).takeIf { it.isNotEmpty() }?.joinToString(" ")

    /**
     * User ID as UUID, or null if not a valid UUID.
     */
    val userIdAsUUID: UUID?
        get() = runCatching { UUID.fromString(userId) }.getOrNull()

    /**
     * Check if user has a specific role.
     */
    fun hasRole(role: String): Boolean = roles.contains(role)

    /**
     * Check if user has any of the specified roles.
     */
    fun hasAnyRole(vararg roles: String): Boolean = roles.any { this.roles.contains(it) }

    /**
     * Check if user has all of the specified roles.
     */
    fun hasAllRoles(vararg roles: String): Boolean = roles.all { this.roles.contains(it) }

    companion object {
        /**
         * Standard Keycloak claims.
         */
        const val CLAIM_SUB = "sub"
        const val CLAIM_PREFERRED_USERNAME = "preferred_username"
        const val CLAIM_EMAIL = "email"
        const val CLAIM_GIVEN_NAME = "given_name"
        const val CLAIM_FAMILY_NAME = "family_name"
        const val CLAIM_NAME = "name"

        /**
         * Extract user details from a JWT token.
         */
        fun fromJwt(
            jwt: Jwt,
            authorities: Collection<GrantedAuthority>,
        ): JwtUserDetails {
            val roles =
                authorities
                    .filter { it.authority.startsWith("ROLE_") }
                    .map { it.authority.removePrefix("ROLE_") }
                    .toSet()

            return JwtUserDetails(
                userId = jwt.getClaimAsString(CLAIM_SUB) ?: "unknown",
                username = jwt.getClaimAsString(CLAIM_PREFERRED_USERNAME),
                email = jwt.getClaimAsString(CLAIM_EMAIL),
                firstName = jwt.getClaimAsString(CLAIM_GIVEN_NAME),
                lastName = jwt.getClaimAsString(CLAIM_FAMILY_NAME),
                roles = roles,
                authorities = authorities,
            )
        }
    }
}
