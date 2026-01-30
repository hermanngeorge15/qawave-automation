# ADR-009: Scalability and Performance Architecture

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs to scale to handle:
- Multiple concurrent QA package runs
- High volume of AI API calls
- Large OpenAPI specifications
- Thousands of test scenarios
- Real-time event streaming

## Decision

We implement a **horizontally scalable architecture** with the following strategies:

### Scalability Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       SCALABILITY ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │    Ingress      │    │    Backend      │    │   PostgreSQL    │         │
│  │    (Nginx)      │───►│   (3 replicas)  │───►│   (Primary +    │         │
│  │                 │    │   HPA: 3-10     │    │    Read Replica)│         │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘         │
│                                │                                            │
│                                │                                            │
│                    ┌───────────┴───────────┐                               │
│                    ▼                       ▼                               │
│           ┌─────────────────┐    ┌─────────────────┐                       │
│           │     Redis       │    │     Kafka       │                       │
│           │   (Cluster)     │    │   (3 brokers)   │                       │
│           │   Cache/Rate    │    │   Partitioned   │                       │
│           └─────────────────┘    └─────────────────┘                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: qawave-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: qawave-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### Performance Targets

| Metric | Target | Measured By |
|--------|--------|-------------|
| API Latency P50 | < 50ms | Prometheus |
| API Latency P95 | < 200ms | Prometheus |
| API Latency P99 | < 500ms | Prometheus |
| Throughput | 1000 req/s | k6 load tests |
| AI Generation | 10 scenarios/min | Custom metric |
| Error Rate | < 0.1% | Prometheus |

### Caching Strategy

```kotlin
@Service
class CachedSpecParser(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val specParser: OpenApiParser
) {
    suspend fun parse(specUrl: String): ParsedSpec {
        val cacheKey = "spec:${specUrl.hashCode()}"

        // Try cache first
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .awaitSingleOrNull()
            ?.let { Json.decodeFromString(it) }
            ?: fetchAndCache(specUrl, cacheKey)
    }

    private suspend fun fetchAndCache(url: String, key: String): ParsedSpec {
        val parsed = specParser.parse(url)
        redisTemplate.opsForValue()
            .set(key, Json.encodeToString(parsed), Duration.ofHours(1))
            .awaitSingle()
        return parsed
    }
}
```

### Connection Pooling

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      max-create-connection-time: 5s
      validation-query: SELECT 1

  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
```

### Async Processing with Backpressure

```kotlin
@Service
class QaPackageRunService {
    private val aiSemaphore = Semaphore(5)   // Limit AI concurrency
    private val execSemaphore = Semaphore(10) // Limit HTTP concurrency

    suspend fun runPackage(request: RunRequest) = coroutineScope {
        operations.asFlow()
            .buffer(Channel.BUFFERED)  // Backpressure
            .map { op ->
                aiSemaphore.withPermit {
                    generateScenario(op)
                }
            }
            .flatMapMerge(concurrency = 10) { scenario ->
                flow {
                    execSemaphore.withPermit {
                        emit(executeScenario(scenario))
                    }
                }
            }
            .toList()
    }
}
```

## Consequences

### Positive
- Handles traffic spikes via auto-scaling
- AI rate limits respected via semaphores
- Fast responses via caching
- Resource-efficient connection pooling

### Negative
- Complexity in distributed state
- Cache invalidation challenges
- Cost scales with load

## References

- [ADR-004: Streaming Pipeline](ADR-004-streaming-pipeline.md)
- [Kubernetes HPA](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
