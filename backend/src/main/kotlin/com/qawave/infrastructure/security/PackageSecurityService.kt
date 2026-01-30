package com.qawave.infrastructure.security

import com.qawave.domain.model.QaPackageId
import com.qawave.infrastructure.persistence.repository.QaPackageR2dbcRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Security service for resource ownership validation.
 *
 * Used in @PreAuthorize expressions to check if the current user owns a resource.
 * Example: @PreAuthorize("@packageSecurityService.isOwner(#id, authentication)")
 */
@Service("packageSecurityService")
class PackageSecurityService(
    private val packageRepository: QaPackageR2dbcRepository,
) {
    private val logger = LoggerFactory.getLogger(PackageSecurityService::class.java)

    /**
     * Check if the authenticated user owns the package with the given ID.
     *
     * @param id The package ID to check
     * @param authentication The current authentication
     * @return true if the user owns the package, false otherwise
     */
    fun isOwner(
        id: UUID,
        authentication: Authentication?,
    ): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            logger.debug("No authenticated user for ownership check")
            return false
        }

        val userId = extractUserId(authentication)
        if (userId == null) {
            logger.debug("Could not extract user ID from authentication")
            return false
        }

        // Check ownership in database
        return runBlocking {
            try {
                val entity = packageRepository.findById(id)
                if (entity == null) {
                    logger.debug("Package {} not found for ownership check", id)
                    false
                } else {
                    val isOwner = entity.triggeredBy == userId
                    logger.debug(
                        "Ownership check for package {}: userId={}, triggeredBy={}, isOwner={}",
                        id,
                        userId,
                        entity.triggeredBy,
                        isOwner,
                    )
                    isOwner
                }
            } catch (e: Exception) {
                logger.error("Error checking package ownership: {}", e.message)
                false
            }
        }
    }

    /**
     * Check if the authenticated user owns the package.
     * Overload that accepts QaPackageId.
     */
    fun isOwner(
        id: QaPackageId,
        authentication: Authentication?,
    ): Boolean {
        return isOwner(id.value, authentication)
    }

    /**
     * Check if the user has access to the package (owner or admin).
     */
    fun hasAccess(
        id: UUID,
        authentication: Authentication?,
    ): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }

        // Admins have access to everything
        if (hasRole(authentication, Roles.ADMIN)) {
            return true
        }

        // Otherwise, check ownership
        return isOwner(id, authentication)
    }

    /**
     * Extract user ID from authentication.
     */
    private fun extractUserId(authentication: Authentication): String? {
        return when (authentication) {
            is JwtAuthenticationToken -> {
                authentication.token.getClaimAsString("sub")
            }
            else -> {
                authentication.name
            }
        }
    }

    /**
     * Check if authentication has a specific role.
     */
    private fun hasRole(
        authentication: Authentication,
        role: String,
    ): Boolean {
        return authentication.authorities.any { it.authority == "ROLE_$role" }
    }
}
