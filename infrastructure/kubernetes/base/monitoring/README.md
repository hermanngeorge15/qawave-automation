# QAWave Monitoring Stack

This directory contains the monitoring configuration for QAWave using the kube-prometheus-stack.

## Overview

The monitoring stack provides:
- **Prometheus**: Metrics collection, storage, and alerting rules
- **Alertmanager**: Alert routing and notifications
- **Grafana**: Visualization dashboards
- **ServiceMonitors**: Automatic service discovery for QAWave components

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Monitoring Namespace                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐       │
│  │   Prometheus    │────▶│   Alertmanager  │────▶│    Slack/       │       │
│  │   (metrics)     │     │   (routing)     │     │    PagerDuty    │       │
│  └────────┬────────┘     └─────────────────┘     └─────────────────┘       │
│           │                                                                  │
│           │ scrape                                                          │
│           ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │                   ServiceMonitors                            │           │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │           │
│  │  │ Backend  │ │PostgreSQL│ │  Redis   │ │  Kafka   │       │           │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                                                                              │
│  ┌─────────────────┐                                                        │
│  │    Grafana      │◀─── Dashboards                                        │
│  │  (visualization)│                                                        │
│  └─────────────────┘                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              │ scrape targets
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           QAWave Namespace                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ Backend  │  │PostgreSQL│  │  Redis   │  │  Kafka   │  │ Keycloak │     │
│  │ :8080    │  │ :9187    │  │ :9121    │  │ :9404    │  │ :8080    │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Components

### Prometheus

- **Version**: Latest via kube-prometheus-stack
- **Retention**: 15 days (45GB max)
- **Storage**: 50Gi persistent volume
- **Scrape interval**: 30 seconds

### Alertmanager

- **Storage**: 10Gi persistent volume
- **Receivers**: Configurable (Slack, PagerDuty, Email)

### Grafana

- **Storage**: 10Gi persistent volume
- **Authentication**: Admin credentials via secret
- **Dashboards**: Auto-discovered from ConfigMaps

## ServiceMonitors

| Service | Metrics Path | Port | Interval |
|---------|-------------|------|----------|
| Backend | `/actuator/prometheus` | http | 30s |
| PostgreSQL | `/metrics` | metrics | 30s |
| Redis | `/metrics` | metrics | 30s |
| Kafka | `/metrics` | metrics | 30s |
| Keycloak | `/metrics` | http | 30s |

## Installation

### Prerequisites

1. Kubernetes cluster with ingress controller
2. Storage class for persistent volumes
3. cert-manager (optional, for TLS)

### Deploy via ArgoCD

```bash
# Apply ArgoCD application
kubectl apply -f infrastructure/argocd/applications/monitoring.yaml
```

### Manual Helm Installation

```bash
# Add Prometheus community Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Create namespace
kubectl create namespace monitoring

# Install kube-prometheus-stack
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values values.yaml
```

## Accessing Dashboards

### Grafana

- **URL**: https://grafana.qawave.local
- **Default admin**: From `grafana-admin-credentials` secret

```bash
# Port forward for local access
kubectl port-forward svc/kube-prometheus-stack-grafana -n monitoring 3000:80
# Access at http://localhost:3000
```

### Prometheus

- **URL**: https://prometheus.qawave.local
- **Authentication**: Basic auth via `monitoring-basic-auth` secret

```bash
# Port forward
kubectl port-forward svc/kube-prometheus-stack-prometheus -n monitoring 9090:9090
# Access at http://localhost:9090
```

### Alertmanager

- **URL**: https://alertmanager.qawave.local
- **Authentication**: Basic auth via `monitoring-basic-auth` secret

```bash
# Port forward
kubectl port-forward svc/kube-prometheus-stack-alertmanager -n monitoring 9093:9093
# Access at http://localhost:9093
```

## Configuration

### Adding Custom Alerts

Create a PrometheusRule resource:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: qawave-alerts
  namespace: monitoring
  labels:
    app: kube-prometheus-stack
    release: kube-prometheus-stack
spec:
  groups:
    - name: qawave.rules
      rules:
        - alert: QAWaveBackendDown
          expr: up{application="qawave-backend"} == 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "QAWave Backend is down"
            description: "QAWave Backend has been down for more than 5 minutes."
```

### Adding Custom Dashboards

Create a ConfigMap with the `grafana_dashboard: "1"` label:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: qawave-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  qawave-overview.json: |
    {
      "title": "QAWave Overview",
      ...
    }
```

## Secrets Required

### Grafana Admin Credentials

```bash
kubectl create secret generic grafana-admin-credentials \
  --namespace monitoring \
  --from-literal=admin-user=admin \
  --from-literal=admin-password='<your-password>'
```

### Basic Auth for Prometheus/Alertmanager

```bash
# Generate htpasswd
htpasswd -c auth admin
kubectl create secret generic monitoring-basic-auth \
  --namespace monitoring \
  --from-file=auth
```

## Metrics Reference

### Backend (Spring Boot)

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds` | HTTP request latency |
| `jvm_memory_used_bytes` | JVM memory usage |
| `r2dbc_pool_acquired` | Database connection pool |
| `spring_kafka_listener_*` | Kafka consumer metrics |

### PostgreSQL

| Metric | Description |
|--------|-------------|
| `pg_stat_database_*` | Database statistics |
| `pg_stat_user_tables_*` | Table statistics |
| `pg_settings_*` | PostgreSQL settings |

### Redis

| Metric | Description |
|--------|-------------|
| `redis_connected_clients` | Connected clients |
| `redis_memory_used_bytes` | Memory usage |
| `redis_commands_total` | Command execution |

### Kafka

| Metric | Description |
|--------|-------------|
| `kafka_server_*` | Broker metrics |
| `kafka_consumer_*` | Consumer metrics |
| `kafka_producer_*` | Producer metrics |

## Troubleshooting

### Prometheus Not Scraping Targets

1. Check ServiceMonitor selector matches service labels
2. Verify endpoint port name matches
3. Check network policies allow scraping

```bash
# Check discovered targets
kubectl port-forward svc/kube-prometheus-stack-prometheus -n monitoring 9090:9090
# Visit http://localhost:9090/targets
```

### Grafana Dashboard Not Loading

1. Check datasource configuration
2. Verify Prometheus is reachable from Grafana
3. Check dashboard JSON syntax

### Alerts Not Firing

1. Verify PrometheusRule is loaded
2. Check alert expression in Prometheus UI
3. Verify Alertmanager receiver configuration

## Useful Commands

```bash
# List all ServiceMonitors
kubectl get servicemonitors -A

# Check Prometheus targets
kubectl exec -it -n monitoring prometheus-kube-prometheus-stack-prometheus-0 -- \
  promtool query instant http://localhost:9090 'up'

# View Alertmanager configuration
kubectl get secret -n monitoring alertmanager-kube-prometheus-stack-alertmanager -o jsonpath='{.data.alertmanager\.yaml}' | base64 -d

# Check Grafana logs
kubectl logs -n monitoring -l app.kubernetes.io/name=grafana -f
```

## References

- [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
