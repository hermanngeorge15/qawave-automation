# QAWave - Technical Stack Documentation

## Overview

QAWave is built on a modern, cloud-native architecture with a clear separation between backend services and frontend UI. The backend uses **Spring WebFlux with Kotlin Coroutines** for fully reactive, non-blocking operations using suspend functions (no Mono/Flux in business code).

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 FRONTEND                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   React 18  │  │  TanStack   │  │  TanStack   │  │    Tailwind CSS     │ │
│  │ TypeScript  │  │   Router    │  │    Query    │  │      + HeroUI       │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                              │                                               │
│                              │ HTTP/JSON                                     │
└──────────────────────────────┼───────────────────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────────────────┐
│                              ▼           BACKEND                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │              Spring WebFlux + Kotlin Coroutines (Netty)               │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │ Controllers │  │    CORS     │  │Rate Limiter │  │   Metrics   │  │  │
│  │  │  (suspend)  │  │  WebFilter  │  │ (Resilience4j│  │ (Micrometer)│  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                              │                                               │
│  ┌───────────────────────────┼───────────────────────────────────────────┐  │
│  │                    APPLICATION LAYER                                  │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │  QaPackage  │  │   Test      │  │   Result    │  │     QA      │  │  │
│  │  │  RunService │  │  Executor   │  │  Evaluator  │  │  Evaluation │  │  │
│  │  │  (suspend)  │  │  (suspend)  │  │  (suspend)  │  │  (suspend)  │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                              │                                               │
│  ┌───────────────────────────┼───────────────────────────────────────────┐  │
│  │                   INFRASTRUCTURE LAYER                                │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │  AI Client  │  │   WebClient │  │    R2DBC    │  │   Reactor   │  │  │
│  │  │ (OpenAI/    │  │  (Reactive) │  │ (Coroutines)│  │    Kafka    │  │  │
│  │  │  Venice)    │  │             │  │             │  │             │  │  │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │  │
│  └─────────┼────────────────┼────────────────┼────────────────┼──────────┘  │
└────────────┼────────────────┼────────────────┼────────────────┼─────────────┘
             │                │                │                │
             ▼                ▼                ▼                ▼
      ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
      │  OpenAI  │     │  Target  │     │PostgreSQL│     │  Kafka   │
      │  Venice  │     │   APIs   │     │  (R2DBC) │     │ + Redis  │
      └──────────┘     └──────────┘     └──────────┘     └──────────┘
```

---

## Backend Stack

### Core Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 1.9.x | Primary language - concise, null-safe, coroutine support |
| **Spring Boot** | 3.2.x | Application framework with WebFlux support |
| **Spring WebFlux** | 6.1.x | Reactive web framework - non-blocking |
| **Netty** | 4.x | HTTP server engine - high-performance, non-blocking |
| **Coroutines** | 1.8.x | Structured concurrency with suspend functions |
| **kotlinx-coroutines-reactor** | 1.8.x | Bridge between Reactor and Coroutines |

### Why Spring WebFlux + Kotlin Coroutines?

1. **Suspend Functions**: Write async code like sync code - no Mono/Flux in business logic
2. **Spring Ecosystem**: Full access to Spring Security, Spring Data, Actuator
3. **Production Ready**: Battle-tested with excellent observability
4. **Type Safety**: Kotlin's null safety + Spring's validation
5. **Easy Testing**: suspend functions are straightforward to test

### Coroutine-Based Controller Example

```kotlin
@RestController
@RequestMapping("/api/qa/packages")
class QaPackageController(
    private val qaPackageService: QaPackageService
) {
    
    @GetMapping("/{id}")
    suspend fun getPackage(@PathVariable id: UUID): QaPackageResponse {
        return qaPackageService.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
    
    @GetMapping
    suspend fun listPackages(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<QaPackageResponse> {
        return qaPackageService.findAll(PageRequest.of(page, size))
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createPackage(
        @Valid @RequestBody request: CreateQaPackageRequest
    ): QaPackageResponse {
        return qaPackageService.create(request)
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deletePackage(@PathVariable id: UUID) {
        qaPackageService.delete(id)
    }
}
```

### Database Layer

| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 16.x | Primary production database |
| **R2DBC** | 1.0.x | Reactive database connectivity |
| **Spring Data R2DBC** | 3.2.x | Reactive repository support with coroutines |
| **Flyway** | 10.x | Database migration management |
| **r2dbc-pool** | 1.0.x | Connection pooling for R2DBC |

#### R2DBC with Coroutines Example

```kotlin
// Repository Interface (Spring Data R2DBC with Coroutines)
interface ScenarioRepository : CoroutineCrudRepository<ScenarioEntity, UUID> {
    
    suspend fun findByQaPackageId(qaPackageId: UUID): List<ScenarioEntity>
    
    fun findByStatusIn(statuses: List<String>): Flow<ScenarioEntity>
    
    @Query("SELECT * FROM scenarios WHERE created_at > :since ORDER BY created_at DESC")
    fun findRecentScenarios(since: Instant): Flow<ScenarioEntity>
}

// Entity
@Table("scenarios")
data class ScenarioEntity(
    @Id val id: UUID? = null,
    val qaPackageId: UUID,
    val name: String,
    val description: String?,
    val stepsJson: String,
    val status: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

// Service using suspend functions
@Service
class ScenarioService(
    private val scenarioRepository: ScenarioRepository,
    private val transactionalOperator: TransactionalOperator
) {
    
    suspend fun findById(id: UUID): TestScenario? {
        return scenarioRepository.findById(id)?.toDomain()
    }
    
    suspend fun findByPackageId(packageId: UUID): List<TestScenario> {
        return scenarioRepository.findByQaPackageId(packageId)
            .map { it.toDomain() }
    }
    
    suspend fun create(scenario: TestScenario): TestScenario {
        return transactionalOperator.executeAndAwait {
            scenarioRepository.save(scenario.toEntity())
        }!!.toDomain()
    }
    
    fun streamRecentScenarios(since: Instant): Flow<TestScenario> {
        return scenarioRepository.findRecentScenarios(since)
            .map { it.toDomain() }
    }
}
```

### Redis Caching with Coroutines

| Technology | Version | Purpose |
|------------|---------|---------|
| **Spring Data Redis Reactive** | 3.2.x | Reactive Redis operations |
| **Lettuce** | 6.x | Non-blocking Redis client |

```kotlin
@Service
class CacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun <T> getOrPut(
        key: String,
        ttl: Duration,
        type: Class<T>,
        loader: suspend () -> T
    ): T {
        val cached = redisTemplate.opsForValue()
            .get(key)
            .awaitSingleOrNull()
        
        if (cached != null) {
            return objectMapper.readValue(cached, type)
        }
        
        val value = loader()
        val json = objectMapper.writeValueAsString(value)
        
        redisTemplate.opsForValue()
            .set(key, json, ttl)
            .awaitSingle()
        
        return value
    }
    
    suspend fun invalidate(pattern: String) {
        redisTemplate.keys(pattern)
            .collectList()
            .awaitSingle()
            .forEach { key ->
                redisTemplate.delete(key).awaitSingle()
            }
    }
}
```

### Kafka with Coroutines

| Technology | Version | Purpose |
|------------|---------|---------|
| **Spring Kafka** | 3.1.x | Kafka integration |
| **reactor-kafka** | 1.3.x | Reactive Kafka with coroutine support |

```kotlin
@Service
class QaPackageEventProducer(
    private val kafkaSender: KafkaSender<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun publish(event: QaPackageEvent) {
        val record = SenderRecord.create(
            ProducerRecord(
                "qa-package-events",
                event.packageId.toString(),
                objectMapper.writeValueAsString(event)
            ),
            event.packageId
        )
        
        kafkaSender.send(Mono.just(record))
            .awaitFirst()
    }
}

@Component
class QaPackageEventConsumer(
    private val eventHandler: EventHandler
) {
    
    @KafkaListener(topics = ["qa-package-events"])
    suspend fun consume(record: ConsumerRecord<String, String>) {
        val event = objectMapper.readValue<QaPackageEvent>(record.value())
        eventHandler.handle(event)
    }
}
```

### AI Integration

| Technology | Purpose |
|------------|---------|
| **Spring WebClient** | HTTP calls to AI APIs |
| **Resilience4j** | Circuit breaker, rate limiting, retry |

```kotlin
@Service
class OpenAiClient(
    private val webClient: WebClient,
    @Value("\${ai.openai.api-key}") private val apiKey: String,
    @Value("\${ai.openai.model}") private val model: String,
    private val circuitBreaker: CircuitBreaker
) : AiClient {
    
    override suspend fun complete(request: AiCompletionRequest): AiResponse {
        return circuitBreaker.executeSuspendFunction {
            webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .bodyValue(
                    OpenAiRequest(
                        model = model,
                        messages = listOf(
                            Message("system", request.systemPrompt),
                            Message("user", request.userPrompt)
                        ),
                        temperature = request.temperature,
                        maxTokens = request.maxTokens
                    )
                )
                .retrieve()
                .awaitBody<OpenAiResponse>()
                .toAiResponse()
        }
    }
}

// Resilience4j suspend function extension
suspend fun <T> CircuitBreaker.executeSuspendFunction(
    block: suspend () -> T
): T = withContext(Dispatchers.IO) {
    decorateSuspendFunction(block).invoke()
}
```

### HTTP Client for Test Execution

```kotlin
@Service
class WebClientTestExecutor(
    private val webClient: WebClient,
    private val interactionLogger: InteractionLogger
) : TestExecutor {
    
    override suspend fun execute(
        step: TestStep,
        baseUrl: String,
        context: ExecutionContext
    ): TestStepResult {
        val resolvedEndpoint = resolvePlaceholders(step.endpoint, context)
        val resolvedBody = resolvePlaceholders(step.body, context)
        val startTime = Instant.now()
        
        return try {
            val response = withTimeout(step.timeoutMs.milliseconds) {
                webClient.method(HttpMethod.valueOf(step.method))
                    .uri("$baseUrl$resolvedEndpoint")
                    .headers { headers ->
                        step.headers.forEach { (k, v) -> headers.add(k, v) }
                    }
                    .bodyValue(resolvedBody ?: "")
                    .exchangeToMono { clientResponse ->
                        clientResponse.bodyToMono<String>().map { body ->
                            HttpResponse(
                                statusCode = clientResponse.statusCode().value(),
                                headers = clientResponse.headers().asHttpHeaders().toSingleValueMap(),
                                body = body
                            )
                        }
                    }
                    .awaitSingle()
            }
            
            evaluateResponse(step, response, context)
        } catch (e: TimeoutCancellationException) {
            TestStepResult.timeout(step, startTime)
        } catch (e: Exception) {
            TestStepResult.error(step, e, startTime)
        }
    }
}
```

### Resilience Patterns with Resilience4j

```kotlin
@Configuration
class ResilienceConfig {
    
    @Bean
    fun aiCircuitBreaker(): CircuitBreaker {
        return CircuitBreaker.of("ai-client", CircuitBreakerConfig.custom()
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(10)
            .build()
        )
    }
    
    @Bean
    fun aiRateLimiter(): RateLimiter {
        return RateLimiter.of("ai-client", RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ofSeconds(5))
            .build()
        )
    }
    
    @Bean
    fun aiRetry(): Retry {
        return Retry.of("ai-client", RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .exponentialBackoffMultiplier(2.0)
            .retryExceptions(IOException::class.java)
            .build()
        )
    }
}
```

### Configuration

```kotlin
@ConfigurationProperties(prefix = "qawave")
data class QaWaveProperties(
    val ai: AiProperties = AiProperties(),
    val execution: ExecutionProperties = ExecutionProperties(),
    val validation: ValidationProperties = ValidationProperties()
)

data class AiProperties(
    val provider: String = "openai",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val temperature: Double = 0.2,
    val maxAttempts: Int = 3,
    val concurrencyLimit: Int = 5
)

data class ExecutionProperties(
    val timeoutMaxMs: Long = 300_000,
    val timeoutDefaultMs: Long = 30_000
)

data class ValidationProperties(
    val maxOpenApiLength: Int = 100_000,
    val maxScenarioJsonSizeBytes: Int = 5 * 1024 * 1024
)
```

### Project Structure

```
backend/
├── src/main/kotlin/com/qawave/
│   ├── QaWaveApplication.kt
│   │
│   ├── domain/                    # Pure domain models
│   │   ├── model/
│   │   │   ├── TestScenario.kt
│   │   │   ├── TestStep.kt
│   │   │   ├── TestRun.kt
│   │   │   ├── QaPackage.kt
│   │   │   └── ExecutionContext.kt
│   │   ├── repository/            # Repository interfaces (suspend)
│   │   │   ├── ScenarioRepository.kt
│   │   │   ├── TestRunRepository.kt
│   │   │   └── QaPackageRepository.kt
│   │   └── event/
│   │       └── QaPackageEvent.kt
│   │
│   ├── application/               # Business logic (suspend functions)
│   │   ├── service/
│   │   │   ├── QaPackageRunService.kt
│   │   │   ├── TestExecutionService.kt
│   │   │   ├── QaEvaluationService.kt
│   │   │   └── ScenarioGenerationService.kt
│   │   ├── usecase/
│   │   │   ├── CreateQaPackageUseCase.kt
│   │   │   ├── RunTestScenarioUseCase.kt
│   │   │   └── EvaluateResultsUseCase.kt
│   │   └── port/
│   │       ├── AiClient.kt
│   │       ├── TestExecutor.kt
│   │       └── EventPublisher.kt
│   │
│   ├── infrastructure/            # External integrations
│   │   ├── ai/
│   │   │   ├── OpenAiClient.kt
│   │   │   ├── VeniceAiClient.kt
│   │   │   └── AiScenarioGeneratorAgent.kt
│   │   ├── persistence/
│   │   │   ├── entity/
│   │   │   │   ├── ScenarioEntity.kt
│   │   │   │   └── QaPackageEntity.kt
│   │   │   ├── repository/
│   │   │   │   ├── R2dbcScenarioRepository.kt
│   │   │   │   └── R2dbcQaPackageRepository.kt
│   │   │   └── migration/         # Flyway migrations
│   │   ├── http/
│   │   │   └── WebClientTestExecutor.kt
│   │   ├── cache/
│   │   │   └── RedisCacheService.kt
│   │   ├── messaging/
│   │   │   ├── KafkaEventPublisher.kt
│   │   │   └── KafkaEventConsumer.kt
│   │   └── config/
│   │       ├── R2dbcConfig.kt
│   │       ├── RedisConfig.kt
│   │       ├── KafkaConfig.kt
│   │       ├── WebClientConfig.kt
│   │       └── ResilienceConfig.kt
│   │
│   └── presentation/              # HTTP layer (suspend controllers)
│       ├── controller/
│       │   ├── QaPackageController.kt
│       │   ├── ScenarioController.kt
│       │   ├── TestRunController.kt
│       │   └── HealthController.kt
│       ├── dto/
│       │   ├── request/
│       │   │   └── CreateQaPackageRequest.kt
│       │   └── response/
│       │       └── QaPackageResponse.kt
│       ├── filter/
│       │   ├── CorsFilter.kt
│       │   └── RateLimitFilter.kt
│       └── exception/
│           └── GlobalExceptionHandler.kt
│
├── src/main/resources/
│   ├── application.yml            # Spring configuration
│   ├── application-local.yml      # Local overrides
│   ├── application-prod.yml       # Production config
│   └── db/migration/              # Flyway migrations
│       ├── V1__initial_schema.sql
│       └── V2__add_indexes.sql
│
└── src/test/kotlin/
    └── com/qawave/
        ├── integration/
        │   ├── QaPackageIntegrationTest.kt
        │   └── ScenarioRepositoryTest.kt
        ├── unit/
        │   ├── service/
        │   └── domain/
        └── TestContainersConfig.kt
```

### Dependencies (build.gradle.kts)

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Spring WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // R2DBC + PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
    implementation("org.postgresql:postgresql") // For Flyway
    runtimeOnly("io.r2dbc:r2dbc-pool")
    
    // Flyway (uses JDBC for migrations)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kafka:reactor-kafka:1.3.22")
    
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    
    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:r2dbc")
    testImplementation("io.mockk:mockk:1.13.9")
}
```

### Application Configuration (application.yml)

```yaml
spring:
  application:
    name: qawave
  
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:qawave}
    username: ${DB_USER:qawave}
    password: ${DB_PASSWORD:secret}
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
  
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:qawave}
    user: ${DB_USER:qawave}
    password: ${DB_PASSWORD:secret}
    locations: classpath:db/migration
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: qawave
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

qawave:
  ai:
    provider: ${AI_PROVIDER:openai}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:gpt-4o-mini}
    temperature: 0.2
    max-attempts: 3
    concurrency-limit: 5
  execution:
    timeout-max-ms: 300000
    timeout-default-ms: 30000
  validation:
    max-openapi-length: 100000
    max-scenario-json-size-bytes: 5242880

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}

server:
  port: 8080

logging:
  level:
    com.qawave: DEBUG
    org.springframework.r2dbc: DEBUG
```

---

## Frontend Stack

### Core Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 18.x | UI library - component-based architecture |
| **TypeScript** | 5.x | Type safety for JavaScript |
| **Vite** | 5.x | Build tool - fast HMR, ES modules |

### Routing & Data Fetching

| Technology | Version | Purpose |
|------------|---------|---------|
| **TanStack Router** | 1.x | Type-safe routing with search params |
| **TanStack Query** | 5.x | Server state management, caching, polling |

### Styling

| Technology | Version | Purpose |
|------------|---------|---------|
| **Tailwind CSS** | 3.x | Utility-first CSS framework |
| **HeroUI** | - | Component library |
| **Framer Motion** | 10.x | Animations and transitions |

### Project Structure

```
frontend/
├── src/
│   ├── api/                       # API client functions
│   │   ├── qaPackageRun.ts
│   │   ├── scenarios.ts
│   │   ├── aiLogs.ts
│   │   └── client.ts
│   │
│   ├── components/                # Reusable UI components
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── StatusBadge.tsx
│   │   │   └── ErrorBoundary.tsx
│   │   └── qa/
│   │       ├── NewRunModal.tsx
│   │       └── RunCard.tsx
│   │
│   ├── pages/                     # Page components
│   │   ├── QaPackageRunsPage.tsx
│   │   ├── QaPackageDetailPage.tsx
│   │   └── ScenariosPage.tsx
│   │
│   ├── hooks/                     # Custom React hooks
│   │   └── useQaPackages.ts
│   │
│   ├── utils/                     # Utility functions
│   │   ├── error.ts
│   │   └── format.ts
│   │
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx
│
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

---

## Infrastructure

### Kubernetes (K0s on Hetzner)

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Cluster** | K0s | Lightweight Kubernetes distribution |
| **Ingress** | Nginx Ingress | HTTP routing, TLS termination |
| **GitOps** | ArgoCD | Continuous deployment |
| **Secrets** | Sealed Secrets | Encrypted secrets in Git |
| **Monitoring** | Prometheus + Grafana | Metrics and dashboards |

### Data Services

| Service | Deployment | Purpose |
|---------|------------|---------|
| **PostgreSQL** | StatefulSet | Primary database |
| **Redis** | StatefulSet | Caching, session storage |
| **Kafka** | StatefulSet (Strimzi) | Event streaming |

### CI/CD

| Tool | Purpose |
|------|---------|
| **GitHub Actions** | Build, test, push images |
| **ArgoCD** | Deploy to Kubernetes |
| **Terraform** | Infrastructure provisioning |

---

## Development Environment

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **JDK** | 21+ | Kotlin/JVM runtime |
| **Node.js** | 20+ | Frontend tooling |
| **Docker** | 24+ | Local services |
| **Gradle** | 8.x | Backend build tool |
| **kubectl** | 1.28+ | Kubernetes CLI |
| **Terraform** | 1.6+ | Infrastructure |

### Local Setup

```bash
# Clone repository
git clone https://github.com/your-org/qawave.git
cd qawave

# Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# Backend
cd backend
./gradlew bootRun  # Starts on :8080

# Frontend (new terminal)
cd frontend
npm install
npm run dev  # Starts on :5173
```

### Docker Compose (Local Development)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: qawave
      POSTGRES_USER: qawave
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U qawave"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  kafka:
    image: bitnami/kafka:3.6
    environment:
      KAFKA_CFG_NODE_ID: 1
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    ports:
      - "9092:9092"
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 10s
      timeout: 10s
      retries: 5

volumes:
  pgdata:
```

---

## Testing Strategy

### Backend Testing

| Type | Tool | Purpose |
|------|------|---------|
| Unit Tests | JUnit 5 + MockK | Domain logic, services |
| Integration Tests | Testcontainers | API endpoints, DB, Kafka |
| Contract Tests | Spring Cloud Contract | API contracts |

```bash
# Run all tests
./gradlew test

# Run integration tests only
./gradlew test --tests '*IntegrationTest'

# Run with coverage
./gradlew test jacocoTestReport
```

### Frontend Testing

| Type | Tool | Purpose |
|------|------|---------|
| Unit Tests | Vitest | Component logic, utilities |
| Component Tests | React Testing Library | Component behavior |
| E2E Tests | Playwright | Full user flows |

```bash
# Unit tests
npm run test

# E2E tests
npm run test:e2e
```

---

## Performance Characteristics

### Target Metrics

| Component | Metric | Target |
|-----------|--------|--------|
| API Latency (P95) | Response time | < 100ms |
| Scenario Generation | Throughput | ~10/minute |
| Test Execution | Throughput | ~100 scenarios/minute |
| Database Queries | Latency | < 10ms |

### Scaling Strategy

- **Horizontal Pod Autoscaling**: Scale backend pods based on CPU/memory
- **Database**: Read replicas for query scaling
- **Redis**: Cluster mode for high availability
- **Kafka**: Partition scaling for throughput

---

*Last Updated: January 2026*
*Document Version: 2.0 - Spring WebFlux Edition*
