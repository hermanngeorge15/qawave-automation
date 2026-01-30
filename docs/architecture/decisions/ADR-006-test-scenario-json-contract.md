# ADR-006: Test Scenario JSON Contract

## Status
Accepted

## Date
2026-01-30

## Context

QAWave's AI agents generate test scenarios that need to be:
- Stored in the database
- Executed by the test executor
- Displayed in the frontend
- Exported to external tools (Playwright, RestAssured)

We needed a **stable contract** between:
- AI generation output
- Test execution input
- Database persistence
- API responses

The contract must be:
- Flexible enough for AI to express various test patterns
- Structured enough for reliable execution
- Versioned for backward compatibility
- Human-readable for debugging

## Decision

We defined a **JSON-based Test Scenario Contract** that all components adhere to.

### TestScenario Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["name", "steps"],
  "properties": {
    "name": {
      "type": "string",
      "description": "Human-readable scenario name"
    },
    "description": {
      "type": "string",
      "description": "What this scenario tests"
    },
    "tags": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Categorization tags (e.g., smoke, regression)"
    },
    "steps": {
      "type": "array",
      "items": { "$ref": "#/definitions/TestStep" },
      "minItems": 1
    }
  },
  "definitions": {
    "TestStep": {
      "type": "object",
      "required": ["index", "name", "method", "endpoint", "expected"],
      "properties": {
        "index": { "type": "integer", "minimum": 0 },
        "name": { "type": "string" },
        "method": { "enum": ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"] },
        "endpoint": { "type": "string" },
        "headers": { "type": "object", "additionalProperties": { "type": "string" } },
        "body": { "type": ["object", "array", "string", "null"] },
        "expected": { "$ref": "#/definitions/Expected" },
        "extractions": { "type": "object", "additionalProperties": { "type": "string" } }
      }
    },
    "Expected": {
      "type": "object",
      "required": ["status"],
      "properties": {
        "status": { "type": "integer" },
        "bodyFields": { "type": "object" },
        "headers": { "type": "object", "additionalProperties": { "type": "string" } }
      }
    }
  }
}
```

### Example Scenario

```json
{
  "name": "User can register and login",
  "description": "Happy path for user authentication flow",
  "tags": ["smoke", "auth"],
  "steps": [
    {
      "index": 0,
      "name": "Register new user",
      "method": "POST",
      "endpoint": "{baseUrl}/api/users",
      "headers": { "Content-Type": "application/json" },
      "body": {
        "email": "test@example.com",
        "password": "secret123"
      },
      "expected": {
        "status": 201,
        "bodyFields": {
          "id": "<any>",
          "email": "test@example.com"
        }
      },
      "extractions": { "userId": "$.id" }
    },
    {
      "index": 1,
      "name": "Login with credentials",
      "method": "POST",
      "endpoint": "{baseUrl}/api/auth/login",
      "headers": { "Content-Type": "application/json" },
      "body": {
        "email": "test@example.com",
        "password": "secret123"
      },
      "expected": {
        "status": 200,
        "bodyFields": {
          "token": "<any>",
          "user.id": "{userId}"
        }
      },
      "extractions": { "authToken": "$.token" }
    }
  ]
}
```

### Assertion Syntax

| Syntax | Description | Example |
|--------|-------------|---------|
| `"value"` | Exact match | `"email": "test@example.com"` |
| `<any>` | Wildcard, any value accepted | `"id": "<any>"` |
| `contains:text` | Substring match | `"message": "contains:success"` |
| `regex:pattern` | Regular expression match | `"code": "regex:^[A-Z]{3}\\d{4}$"` |
| `>n`, `<n`, `>=n`, `<=n`, `!=n` | Numeric comparisons | `"count": ">0"` |
| `{placeholder}` | Value from extraction | `"userId": "{userId}"` |

### Placeholder Resolution

Placeholders in `endpoint`, `body`, and `expected` are resolved at execution time:

```kotlin
fun resolvePlaceholders(template: String, context: ExecutionContext): String {
    return PLACEHOLDER_REGEX.replace(template) { match ->
        val key = match.groupValues[1]
        when {
            key == "baseUrl" -> context.baseUrl
            context.extractions.containsKey(key) -> context.extractions[key]!!
            else -> match.value // Leave unresolved for debugging
        }
    }
}
```

### Extraction via JSONPath

```kotlin
// Extract userId from response body using JSONPath
val extractions = step.extractions.mapValues { (_, jsonPath) ->
    JsonPath.read<Any>(response.body, jsonPath).toString()
}
```

### Domain Model (Kotlin)

```kotlin
data class TestScenario(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val steps: List<TestStep>,
    val source: ScenarioSource = ScenarioSource.AI_GENERATED,
    val specHash: String? = null,
    val createdAt: Instant = Instant.now()
)

data class TestStep(
    val index: Int,
    val name: String,
    val method: HttpMethod,
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    val body: Any? = null,
    val expected: Expected,
    val extractions: Map<String, String> = emptyMap()
)

data class Expected(
    val status: Int,
    val bodyFields: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap()
)

enum class ScenarioSource {
    AI_GENERATED,
    MANUAL,
    IMPORTED
}
```

## Consequences

### Positive
- **Stable contract**: AI, executor, UI all speak same language
- **Human-readable**: Easy to debug scenarios in JSON
- **Flexible assertions**: Multiple comparison operators
- **Chainable steps**: Extractions enable multi-step flows
- **Exportable**: Can generate Playwright/RestAssured from JSON

### Negative
- JSON parsing overhead
- Limited expressiveness compared to code-based tests
- JSONPath learning curve for complex extractions

### Versioning Strategy

```kotlin
data class TestScenario(
    val schemaVersion: Int = 1,  // For future migrations
    // ...
)
```

When schema changes:
1. Increment `schemaVersion`
2. Add migration function
3. Migrate existing scenarios on read

## Validation

```kotlin
@Service
class ScenarioValidator {
    fun validate(scenario: TestScenario): ValidationResult {
        val errors = mutableListOf<String>()

        if (scenario.name.isBlank()) {
            errors.add("Scenario name is required")
        }
        if (scenario.steps.isEmpty()) {
            errors.add("At least one step is required")
        }
        scenario.steps.forEachIndexed { i, step ->
            if (step.endpoint.isBlank()) {
                errors.add("Step $i: endpoint is required")
            }
            if (step.expected.status !in 100..599) {
                errors.add("Step $i: invalid expected status")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
```

## References

- [JSONPath Specification](https://goessner.net/articles/JsonPath/)
- [ADR-003: AI Agent Pipeline](ADR-003-ai-agent-pipeline.md)
- RFC-001: AI Generation Verification & Retry Loop (in BUSINESS_REQUIREMENTS.md)
