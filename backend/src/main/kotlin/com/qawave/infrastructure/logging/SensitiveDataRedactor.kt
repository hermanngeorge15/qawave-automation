package com.qawave.infrastructure.logging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component

/**
 * Service for redacting sensitive data from logs.
 *
 * Features:
 * - Header redaction for sensitive headers
 * - JSON field redaction for request/response bodies
 * - Configurable patterns for sensitive data
 */
@Component
class SensitiveDataRedactor(
    private val loggingConfig: LoggingConfig,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val REDACTED = "[REDACTED]"

        // Common patterns for sensitive data
        private val SENSITIVE_PATTERNS =
            listOf(
                Regex("(?i)password"),
                Regex("(?i)secret"),
                Regex("(?i)token"),
                Regex("(?i)api[-_]?key"),
                Regex("(?i)auth"),
                Regex("(?i)credential"),
                Regex("(?i)bearer"),
            )
    }

    /**
     * Checks if a header name is sensitive and should be redacted.
     */
    fun isSensitiveHeader(headerName: String): Boolean {
        val lowerName = headerName.lowercase()
        return loggingConfig.redactedHeaders.any { it.lowercase() == lowerName } ||
            SENSITIVE_PATTERNS.any { it.containsMatchIn(lowerName) }
    }

    /**
     * Checks if a JSON field name is sensitive and should be redacted.
     */
    fun isSensitiveField(fieldName: String): Boolean {
        val lowerName = fieldName.lowercase()
        return loggingConfig.redactedFields.any { it.lowercase() == lowerName } ||
            SENSITIVE_PATTERNS.any { it.containsMatchIn(lowerName) }
    }

    /**
     * Redacts sensitive fields from a JSON string.
     *
     * @param json The JSON string to redact
     * @return Redacted JSON string, or original if parsing fails
     */
    fun redactJson(json: String): String {
        return try {
            val node = objectMapper.readTree(json)
            val redacted = redactNode(node)
            objectMapper.writeValueAsString(redacted)
        } catch (_: Exception) {
            // If JSON parsing fails, mask any obvious sensitive patterns
            redactPlainText(json)
        }
    }

    /**
     * Recursively redacts sensitive fields in a JSON node.
     */
    private fun redactNode(node: JsonNode): JsonNode {
        return when {
            node.isObject -> {
                val objectNode = node.deepCopy<ObjectNode>()
                val iterator = objectNode.fields()
                while (iterator.hasNext()) {
                    val (name, value) = iterator.next()
                    if (isSensitiveField(name)) {
                        objectNode.put(name, REDACTED)
                    } else {
                        objectNode.set<JsonNode>(name, redactNode(value))
                    }
                }
                objectNode
            }
            node.isArray -> {
                val arrayNode = objectMapper.createArrayNode()
                node.forEach { element ->
                    arrayNode.add(redactNode(element))
                }
                arrayNode
            }
            else -> node
        }
    }

    /**
     * Redacts sensitive patterns from plain text.
     */
    fun redactPlainText(text: String): String {
        var result = text

        // Redact potential bearer tokens
        result = result.replace(Regex("Bearer\\s+[A-Za-z0-9\\-_.]+"), "Bearer $REDACTED")

        // Redact potential API keys (common patterns)
        result =
            result.replace(
                Regex("(?i)(api[-_]?key|apikey|secret|password|token)\\s*[=:]\\s*[\"']?[^\"'\\s,}]+"),
                "$1=$REDACTED",
            )

        return result
    }

    /**
     * Redacts a map of headers.
     */
    fun redactHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (isSensitiveHeader(key)) REDACTED else value
        }
    }
}
