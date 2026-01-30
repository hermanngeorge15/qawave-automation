# ADR-001: Spring WebFlux with Kotlin Coroutines

## Status
Accepted

## Date
2026-01-30

## Context

QAWave is an AI-powered QA automation platform that needs to:
- Handle concurrent AI API calls (OpenAI, Venice)
- Execute HTTP requests against target APIs
- Process streaming responses from AI providers
- Scale horizontally to handle multiple QA package runs

Traditional blocking I/O would limit throughput when waiting for:
- AI API responses (latency: 500ms-30s)
- Target API responses during test execution
- Database queries
- Kafka message publishing

We needed to choose between:
1. **Spring MVC with virtual threads (Project Loom)** - Simple blocking code with JVM-level concurrency
2. **Spring WebFlux with Reactor** - Reactive streams with Mono/Flux
3. **Spring WebFlux with Kotlin Coroutines** - Reactive with suspend functions
4. **Ktor** - Kotlin-native async framework

## Decision

We chose **Spring WebFlux with Kotlin Coroutines** for the backend.

### Key Rationale

1. **Clean suspend function API**: Business logic uses `suspend` functions that look synchronous, avoiding Mono/Flux complexity in service layers
2. **Spring ecosystem access**: Security, Actuator, Validation, Spring Data R2DBC all integrate seamlessly
3. **Production-ready**: Battle-tested with excellent observability (Micrometer, Actuator)
4. **Non-blocking from ground up**: Netty server, R2DBC database driver, reactive Redis/Kafka
5. **Type safety**: Kotlin's null safety combined with Spring's validation

### Code Style Guidelines

```kotlin
// GOOD: suspend functions in services
@Service
class QaPackageService(private val repository: QaPackageRepository) {
    suspend fun findById(id: UUID): QaPackage? {
        return repository.findById(id)
    }
}

// BAD: Exposing Mono/Flux in business layer
@Service
class QaPackageService(private val repository: QaPackageRepository) {
    fun findById(id: UUID): Mono<QaPackage> { // Avoid this!
        return repository.findById(id)
    }
}
```

### Layer Boundaries

| Layer | Async Model |
|-------|-------------|
| Controllers | `suspend fun` |
| Services | `suspend fun` |
| Repositories | `suspend fun` (Spring Data R2DBC CoroutineCrudRepository) |
| Infrastructure | Bridge to reactive APIs using `awaitSingle()`, `awaitSingleOrNull()` |

## Consequences

### Positive
- High throughput for I/O-bound operations
- Clean, readable business logic
- Full Spring ecosystem support
- Excellent testing story with `runTest { }`

### Negative
- Learning curve for developers unfamiliar with coroutines
- Debugging stack traces can be more complex
- Must be careful about blocking code (use `withContext(Dispatchers.IO)` for blocking operations)
- Some Spring features not fully coroutine-native yet

### Risks
- **Blocking code contamination**: A single blocking call can degrade performance. Mitigated by code review and BlockHound in tests.
- **Memory overhead**: Each coroutine has small overhead. Not a concern at our scale.

## Alternatives Considered

### Spring MVC + Virtual Threads
- **Pros**: Simple blocking code, good tooling
- **Cons**: Less mature, debugging challenges, doesn't leverage existing reactive drivers

### Ktor
- **Pros**: Kotlin-native, lightweight
- **Cons**: Smaller ecosystem, fewer integrations, less mature than Spring

## References

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring Data R2DBC Coroutine Support](https://docs.spring.io/spring-data/r2dbc/reference/kotlin/coroutines.html)
