# Autoscaling Configuration for QAWave

This directory contains HorizontalPodAutoscalers (HPA) and PodDisruptionBudgets (PDB) for QAWave services.

## Overview

### HorizontalPodAutoscalers

HPAs automatically scale pod replicas based on observed metrics (CPU, memory, custom metrics).

| Service | Min Replicas | Max Replicas | CPU Target | Memory Target |
|---------|--------------|--------------|------------|---------------|
| Backend | 2 | 10 | 70% | 80% |
| Frontend | 2 | 6 | 75% | 85% |
| Keycloak | 1 | 4 | 70% | 75% |

### PodDisruptionBudgets

PDBs ensure minimum availability during voluntary disruptions (node maintenance, cluster upgrades).

| Service | Min Available | Max Unavailable |
|---------|---------------|-----------------|
| Backend | 1 | - |
| Frontend | 1 | - |
| Keycloak | 1 | - |
| PostgreSQL | - | 0 |
| Redis | - | 0 |
| Kafka | - | 1 |

## Prerequisites

1. **Metrics Server**: Required for CPU/memory metrics
2. **Resource Requests**: Pods must have resource requests defined
3. **Deployments**: Target deployments must exist

### Install Metrics Server

```bash
# Check if metrics server is installed
kubectl get deployment metrics-server -n kube-system

# Install if needed (k0s usually includes it)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

## Scaling Behavior

### Backend

- **Scale Up**: Aggressive (60s stabilization)
  - Up to 100% increase or 4 pods per minute
- **Scale Down**: Conservative (300s stabilization)
  - Max 25% decrease or 2 pods per minute

### Frontend

- **Scale Up**: Moderate (60s stabilization)
  - Up to 2 pods per minute
- **Scale Down**: Conservative (300s stabilization)
  - Max 1 pod per minute

### Keycloak

- **Scale Up**: Very conservative (120s stabilization)
  - Max 1 pod every 2 minutes
- **Scale Down**: Very conservative (600s stabilization)
  - Max 1 pod every 5 minutes

## Resource Requirements

Ensure deployments have proper resource requests:

```yaml
resources:
  requests:
    cpu: 250m      # HPA uses this for % calculation
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

## Monitoring

### Check HPA Status

```bash
# List all HPAs
kubectl get hpa -n qawave

# Describe HPA
kubectl describe hpa qawave-backend-hpa -n qawave

# Watch scaling events
kubectl get hpa -n qawave -w
```

### Check PDB Status

```bash
# List all PDBs
kubectl get pdb -n qawave

# Check disruption status
kubectl describe pdb backend-pdb -n qawave
```

### Metrics

```bash
# Check current pod metrics
kubectl top pods -n qawave

# Check node metrics
kubectl top nodes
```

## Troubleshooting

### HPA Shows "unknown" Metrics

1. Check metrics server is running
2. Verify resource requests are set
3. Wait for metrics to be collected (1-2 minutes)

```bash
# Check metrics server
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq .
```

### Pods Not Scaling Up

1. Check current metrics vs target
2. Verify max replicas not reached
3. Check for resource quota limits

### Pods Not Scaling Down

1. Check stabilization window (default 300s)
2. Verify metrics are below target
3. Check for incoming traffic patterns

## Custom Metrics (Optional)

For advanced scaling based on request rate:

1. Install prometheus-adapter
2. Configure custom metrics rules
3. Add custom metric to HPA

```yaml
metrics:
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

## Best Practices

1. **Start Conservative**: Begin with higher utilization targets
2. **Monitor Before Scaling**: Observe metrics before adjusting
3. **Test Scaling**: Use load testing to verify behavior
4. **Set Appropriate Limits**: Prevent runaway scaling
5. **Use PDBs**: Always pair HPAs with PDBs
