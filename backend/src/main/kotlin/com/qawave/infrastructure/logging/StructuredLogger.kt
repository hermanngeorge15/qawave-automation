package com.qawave.infrastructure.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * Utility class for structured logging with additional context.
 *
 * Usage:
 * ```kotlin
 * class MyService {
 *     private val logger = StructuredLogger.getLogger<MyService>()
 *
 *     fun process() {
 *         logger.withContext("userId" to "123", "action" to "process") {
 *             info("Processing started")
 *         }
 *     }
 * }
 * ```
 */
class StructuredLogger(
    @PublishedApi internal val delegate: Logger
) : Logger by delegate {
    companion object {
        inline fun <reified T> getLogger(): StructuredLogger {
            return StructuredLogger(LoggerFactory.getLogger(T::class.java))
        }

        fun getLogger(name: String): StructuredLogger {
            return StructuredLogger(LoggerFactory.getLogger(name))
        }

        fun getLogger(clazz: Class<*>): StructuredLogger {
            return StructuredLogger(LoggerFactory.getLogger(clazz))
        }
    }

    /**
     * Executes a block with additional MDC context.
     * Context is automatically cleaned up after the block executes.
     */
    inline fun <T> withContext(vararg context: Pair<String, String?>, block: Logger.() -> T): T {
        val previousValues = mutableMapOf<String, String?>()

        try {
            // Store previous values and set new context
            context.forEach { (key, value) ->
                previousValues[key] = MDC.get(key)
                if (value != null) {
                    MDC.put(key, value)
                } else {
                    MDC.remove(key)
                }
            }

            return block(delegate)
        } finally {
            // Restore previous values
            previousValues.forEach { (key, value) ->
                if (value != null) {
                    MDC.put(key, value)
                } else {
                    MDC.remove(key)
                }
            }
        }
    }

    /**
     * Logs a message with additional structured context.
     */
    fun infoWithContext(message: String, vararg context: Pair<String, String?>) {
        withContext(*context) {
            info(message)
        }
    }

    /**
     * Logs a warning with additional structured context.
     */
    fun warnWithContext(message: String, vararg context: Pair<String, String?>) {
        withContext(*context) {
            warn(message)
        }
    }

    /**
     * Logs an error with additional structured context.
     */
    fun errorWithContext(message: String, throwable: Throwable? = null, vararg context: Pair<String, String?>) {
        withContext(*context) {
            if (throwable != null) {
                error(message, throwable)
            } else {
                error(message)
            }
        }
    }

    /**
     * Logs a debug message with additional structured context.
     */
    fun debugWithContext(message: String, vararg context: Pair<String, String?>) {
        withContext(*context) {
            debug(message)
        }
    }
}

/**
 * Extension function to add context to any logger operation.
 */
inline fun <T> Logger.withMdcContext(vararg context: Pair<String, String?>, block: Logger.() -> T): T {
    val previousValues = mutableMapOf<String, String?>()

    try {
        context.forEach { (key, value) ->
            previousValues[key] = MDC.get(key)
            if (value != null) {
                MDC.put(key, value)
            } else {
                MDC.remove(key)
            }
        }
        return block()
    } finally {
        previousValues.forEach { (key, value) ->
            if (value != null) {
                MDC.put(key, value)
            } else {
                MDC.remove(key)
            }
        }
    }
}
