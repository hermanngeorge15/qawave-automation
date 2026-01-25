package com.qawave.infrastructure.ai

import com.qawave.domain.model.QaPackageConfig

/**
 * Templates for AI prompts used in scenario generation and evaluation.
 */
object PromptTemplates {
    /**
     * System prompt for scenario generation.
     */
    val SCENARIO_GENERATION_SYSTEM =
        """
        You are an expert QA engineer specializing in API testing. Your task is to generate
        comprehensive test scenarios for REST APIs based on OpenAPI specifications.

        Guidelines:
        - Generate realistic and meaningful test scenarios
        - Cover both happy path and edge cases
        - Include validation of response schemas
        - Consider authentication and authorization
        - Test error handling and edge cases
        - Use realistic test data

        Output format: Return valid JSON matching the TestScenario schema.
        """.trimIndent()

    /**
     * System prompt for QA evaluation.
     */
    val QA_EVALUATION_SYSTEM =
        """
        You are an expert QA evaluator analyzing test execution results. Your task is to
        provide a comprehensive assessment of API quality based on test results.

        Guidelines:
        - Identify patterns in failures
        - Assess overall API reliability
        - Highlight security concerns if any
        - Provide actionable recommendations
        - Rate quality, stability, and security

        Output format: Return valid JSON matching the QaSummary schema.
        """.trimIndent()

    /**
     * Generates a prompt for scenario generation.
     */
    fun generateScenarioPrompt(
        openApiSpec: String,
        requirements: String?,
        config: QaPackageConfig,
    ): String {
        return buildString {
            appendLine("Generate test scenarios for the following API:")
            appendLine()
            appendLine("## OpenAPI Specification")
            appendLine("```yaml")
            appendLine(openApiSpec.take(50000)) // Limit spec size
            appendLine("```")
            appendLine()

            if (!requirements.isNullOrBlank()) {
                appendLine("## Additional Requirements")
                appendLine(requirements)
                appendLine()
            }

            appendLine("## Configuration")
            appendLine("- Maximum scenarios: ${config.maxScenarios}")
            appendLine("- Maximum steps per scenario: ${config.maxStepsPerScenario}")
            appendLine("- Include security tests: ${config.includeSecurityTests}")
            appendLine()

            appendLine("## Output Requirements")
            appendLine("Generate a JSON array of test scenarios with the following structure:")
            appendLine(
                """
                ```json
                {
                    "scenarios": [
                        {
                            "name": "string - descriptive scenario name",
                            "description": "string - what this scenario tests",
                            "priority": "HIGH|MEDIUM|LOW",
                            "tags": ["string"],
                            "steps": [
                                {
                                    "name": "string - step description",
                                    "method": "GET|POST|PUT|DELETE|PATCH",
                                    "path": "string - API path",
                                    "headers": {"key": "value"},
                                    "queryParams": {"key": "value"},
                                    "body": {},
                                    "expectedStatus": 200,
                                    "assertions": ["string - assertion expressions"],
                                    "extractors": {"varName": "jsonPath"}
                                }
                            ]
                        }
                    ]
                }
                ```
                """.trimIndent(),
            )
        }
    }

    /**
     * Generates a prompt for QA evaluation.
     */
    fun generateEvaluationPrompt(
        scenarioResults: String,
        coverageReport: String?,
    ): String {
        return buildString {
            appendLine("Evaluate the following API test results:")
            appendLine()
            appendLine("## Test Results")
            appendLine("```json")
            appendLine(scenarioResults)
            appendLine("```")
            appendLine()

            if (!coverageReport.isNullOrBlank()) {
                appendLine("## Coverage Report")
                appendLine("```json")
                appendLine(coverageReport)
                appendLine("```")
                appendLine()
            }

            appendLine("## Output Requirements")
            appendLine("Provide a QA evaluation with the following structure:")
            appendLine(
                """
                ```json
                {
                    "overallVerdict": "PASS|FAIL|PASS_WITH_WARNINGS|ERROR|INCONCLUSIVE",
                    "summary": "string - executive summary",
                    "passedScenarios": 0,
                    "failedScenarios": 0,
                    "erroredScenarios": 0,
                    "keyFindings": [
                        {
                            "type": "BUG|REGRESSION|PERFORMANCE_ISSUE|SECURITY_CONCERN|DATA_INCONSISTENCY|API_CONTRACT_VIOLATION",
                            "severity": "INFO|LOW|MEDIUM|HIGH|CRITICAL",
                            "title": "string",
                            "description": "string",
                            "affectedScenarios": ["scenario-ids"]
                        }
                    ],
                    "recommendations": [
                        {
                            "priority": "LOW|MEDIUM|HIGH|IMMEDIATE",
                            "title": "string",
                            "description": "string",
                            "actionItems": ["string"]
                        }
                    ],
                    "riskAssessment": {
                        "overallRisk": "LOW|MEDIUM|HIGH|CRITICAL",
                        "qualityScore": 0-100,
                        "stabilityScore": 0-100,
                        "securityScore": 0-100,
                        "riskFactors": ["string"]
                    }
                }
                ```
                """.trimIndent(),
            )
        }
    }

    /**
     * Generates a prompt for coverage analysis.
     */
    fun generateCoveragePrompt(
        openApiSpec: String,
        scenarios: String,
    ): String {
        return buildString {
            appendLine("Analyze the test coverage for the following API:")
            appendLine()
            appendLine("## OpenAPI Specification")
            appendLine("```yaml")
            appendLine(openApiSpec.take(30000))
            appendLine("```")
            appendLine()
            appendLine("## Test Scenarios")
            appendLine("```json")
            appendLine(scenarios)
            appendLine("```")
            appendLine()
            appendLine("## Output Requirements")
            appendLine("Provide a coverage analysis with the following structure:")
            appendLine(
                """
                ```json
                {
                    "totalOperations": 0,
                    "coveredOperations": 0,
                    "coveragePercentage": 0.0,
                    "operationDetails": [
                        {
                            "operationId": "string",
                            "method": "GET|POST|PUT|DELETE|PATCH",
                            "path": "string",
                            "status": "COVERED|FAILING|UNTESTED",
                            "scenarioIds": ["string"]
                        }
                    ],
                    "gaps": [
                        {
                            "type": "UNCOVERED_OPERATION|FAILING_OPERATION|UNRESOLVED_PLACEHOLDER|WEAK_ASSERTIONS|MISSING_ERROR_CASES",
                            "operationId": "string",
                            "description": "string",
                            "severity": "LOW|MEDIUM|HIGH|CRITICAL"
                        }
                    ]
                }
                ```
                """.trimIndent(),
            )
        }
    }
}
