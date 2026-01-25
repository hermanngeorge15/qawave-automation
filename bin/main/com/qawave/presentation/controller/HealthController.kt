package com.qawave.presentation.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    suspend fun health(): HealthResponse {
        return HealthResponse(status = "UP", service = "qawave")
    }
}

data class HealthResponse(
    val status: String,
    val service: String
)
