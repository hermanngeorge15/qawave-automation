package com.qawave.infrastructure.resilience

import com.qawave.infrastructure.ai.AiCompletionRequest
import com.qawave.infrastructure.ai.AiCompletionResponse
import com.qawave.infrastructure.ai.FinishReason
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Fallback handler for AI client failures.
 * Provides graceful degradation when the AI service is unavailable.
 */
@Component
class AiFallbackHandler {

    private val logger = LoggerFactory.getLogger(AiFallbackHandler::class.java)

    /**
     * Returns a fallback response when AI completion fails due to resilience patterns.
     */
    fun handleCompletionFallback(
        request: AiCompletionRequest,
        cause: Throwable
    ): AiCompletionResponse {
        logger.warn(
            "Returning fallback response for AI completion. Cause: {} - {}",
            cause::class.simpleName,
            cause.message
        )

        val fallbackContent = when {
            request.prompt.contains("scenario", ignoreCase = true) ->
                generateScenarioFallback()
            request.prompt.contains("evaluate", ignoreCase = true) ->
                generateEvaluationFallback()
            request.prompt.contains("coverage", ignoreCase = true) ->
                generateCoverageFallback()
            else ->
                generateGenericFallback()
        }

        return AiCompletionResponse(
            content = fallbackContent,
            model = "fallback",
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            finishReason = FinishReason.ERROR
        )
    }

    /**
     * Returns a simple fallback message for streaming responses.
     */
    fun getFallbackMessage(): String {
        return "AI service is temporarily unavailable. Please try again later."
    }

    private fun generateScenarioFallback(): String {
        return """
        {
            "scenarios": [],
            "error": "AI service temporarily unavailable. No scenarios could be generated.",
            "fallback": true
        }
        """.trimIndent()
    }

    private fun generateEvaluationFallback(): String {
        return """
        {
            "overallVerdict": "INCONCLUSIVE",
            "summary": "Unable to evaluate test results - AI service temporarily unavailable.",
            "passedScenarios": 0,
            "failedScenarios": 0,
            "erroredScenarios": 0,
            "keyFindings": [],
            "recommendations": [{
                "priority": "IMMEDIATE",
                "title": "Retry evaluation",
                "description": "AI service was unavailable during evaluation. Please retry.",
                "actionItems": ["Wait a few minutes and retry the evaluation"]
            }],
            "riskAssessment": {
                "overallRisk": "MEDIUM",
                "qualityScore": 0,
                "stabilityScore": 0,
                "securityScore": 0,
                "riskFactors": ["Unable to complete AI evaluation"]
            },
            "fallback": true
        }
        """.trimIndent()
    }

    private fun generateCoverageFallback(): String {
        return """
        {
            "totalOperations": 0,
            "coveredOperations": 0,
            "coveragePercentage": 0.0,
            "operationDetails": [],
            "gaps": [{
                "type": "UNCOVERED_OPERATION",
                "operationId": null,
                "description": "Coverage analysis unavailable - AI service temporarily unavailable",
                "severity": "MEDIUM"
            }],
            "fallback": true
        }
        """.trimIndent()
    }

    private fun generateGenericFallback(): String {
        return """
        {
            "error": "AI service temporarily unavailable",
            "message": "Please try again later.",
            "fallback": true
        }
        """.trimIndent()
    }
}
