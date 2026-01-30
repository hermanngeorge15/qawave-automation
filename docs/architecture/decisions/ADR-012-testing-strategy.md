# ADR-012: Testing Strategy and Pyramid

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs a comprehensive testing strategy to ensure quality across:
- Backend Kotlin/Spring code
- Frontend React/TypeScript code
- Infrastructure as Code
- End-to-end user flows

## Decision

We follow the **Testing Pyramid** with emphasis on fast, isolated unit tests.

### Testing Pyramid

```
                          ┌─────────────┐
                         /│    E2E      │\
                        / │  (Playwright)│ \
                       /  │   ~50 tests │  \
                      /   └─────────────┘   \
                     /    ┌─────────────┐    \
                    /     │ Integration │     \
                   /      │(Testcontainers)    \
                  /       │  ~150 tests │       \
                 /        └─────────────┘        \
                /         ┌─────────────┐         \
               /          │    Unit     │          \
              /           │  (JUnit)    │           \
             /            │  ~500 tests │            \
            /             └─────────────┘             \
           ──────────────────────────────────────────────
```

### Backend Testing

| Level | Tool | Coverage Target | Speed |
|-------|------|-----------------|-------|
| Unit | JUnit 5 + MockK | 80% | < 5s |
| Integration | Testcontainers | 60% | < 60s |
| Contract | Spring Cloud Contract | APIs | < 30s |

**Unit Test Example:**
```kotlin
@Test
fun `generates scenarios from openapi spec`() = runTest {
    val mockAiClient = mockk<AiClient> {
        coEvery { complete(any()) } returns AiResponse(scenarioJson)
    }
    val generator = ScenarioGeneratorAgent(mockAiClient)

    val scenarios = generator.generateForOperations(operations)

    assertEquals(2, scenarios.size)
    assertEquals("GET", scenarios[0].steps[0].method)
}
```

**Integration Test Example:**
```kotlin
@SpringBootTest
@Testcontainers
class QaPackageIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Test
    fun `creates and retrieves package`() = runTest {
        val request = CreateQaPackageRequest(name = "Test", baseUrl = "https://api.test")
        val created = webClient.post("/api/qa/packages")
            .body(request)
            .retrieve()
            .awaitBody<QaPackageResponse>()

        assertNotNull(created.id)
    }
}
```

### Frontend Testing

| Level | Tool | Coverage Target |
|-------|------|-----------------|
| Unit | Vitest | 70% |
| Component | React Testing Library | 60% |
| E2E | Playwright | Critical paths |

### E2E Testing

**Playwright Configuration:**
```typescript
// playwright.config.ts
export default defineConfig({
  testDir: './e2e-tests',
  workers: process.env.CI ? 4 : 1,
  retries: process.env.CI ? 2 : 0,
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
  ],
});
```

**Critical E2E Flows:**
1. Login → Create Package → View Results
2. Run Package → View Scenarios
3. Export Results → Download

### Load Testing

| Test Type | VUs | Duration | Threshold |
|-----------|-----|----------|-----------|
| Smoke | 1 | 1 min | P95 < 1s |
| Load | 50 | 10 min | P95 < 500ms |
| Stress | 200 | 15 min | P95 < 2s |
| Soak | 30 | 30 min | P95 < 500ms |

## Consequences

### Positive
- Fast feedback via unit tests
- Confidence in integrations
- Regression protection
- Performance baselines

### Negative
- Test maintenance overhead
- CI time increases
- Infrastructure for Testcontainers

## References

- [Testing Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Testcontainers](https://testcontainers.com/)
- [Playwright](https://playwright.dev/)
