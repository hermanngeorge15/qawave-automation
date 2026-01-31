package com.qawave.unit

import com.qawave.infrastructure.security.UrlValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for URL Validator SSRF protection.
 */
class UrlValidatorTest {
    private val validator = UrlValidator()

    // ==================== Valid URLs ====================

    @Test
    fun `should allow valid HTTPS URLs`() {
        val result = validator.validate("https://api.example.com/v1/users")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `should allow valid HTTP URLs`() {
        val result = validator.validate("http://api.example.com/v1/users")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `should allow URLs with custom ports`() {
        val result = validator.validate("https://api.example.com:8080/v1/users")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `should allow URLs with query parameters`() {
        val result = validator.validate("https://api.example.com/search?q=test&page=1")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    // ==================== Blocked Protocols ====================

    @ParameterizedTest
    @ValueSource(strings = [
        "file:///etc/passwd",
        "gopher://localhost:5432/",
        "ftp://internal.server/data",
        "dict://localhost:2628/show",
        "ldap://localhost/",
    ])
    fun `should block non-HTTP protocols`(url: String) {
        val result = validator.validate(url)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("not allowed"))
    }

    // ==================== Blocked Hostnames ====================

    @ParameterizedTest
    @ValueSource(strings = [
        "http://localhost/api",
        "http://127.0.0.1/api",
        "http://0.0.0.0/api",
        "http://[::1]/api",
        "http://169.254.169.254/latest/meta-data/",
        "http://metadata.google.internal/",
    ])
    fun `should block localhost and metadata endpoints`(url: String) {
        val result = validator.validate(url)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // ==================== Blocked Private IPs ====================

    @ParameterizedTest
    @ValueSource(strings = [
        "http://10.0.0.1/api",
        "http://10.255.255.255/api",
        "http://172.16.0.1/api",
        "http://172.31.255.255/api",
        "http://192.168.0.1/api",
        "http://192.168.255.255/api",
    ])
    fun `should block private IP addresses`(url: String) {
        val result = validator.validate(url)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Private") || result.errorMessage!!.contains("internal"))
    }

    // ==================== Blocked Internal Suffixes ====================

    @ParameterizedTest
    @ValueSource(strings = [
        "http://backend.svc.cluster.local/api",
        "http://database.internal/api",
        "http://service.local/api",
        "http://app.localhost/api",
    ])
    fun `should block internal hostname suffixes`(url: String) {
        val result = validator.validate(url)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("blocked pattern"))
    }

    // ==================== Blocked Ports ====================

    @ParameterizedTest
    @ValueSource(strings = [
        "http://api.example.com:22/",      // SSH
        "http://api.example.com:3306/",    // MySQL
        "http://api.example.com:5432/",    // PostgreSQL
        "http://api.example.com:6379/",    // Redis
        "http://api.example.com:27017/",   // MongoDB
    ])
    fun `should block dangerous ports`(url: String) {
        val result = validator.validate(url)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("blocked"))
    }

    // ==================== Invalid URLs ====================

    @Test
    fun `should reject malformed URLs`() {
        val result = validator.validate("not-a-valid-url")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `should reject URLs without hostname`() {
        val result = validator.validate("http:///path")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `should reject empty URLs`() {
        val result = validator.validate("")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }
}
