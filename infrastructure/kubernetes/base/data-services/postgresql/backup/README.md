# PostgreSQL Backup and Recovery

This directory contains Kubernetes resources for automated database backup and recovery for QAWave.

## Backup Strategy

| Type | Schedule | Retention | RPO |
|------|----------|-----------|-----|
| Full Backup | Daily 2:00 AM UTC | 30 days | 24 hours |
| WAL Checkpoint | Every 6 hours | N/A | 6 hours |

### Recovery Objectives

- **RTO (Recovery Time Objective)**: 4 hours
- **RPO (Recovery Point Objective)**: 6 hours

## Quick Start

### Prerequisites

1. S3-compatible storage bucket created
2. AWS credentials with S3 write access
3. PostgreSQL secrets deployed (`postgresql-secrets`)

### Setup

```bash
# 1. Copy and configure secrets
cp backup-secrets.yaml.example backup-secrets.yaml
# Edit backup-secrets.yaml with your S3 credentials

# 2. Create sealed secret (for production)
kubeseal --format yaml < backup-secrets.yaml > backup-secrets-sealed.yaml

# 3. Apply backup resources
kubectl apply -k .
```

### Verify Backup is Working

```bash
# Check CronJob status
kubectl get cronjobs -n qawave

# Trigger a manual backup
kubectl create job --from=cronjob/postgresql-backup manual-backup -n qawave

# Monitor backup progress
kubectl logs -f job/manual-backup -n qawave

# List backups in S3
aws s3 ls s3://qawave-backups/postgresql/daily/
```

## Backup Files

| File | Description |
|------|-------------|
| `backup-cronjob.yaml` | Daily backup and WAL checkpoint CronJobs |
| `backup-configmap.yaml` | Non-sensitive backup configuration |
| `backup-secrets.yaml.example` | Template for S3 credentials |
| `restore-job.yaml` | Job templates for restore and verification |
| `kustomization.yaml` | Kustomize configuration |

## Restore Procedures

### 1. Point-in-Time Recovery

For recovering to a specific backup:

```bash
# 1. List available backups
aws s3 ls s3://qawave-backups/postgresql/daily/

# 2. Edit restore-job.yaml
# Set BACKUP_FILE to the desired backup filename
# Example: qawave_20260130_020000.dump

# 3. Apply the restore job
kubectl apply -f restore-job.yaml

# 4. Monitor restore progress
kubectl logs -f job/postgresql-restore -n qawave

# 5. Verify the restore
kubectl apply -f restore-job.yaml  # The verify job is in the same file
kubectl logs -f job/postgresql-verify -n qawave
```

### 2. Emergency Recovery

If PostgreSQL pod is not running:

```bash
# 1. Scale down the application
kubectl scale deployment backend --replicas=0 -n qawave

# 2. Ensure PostgreSQL is running
kubectl get pods -n qawave -l app.kubernetes.io/name=postgresql

# 3. Follow restore procedure above

# 4. Scale application back up
kubectl scale deployment backend --replicas=3 -n qawave
```

### 3. Recovery to New Cluster

For disaster recovery to a new Kubernetes cluster:

```bash
# 1. Deploy PostgreSQL with same configuration
helm install postgresql bitnami/postgresql -n qawave -f ../values.yaml

# 2. Apply secrets
kubectl apply -f backup-secrets.yaml

# 3. Run restore job
kubectl apply -f restore-job.yaml

# 4. Verify data
kubectl apply -f restore-job.yaml  # verify job
```

## Monitoring

### Backup Alerts

Configure alerting for backup failures:

```yaml
# PrometheusRule (add to monitoring configuration)
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: postgresql-backup-alerts
spec:
  groups:
    - name: postgresql-backup
      rules:
        - alert: PostgreSQLBackupFailed
          expr: kube_job_status_failed{job_name=~"postgresql-backup.*"} > 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "PostgreSQL backup failed"
            description: "Backup job {{ $labels.job_name }} has failed"

        - alert: PostgreSQLBackupMissing
          expr: time() - kube_cronjob_status_last_successful_time{cronjob="postgresql-backup"} > 129600
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "PostgreSQL backup overdue"
            description: "No successful backup in the last 36 hours"
```

### Backup Metrics

Check backup health:

```bash
# Last successful backup time
kubectl get cronjob postgresql-backup -n qawave -o jsonpath='{.status.lastSuccessfulTime}'

# Recent job history
kubectl get jobs -n qawave -l app.kubernetes.io/name=postgresql-backup --sort-by=.status.startTime

# S3 backup sizes
aws s3 ls s3://qawave-backups/postgresql/daily/ --summarize
```

## S3 Lifecycle Policy

Configure 30-day retention in S3:

```json
{
  "Rules": [
    {
      "ID": "DeleteOldBackups",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "postgresql/daily/"
      },
      "Expiration": {
        "Days": 30
      }
    }
  ]
}
```

Apply with:

```bash
aws s3api put-bucket-lifecycle-configuration \
  --bucket qawave-backups \
  --lifecycle-configuration file://lifecycle.json
```

## Recovery Testing

Quarterly recovery drills are required. Use this checklist:

### Pre-Recovery Checklist

- [ ] Backup file selected and verified accessible
- [ ] Target environment ready (staging/DR cluster)
- [ ] Application scaled down
- [ ] Stakeholders notified

### Recovery Steps

- [ ] Download backup from S3
- [ ] Verify backup integrity (pg_restore --list)
- [ ] Perform restore
- [ ] Run verification job
- [ ] Smoke test application
- [ ] Document recovery time

### Post-Recovery Checklist

- [ ] Application functionality verified
- [ ] Data integrity confirmed
- [ ] Performance acceptable
- [ ] Recovery time documented
- [ ] Lessons learned recorded

## Troubleshooting

### Backup Job Fails

```bash
# Check job logs
kubectl logs job/postgresql-backup-<timestamp> -n qawave

# Common issues:
# - S3 credentials expired: Update backup-secrets
# - Disk full: Check tmp volume
# - Network issues: Check egress policies
```

### Restore Fails

```bash
# Check restore logs
kubectl logs job/postgresql-restore -n qawave

# Common issues:
# - Active connections: Disconnect clients first
# - Permissions: Ensure PGUSER has sufficient privileges
# - Disk space: Check PVC capacity
```

### Verification Fails

```bash
# Check verification output
kubectl logs job/postgresql-verify -n qawave

# Compare with expected:
# - Table count
# - Row counts
# - Flyway migration version
```

## Security Considerations

1. **Encryption at Rest**: Backups are encrypted with AES-256 (S3 SSE)
2. **Encryption in Transit**: HTTPS for S3 transfers
3. **Access Control**: Use IAM roles with least privilege
4. **Credential Rotation**: Rotate S3 credentials quarterly
5. **Audit Logging**: Enable S3 access logging

## Related Documentation

- [PostgreSQL Helm Values](../values.yaml)
- [Database Migration Guide](../../../../../backend/src/main/resources/db/migration/README.md)
- [Disaster Recovery Plan](../../../../docs/disaster-recovery.md)
