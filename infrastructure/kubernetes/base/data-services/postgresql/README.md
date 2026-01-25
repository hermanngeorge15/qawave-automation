# PostgreSQL Deployment for QAWave

PostgreSQL 16 database deployment using Bitnami Helm chart.

## Quick Start

### 1. Add Helm Repository

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2. Create Namespace

```bash
kubectl create namespace qawave
```

### 3. Generate Passwords

```bash
# Generate secure passwords
POSTGRES_PASSWORD=$(openssl rand -base64 32)
QAWAVE_PASSWORD=$(openssl rand -base64 32)

echo "Postgres admin password: $POSTGRES_PASSWORD"
echo "QAWave app password: $QAWAVE_PASSWORD"
```

### 4. Install PostgreSQL

```bash
helm install postgresql bitnami/postgresql \
  --namespace qawave \
  --values values.yaml \
  --set auth.postgresPassword="$POSTGRES_PASSWORD" \
  --set auth.password="$QAWAVE_PASSWORD"
```

### 5. Apply Connection Secrets

```bash
kubectl apply -f secrets.yaml
kubectl apply -f configmap.yaml
```

## Verify Installation

```bash
# Check pod status
kubectl get pods -n qawave -l app.kubernetes.io/name=postgresql

# Check service
kubectl get svc -n qawave postgresql

# Test connection
kubectl run postgresql-client --rm --tty -i --restart='Never' \
  --namespace qawave \
  --image docker.io/bitnami/postgresql:16 \
  --env="PGPASSWORD=$QAWAVE_PASSWORD" \
  --command -- psql --host postgresql -U qawave -d qawave -p 5432
```

## Configuration

### values.yaml

| Setting | Value | Description |
|---------|-------|-------------|
| `image.tag` | 16.2.0 | PostgreSQL version |
| `auth.database` | qawave | Application database |
| `auth.username` | qawave | Application user |
| `primary.persistence.size` | 20Gi | Data volume size |
| `primary.resources.requests.memory` | 512Mi | Memory request |
| `primary.resources.limits.memory` | 2Gi | Memory limit |

### PostgreSQL Tuning

Configuration optimized for:
- Up to 200 concurrent connections
- 2GB memory limit
- SSD storage (random_page_cost = 1.1)

Key parameters:
```
shared_buffers = 512MB
effective_cache_size = 1536MB
max_connections = 200
```

## Connection Strings

### R2DBC (for Spring WebFlux)

```
r2dbc:postgresql://postgresql.qawave.svc.cluster.local:5432/qawave
```

### JDBC (for migrations)

```
jdbc:postgresql://postgresql.qawave.svc.cluster.local:5432/qawave
```

## Backup Strategy

### Option 1: pg_dump CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgresql-backup
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: bitnami/postgresql:16
            command:
            - /bin/bash
            - -c
            - |
              PGPASSWORD=$POSTGRES_PASSWORD pg_dump \
                -h postgresql -U postgres qawave \
                | gzip > /backup/qawave-$(date +%Y%m%d).sql.gz
            env:
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql
                  key: postgres-password
            volumeMounts:
            - name: backup
              mountPath: /backup
          volumes:
          - name: backup
            persistentVolumeClaim:
              claimName: postgresql-backup
          restartPolicy: OnFailure
```

### Option 2: WAL-G Continuous Backup

For production, consider using WAL-G with S3-compatible storage:
- Point-in-time recovery
- Continuous archiving
- Minimal data loss

## Monitoring

### Prometheus Metrics

PostgreSQL exporter is enabled via `metrics.enabled: true`.

Key metrics:
- `pg_stat_activity_count` - Active connections
- `pg_database_size_bytes` - Database size
- `pg_stat_user_tables_*` - Table statistics
- `pg_replication_lag` - Replication lag (if replicas)

### Grafana Dashboard

Import dashboard ID: **9628** (PostgreSQL Database)

## Troubleshooting

### Cannot connect to database

```bash
# Check pod status
kubectl describe pod -n qawave -l app.kubernetes.io/name=postgresql

# Check logs
kubectl logs -n qawave -l app.kubernetes.io/name=postgresql

# Verify service
kubectl get endpoints -n qawave postgresql
```

### Out of connections

```bash
# Check current connections
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -c \
  "SELECT count(*) FROM pg_stat_activity;"

# Kill idle connections
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < now() - interval '1 hour';"
```

### Slow queries

```bash
# Find slow queries
kubectl exec -it postgresql-0 -n qawave -- psql -U postgres -c \
  "SELECT pid, now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC;"
```

## Upgrade

```bash
# Backup first!
helm upgrade postgresql bitnami/postgresql \
  --namespace qawave \
  --values values.yaml \
  --set auth.postgresPassword="$POSTGRES_PASSWORD" \
  --set auth.password="$QAWAVE_PASSWORD"
```

## Related Documentation

- [Bitnami PostgreSQL Chart](https://github.com/bitnami/charts/tree/main/bitnami/postgresql)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)
- [R2DBC PostgreSQL](https://github.com/pgjdbc/r2dbc-postgresql)
