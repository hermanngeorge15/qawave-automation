package com.qawave.infrastructure.security

/**
 * Role constants for QAWave application.
 *
 * Keycloak realm roles are prefixed with "ROLE_" by Spring Security.
 *
 * Note: SpEL expressions must be literal strings for use in @PreAuthorize annotations.
 */
object Roles {
    // Role names (without ROLE_ prefix, as used in @PreAuthorize)
    const val ADMIN = "admin"
    const val TESTER = "tester"
    const val VIEWER = "viewer"

    // SpEL expressions for common authorization checks
    // These must be literal strings for use in annotations
    const val HAS_ADMIN = "hasRole('admin')"
    const val HAS_TESTER = "hasRole('tester')"
    const val HAS_VIEWER = "hasRole('viewer')"
    const val HAS_ADMIN_OR_TESTER = "hasAnyRole('admin', 'tester')"
    const val HAS_ANY_ROLE = "hasAnyRole('admin', 'tester', 'viewer')"

    // Composite expressions
    const val CAN_CREATE = "hasAnyRole('admin', 'tester')"
    const val CAN_UPDATE = "hasAnyRole('admin', 'tester')"
    const val CAN_DELETE = "hasRole('admin')"
    const val CAN_READ = "hasAnyRole('admin', 'tester', 'viewer')"

    // Resource ownership expressions (to be used with @packageSecurityService)
    const val IS_OWNER_OR_ADMIN = "hasRole('admin') or @packageSecurityService.isOwner(#id, authentication)"
    const val CAN_UPDATE_OWNED = "hasRole('admin') or (hasRole('tester') and @packageSecurityService.isOwner(#id, authentication))"
    const val CAN_DELETE_OWNED = "hasRole('admin') or @packageSecurityService.isOwner(#id, authentication)"
}
