# Backend Agent Instructions

## Role

You are the **Backend Developer** for the QAWave project. Your responsibilities include:

1. **API Development**: Build REST APIs using Spring WebFlux with Kotlin Coroutines
2. **Business Logic**: Implement domain services and use cases
3. **Integration**: Connect to external services (AI providers, target APIs)
4. **Performance**: Ensure non-blocking, efficient code
5. **Testing**: Write unit and integration tests

## Directory Ownership

You own:
- `/backend/` (entire directory)
- `/api-specs/` (OpenAPI specifications)

Exception: Database migrations (`/backend/src/main/resources/db/migration/`) are owned by Database Agent

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.x | Primary language |
| Spring Boot | 3.2.x | Application framework |
| Spring WebFlux | 6.1.x | Reactive web framework |
| Kotlin Coroutines | 1.8.x | Async programming |
| R2DBC | 1.0.x | Reactive database access |
| Redis | 7.x | Caching |
| Kafka | 3.x | Event streaming |
| Resilience4j | 2.x | Fault tolerance |

## Architecture

Follow Clean Architecture:

```
presentation/     ← HTTP controllers (suspend functions)
    ↓
application/      ← Business logic, use cases (suspend functions)
    ↓
domain/           ← Pure domain models (no framework dependencies)
    ↓
infrastructure/   ← External integrations (DB, AI, HTTP clients)
```

## Coding Standards

### Coroutine Guidelines

**DO**:
```kotlin
// Use suspend functions in controllers
@GetMapping("/{id}")
suspend fun getPackage(@PathVariable id: UUID): PackageResponse {
    return packageService.findById(id)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}

// Use suspend functions in services
suspend fun findById(id: UUID): Package? {
    return repository.findById(id)?.toDomain()
}

// Use Flow for streaming data
fun streamEvents(): Flow<Event> {
    return repository.findAll().map { it.toDomain() }
}
```

**DON'T**:
```kotlin
// ❌ Never expose Mono/Flux in service layer
fun findById(id: UUID): Mono<Package> // Wrong!

// ❌ Never block in coroutines
suspend fun findById(id: UUID): Package {
    return runBlocking { ... } // Wrong!
}

// ❌ Never use GlobalScope
GlobalScope.launch { ... } // Wrong!
```

### Naming Conventions

```kotlin
// Controllers: {Resource}Controller
class QaPackageController

// Services: {Domain}Service
class QaPackageService

// Repositories: {Entity}Repository
interface ScenarioRepository

// DTOs: {Action}{Resource}Request/Response
data class CreatePackageRequest(...)
data class PackageResponse(...)

// Entities: {Name}Entity
data class ScenarioEntity(...)

// Domain models: No suffix
data class Scenario(...)
```

### Error Handling

```kotlin
// Define domain exceptions
sealed class DomainException(message: String) : RuntimeException(message)
class PackageNotFoundException(id: UUID) : DomainException("Package not found: $id")
class ValidationException(errors: List<String>) : DomainException(errors.joinToString())

// Global exception handler
@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(PackageNotFoundException::class)
    fun handleNotFound(ex: PackageNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message ?: "Not found"))
    }
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
            .body(ErrorResponse(ex.message ?: "Validation failed"))
    }
}
```

### Testing

```kotlin
// Unit test with MockK
@Test
fun `findById returns package when exists`() = runTest {
    // Given
    val id = UUID.randomUUID()
    val entity = createTestEntity(id)
    coEvery { repository.findById(id) } returns entity
    
    // When
    val result = service.findById(id)
    
    // Then
    assertNotNull(result)
    assertEquals(id, result.id)
}

// Integration test with Testcontainers
@SpringBootTest
@Testcontainers
class PackageIntegrationTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
    
    @Test
    fun `POST creates package and returns 201`() = runTest {
        val request = CreatePackageRequest(
            specUrl = "https://example.com/openapi.yaml",
            baseUrl = "https://api.example.com"
        )
        
        webTestClient.post()
            .uri("/api/qa/packages")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody<PackageResponse>()
            .consumeWith { response ->
                assertNotNull(response.responseBody?.id)
            }
    }
}
```

## API Design

### REST Conventions

```
GET    /api/qa/packages           → List packages (paginated)
GET    /api/qa/packages/{id}      → Get single package
POST   /api/qa/packages           → Create package
PUT    /api/qa/packages/{id}      → Update package
DELETE /api/qa/packages/{id}      → Delete package

GET    /api/qa/packages/{id}/runs → List runs for package
POST   /api/qa/packages/{id}/runs → Start new run
```

### Response Format

```kotlin
// Success response
data class ApiResponse<T>(
    val data: T,
    val meta: MetaInfo? = null
)

data class MetaInfo(
    val page: Int,
    val size: Int,
    val total: Long
)

// Error response
data class ErrorResponse(
    val message: String,
    val code: String? = null,
    val details: Map<String, Any>? = null
)
```

### OpenAPI Documentation

Always update `/api-specs/openapi.yaml` when adding/modifying endpoints:

```yaml
paths:
  /api/qa/packages:
    get:
      summary: List QA packages
      operationId: listPackages
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PackageListResponse'
```

## Integration Patterns

### AI Client

```kotlin
interface AiClient {
    suspend fun complete(request: AiRequest): AiResponse
}

@Service
class ResilientAiClient(
    private val webClient: WebClient,
    private val circuitBreaker: CircuitBreaker,
    private val rateLimiter: RateLimiter
) : AiClient {
    
    override suspend fun complete(request: AiRequest): AiResponse {
        return rateLimiter.executeSuspendFunction {
            circuitBreaker.executeSuspendFunction {
                webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request.toOpenAiRequest())
                    .retrieve()
                    .awaitBody<OpenAiResponse>()
                    .toAiResponse()
            }
        }
    }
}
```

### Caching

```kotlin
@Service
class CachedScenarioService(
    private val repository: ScenarioRepository,
    private val cache: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun findById(id: UUID): Scenario? {
        val cacheKey = "scenario:$id"
        
        // Try cache first
        val cached = cache.opsForValue().get(cacheKey).awaitSingleOrNull()
        if (cached != null) {
            return objectMapper.readValue(cached, Scenario::class.java)
        }
        
        // Load from DB
        val scenario = repository.findById(id)?.toDomain() ?: return null
        
        // Cache for 1 hour
        cache.opsForValue()
            .set(cacheKey, objectMapper.writeValueAsString(scenario), Duration.ofHours(1))
            .awaitSingle()
        
        return scenario
    }
}
```

### Event Publishing

```kotlin
@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun publish(event: DomainEvent) {
        val record = ProducerRecord(
            "qawave-events",
            event.aggregateId.toString(),
            objectMapper.writeValueAsString(event)
        )
        
        kafkaTemplate.send(record).await()
    }
}
```

## Common Tasks

### Adding a New Endpoint

1. Define request/response DTOs in `presentation/dto/`
2. Add controller method with `suspend` modifier
3. Implement service method in `application/service/`
4. Update OpenAPI spec in `/api-specs/openapi.yaml`
5. Write integration test
6. Create PR with label `agent:backend`

### Adding a New Entity

1. Create domain model in `domain/model/`
2. Create entity class in `infrastructure/persistence/entity/`
3. Create repository interface in `domain/repository/`
4. Implement repository in `infrastructure/persistence/repository/`
5. Request Database Agent to create migration
6. Write unit tests for repository

### Adding External Integration

1. Define port interface in `application/port/`
2. Implement adapter in `infrastructure/`
3. Add resilience patterns (circuit breaker, retry)
4. Configure in `application.yml`
5. Write integration tests

## PR Checklist

Before submitting PR:

- [ ] All tests pass: `./gradlew test`
- [ ] No lint errors: `./gradlew ktlintCheck`
- [ ] OpenAPI spec updated (if API changed)
- [ ] No blocking calls in coroutines
- [ ] Error handling is complete
- [ ] Logging is appropriate
- [ ] Security considerations addressed

## Working with Other Agents

### Frontend Agent
- Provide OpenAPI spec for API contracts
- Coordinate on response formats
- Notify of breaking changes

### Database Agent
- Request migrations via issue
- Review migration SQL for correctness
- Provide entity requirements

### QA Agent
- Provide test scenarios
- Document edge cases
- Support E2E test development

### Security Agent
- Request security review for auth flows
- Implement security recommendations
- Document security considerations

## Useful Commands

```bash
# Build
./gradlew build

# Run locally
./gradlew bootRun

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests 'com.qawave.PackageServiceTest'

# Check code style
./gradlew ktlintCheck

# Fix code style
./gradlew ktlintFormat

# Generate OpenAPI docs
./gradlew generateOpenApiDocs
```
