package com.qawave.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qawave.domain.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for generating test scenarios using AI.
 */
@Service
class AiScenarioGenerator(
    private val aiClient: AiClient,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(AiScenarioGenerator::class.java)

    /**
     * Generates test scenarios from an OpenAPI specification.
     *
     * @param packageId The QA package ID
     * @param openApiSpec The OpenAPI specification
     * @param requirements Additional requirements
     * @param config The package configuration
     * @return List of generated test scenarios
     */
    suspend fun generateScenarios(
        packageId: QaPackageId,
        openApiSpec: String,
        requirements: String?,
        config: QaPackageConfig,
    ): List<TestScenario> {
        logger.info(
            "Generating scenarios for package: id={}, maxScenarios={}",
            packageId,
            config.maxScenarios,
        )

        val prompt = PromptTemplates.generateScenarioPrompt(openApiSpec, requirements, config)

        val request =
            AiCompletionRequest(
                prompt = prompt,
                systemPrompt = PromptTemplates.SCENARIO_GENERATION_SYSTEM,
                model = config.aiModel,
                temperature = 0.3,
                maxTokens = 8192,
            )

        val response = aiClient.complete(request)

        logger.debug(
            "AI response tokens: prompt={}, completion={}, total={}",
            response.promptTokens,
            response.completionTokens,
            response.totalTokens,
        )

        return parseScenarioResponse(packageId, response.content)
    }

    /**
     * Evaluates test results and generates a QA summary.
     *
     * @param scenarioResults JSON string of scenario execution results
     * @param coverageReport Optional coverage report JSON
     * @return QA summary
     */
    suspend fun evaluateResults(
        scenarioResults: String,
        coverageReport: String?,
    ): QaSummary {
        logger.info("Evaluating test results")

        val prompt = PromptTemplates.generateEvaluationPrompt(scenarioResults, coverageReport)

        val request =
            AiCompletionRequest(
                prompt = prompt,
                systemPrompt = PromptTemplates.QA_EVALUATION_SYSTEM,
                temperature = 0.2,
                maxTokens = 4096,
            )

        val response = aiClient.complete(request)

        return parseEvaluationResponse(response.content)
    }

    /**
     * Analyzes test coverage for an API.
     *
     * @param openApiSpec The OpenAPI specification
     * @param scenarios JSON string of test scenarios
     * @return Coverage report
     */
    suspend fun analyzeCoverage(
        openApiSpec: String,
        scenarios: String,
    ): CoverageReport {
        logger.info("Analyzing test coverage")

        val prompt = PromptTemplates.generateCoveragePrompt(openApiSpec, scenarios)

        val request =
            AiCompletionRequest(
                prompt = prompt,
                temperature = 0.1,
                maxTokens = 4096,
            )

        val response = aiClient.complete(request)

        return parseCoverageResponse(response.content)
    }

    private fun parseScenarioResponse(
        packageId: QaPackageId,
        content: String,
    ): List<TestScenario> {
        val jsonContent = extractJson(content)

        return try {
            val response = objectMapper.readValue<ScenarioGenerationResponse>(jsonContent)
            val now = Instant.now()

            response.scenarios.map { scenario ->
                TestScenario(
                    id = ScenarioId.generate(),
                    suiteId = null,
                    qaPackageId = packageId,
                    name = scenario.name,
                    description = scenario.description,
                    tags = scenario.tags.toSet(),
                    source = ScenarioSource.AI_GENERATED,
                    status = ScenarioStatus.PENDING,
                    steps =
                        scenario.steps.mapIndexed { stepIndex, step ->
                            TestStep(
                                index = stepIndex,
                                name = step.name,
                                method = parseHttpMethod(step.method),
                                endpoint = step.path,
                                headers = step.headers ?: emptyMap(),
                                body = step.body?.let { objectMapper.writeValueAsString(it) },
                                expected =
                                    ExpectedResult(
                                        status = step.expectedStatus,
                                        bodyContains = step.assertions ?: emptyList(),
                                    ),
                                extractions = step.extractors ?: emptyMap(),
                            )
                        },
                    createdAt = now,
                    updatedAt = now,
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to parse scenario response: {}", e.message)
            throw AiClientException("Failed to parse AI response: ${e.message}", e)
        }
    }

    private fun parseEvaluationResponse(content: String): QaSummary {
        val jsonContent = extractJson(content)

        return try {
            val response = objectMapper.readValue<EvaluationResponse>(jsonContent)
            QaSummary(
                overallVerdict = parseVerdict(response.overallVerdict),
                summary = response.summary,
                passedScenarios = response.passedScenarios,
                failedScenarios = response.failedScenarios,
                erroredScenarios = response.erroredScenarios,
                keyFindings =
                    response.keyFindings?.map { finding ->
                        Finding(
                            type = parseFindingType(finding.type),
                            severity = parseFindingSeverity(finding.severity),
                            title = finding.title,
                            description = finding.description,
                            affectedScenarios = finding.affectedScenarios?.map { ScenarioId.from(it) } ?: emptyList(),
                        )
                    } ?: emptyList(),
                recommendations =
                    response.recommendations?.map { rec ->
                        Recommendation(
                            priority = parseRecommendationPriority(rec.priority),
                            title = rec.title,
                            description = rec.description,
                            actionItems = rec.actionItems ?: emptyList(),
                        )
                    } ?: emptyList(),
                riskAssessment =
                    response.riskAssessment?.let { risk ->
                        RiskAssessment(
                            overallRisk = parseRiskLevel(risk.overallRisk),
                            qualityScore = risk.qualityScore,
                            stabilityScore = risk.stabilityScore,
                            securityScore = risk.securityScore,
                            riskFactors = risk.riskFactors ?: emptyList(),
                        )
                    },
                generatedAt = Instant.now(),
            )
        } catch (e: Exception) {
            logger.error("Failed to parse evaluation response: {}", e.message)
            throw AiClientException("Failed to parse AI evaluation: ${e.message}", e)
        }
    }

    private fun parseCoverageResponse(content: String): CoverageReport {
        val jsonContent = extractJson(content)

        return try {
            val response = objectMapper.readValue<CoverageResponse>(jsonContent)
            CoverageReport(
                totalOperations = response.totalOperations,
                coveredOperations = response.coveredOperations,
                coveragePercentage = response.coveragePercentage,
                operationDetails =
                    response.operationDetails?.map { op ->
                        OperationCoverage(
                            operationId = op.operationId,
                            method = parseHttpMethod(op.method),
                            path = op.path,
                            status = parseOperationCoverageStatus(op.status),
                            scenarioIds = op.scenarioIds?.map { ScenarioId.from(it) } ?: emptyList(),
                        )
                    } ?: emptyList(),
                gaps =
                    response.gaps?.map { gap ->
                        CoverageGap(
                            type = parseCoverageGapType(gap.type),
                            operationId = gap.operationId,
                            description = gap.description,
                            severity = parseGapSeverity(gap.severity),
                        )
                    } ?: emptyList(),
                generatedAt = Instant.now(),
            )
        } catch (e: Exception) {
            logger.error("Failed to parse coverage response: {}", e.message)
            throw AiClientException("Failed to parse AI coverage: ${e.message}", e)
        }
    }

    private fun extractJson(content: String): String {
        // Try to extract JSON from markdown code blocks
        val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        val match = jsonPattern.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Try to find JSON object or array directly
        val trimmed = content.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed
        }

        return content
    }

    private fun parseHttpMethod(method: String): HttpMethod {
        return HttpMethod.valueOf(method.uppercase())
    }

    private fun parseVerdict(verdict: String): QaVerdict {
        return when (verdict.uppercase()) {
            "PASS" -> QaVerdict.PASS
            "FAIL" -> QaVerdict.FAIL
            "PASS_WITH_WARNINGS" -> QaVerdict.PASS_WITH_WARNINGS
            "ERROR" -> QaVerdict.ERROR
            else -> QaVerdict.INCONCLUSIVE
        }
    }

    private fun parseFindingType(type: String): FindingType {
        return FindingType.valueOf(type.uppercase())
    }

    private fun parseFindingSeverity(severity: String): FindingSeverity {
        return FindingSeverity.valueOf(severity.uppercase())
    }

    private fun parseRecommendationPriority(priority: String): RecommendationPriority {
        return RecommendationPriority.valueOf(priority.uppercase())
    }

    private fun parseRiskLevel(level: String): RiskLevel {
        return RiskLevel.valueOf(level.uppercase())
    }

    private fun parseOperationCoverageStatus(status: String): OperationCoverageStatus {
        return OperationCoverageStatus.valueOf(status.uppercase())
    }

    private fun parseCoverageGapType(type: String): CoverageGapType {
        return CoverageGapType.valueOf(type.uppercase())
    }

    private fun parseGapSeverity(severity: String): GapSeverity {
        return GapSeverity.valueOf(severity.uppercase())
    }
}

// ==================== Response DTOs ====================

data class ScenarioGenerationResponse(
    val scenarios: List<GeneratedScenario>,
)

data class GeneratedScenario(
    val name: String,
    val description: String?,
    val priority: String?,
    val tags: List<String> = emptyList(),
    val steps: List<GeneratedStep>,
)

data class GeneratedStep(
    val name: String,
    val method: String,
    val path: String,
    val headers: Map<String, String>?,
    val queryParams: Map<String, String>?,
    val body: Any?,
    val expectedStatus: Int?,
    val assertions: List<String>?,
    val extractors: Map<String, String>?,
)

data class EvaluationResponse(
    val overallVerdict: String,
    val summary: String,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val erroredScenarios: Int,
    val keyFindings: List<FindingResponse>?,
    val recommendations: List<RecommendationResponse>?,
    val riskAssessment: RiskAssessmentResponse?,
)

data class FindingResponse(
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val affectedScenarios: List<String>?,
)

data class RecommendationResponse(
    val priority: String,
    val title: String,
    val description: String,
    val actionItems: List<String>?,
)

data class RiskAssessmentResponse(
    val overallRisk: String,
    val qualityScore: Int,
    val stabilityScore: Int,
    val securityScore: Int?,
    val riskFactors: List<String>?,
)

data class CoverageResponse(
    val totalOperations: Int,
    val coveredOperations: Int,
    val coveragePercentage: Double,
    val operationDetails: List<OperationCoverageResponse>?,
    val gaps: List<CoverageGapResponse>?,
)

data class OperationCoverageResponse(
    val operationId: String,
    val method: String,
    val path: String,
    val status: String,
    val scenarioIds: List<String>?,
)

data class CoverageGapResponse(
    val type: String,
    val operationId: String?,
    val description: String,
    val severity: String,
)
