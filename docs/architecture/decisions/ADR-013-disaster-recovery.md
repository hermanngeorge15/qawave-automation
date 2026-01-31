# ADR-013: Disaster Recovery and High Availability

## Status
Accepted

## Date
2026-01-30

## Context

QAWave requires high availability and disaster recovery capabilities to ensure:
- 99.9% uptime SLA
- Data protection and recovery
- Business continuity during failures
- Rapid incident response and recovery

## Decision

We implement a **multi-layered HA/DR architecture** with defined RTO/RPO objectives.

### High Availability Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    HIGH AVAILABILITY ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         ┌─────────────────────┐                             │
│                         │   Load Balancer     │                             │
│                         │   (Health Checks)   │                             │
│                         └──────────┬──────────┘                             │
│                                    │                                         │
│              ┌─────────────────────┼─────────────────────┐                  │
│              │                     │                     │                  │
│              ▼                     ▼                     ▼                  │
│    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐         │
│    │  Backend Pod 1  │   │  Backend Pod 2  │   │  Backend Pod 3  │         │
│    │   (Active)      │   │   (Active)      │   │   (Active)      │         │
│    └─────────────────┘   └─────────────────┘   └─────────────────┘         │
│              │                     │                     │                  │
│              └─────────────────────┼─────────────────────┘                  │
│                                    │                                         │
│    ┌───────────────────────────────┼───────────────────────────────┐        │
│    │                               │                               │        │
│    ▼                               ▼                               ▼        │
│ ┌──────────────┐           ┌──────────────┐           ┌──────────────┐      │
│ │ PostgreSQL   │◄─────────►│   Redis      │           │    Kafka     │      │
│ │ Primary      │ Streaming │ (Sentinel)   │           │  (3 Brokers) │      │
│ │  + Replica   │ Replicate └──────────────┘           └──────────────┘      │
│ └──────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Recovery Objectives

| Failure Scenario | RTO | RPO | Automated |
|-----------------|-----|-----|-----------|
| Pod failure | 30s | 0 | Yes |
| Node failure | 5m | 0 | Yes |
| Zone failure | 15m | 1m | Yes |
| Region failure | 4h | 15m | Manual |
| Data corruption | 1h | 5m | Manual |

### Pod-Level HA

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: qawave-backend-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: qawave-backend
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: qawave-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: qawave-backend
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  app: qawave-backend
              topologyKey: kubernetes.io/hostname
```

### Database HA with Streaming Replication

```yaml
# CloudNativePG Configuration
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: qawave-postgres
spec:
  instances: 3
  primaryUpdateStrategy: unsupervised

  postgresql:
    parameters:
      wal_level: replica
      max_wal_senders: 10
      max_replication_slots: 10
      hot_standby: "on"
      hot_standby_feedback: "on"

  bootstrap:
    recovery:
      source: qawave-postgres-backup

  backup:
    barmanObjectStore:
      destinationPath: s3://qawave-backups/postgres
      s3Credentials:
        accessKeyId:
          name: s3-credentials
          key: ACCESS_KEY_ID
        secretAccessKey:
          name: s3-credentials
          key: ACCESS_SECRET_KEY
      wal:
        compression: gzip
        maxParallel: 4
    retentionPolicy: "30d"
```

### Backup Strategy

| Component | Frequency | Retention | Location |
|-----------|-----------|-----------|----------|
| PostgreSQL WAL | Continuous | 7 days | S3 |
| PostgreSQL Full | Daily | 30 days | S3 |
| Redis RDB | Hourly | 24 hours | S3 |
| Kafka Topics | Continuous | 7 days | Replicas |
| Secrets | Daily | 90 days | Vault |
| Kubernetes State | Hourly | 30 days | S3 |

### Automated Backup Schedule

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: ScheduledBackup
metadata:
  name: qawave-postgres-backup
spec:
  schedule: "0 0 * * *"  # Daily at midnight
  backupOwnerReference: self
  cluster:
    name: qawave-postgres
  target: prefer-standby
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: velero-backup
spec:
  schedule: "0 */6 * * *"  # Every 6 hours
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: velero-backup
              image: velero/velero:v1.13.0
              command:
                - /velero
                - backup
                - create
                - qawave-$(date +%Y%m%d%H%M)
                - --include-namespaces=qawave
                - --ttl=720h
```

### Failover Procedures

**Automatic Failover (Pod/Node):**
```
1. Health check fails (liveness probe)
2. Kubernetes terminates pod
3. New pod scheduled on healthy node
4. Service discovery updates
5. Traffic routes to healthy pods
```

**Database Failover:**
```
1. Primary becomes unavailable
2. CloudNativePG detects failure
3. Replica promoted to primary
4. Connection pool reconnects
5. Writes resume (< 30s)
```

### Health Check Configuration

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: backend
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 5
        failureThreshold: 3
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        timeoutSeconds: 3
        failureThreshold: 2
      startupProbe:
        httpGet:
          path: /actuator/health
          port: 8080
        initialDelaySeconds: 0
        periodSeconds: 5
        failureThreshold: 30
```

### Chaos Engineering

```kotlin
// Chaos Monkey for Spring Boot
@Configuration
@EnableChaosMonkey
class ChaosConfig {
    @Bean
    fun chaosMonkeySettings() = ChaosMonkeySettings(
        enabled = envProfile == "chaos-test",
        assaults = AssaultsProperties(
            level = 5,              // 5% of requests
            latencyActive = true,
            latencyRangeStart = 500,
            latencyRangeEnd = 2000,
            exceptionsActive = true,
            killApplicationActive = false
        ),
        watchedCustomServices = listOf(
            "qaPackageService",
            "scenarioGeneratorAgent"
        )
    )
}
```

### Incident Response Runbook

| Severity | Definition | Response Time | Escalation |
|----------|------------|---------------|------------|
| P1 | Service Down | 15 min | Immediate |
| P2 | Degraded Performance | 30 min | 1 hour |
| P3 | Non-critical Issue | 4 hours | 24 hours |
| P4 | Minor Issue | 24 hours | None |

## Consequences

### Positive
- Meets 99.9% uptime SLA
- Automated recovery for common failures
- Data protection with continuous backups
- Clear incident response procedures

### Negative
- Infrastructure cost increase
- Operational complexity
- Backup storage costs
- Testing overhead for DR procedures

## References

- [CloudNativePG](https://cloudnative-pg.io/documentation/)
- [Velero Backup](https://velero.io/docs/)
- [Chaos Monkey](https://codecentric.github.io/chaos-monkey-spring-boot/)
- [Kubernetes HA Best Practices](https://kubernetes.io/docs/setup/production-environment/)
