# ADR-003: AI Agent Pipeline Architecture

## Status
Accepted

## Date
2026-01-30

## Context

QAWave uses AI to automatically generate test scenarios from requirements and OpenAPI specifications. The AI integration involves:
- Multiple AI providers (OpenAI, Venice)
- Different AI tasks (scenario generation, result evaluation, requirements analysis)
- Need for reliability (retries, circuit breaker)
- Need for observability (logging AI interactions)
- Potential for multiple AI calls per QA package run

We needed a pattern that:
- Abstracts AI provider differences
- Makes AI agents testable
- Provides resilience for unreliable external APIs
- Enables easy addition of new AI capabilities

## Decision

We adopted an **AI Agent Pipeline** pattern with the following components:

### Agent Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AI AGENT PIPELINE                                   │
└─────────────────────────────────────────────────────────────────────────────┘

     ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
     │  INPUTS      │         │  AI AGENTS   │         │  OUTPUTS     │
     └──────────────┘         └──────────────┘         └──────────────┘

  ┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────┐
  │ Requirements    │────▶│ Requirements        │────▶│ Parsed User     │
  │ Document        │     │ Analyzer Agent      │     │ Flows & Criteria│
  └─────────────────┘     └─────────────────────┘     └─────────────────┘
          │                         │                         │
          │                         ▼                         │
          │               ┌─────────────────────┐             │
          │               │ Scenario Generator  │             │
          └──────────────▶│ Agent               │◀────────────┘
                          └─────────────────────┘
  ┌─────────────────┐               │
  │ OpenAPI Spec    │───────────────┤
  └─────────────────┘               ▼
                          ┌─────────────────────┐
                          │ Generated Test      │
                          │ Scenarios           │
                          └─────────────────────┘
                                    │
                                    ▼ (execution)
                          ┌─────────────────────┐     ┌─────────────────┐
                          │ Result Evaluator    │────▶│ Pass/Fail       │
                          │ Agent               │     │ Verdicts        │
                          └─────────────────────┘     └─────────────────┘
```

### Agent Responsibilities

| Agent | Input | Processing | Output |
|-------|-------|------------|--------|
| **RequirementsAnalyzer** | Natural language requirements | Extract user flows, acceptance criteria | Structured test objectives |
| **ScenarioGenerator** | Operations + spec + objectives | AI prompt → JSON scenarios | `List<TestScenario>` |
| **ResultEvaluator** | Expected vs actual | Assertion matching | Pass/fail verdicts |
| **ResultReviewer** | All run results | AI summary generation | QA report |

### Interface Design

```kotlin
// Base AI client abstraction
interface AiClient {
    suspend fun complete(request: AiCompletionRequest): AiResponse
}

// Agent interface for scenario generation
interface ScenarioGeneratorAgent {
    suspend fun generate(
        requirementText: String,
        baseUrl: String,
        openApiSpec: String?
    ): List<TestScenario>

    suspend fun generateForOperations(
        operations: List<SpecOperation>,
        baseUrl: String,
        promptTemplate: String? = null
    ): List<TestScenario>
}

// Agent interface for result evaluation
interface ResultEvaluatorAgent {
    fun evaluate(
        step: TestStep,
        execution: StepExecution
    ): TestStepResult
}
```

### Resilience Wrapper

```kotlin
@Service
class ResilientAiClient(
    private val delegate: AiClient,
    private val circuitBreaker: CircuitBreaker,
    private val rateLimiter: RateLimiter,
    private val retry: Retry
) : AiClient {

    override suspend fun complete(request: AiCompletionRequest): AiResponse {
        return rateLimiter.executeSuspendFunction {
            circuitBreaker.executeSuspendFunction {
                retry.executeSuspendFunction {
                    delegate.complete(request)
                }
            }
        }
    }
}
```

### Configuration

```yaml
qawave:
  ai:
    provider: openai          # openai | venice | stub
    model: gpt-4o-mini
    temperature: 0.2          # Low for consistent output
    max-attempts: 3           # Retry attempts
    concurrency-limit: 5      # Parallel AI calls
    timeout-ms: 60000         # Per-request timeout
```

## Consequences

### Positive
- Clean separation between AI orchestration and AI provider
- Easy to add new AI providers (Venice, Anthropic, etc.)
- Testable with stub implementations
- Resilience patterns prevent cascading failures
- Observability through interaction logging

### Negative
- Abstraction overhead for simple AI calls
- Need to maintain prompt templates
- AI response parsing can be fragile

### Risks
- **AI Output Quality**: Generated scenarios may be invalid. Mitigated by:
  - Schema validation
  - Spec alignment checking
  - Retry with corrective hints (RFC-001)
- **Cost Control**: Many AI calls can be expensive. Mitigated by:
  - Rate limiting
  - Concurrency limits
  - Token usage monitoring

## Testing Strategy

```kotlin
// Stub implementation for testing
class StubAiClient : AiClient {
    var responses: Queue<AiResponse> = LinkedList()

    override suspend fun complete(request: AiCompletionRequest): AiResponse {
        return responses.poll() ?: AiResponse.empty()
    }
}

// Test
@Test
fun `generates scenarios from openapi spec`() = runTest {
    val stubClient = StubAiClient().apply {
        responses.add(AiResponse(scenarioJson))
    }
    val agent = AiScenarioGeneratorAgent(stubClient)

    val scenarios = agent.generateForOperations(operations, baseUrl)

    assertEquals(1, scenarios.size)
    assertEquals("GET", scenarios[0].steps[0].method)
}
```

## References

- [ADR-001: Spring WebFlux with Kotlin Coroutines](ADR-001-spring-webflux-kotlin-coroutines.md)
- RFC-001: AI Generation Verification & Retry Loop (in BUSINESS_REQUIREMENTS.md)
- RFC-004: AI Interaction Observatory (in BUSINESS_REQUIREMENTS.md)
