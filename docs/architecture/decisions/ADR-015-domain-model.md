# ADR-015: Domain Model and Bounded Contexts

## Status
Accepted

## Date
2026-01-30

## Context

QAWave requires a well-defined domain model following Domain-Driven Design (DDD) principles to:
- Maintain clear boundaries between subsystems
- Enable independent evolution of contexts
- Establish a ubiquitous language for the team
- Support complex business logic in the domain layer

## Decision

We implement a **domain model with four bounded contexts** following DDD tactical and strategic patterns.

### Context Map

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CONTEXT MAP                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌─────────────────────┐         ┌─────────────────────┐                  │
│    │  Identity Context   │         │  Test Management    │                  │
│    │  (Upstream)         │────────►│  Context (Core)     │                  │
│    │                     │ ACL     │                     │                  │
│    │  • User             │         │  • QaPackage        │                  │
│    │  • Role             │         │  • TestScenario     │                  │
│    │  • Permission       │         │  • TestRun          │                  │
│    └─────────────────────┘         └──────────┬──────────┘                  │
│                                               │                              │
│                                    Published  │ Domain                       │
│                                    Language   │ Events                       │
│                                               │                              │
│    ┌─────────────────────┐         ┌─────────▼──────────┐                  │
│    │  AI Generation      │◄────────│  Execution         │                  │
│    │  Context            │ Events  │  Context           │                  │
│    │                     │         │                     │                  │
│    │  • ScenarioGenerator│         │  • TestExecutor    │                  │
│    │  • ResultEvaluator  │         │  • StepResult      │                  │
│    │  • ResultReviewer   │         │  • ExecutionContext│                  │
│    └─────────────────────┘         └─────────────────────┘                  │
│                                                                              │
│    Relationship Types:                                                       │
│    ────────► Conformist (downstream conforms to upstream)                   │
│    ◄──────── Anti-Corruption Layer (translates upstream concepts)           │
│    - - - - ► Published Language (shared events/contracts)                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Bounded Contexts

#### 1. Test Management Context (Core Domain)

The primary business domain handling QA packages and scenarios.

```kotlin
// Aggregate Root: QaPackage
@AggregateRoot
data class QaPackage private constructor(
    val id: QaPackageId,
    val name: PackageName,
    val description: Description?,
    val specSource: SpecSource,
    val baseUrl: BaseUrl,
    val requirements: Requirements?,
    val config: QaPackageConfig,
    val status: QaPackageStatus,
    val createdBy: UserId,
    val createdAt: Instant,
    val updatedAt: Instant,
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {
    companion object {
        fun create(command: CreateQaPackageCommand): QaPackage {
            val package = QaPackage(
                id = QaPackageId.generate(),
                name = PackageName(command.name),
                description = command.description?.let { Description(it) },
                specSource = SpecSource.from(command.specUrl, command.specContent),
                baseUrl = BaseUrl(command.baseUrl),
                requirements = command.requirements?.let { Requirements(it) },
                config = command.config,
                status = QaPackageStatus.DRAFT,
                createdBy = command.triggeredBy,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            package.registerEvent(QaPackageCreated(package.id, package.name))
            return package
        }
    }

    fun start(): QaPackage {
        require(status == QaPackageStatus.READY) {
            "Package must be READY to start, current: $status"
        }
        return copy(
            status = QaPackageStatus.RUNNING,
            updatedAt = Instant.now()
        ).also {
            it.registerEvent(QaPackageStarted(id))
        }
    }

    fun complete(result: PackageResult): QaPackage {
        require(status == QaPackageStatus.RUNNING) {
            "Package must be RUNNING to complete"
        }
        val newStatus = if (result.allPassed) QaPackageStatus.COMPLETED else QaPackageStatus.FAILED
        return copy(
            status = newStatus,
            updatedAt = Instant.now()
        ).also {
            it.registerEvent(QaPackageCompleted(id, newStatus, result))
        }
    }

    private fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun pullDomainEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
}

// Value Objects
@JvmInline
value class QaPackageId(val value: UUID) {
    companion object {
        fun generate() = QaPackageId(UUID.randomUUID())
        fun from(value: String) = QaPackageId(UUID.fromString(value))
    }
}

@JvmInline
value class PackageName(val value: String) {
    init {
        require(value.isNotBlank()) { "Package name cannot be blank" }
        require(value.length <= 255) { "Package name too long" }
    }
}

sealed class SpecSource {
    data class Url(val url: String) : SpecSource()
    data class Content(val content: String) : SpecSource()

    companion object {
        fun from(url: String?, content: String?): SpecSource {
            return when {
                url != null -> Url(url)
                content != null -> Content(content)
                else -> throw IllegalArgumentException("Either URL or content required")
            }
        }
    }
}
```

#### 2. AI Generation Context (Supporting Domain)

Handles AI-powered scenario generation and evaluation.

```kotlin
// Aggregate Root: GenerationSession
@AggregateRoot
data class GenerationSession(
    val id: GenerationSessionId,
    val packageId: QaPackageId,
    val operations: List<ApiOperation>,
    val generatedScenarios: List<GeneratedScenario>,
    val status: GenerationStatus,
    val startedAt: Instant,
    val completedAt: Instant?
) {
    fun addScenario(scenario: GeneratedScenario): GenerationSession {
        require(status == GenerationStatus.IN_PROGRESS)
        return copy(
            generatedScenarios = generatedScenarios + scenario
        )
    }

    fun complete(): GenerationSession {
        return copy(
            status = GenerationStatus.COMPLETED,
            completedAt = Instant.now()
        )
    }
}

// Domain Service: ScenarioGeneratorAgent
interface ScenarioGeneratorAgent {
    suspend fun generateScenarios(
        operations: List<ApiOperation>,
        requirements: Requirements?
    ): Flow<GeneratedScenario>
}

// Domain Service: ResultEvaluatorAgent
interface ResultEvaluatorAgent {
    suspend fun evaluate(
        scenario: TestScenario,
        results: List<StepResult>
    ): EvaluationResult
}
```

#### 3. Execution Context (Supporting Domain)

Manages test execution and result collection.

```kotlin
// Aggregate Root: TestExecution
@AggregateRoot
data class TestExecution(
    val id: TestExecutionId,
    val runId: TestRunId,
    val scenario: TestScenario,
    val context: ExecutionContext,
    val stepResults: List<StepResult>,
    val status: ExecutionStatus,
    val startedAt: Instant,
    val completedAt: Instant?
) {
    fun executeStep(stepIndex: Int, result: StepResult): TestExecution {
        require(status == ExecutionStatus.RUNNING)
        require(stepIndex == stepResults.size) { "Steps must execute in order" }

        val newResults = stepResults + result
        val newContext = context.applyExtractions(result.extractions)

        return copy(
            stepResults = newResults,
            context = newContext
        )
    }

    fun complete(): TestExecution {
        val finalStatus = if (stepResults.all { it.passed }) {
            ExecutionStatus.PASSED
        } else {
            ExecutionStatus.FAILED
        }
        return copy(
            status = finalStatus,
            completedAt = Instant.now()
        )
    }
}

// Value Object: ExecutionContext
data class ExecutionContext(
    val baseUrl: String,
    val variables: Map<String, Any>,
    val headers: Map<String, String>,
    val authToken: String?
) {
    fun applyExtractions(extractions: Map<String, Any>): ExecutionContext {
        return copy(variables = variables + extractions)
    }

    fun resolveVariable(name: String): Any? = variables[name]
}
```

#### 4. Identity Context (Generic Domain)

Handles user identity and authorization (integrated with Keycloak).

```kotlin
// Aggregate Root: User (read model from Keycloak)
data class User(
    val id: UserId,
    val keycloakId: KeycloakId,
    val email: Email,
    val displayName: String?,
    val roles: Set<Role>
) {
    fun hasPermission(permission: Permission): Boolean {
        return roles.any { it.permissions.contains(permission) }
    }

    fun canAccessPackage(package: QaPackage): Boolean {
        return hasPermission(Permission.READ_ALL_PACKAGES) ||
               package.createdBy == id
    }
}

enum class Role(val permissions: Set<Permission>) {
    ADMIN(Permission.values().toSet()),
    TESTER(setOf(
        Permission.CREATE_PACKAGE,
        Permission.READ_ALL_PACKAGES,
        Permission.RUN_PACKAGE,
        Permission.VIEW_RESULTS
    )),
    VIEWER(setOf(
        Permission.READ_ALL_PACKAGES,
        Permission.VIEW_RESULTS
    ))
}
```

### Domain Events Catalog

| Event | Context | Triggers |
|-------|---------|----------|
| `QaPackageCreated` | Test Management | Notification, Audit |
| `QaPackageStarted` | Test Management | Start execution |
| `QaPackageCompleted` | Test Management | Notification, Metrics |
| `ScenarioGenerated` | AI Generation | Persist scenario |
| `GenerationCompleted` | AI Generation | Mark package ready |
| `TestExecutionStarted` | Execution | Update run status |
| `StepExecuted` | Execution | Real-time updates |
| `TestExecutionCompleted` | Execution | Aggregate results |

### Ubiquitous Language Glossary

| Term | Definition |
|------|------------|
| **QA Package** | A collection of test scenarios targeting a specific API |
| **Test Scenario** | A sequence of HTTP requests with assertions |
| **Test Step** | A single HTTP request with expected assertions |
| **Test Run** | An execution instance of a package or scenario |
| **Assertion** | A validation rule for response data |
| **Extraction** | A rule for capturing response data into variables |
| **Generation Session** | An AI-powered scenario creation process |

### Anti-Corruption Layer

```kotlin
// ACL for Keycloak integration
@Component
class KeycloakUserAdapter(
    private val keycloakClient: KeycloakClient
) : UserRepository {

    override suspend fun findByKeycloakId(id: KeycloakId): User? {
        val keycloakUser = keycloakClient.getUser(id.value) ?: return null

        // Translate Keycloak model to domain model
        return User(
            id = UserId.generate(),
            keycloakId = id,
            email = Email(keycloakUser.email),
            displayName = keycloakUser.firstName,
            roles = translateRoles(keycloakUser.realmRoles)
        )
    }

    private fun translateRoles(keycloakRoles: List<String>): Set<Role> {
        return keycloakRoles.mapNotNull { roleName ->
            when (roleName) {
                "qawave-admin" -> Role.ADMIN
                "qawave-tester" -> Role.TESTER
                "qawave-viewer" -> Role.VIEWER
                else -> null
            }
        }.toSet()
    }
}
```

## Consequences

### Positive
- Clear domain boundaries prevent coupling
- Rich domain model encapsulates business logic
- Events enable loose coupling between contexts
- Ubiquitous language improves team communication

### Negative
- Learning curve for DDD patterns
- More classes and abstractions
- Event handling complexity
- Translation overhead at boundaries

## References

- [Domain-Driven Design](https://domainlanguage.com/ddd/)
- [Implementing DDD](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [Context Mapping](https://www.infoq.com/articles/ddd-contextmapping/)
