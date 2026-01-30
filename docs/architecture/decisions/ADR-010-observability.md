# ADR-010: Observability Architecture

## Status
Accepted

## Date
2026-01-30

## Context

QAWave needs comprehensive observability for:
- Debugging issues in production
- Performance monitoring
- Capacity planning
- Audit and compliance

## Decision

We implement the **three pillars of observability**: Metrics, Logs, and Traces.

### Observability Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       OBSERVABILITY ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                          COLLECTION                                  │    │
│  │                                                                      │    │
│  │   ┌───────────┐      ┌───────────┐      ┌───────────┐              │    │
│  │   │  Metrics  │      │   Logs    │      │  Traces   │              │    │
│  │   │ Micrometer│      │  Logback  │      │  OpenTel  │              │    │
│  │   └─────┬─────┘      └─────┬─────┘      └─────┬─────┘              │    │
│  │         │                  │                  │                     │    │
│  └─────────┼──────────────────┼──────────────────┼─────────────────────┘    │
│            │                  │                  │                          │
│            ▼                  ▼                  ▼                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                          STORAGE                                    │    │
│  │                                                                      │    │
│  │   ┌───────────┐      ┌───────────┐      ┌───────────┐              │    │
│  │   │Prometheus │      │   Loki    │      │   Tempo   │              │    │
│  │   │ (15 days) │      │ (30 days) │      │ (7 days)  │              │    │
│  │   └─────┬─────┘      └─────┬─────┘      └─────┬─────┘              │    │
│  │         │                  │                  │                     │    │
│  └─────────┼──────────────────┼──────────────────┼─────────────────────┘    │
│            │                  │                  │                          │
│            └──────────────────┼──────────────────┘                          │
│                               ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        VISUALIZATION                                │    │
│  │                                                                      │    │
│  │   ┌─────────────────────────────────────────────────────────┐      │    │
│  │   │                      Grafana                              │      │    │
│  │   │  • Metrics dashboards                                     │      │    │
│  │   │  • Log exploration                                        │      │    │
│  │   │  • Trace analysis                                         │      │    │
│  │   │  • Alerting                                               │      │    │
│  │   └─────────────────────────────────────────────────────────┘      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Metrics (Prometheus + Micrometer)

**Custom Application Metrics:**

```kotlin
@Component
class QaPackageMetrics(private val meterRegistry: MeterRegistry) {

    private val runDuration = Timer.builder("qawave.package.run.duration")
        .description("QA package run duration")
        .tags("status", "completed")
        .register(meterRegistry)

    private val scenarioCount = Counter.builder("qawave.scenarios.generated")
        .description("Number of scenarios generated")
        .register(meterRegistry)

    private val aiLatency = Timer.builder("qawave.ai.generation.latency")
        .description("AI scenario generation latency")
        .register(meterRegistry)

    fun recordRun(duration: Duration, status: String) {
        runDuration.record(duration)
    }

    fun incrementScenarios(count: Int) {
        scenarioCount.increment(count.toDouble())
    }

    fun recordAiLatency(duration: Duration) {
        aiLatency.record(duration)
    }
}
```

**Key Metrics:**

| Metric | Type | Description |
|--------|------|-------------|
| `qawave.package.run.duration` | Timer | Package execution time |
| `qawave.scenarios.generated` | Counter | Scenarios created |
| `qawave.ai.generation.latency` | Timer | AI API response time |
| `qawave.http.execution.latency` | Timer | Test HTTP call latency |
| `qawave.scenarios.pass_rate` | Gauge | % of passing scenarios |

### Logging (Logback + JSON)

**Structured Logging Configuration:**

```xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>planId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>correlationId</includeMdcKeyName>
        </encoder>
    </appender>
</configuration>
```

**Log Levels:**

| Package | Level | Rationale |
|---------|-------|-----------|
| `com.qawave` | DEBUG | Full application logging |
| `com.qawave.infrastructure.ai` | INFO | AI interactions (redact prompts) |
| `org.springframework.security` | INFO | Auth events |
| `io.r2dbc` | WARN | DB only on issues |

### Distributed Tracing (OpenTelemetry)

```kotlin
@Configuration
class TracingConfig {
    @Bean
    fun tracer(): Tracer {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                            .setEndpoint("http://tempo:4317")
                            .build()
                    ).build())
                    .build()
            )
            .build()
            .getTracer("qawave")
    }
}
```

### Alerting Rules

```yaml
groups:
  - name: qawave-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: High error rate detected

      - alert: SlowAiGeneration
        expr: histogram_quantile(0.95, qawave_ai_generation_latency_seconds_bucket) > 30
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: AI generation P95 > 30s

      - alert: LowPassRate
        expr: qawave_scenarios_pass_rate < 0.5
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: Scenario pass rate below 50%
```

## Consequences

### Positive
- Full visibility into system behavior
- Fast incident detection and response
- Data-driven capacity planning
- Compliance audit trail

### Negative
- Storage costs for metrics/logs/traces
- Performance overhead from instrumentation
- Complexity in correlation

## References

- [Prometheus Metrics](https://prometheus.io/docs/concepts/metric_types/)
- [OpenTelemetry](https://opentelemetry.io/docs/)
- [Grafana Loki](https://grafana.com/docs/loki/latest/)
