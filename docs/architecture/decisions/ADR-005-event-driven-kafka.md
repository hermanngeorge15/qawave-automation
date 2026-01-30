# ADR-005: Event-Driven Architecture with Kafka

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs to:
- Provide real-time updates to the frontend during QA package execution
- Decouple long-running operations from HTTP request/response cycles
- Support audit logging of all significant events
- Enable future integrations (webhooks, notifications, analytics)

We considered:
1. **WebSockets** - Direct push to clients
2. **Server-Sent Events (SSE)** - Simpler than WebSockets
3. **Polling** - Client periodically checks for updates
4. **Event Broker (Kafka)** - Central event bus with persistence

## Decision

We adopted **Apache Kafka** as the event backbone with:
- **Kafka** for event persistence and distribution
- **Polling** from frontend to backend API (events persisted in DB)
- **In-memory fallback** for local development without Kafka

### Event Flow Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           EVENT FLOW                                        │
└────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
  │   Service   │────▶│   Kafka     │────▶│   Consumer  │────▶│  Database   │
  │  (Producer) │     │   Topic     │     │   Service   │     │  (Events)   │
  └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                      │
                                                                      ▼
                                                              ┌─────────────┐
                                                              │  Frontend   │
                                                              │  (Polling)  │
                                                              └─────────────┘
```

### Event Types

```kotlin
sealed class QaPackageEvent {
    abstract val planId: String
    abstract val timestamp: Instant
    abstract val type: String
}

data class RunRequestedEvent(
    override val planId: String,
    val request: RunPackageRequest,
    override val timestamp: Instant = Instant.now()
) : QaPackageEvent() {
    override val type = "REQUESTED"
}

data class ScenarioCreatedEvent(
    override val planId: String,
    val scenarioId: String,
    val scenarioName: String,
    override val timestamp: Instant = Instant.now()
) : QaPackageEvent() {
    override val type = "SCENARIO_CREATED"
}

data class ExecutionCompleteEvent(
    override val planId: String,
    val scenarioId: String,
    val passed: Boolean,
    val durationMs: Long,
    override val timestamp: Instant = Instant.now()
) : QaPackageEvent() {
    override val type = if (passed) "EXECUTION_SUCCESS" else "EXECUTION_FAILED"
}
```

### Kafka Topics

| Topic | Purpose | Retention |
|-------|---------|-----------|
| `qa-package-events` | All QA package lifecycle events | 7 days |
| `ai-interaction-logs` | AI request/response logs | 30 days |
| `audit-events` | User actions for compliance | 365 days |

### Producer Implementation

```kotlin
@Service
class KafkaEventPublisher(
    private val kafkaSender: KafkaSender<String, String>,
    private val objectMapper: ObjectMapper
) : EventPublisher {

    override suspend fun publish(event: QaPackageEvent) {
        val record = SenderRecord.create(
            ProducerRecord(
                "qa-package-events",
                event.planId,
                objectMapper.writeValueAsString(event)
            ),
            event.planId
        )

        kafkaSender.send(Mono.just(record))
            .awaitFirst()
    }
}
```

### Consumer Implementation

```kotlin
@Component
class QaPackageEventConsumer(
    private val eventRepository: EventRepository
) {

    @KafkaListener(topics = ["qa-package-events"])
    suspend fun consume(record: ConsumerRecord<String, String>) {
        val event = objectMapper.readValue<QaPackageEvent>(record.value())

        // Persist event for API queries
        eventRepository.save(EventEntity.from(event))

        // Additional processing (notifications, analytics, etc.)
        when (event) {
            is RunCompleteEvent -> notifyIfConfigured(event)
            is ExecutionFailedEvent -> trackFailure(event)
            else -> { /* no-op */ }
        }
    }
}
```

### In-Memory Fallback

For local development without Kafka:

```kotlin
@Service
@Profile("local", "test")
class InMemoryEventPublisher : EventPublisher {
    private val events = ConcurrentHashMap<String, MutableList<QaPackageEvent>>()

    override suspend fun publish(event: QaPackageEvent) {
        events.getOrPut(event.planId) { mutableListOf() }.add(event)
    }

    fun getEvents(planId: String): List<QaPackageEvent> =
        events[planId] ?: emptyList()
}
```

### Frontend Polling

```typescript
// TanStack Query with conditional polling
const { data: events } = useQuery({
  queryKey: ['qa-package-events', planId],
  queryFn: () => fetchEvents(planId),
  refetchInterval: (data) => {
    // Stop polling when complete
    const isComplete = data?.some(e => e.type === 'COMPLETE' || e.type === 'FAILED')
    return isComplete ? false : 2000 // Poll every 2 seconds
  }
})
```

## Consequences

### Positive
- **Durability**: Events persisted, can replay history
- **Decoupling**: Services don't need to know about consumers
- **Scalability**: Kafka handles high throughput
- **Audit Trail**: Every event recorded for compliance
- **Future-proof**: Easy to add new consumers (webhooks, analytics)

### Negative
- Infrastructure complexity (Kafka cluster needed)
- Eventually consistent (events processed asynchronously)
- Frontend polling vs real-time push
- Local development needs fallback

### Trade-offs
- We accept eventual consistency for durability and decoupling
- Polling is simpler than WebSockets for MVP; can add SSE later

## Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all                    # Durability guarantee
      retries: 3
    consumer:
      group-id: qawave
      auto-offset-reset: earliest  # Process all events
      enable-auto-commit: false    # Manual commit after processing
```

## Future Enhancements

1. **SSE Endpoint**: Add Server-Sent Events for real-time updates
2. **Webhooks**: Consume events and POST to configured URLs
3. **Slack/Teams**: Notify on failures
4. **Analytics**: Stream to ClickHouse/BigQuery for dashboards

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [ADR-004: Streaming Pipeline](ADR-004-streaming-pipeline.md)
