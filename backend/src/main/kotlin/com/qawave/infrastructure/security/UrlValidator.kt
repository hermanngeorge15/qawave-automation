package com.qawave.infrastructure.security

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException

/**
 * URL Validator to prevent SSRF (Server-Side Request Forgery) attacks.
 * Validates URLs before making HTTP requests to ensure they don't target
 * internal networks, cloud metadata endpoints, or dangerous protocols.
 */
@Component
class UrlValidator {
    companion object {
        // Allowed protocols for HTTP requests
        private val ALLOWED_PROTOCOLS = setOf("http", "https")

        // Blocked hostnames and patterns
        private val BLOCKED_HOSTNAMES =
            setOf(
                "localhost",
                "127.0.0.1",
                "::1",
                "[::1]",
                "0.0.0.0",
                "metadata.google.internal",
                "metadata.google.com",
                "169.254.169.254", // AWS/Azure/GCP metadata endpoint
            )

        // Blocked hostname suffixes (internal Kubernetes services, etc.)
        private val BLOCKED_HOSTNAME_SUFFIXES =
            listOf(
                ".internal",
                ".local",
                ".localhost",
                ".svc.cluster.local", // Kubernetes services
            )

        // Private IP ranges (RFC 1918 and others)
        private val PRIVATE_IP_PREFIXES =
            listOf(
                "10.", // 10.0.0.0/8
                "172.16.", "172.17.", "172.18.", "172.19.", // 172.16.0.0/12
                "172.20.", "172.21.", "172.22.", "172.23.",
                "172.24.", "172.25.", "172.26.", "172.27.",
                "172.28.", "172.29.", "172.30.", "172.31.",
                "192.168.", // 192.168.0.0/16
                "169.254.", // Link-local
                "127.", // Loopback
                "0.", // Current network
            )

        // Blocked ports that could indicate internal services
        private val BLOCKED_PORTS =
            setOf(
                22, // SSH
                25, // SMTP
                3306, // MySQL
                5432, // PostgreSQL
                6379, // Redis
                27017, // MongoDB
                9200, // Elasticsearch
                2379, // etcd
                10250, // Kubernetes kubelet
                10255, // Kubernetes kubelet read-only
            )
    }

    /**
     * Validates a URL for safe external requests.
     * @param url The URL to validate
     * @return ValidationResult with success/failure and error message
     */
    @Suppress("ReturnCount")
    fun validate(url: String): ValidationResult {
        // Parse URL
        val uri =
            try {
                URI(url)
            } catch (e: URISyntaxException) {
                return ValidationResult.invalid("Invalid URL format: ${e.message}")
            }

        // Check protocol
        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme !in ALLOWED_PROTOCOLS) {
            return ValidationResult.invalid("Protocol '$scheme' is not allowed. Only HTTP and HTTPS are permitted.")
        }

        // Check host
        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) {
            return ValidationResult.invalid("URL must have a valid hostname")
        }

        // Check blocked hostnames
        if (host in BLOCKED_HOSTNAMES) {
            return ValidationResult.invalid("Hostname '$host' is not allowed")
        }

        // Check blocked hostname suffixes
        for (suffix in BLOCKED_HOSTNAME_SUFFIXES) {
            if (host.endsWith(suffix)) {
                return ValidationResult.invalid("Hostname '$host' matches blocked pattern '*$suffix'")
            }
        }

        // Check if it's a private IP address
        if (isPrivateIpAddress(host)) {
            return ValidationResult.invalid("Private/internal IP addresses are not allowed: $host")
        }

        // Resolve hostname and check if it resolves to a private IP
        try {
            val resolvedAddresses = InetAddress.getAllByName(host)
            for (address in resolvedAddresses) {
                val ip = address.hostAddress
                if (isPrivateIpAddress(ip)) {
                    return ValidationResult.invalid("Hostname '$host' resolves to private IP address: $ip")
                }
            }
        } catch (e: Exception) {
            // If we can't resolve, allow the request (it will fail naturally)
            // This prevents DNS rebinding attacks from blocking legitimate hosts
        }

        // Check port
        val port = uri.port
        if (port > 0 && port in BLOCKED_PORTS) {
            return ValidationResult.invalid("Port $port is blocked for security reasons")
        }

        return ValidationResult.valid()
    }

    /**
     * Checks if the given string is a private/internal IP address.
     */
    private fun isPrivateIpAddress(ip: String): Boolean {
        // Check IPv4 private ranges
        for (prefix in PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true
            }
        }

        // Check IPv6 loopback and link-local
        if (isIpv6PrivateAddress(ip)) {
            return true
        }

        return false
    }

    /**
     * Checks if the IP is a private IPv6 address.
     */
    private fun isIpv6PrivateAddress(ip: String): Boolean {
        return ip == "::1" ||
            ip.startsWith("fe80:") ||
            ip.startsWith("fc") ||
            ip.startsWith("fd")
    }

    /**
     * Result of URL validation.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?,
    ) {
        companion object {
            fun valid() = ValidationResult(true, null)

            fun invalid(message: String) = ValidationResult(false, message)
        }
    }
}

/**
 * Exception thrown when SSRF attempt is detected.
 */
class SsrfException(message: String) : SecurityException(message)
