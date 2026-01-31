package com.qawave.presentation.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * Request for creating a shareable link.
 */
data class CreateShareRequest(
    @field:Min(1, message = "Expiration days must be at least 1")
    @field:Max(30, message = "Expiration days cannot exceed 30")
    val expirationDays: Long = 7,
)
