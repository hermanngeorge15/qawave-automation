package com.qawave.application.service

import com.qawave.domain.model.ExpectedResult
import com.qawave.domain.model.FieldMatcher
import com.qawave.domain.model.QaPackageId
import com.qawave.domain.model.ScenarioId
import com.qawave.domain.model.TestScenario
import com.qawave.domain.model.TestStep
import com.qawave.domain.model.TestSuiteId
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Service for exporting test scenarios as Playwright test code.
 */
interface PlaywrightExportService {
    /**
     * Export a single scenario as Playwright test code.
     */
    suspend fun exportScenario(scenarioId: ScenarioId): PlaywrightExportResult

    /**
     * Export a test suite as a ZIP archive with Playwright tests.
     */
    suspend fun exportSuite(suiteId: TestSuiteId): ByteArray

    /**
     * Export all scenarios for a QA package as a ZIP archive.
     */
    suspend fun exportPackage(packageId: QaPackageId): ByteArray
}

/**
 * Result of exporting a scenario.
 */
data class PlaywrightExportResult(
    val scenarioId: String,
    val scenarioName: String,
    val testCode: String,
    val filename: String,
)

@Service
class PlaywrightExportServiceImpl(
    private val scenarioService: ScenarioService,
    private val qaPackageService: QaPackageService,
) : PlaywrightExportService {
    override suspend fun exportScenario(scenarioId: ScenarioId): PlaywrightExportResult {
        val scenario =
            scenarioService.findById(scenarioId)
                ?: throw ExportNotFoundException("Scenario not found: $scenarioId")

        val testCode = generateTestCode(scenario)
        val filename = toFilename(scenario.name)

        return PlaywrightExportResult(
            scenarioId = scenario.id.toString(),
            scenarioName = scenario.name,
            testCode = testCode,
            filename = filename,
        )
    }

    override suspend fun exportSuite(suiteId: TestSuiteId): ByteArray {
        val scenarios = scenarioService.findBySuiteId(suiteId)
        if (scenarios.isEmpty()) {
            throw ExportNotFoundException("No scenarios found in suite: $suiteId")
        }

        return createZipArchive(scenarios, "suite-$suiteId")
    }

    override suspend fun exportPackage(packageId: QaPackageId): ByteArray {
        val qaPackage =
            qaPackageService.findById(packageId)
                ?: throw ExportNotFoundException("QA Package not found: $packageId")

        val scenarios = scenarioService.findByPackageId(packageId).toList()
        if (scenarios.isEmpty()) {
            throw ExportNotFoundException("No scenarios found in package: $packageId")
        }

        return createZipArchive(scenarios, "package-${qaPackage.name.lowercase().replace(" ", "-")}")
    }

    private fun createZipArchive(
        scenarios: List<TestScenario>,
        projectName: String,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            // Add package.json
            zip.putNextEntry(ZipEntry("$projectName/package.json"))
            zip.write(generatePackageJson(projectName).toByteArray())
            zip.closeEntry()

            // Add playwright.config.ts
            zip.putNextEntry(ZipEntry("$projectName/playwright.config.ts"))
            zip.write(generatePlaywrightConfig().toByteArray())
            zip.closeEntry()

            // Add tsconfig.json
            zip.putNextEntry(ZipEntry("$projectName/tsconfig.json"))
            zip.write(generateTsConfig().toByteArray())
            zip.closeEntry()

            // Add .env.example
            zip.putNextEntry(ZipEntry("$projectName/.env.example"))
            zip.write(generateEnvExample().toByteArray())
            zip.closeEntry()

            // Add README.md
            zip.putNextEntry(ZipEntry("$projectName/README.md"))
            zip.write(generateReadme(projectName, scenarios.size).toByteArray())
            zip.closeEntry()

            // Add test files
            scenarios.forEach { scenario ->
                val filename = toFilename(scenario.name)
                val testCode = generateTestCode(scenario)
                zip.putNextEntry(ZipEntry("$projectName/tests/$filename"))
                zip.write(testCode.toByteArray())
                zip.closeEntry()
            }
        }

        return outputStream.toByteArray()
    }

    private fun generateTestCode(scenario: TestScenario): String {
        val sb = StringBuilder()

        // Header comment
        sb.appendLine("/**")
        sb.appendLine(" * Playwright API Test: ${scenario.name}")
        sb.appendLine(" *")
        scenario.description?.let {
            sb.appendLine(" * Description: $it")
            sb.appendLine(" *")
        }
        sb.appendLine(" * Scenario ID: ${scenario.id}")
        sb.appendLine(" * Generated: ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}")
        sb.appendLine(" * Source: QAWave Test Exporter")
        sb.appendLine(" */")
        sb.appendLine()

        // Imports
        sb.appendLine("import { test, expect } from '@playwright/test';")
        sb.appendLine()

        // Constants
        sb.appendLine("// Base URL from environment variable")
        sb.appendLine("const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';")
        sb.appendLine()

        // Test describe block
        val testName = escapeString(scenario.name)
        sb.appendLine("test.describe('$testName', () => {")

        // Context for variable extractions
        sb.appendLine("  // Extracted values from previous steps")
        sb.appendLine("  const context: Record<string, string> = {};")
        sb.appendLine()

        // Generate test for each step
        scenario.orderedSteps.forEachIndexed { index, step ->
            generateStepTest(sb, step, index == 0)
        }

        sb.appendLine("});")

        return sb.toString()
    }

    private fun generateStepTest(
        sb: StringBuilder,
        step: TestStep,
        isFirst: Boolean,
    ) {
        val stepName = escapeString(step.name)
        val serialModifier = if (!isFirst) ".serial" else ""

        sb.appendLine("  test$serialModifier('Step ${step.index}: $stepName', async ({ request }) => {")

        // Build URL
        val endpoint = step.endpoint.replace("\${", "' + context['").replace("}", "'] + '")
        sb.appendLine("    const url = `\${BASE_URL}$endpoint`;")
        sb.appendLine()

        // Build headers
        if (step.headers.isNotEmpty()) {
            sb.appendLine("    const headers: Record<string, string> = {")
            step.headers.forEach { (key, value) ->
                val processedValue = value.replace("\${", "' + context['").replace("}", "'] + '")
                sb.appendLine("      '$key': '$processedValue',")
            }
            sb.appendLine("    };")
            sb.appendLine()
        }

        // Make request
        sb.appendLine("    // Make ${step.method} request")
        val requestCall = buildRequestCall(step)
        sb.appendLine("    const response = await request.${step.method.name.lowercase()}(url$requestCall);")
        sb.appendLine()

        // Assertions
        generateAssertions(sb, step.expected)

        // Extractions
        if (step.extractions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("    // Extract values for subsequent steps")
            sb.appendLine("    const responseBody = await response.json();")
            step.extractions.forEach { (key, jsonPath) ->
                val path =
                    jsonPath.replace("$.", "").split(".")
                        .joinToString("?.") { "['$it']" }
                sb.appendLine("    context['$key'] = responseBody$path;")
            }
        }

        sb.appendLine("  });")
        sb.appendLine()
    }

    private fun buildRequestCall(step: TestStep): String {
        val parts = mutableListOf<String>()

        if (step.headers.isNotEmpty()) {
            parts.add("headers")
        }

        if (step.body != null) {
            parts.add("data: ${formatBody(step.body)}")
        }

        return if (parts.isEmpty()) {
            ""
        } else {
            ", { ${parts.joinToString(", ")} }"
        }
    }

    private fun formatBody(body: String): String {
        // Try to detect if it's JSON
        return if (body.trim().startsWith("{") || body.trim().startsWith("[")) {
            body.replace("\${", "' + context['").replace("}", "'] + '")
        } else {
            "'${escapeString(body)}'"
        }
    }

    private fun generateAssertions(
        sb: StringBuilder,
        expected: ExpectedResult,
    ) {
        sb.appendLine("    // Assertions")

        // Status code assertion
        expected.status?.let { status ->
            sb.appendLine("    expect(response.status()).toBe($status);")
        }

        expected.statusRange?.let { range ->
            sb.appendLine("    expect(response.status()).toBeGreaterThanOrEqual(${range.first});")
            sb.appendLine("    expect(response.status()).toBeLessThanOrEqual(${range.last});")
        }

        // Headers assertions
        expected.headers.forEach { (key, value) ->
            sb.appendLine("    expect(response.headers()['${key.lowercase()}']).toBe('$value');")
        }

        // Body contains assertions
        if (expected.bodyContains.isNotEmpty()) {
            sb.appendLine("    const bodyText = await response.text();")
            expected.bodyContains.forEach { text ->
                sb.appendLine("    expect(bodyText).toContain('${escapeString(text)}');")
            }
        }

        // Body field assertions
        if (expected.bodyFields.isNotEmpty()) {
            if (expected.bodyContains.isEmpty()) {
                sb.appendLine("    const body = await response.json();")
            } else {
                sb.appendLine("    const body = JSON.parse(bodyText);")
            }

            expected.bodyFields.forEach { (field, matcher) ->
                generateFieldAssertion(sb, field, matcher)
            }
        }
    }

    private fun generateFieldAssertion(
        sb: StringBuilder,
        field: String,
        matcher: FieldMatcher,
    ) {
        val fieldPath = "body${field.split(".").joinToString("") { "['$it']" }}"

        when (matcher) {
            is FieldMatcher.Exact -> {
                val value = formatValue(matcher.value)
                sb.appendLine("    expect($fieldPath).toBe($value);")
            }
            is FieldMatcher.Any -> {
                sb.appendLine("    expect($fieldPath).toBeDefined();")
            }
            is FieldMatcher.Regex -> {
                sb.appendLine("    expect($fieldPath).toMatch(/${matcher.pattern}/);")
            }
            is FieldMatcher.GreaterThan -> {
                sb.appendLine("    expect($fieldPath).toBeGreaterThan(${matcher.value});")
            }
            is FieldMatcher.LessThan -> {
                sb.appendLine("    expect($fieldPath).toBeLessThan(${matcher.value});")
            }
            is FieldMatcher.OneOf -> {
                val values = matcher.values.joinToString(", ") { formatValue(it) }
                sb.appendLine("    expect([$values]).toContain($fieldPath);")
            }
            is FieldMatcher.NotNull -> {
                sb.appendLine("    expect($fieldPath).not.toBeNull();")
            }
            is FieldMatcher.IsNull -> {
                sb.appendLine("    expect($fieldPath).toBeNull();")
            }
        }
    }

    private fun formatValue(value: kotlin.Any): String {
        return when (value) {
            is String -> "'${escapeString(value)}'"
            is Boolean -> value.toString()
            is Number -> value.toString()
            else -> "'${escapeString(value.toString())}'"
        }
    }

    private fun escapeString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun toFilename(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("^-|-$"), "") + ".spec.ts"
    }

    private fun generatePackageJson(projectName: String): String {
        return """
            {
              "name": "$projectName",
              "version": "1.0.0",
              "description": "Playwright API tests exported from QAWave",
              "scripts": {
                "test": "playwright test",
                "test:headed": "playwright test --headed",
                "test:debug": "PWDEBUG=1 playwright test"
              },
              "devDependencies": {
                "@playwright/test": "^1.42.0",
                "typescript": "^5.4.0"
              }
            }
            """.trimIndent()
    }

    private fun generatePlaywrightConfig(): String {
        return """
            import { defineConfig } from '@playwright/test';

            /**
             * Playwright configuration for API tests.
             * @see https://playwright.dev/docs/test-configuration
             */
            export default defineConfig({
              testDir: './tests',
              fullyParallel: false,
              forbidOnly: !!process.env.CI,
              retries: process.env.CI ? 2 : 0,
              workers: 1,
              reporter: [
                ['list'],
                ['html', { outputFolder: 'playwright-report' }],
                ['json', { outputFile: 'test-results.json' }],
              ],
              use: {
                baseURL: process.env.BASE_URL || 'http://localhost:8080',
                extraHTTPHeaders: {
                  'Content-Type': 'application/json',
                },
                trace: 'on-first-retry',
              },
              projects: [
                {
                  name: 'api-tests',
                  testMatch: '**/*.spec.ts',
                },
              ],
            });
            """.trimIndent()
    }

    private fun generateTsConfig(): String {
        return """
            {
              "compilerOptions": {
                "target": "ES2020",
                "module": "commonjs",
                "strict": true,
                "esModuleInterop": true,
                "skipLibCheck": true,
                "forceConsistentCasingInFileNames": true,
                "outDir": "./dist",
                "resolveJsonModule": true
              },
              "include": ["tests/**/*.ts"],
              "exclude": ["node_modules"]
            }
            """.trimIndent()
    }

    private fun generateEnvExample(): String {
        return """
            # Base URL for API tests
            BASE_URL=http://localhost:8080

            # Optional: API key or token for authentication
            # API_KEY=your-api-key

            # Optional: OAuth token
            # AUTH_TOKEN=your-auth-token
            """.trimIndent()
    }

    private fun generateReadme(
        projectName: String,
        scenarioCount: Int,
    ): String {
        return """
            # $projectName

            Playwright API tests exported from QAWave.

            ## Overview

            This project contains $scenarioCount test scenario(s) exported from QAWave.

            ## Prerequisites

            - Node.js 18 or higher
            - npm or yarn

            ## Installation

            ```bash
            npm install
            ```

            ## Configuration

            1. Copy `.env.example` to `.env`:
               ```bash
               cp .env.example .env
               ```

            2. Update the `BASE_URL` in `.env` to point to your API server.

            ## Running Tests

            ```bash
            # Run all tests
            npm test

            # Run tests with debug mode
            npm run test:debug

            # Run tests with headed browser (for debugging)
            npm run test:headed
            ```

            ## Test Reports

            After running tests, reports are available at:
            - HTML Report: `playwright-report/index.html`
            - JSON Report: `test-results.json`

            ## Generated by QAWave

            This project was automatically generated by the QAWave Test Exporter.
            For more information, visit: https://qawave.com
            """.trimIndent()
    }
}
