# QAWave Test Strategy & Coverage Requirements

This document defines the testing standards, coverage requirements, and best practices for the QAWave platform.

## Testing Pyramid

```
                    ╭───────────────╮
                   │   E2E Tests   │  ← Few, slow, high confidence
                  │  (Playwright)  │
                 ╰─────────────────╯
               ╭─────────────────────╮
              │  Integration Tests  │  ← Moderate, medium speed
             │   (Testcontainers)   │
            ╰───────────────────────╯
          ╭───────────────────────────╮
         │       Unit Tests          │  ← Many, fast, focused
        │  (JUnit/Kotest, Vitest)   │
       ╰─────────────────────────────╯
```

## Coverage Targets

### Backend (Kotlin/Spring)

| Test Type | Coverage Target | Tool |
|-----------|-----------------|------|
| Unit Tests | 80% line coverage | JUnit 5 / Kotest |
| Integration Tests | Critical paths | Testcontainers |
| API Contract Tests | All endpoints | MockMvc |

**Focus Areas:**
- Domain logic: 90%+ coverage
- Application services: 80%+ coverage
- Infrastructure adapters: 70%+ coverage
- Controllers: Contract tests

### Frontend (React/TypeScript)

| Test Type | Coverage Target | Tool |
|-----------|-----------------|------|
| Unit Tests | 70% line coverage | Vitest |
| Component Tests | All components | React Testing Library |
| Integration Tests | Key workflows | Vitest |

**Focus Areas:**
- Hooks and utilities: 90%+ coverage
- Components: Render and interaction tests
- State management: All reducers/selectors

### E2E Tests (Playwright)

| Test Type | Coverage Target | Tool |
|-----------|-----------------|------|
| Smoke Tests | Critical paths 100% | Playwright |
| Feature Tests | All user stories | Playwright |
| API Tests | All endpoints | Playwright API |

**Critical Paths:**
1. User authentication (login/logout)
2. Package CRUD operations
3. Test run execution
4. Results viewing

## Test Naming Conventions

### Unit Tests

```kotlin
// Kotlin - Method under test, scenario, expected result
fun `createPackage should throw when specUrl is invalid`()
fun `findById should return empty when package not found`()
```

```typescript
// TypeScript - describe/it pattern
describe('usePackages hook', () => {
  it('should return packages list when query succeeds', () => {})
  it('should return error state when query fails', () => {})
})
```

### E2E Tests

```typescript
// Feature.Scenario pattern
test.describe('Package Management', () => {
  test('should create new package with valid data', async () => {})
  test('should show validation error for invalid URL', async () => {})
})
```

## Test Structure

### Arrange-Act-Assert Pattern

```kotlin
@Test
fun `createPackage should return created package`() {
    // Arrange
    val request = CreatePackageRequest(specUrl = "https://example.com/api.json")

    // Act
    val result = packageService.create(request)

    // Assert
    assertThat(result.specUrl).isEqualTo(request.specUrl)
    assertThat(result.id).isNotNull()
}
```

### Given-When-Then for BDD

```typescript
test('given authenticated user, when creating package, then package is saved', async () => {
  // Given
  await loginAsTestUser(page);

  // When
  await packagesPage.createPackage(testData);

  // Then
  await expect(page).toHaveURL(/packages\/[\w-]+/);
});
```

## Test Isolation Requirements

### Unit Tests
- **NO** external dependencies (database, API, filesystem)
- **NO** shared state between tests
- Use mocks/stubs for dependencies
- Each test must be independently runnable

### Integration Tests
- Use Testcontainers for databases
- Use WireMock for external APIs
- Clean up test data after each test
- Use unique identifiers for test data

### E2E Tests
- Clear cookies/storage before each test
- Create test data via API (not UI)
- Clean up created data in `afterEach`
- Use Page Object Model pattern

## Mock/Stub Guidelines

### When to Mock

✅ **Do Mock:**
- External APIs (AI services, third-party APIs)
- Time-sensitive operations
- Network failures/timeouts
- Rate-limited services

❌ **Don't Mock:**
- Domain logic
- Internal service calls in integration tests
- Database in integration tests

### Mock Implementation

```kotlin
// Kotlin - Use MockK
@Test
fun `should retry on AI service failure`() {
    every { aiService.generate(any()) } throws ServiceException()

    val result = runCatching { packageService.generateScenarios(packageId) }

    verify(exactly = 3) { aiService.generate(any()) }
    assertThat(result.isFailure).isTrue()
}
```

## CI/CD Testing Requirements

### Pull Request Checks

| Check | Required | Blocking |
|-------|----------|----------|
| Unit Tests | ✅ | ✅ |
| Integration Tests | ✅ | ✅ |
| E2E Smoke Tests | ✅ | ✅ |
| Coverage Threshold | ✅ | ⚠️ Warning only |
| Lint/Format | ✅ | ✅ |

### Merge to Main

| Check | Required | Blocking |
|-------|----------|----------|
| All PR checks | ✅ | ✅ |
| Full E2E Suite | ✅ | ✅ |
| Security Scan | ✅ | ✅ |

### Nightly Runs

| Check | Schedule |
|-------|----------|
| Full E2E Suite | 2 AM UTC |
| Load Tests (smoke) | 3 AM UTC |
| Security Scan | 4 AM UTC |

## Test Categorization

### Tags

| Tag | Description | When to Run |
|-----|-------------|-------------|
| `@smoke` | Critical path tests | Every PR |
| `@api` | API contract tests | Every PR |
| `@auth` | Authentication tests | Auth changes |
| `@slow` | Long-running tests | Nightly |
| `@flaky` | Known flaky tests | Manual only |

### Running by Tag

```bash
# Playwright
npm test -- --grep @smoke
npm test -- --grep @api
npm test -- --grep @auth

# Jest/Vitest
npm test -- --testPathPattern="smoke"
```

## Manual Testing Checklist

### Before Release

- [ ] Complete user registration flow (if applicable)
- [ ] Login/logout on all supported browsers
- [ ] Create package with real OpenAPI spec
- [ ] Run tests and verify results
- [ ] Check mobile responsive layouts
- [ ] Verify error messages are user-friendly
- [ ] Test with slow network (Chrome DevTools throttling)
- [ ] Test keyboard navigation
- [ ] Verify screen reader compatibility

### Browser Matrix

| Browser | Priority | Test Frequency |
|---------|----------|----------------|
| Chrome (latest) | P0 | Every release |
| Firefox (latest) | P1 | Every release |
| Safari (latest) | P1 | Every release |
| Edge (latest) | P2 | Major releases |
| Chrome Mobile | P1 | Every release |
| Safari Mobile | P1 | Every release |

## Test Maintenance Guidelines

### Flaky Test Handling

1. **Identify**: Mark with `@flaky` tag
2. **Investigate**: Root cause analysis within 48 hours
3. **Fix or Quarantine**: Fix issue or move to quarantine
4. **Review**: Weekly review of quarantined tests

### Flaky Test Patterns to Avoid

- Hardcoded waits (`sleep(1000)`)
- Race conditions in assertions
- External service dependencies
- Shared test data

### Preferred Patterns

```typescript
// Bad - hardcoded wait
await page.waitForTimeout(1000);

// Good - wait for specific condition
await expect(page.getByTestId('result')).toBeVisible();
```

### Test Review Process

**PR Review Checklist:**
- [ ] Test names are descriptive
- [ ] Tests follow AAA/GWT pattern
- [ ] No hardcoded waits
- [ ] Proper cleanup in afterEach
- [ ] No shared mutable state
- [ ] Coverage meets threshold

### Deprecation Process

1. Mark test with `@deprecated` comment
2. Create issue to remove/replace
3. Remove in next sprint if not needed
4. Update documentation

## Performance Test Requirements

### Thresholds

| Metric | Target | Critical |
|--------|--------|----------|
| P95 Response Time | < 500ms | < 1000ms |
| P99 Response Time | < 1000ms | < 2000ms |
| Error Rate | < 1% | < 5% |
| Throughput | > 100 RPS | > 50 RPS |

### Load Test Scenarios

| Test | VUs | Duration | Schedule |
|------|-----|----------|----------|
| Smoke | 1 | 1 min | Every PR |
| Load | 50 | 7 min | Weekly |
| Stress | 200 | 15 min | Monthly |
| Soak | 30 | 30 min | Monthly |

## Quality Gates

### Code Cannot Merge If:

1. Unit test coverage drops below threshold
2. Any test fails
3. New code has no tests
4. Lint errors present

### Warning (Non-Blocking):

1. Coverage decreases but stays above threshold
2. Skipped tests increase
3. Test execution time increases significantly

## Tools Reference

| Category | Tool | Documentation |
|----------|------|---------------|
| Unit (Backend) | JUnit 5 / Kotest | [Kotest Docs](https://kotest.io/) |
| Unit (Frontend) | Vitest | [Vitest Docs](https://vitest.dev/) |
| Component | React Testing Library | [RTL Docs](https://testing-library.com/) |
| E2E | Playwright | [Playwright Docs](https://playwright.dev/) |
| Load | k6 | [k6 Docs](https://k6.io/docs/) |
| Mocking | MockK, MSW | [MockK](https://mockk.io/) |
| Coverage | JaCoCo, c8 | CI Reports |

---

*Last Updated: 2026-01-30*
*QA Agent*
