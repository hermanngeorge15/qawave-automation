# ADR-002: Clean Architecture with Domain-Centric Layers

## Status
Accepted

## Date
2026-01-30

## Context

QAWave has multiple integration points:
- AI providers (OpenAI, Venice) that may change
- Database technology (PostgreSQL via R2DBC)
- Message broker (Kafka)
- External HTTP APIs (systems under test)

We needed an architecture that:
- Isolates business logic from infrastructure concerns
- Makes it easy to swap implementations (e.g., different AI providers)
- Enables testing business logic without infrastructure
- Provides clear dependency direction

## Decision

We adopted **Clean Architecture** with four layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                            │
│  Controllers → HTTP DTOs → Validation                           │
│  Only: deserialize, validate, call service, serialize           │
└─────────────────────────────────────────────────────────────────┘
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                             │
│  Services, Use Cases, Application DTOs                          │
│  Orchestration, business logic                                  │
│  Depends only on: domain interfaces (ports)                     │
└─────────────────────────────────────────────────────────────────┘
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                                │
│  Entities, Value Objects, Repository Interfaces                 │
│  Pure Kotlin, NO framework dependencies                         │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ implements
┌─────────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                           │
│  Repository Implementations, AI Clients, Kafka, HTTP Clients    │
│  Framework-specific code (Spring, R2DBC, etc.)                  │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.qawave/
├── domain/                    # Pure domain models and ports
│   ├── model/
│   │   ├── TestScenario.kt
│   │   ├── TestStep.kt
│   │   └── QaPackage.kt
│   ├── repository/            # Repository interfaces (ports)
│   │   └── QaPackageRepository.kt
│   └── event/
│       └── QaPackageEvent.kt
│
├── application/               # Business logic (use cases)
│   ├── service/
│   │   ├── QaPackageRunService.kt
│   │   └── TestExecutionService.kt
│   ├── usecase/
│   │   └── RunQaPackageUseCase.kt
│   └── port/                  # Application-level ports
│       ├── AiClient.kt
│       └── EventPublisher.kt
│
├── infrastructure/            # External integrations (adapters)
│   ├── ai/
│   │   ├── OpenAiClient.kt
│   │   └── VeniceAiClient.kt
│   ├── persistence/
│   │   └── R2dbcQaPackageRepository.kt
│   └── messaging/
│       └── KafkaEventPublisher.kt
│
└── presentation/              # HTTP layer
    ├── controller/
    │   └── QaPackageController.kt
    └── dto/
        ├── request/
        └── response/
```

### Dependency Rules

1. **Domain has no dependencies** - Pure Kotlin, no Spring annotations
2. **Application depends only on domain** - Uses domain models and ports
3. **Infrastructure implements domain ports** - Adapters for external systems
4. **Presentation depends on application** - Thin controllers calling services

### Port/Adapter Pattern

```kotlin
// Port (in domain/application layer)
interface AiClient {
    suspend fun generateScenarios(request: ScenarioRequest): List<TestScenario>
}

// Adapter (in infrastructure layer)
@Service
class OpenAiClient(
    private val webClient: WebClient,
    private val config: AiConfig
) : AiClient {
    override suspend fun generateScenarios(request: ScenarioRequest): List<TestScenario> {
        // Implementation details
    }
}
```

## Consequences

### Positive
- Business logic isolated from infrastructure changes
- Easy to test domain/application layers with mocks
- Clear boundaries make code navigation intuitive
- Can swap AI providers without touching business logic

### Negative
- More boilerplate (interfaces, mappers)
- Mapping between DTOs at layer boundaries
- May feel over-engineered for simple features

### Trade-offs
- We accept some mapping overhead for better testability and flexibility
- Domain models may need to be mapped to/from persistence entities

## Guidelines for Developers

### DO
- Put business rules in the application layer
- Define interfaces (ports) in domain/application for external dependencies
- Keep domain models free of framework annotations
- Use mappers at layer boundaries

### DON'T
- Put Spring annotations in domain layer
- Call infrastructure directly from controllers
- Let domain models know about persistence (no `@Entity` etc.)
- Return infrastructure-specific types from services

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
