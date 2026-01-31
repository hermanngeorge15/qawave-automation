# Grafana Dashboards

Pre-configured Grafana dashboards for QAWave monitoring.

## Available Dashboards

### PostgreSQL Performance (`postgresql-performance.json`)

Comprehensive PostgreSQL monitoring dashboard with:

**Overview Panels:**
- Connection usage percentage
- Active connections count
- Database size
- Total deadlocks
- Transaction rate (commits/rollbacks)

**Query Performance:**
- Row operations rate (fetch, insert, update, delete)
- Buffer cache hit ratio (target: >99%)

**Table Statistics:**
- Row count by table (qa_packages, test_scenarios, test_runs, test_step_results)
- Table scan types (sequential vs index scans)

**Index Performance:**
- Top 10 most used indexes
- Least used indexes (candidates for removal)

**Replication & WAL:**
- Buffer writes rate
- Checkpoint statistics

## Prerequisites

1. PostgreSQL exporter (postgres_exporter) deployed
2. Prometheus scraping PostgreSQL metrics
3. Grafana with Prometheus data source configured

## Installation

### Using Kubernetes ConfigMap

```bash
kubectl create configmap grafana-dashboards \
  --from-file=postgresql-performance.json \
  -n monitoring
```

### Using Grafana Provisioning

Add to `grafana.ini`:
```ini
[dashboards]
provisioning_path = /etc/grafana/provisioning/dashboards
```

Mount the dashboard files to `/etc/grafana/provisioning/dashboards/`.

### Manual Import

1. Open Grafana UI
2. Go to Dashboards → Import
3. Upload the JSON file or paste contents
4. Select Prometheus data source
5. Save

## Key Metrics to Watch

| Metric | Healthy Range | Action if Unhealthy |
|--------|--------------|---------------------|
| Connection Usage | < 80% | Increase max_connections or add connection pooling |
| Cache Hit Ratio | > 99% | Increase shared_buffers |
| Sequential Scans | Low on large tables | Add missing indexes |
| Deadlocks | 0 | Review transaction ordering |

## Customization

### Adding Application-Specific Queries

```promql
# QA Package execution time
histogram_quantile(0.95, rate(qa_package_duration_seconds_bucket[5m]))

# Active test runs
pg_stat_user_tables_n_live_tup{relname="test_runs"} - pg_stat_user_tables_n_dead_tup{relname="test_runs"}
```

### Alerting Rules

Recommended alerts (add to Prometheus):

```yaml
groups:
  - name: postgresql
    rules:
      - alert: PostgreSQLHighConnectionUsage
        expr: (pg_stat_activity_count / pg_settings_max_connections) * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL connection usage is high"

      - alert: PostgreSQLLowCacheHitRatio
        expr: (pg_stat_database_blks_hit / (pg_stat_database_blks_hit + pg_stat_database_blks_read)) * 100 < 95
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL cache hit ratio is low"
```

## Related Documentation

- [PostgreSQL Exporter](https://github.com/prometheus-community/postgres_exporter)
- [ServiceMonitor Configuration](../servicemonitors/postgresql-servicemonitor.yaml)
- [Backup Monitoring](../../../data-services/postgresql/backup/README.md)
