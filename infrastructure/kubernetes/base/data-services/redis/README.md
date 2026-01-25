# Redis Deployment for QAWave

Redis 7 cache deployment using Bitnami Helm chart.

## Quick Start

### 1. Add Helm Repository

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2. Install Redis

```bash
# With auto-generated password
helm install redis bitnami/redis \
  --namespace qawave \
  --values values.yaml

# Or with specific password
helm install redis bitnami/redis \
  --namespace qawave \
  --values values.yaml \
  --set auth.password="your-secure-password"
```

### 3. Get Password

```bash
export REDIS_PASSWORD=$(kubectl get secret --namespace qawave redis \
  -o jsonpath="{.data.redis-password}" | base64 -d)
echo $REDIS_PASSWORD
```

### 4. Apply Connection Secret

```bash
kubectl apply -f secrets.yaml
```

## Verify Installation

```bash
# Check pod status
kubectl get pods -n qawave -l app.kubernetes.io/name=redis

# Check service
kubectl get svc -n qawave redis-master

# Test connection
kubectl run redis-client --rm --tty -i --restart='Never' \
  --namespace qawave \
  --image docker.io/bitnami/redis:7.2 \
  --command -- redis-cli -h redis-master -a $REDIS_PASSWORD ping
```

## Configuration

### Architecture Options

| Mode | Description | Use Case |
|------|-------------|----------|
| standalone | Single Redis instance | Development, low-traffic |
| replication | Master + replicas | Production, read scaling |
| sentinel | HA with automatic failover | High availability required |

Current configuration: **standalone** (modify `architecture` in values.yaml)

### Resource Limits

| Setting | Value |
|---------|-------|
| CPU Request | 100m |
| CPU Limit | 500m |
| Memory Request | 256Mi |
| Memory Limit | 512Mi |
| Max Memory (Redis) | 400MB |

### Memory Policy

Using `allkeys-lru` eviction policy:
- Evicts least recently used keys when memory limit reached
- Suitable for cache use cases
- Does not guarantee key persistence

## Connection

### From Application

```yaml
# Spring Boot configuration
spring:
  data:
    redis:
      host: redis-master.qawave.svc.cluster.local
      port: 6379
      password: ${REDIS_PASSWORD}
```

### Environment Variables

The `redis-connection` secret provides:
- `REDIS_HOST` - Redis hostname
- `REDIS_PORT` - Redis port
- `REDIS_URL` - Connection URL
- `SPRING_DATA_REDIS_HOST` - Spring config
- `SPRING_DATA_REDIS_PORT` - Spring config

Password is stored in Helm-generated secret: `redis`

## Use Cases

### Session Storage

```kotlin
// Spring Session with Redis
@EnableRedisHttpSession
class SessionConfig
```

### Cache

```kotlin
// Spring Cache with Redis
@Cacheable("scenarios")
suspend fun getScenario(id: ScenarioId): Scenario
```

### Rate Limiting

```kotlin
// Using Redis for rate limiting
redisTemplate.opsForValue()
    .setIfAbsent("rate:$key", "1", Duration.ofMinutes(1))
```

## Monitoring

### Prometheus Metrics

Redis exporter enabled via `metrics.enabled: true`.

Key metrics:
- `redis_connected_clients` - Current connections
- `redis_memory_used_bytes` - Memory usage
- `redis_commands_processed_total` - Command throughput
- `redis_keyspace_hits_total` / `redis_keyspace_misses_total` - Cache hit rate

### Calculate Hit Rate

```promql
sum(rate(redis_keyspace_hits_total[5m])) /
(sum(rate(redis_keyspace_hits_total[5m])) + sum(rate(redis_keyspace_misses_total[5m])))
```

### Grafana Dashboard

Import dashboard ID: **11835** (Redis Dashboard)

## High Availability Setup

To enable HA with Sentinel:

```yaml
# values.yaml
architecture: replication

replica:
  replicaCount: 2

sentinel:
  enabled: true
  quorum: 2
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
```

## Troubleshooting

### Cannot connect

```bash
# Check pod logs
kubectl logs -n qawave -l app.kubernetes.io/name=redis

# Test from inside cluster
kubectl run debug --rm -it --restart=Never \
  --image=busybox --namespace=qawave -- \
  sh -c "nc -zv redis-master 6379"
```

### Memory issues

```bash
# Check memory usage
kubectl exec -it redis-master-0 -n qawave -- redis-cli INFO memory

# Check evicted keys
kubectl exec -it redis-master-0 -n qawave -- redis-cli INFO stats | grep evicted
```

### Slow performance

```bash
# Check slow log
kubectl exec -it redis-master-0 -n qawave -- redis-cli SLOWLOG GET 10
```

## Related Documentation

- [Bitnami Redis Chart](https://github.com/bitnami/charts/tree/main/bitnami/redis)
- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
