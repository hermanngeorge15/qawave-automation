# Database Backup Runbook

## Overview

This runbook covers database backup procedures for QAWave PostgreSQL databases.

## Backup Strategy

| Environment | Frequency | Retention | Storage |
|-------------|-----------|-----------|---------|
| Staging | Daily | 7 days | Local volume |
| Production | Daily + WAL | 30 days | Hetzner Storage Box |

## Automated Backups

### Production Backup Schedule

| Type | Schedule | Retention |
|------|----------|-----------|
| Full backup | Daily 02:00 UTC | 30 days |
| WAL archiving | Continuous | 7 days |
| Point-in-time | On-demand | N/A |

### Backup Job (Kubernetes CronJob)

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgresql-backup
  namespace: production
spec:
  schedule: "0 2 * * *"  # Daily at 02:00 UTC
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:16-alpine
            command:
            - /bin/sh
            - -c
            - |
              BACKUP_FILE="/backups/qawave-$(date +%Y%m%d-%H%M%S).sql.gz"
              pg_dump -h postgresql -U qawave -d qawave | gzip > $BACKUP_FILE
              echo "Backup completed: $BACKUP_FILE"
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql-credentials
                  key: password
            volumeMounts:
            - name: backup-volume
              mountPath: /backups
          restartPolicy: OnFailure
          volumes:
          - name: backup-volume
            persistentVolumeClaim:
              claimName: backup-pvc
```

## Manual Backup Procedures

### Create Manual Backup

```bash
# Set environment
export KUBECONFIG=~/.kube/config-qawave-staging  # or -prod

# Create backup
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave > backup-$(date +%Y%m%d).sql

# Compressed backup
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave | gzip > backup-$(date +%Y%m%d).sql.gz
```

### Schema-Only Backup

```bash
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave --schema-only > schema-$(date +%Y%m%d).sql
```

### Data-Only Backup

```bash
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave --data-only > data-$(date +%Y%m%d).sql
```

### Specific Table Backup

```bash
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave -t users -t projects > tables-backup.sql
```

## Restore Procedures

### Full Database Restore

> **Warning:** This will overwrite all existing data!

```bash
# Stop application to prevent writes
kubectl scale deployment backend --replicas=0 -n staging

# Restore from backup
kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d qawave < backup-20240115.sql

# Or from compressed backup
gunzip -c backup-20240115.sql.gz | kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d qawave

# Restart application
kubectl scale deployment backend --replicas=2 -n staging
```

### Restore to New Database

```bash
# Create new database
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -c "CREATE DATABASE qawave_restored;"

# Restore to new database
kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d qawave_restored < backup-20240115.sql

# Verify
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -d qawave_restored -c "\dt"
```

### Point-in-Time Recovery (PITR)

```bash
# Requires WAL archiving enabled

# 1. Stop PostgreSQL
kubectl scale statefulset postgresql --replicas=0 -n production

# 2. Restore base backup
# 3. Configure recovery.conf with target time
# 4. Start PostgreSQL
kubectl scale statefulset postgresql --replicas=1 -n production

# 5. PostgreSQL replays WAL to target time
```

## Backup Verification

### Verify Backup Integrity

```bash
# List tables in backup
grep "CREATE TABLE" backup-20240115.sql

# Count records in backup
grep "COPY.*FROM stdin" backup-20240115.sql -A 10000 | head -20

# Test restore to temp database
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -c "CREATE DATABASE backup_test;"
kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d backup_test < backup-20240115.sql

# Verify data
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -d backup_test -c "SELECT COUNT(*) FROM users;"

# Cleanup
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -c "DROP DATABASE backup_test;"
```

### Monthly Backup Test

Schedule monthly backup restoration test:

1. Create test environment
2. Restore latest backup
3. Verify data integrity
4. Run application tests
5. Document results
6. Destroy test environment

## Backup Storage

### Local Storage (Staging)

```bash
# Check backup volume
kubectl get pvc backup-pvc -n staging

# List backups
kubectl exec -it postgresql-0 -n staging -- ls -la /backups/
```

### Remote Storage (Production)

```bash
# Upload to Hetzner Storage Box
scp backup-20240115.sql.gz uXXXXX@uXXXXX.your-storagebox.de:/backups/

# Or use rclone
rclone copy backup-20240115.sql.gz storagebox:/backups/

# List remote backups
rclone ls storagebox:/backups/
```

### Backup Rotation

```bash
# Delete backups older than 30 days
find /backups -name "*.sql.gz" -mtime +30 -delete

# Kubernetes job for cleanup
kubectl exec -it postgresql-0 -n production -- find /backups -name "*.sql.gz" -mtime +30 -delete
```

## Monitoring

### Backup Job Status

```bash
# Check CronJob
kubectl get cronjob postgresql-backup -n production

# Check recent jobs
kubectl get jobs -n production | grep postgresql-backup

# Check job logs
kubectl logs job/postgresql-backup-xxxxx -n production
```

### Alerts

Set up alerts for:
- Backup job failure
- Backup size anomaly
- Missing daily backup
- Storage space low

## Troubleshooting

### Backup Job Fails

```bash
# Check job status
kubectl describe job postgresql-backup-xxxxx -n production

# Check pod logs
kubectl logs -l job-name=postgresql-backup-xxxxx -n production

# Common issues:
# - Disk full
# - Connection refused
# - Permission denied
```

### Restore Fails

```bash
# Check for syntax errors
psql -f backup.sql 2>&1 | head -50

# Restore with single transaction (rollback on error)
kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d qawave --single-transaction < backup.sql

# Ignore errors and continue
kubectl exec -i postgresql-0 -n staging -- psql -U qawave -d qawave -v ON_ERROR_STOP=0 < backup.sql
```

### Large Backup Performance

```bash
# Parallel backup (pg_dump custom format)
kubectl exec -it postgresql-0 -n staging -- pg_dump -U qawave -d qawave -Fc -j 4 -f /backups/backup.dump

# Parallel restore
kubectl exec -it postgresql-0 -n staging -- pg_restore -U qawave -d qawave -j 4 /backups/backup.dump
```

## Related Documentation

- [Disaster Recovery](./DISASTER_RECOVERY.md)
- [Staging Environment](../environments/STAGING.md)
- [Production Environment](../environments/PRODUCTION.md)
