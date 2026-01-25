# Kafka Deployment for QAWave

Apache Kafka deployment using Strimzi operator for event streaming.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Strimzi Operator                              │
│                     (Manages Kafka lifecycle)                        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │  Kafka Broker   │  │    Zookeeper    │  │ Entity Operator │     │
│  │  (qawave-kafka) │  │                 │  │ (Topic/User)    │     │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘     │
│           │                                                          │
│           ▼                                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                        Topics                                │   │
│  │  • qa-package-events (3 partitions)                         │   │
│  │  • test-run-events (6 partitions)                           │   │
│  │  • scenario-events (3 partitions)                           │   │
│  │  • dlq (1 partition)                                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Install Strimzi Operator

```bash
# Create namespace
kubectl create namespace kafka

# Install Strimzi operator (watches all namespaces)
kubectl apply -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

# Wait for operator to be ready
kubectl wait --for=condition=available --timeout=300s \
    deployment/strimzi-cluster-operator -n kafka
```

### 2. Create Kafka Cluster

```bash
# Ensure qawave namespace exists
kubectl create namespace qawave --dry-run=client -o yaml | kubectl apply -f -

# Deploy Kafka cluster
kubectl apply -f kafka-cluster.yaml

# Wait for Kafka to be ready (takes a few minutes)
kubectl wait kafka/qawave-kafka --for=condition=Ready --timeout=600s -n qawave
```

### 3. Create Topics

```bash
kubectl apply -f topics.yaml
```

### 4. Apply Connection Secret

```bash
kubectl apply -f secrets.yaml
```

## Verify Installation

```bash
# Check Kafka cluster status
kubectl get kafka -n qawave

# Check Kafka pods
kubectl get pods -n qawave -l strimzi.io/cluster=qawave-kafka

# Check topics
kubectl get kafkatopic -n qawave

# List topics using Kafka CLI
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

## Configuration

### Current Setup (Development)

| Component | Replicas | Storage |
|-----------|----------|---------|
| Kafka Broker | 1 | 10Gi |
| Zookeeper | 1 | 5Gi |

### Production Setup

For production, update `kafka-cluster.yaml`:

```yaml
spec:
  kafka:
    replicas: 3
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      size: 50Gi

  zookeeper:
    replicas: 3
    storage:
      size: 10Gi
```

Also update topic replicas in `topics.yaml`:
```yaml
spec:
  replicas: 3
```

## Topics

| Topic | Partitions | Retention | Use Case |
|-------|------------|-----------|----------|
| qa-package-events | 3 | 7 days | QA package lifecycle |
| test-run-events | 6 | 3 days | Test execution events |
| scenario-events | 3 | 7 days | Scenario generation |
| dlq | 1 | 30 days | Failed message storage |

## Connection

### Bootstrap Servers

```
qawave-kafka-kafka-bootstrap.qawave.svc.cluster.local:9092
```

### Spring Kafka Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP_ID}
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3
```

### Kotlin Producer Example

```kotlin
@Service
class EventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {

    suspend fun publishQaPackageEvent(event: QaPackageEvent) {
        kafkaTemplate.send("qa-package-events", event.id, event.toJson())
            .await()
    }
}
```

### Kotlin Consumer Example

```kotlin
@KafkaListener(topics = ["qa-package-events"], groupId = "qawave")
suspend fun handleQaPackageEvent(event: ConsumerRecord<String, String>) {
    val qaPackageEvent = event.value().toQaPackageEvent()
    // Process event
}
```

## Monitoring

### Prometheus Metrics

Kafka JMX metrics are exposed via the Prometheus exporter.

Key metrics:
- `kafka_server_broker_topic_metrics_*` - Topic throughput
- `kafka_server_replica_manager_*` - Partition health
- `kafka_controller_*` - Controller metrics
- `kafka_network_*` - Network metrics

### Grafana Dashboard

Import dashboard ID: **7589** (Strimzi Kafka)

### Check Consumer Lag

```bash
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group qawave \
    --describe
```

## Troubleshooting

### Kafka not starting

```bash
# Check Kafka pod logs
kubectl logs qawave-kafka-kafka-0 -n qawave

# Check Zookeeper logs
kubectl logs qawave-kafka-zookeeper-0 -n qawave

# Check operator logs
kubectl logs deployment/strimzi-cluster-operator -n kafka
```

### Topic not created

```bash
# Check topic status
kubectl describe kafkatopic qa-package-events -n qawave

# Check entity operator logs
kubectl logs deployment/qawave-kafka-entity-operator -n qawave -c topic-operator
```

### Consumer lag issues

```bash
# Check consumer group status
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --all-groups --describe

# Reset consumer offset (use with caution!)
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group qawave \
    --topic qa-package-events \
    --reset-offsets --to-earliest --execute
```

### Test producing/consuming

```bash
# Produce test message
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic qa-package-events

# Consume messages
kubectl exec -it qawave-kafka-kafka-0 -n qawave -- \
    bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic qa-package-events \
    --from-beginning
```

## Related Documentation

- [Strimzi Documentation](https://strimzi.io/documentation/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
