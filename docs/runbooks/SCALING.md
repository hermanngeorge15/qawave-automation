# Scaling Runbook

This runbook covers manual and emergency scaling procedures for QAWave services.

## Table of Contents

1. [Overview](#overview)
2. [Auto-Scaling Configuration](#auto-scaling-configuration)
3. [Manual Scaling](#manual-scaling)
4. [Emergency Scaling](#emergency-scaling)
5. [Capacity Planning](#capacity-planning)
6. [Troubleshooting](#troubleshooting)

## Overview

QAWave uses Kubernetes Horizontal Pod Autoscaler (HPA) for automatic scaling. This runbook covers scenarios where manual intervention is required.

### Service Scaling Limits

| Service | Min Replicas | Max Replicas | CPU Target | Memory Target |
|---------|--------------|--------------|------------|---------------|
| Backend | 2 | 10 | 70% | 80% |
| Frontend | 2 | 6 | 75% | 85% |
| Keycloak | 1 | 4 | 70% | 75% |

### When to Scale Manually

- Anticipated traffic spikes (marketing campaigns, product launches)
- HPA not responding as expected
- Emergency situations requiring immediate action
- Planned maintenance windows

## Auto-Scaling Configuration

### Check Current HPA Status

```bash
# View all HPAs
kubectl get hpa -n qawave

# Detailed HPA status
kubectl describe hpa qawave-backend-hpa -n qawave

# Watch scaling events
kubectl get hpa -n qawave -w
```

### Verify Metrics

```bash
# Check if metrics are being collected
kubectl top pods -n qawave

# Check metrics server
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq '.items[].metadata.name'
```

## Manual Scaling

### Scale Up Deployment

```bash
# Scale backend to 5 replicas
kubectl scale deployment backend -n qawave --replicas=5

# Scale frontend to 4 replicas
kubectl scale deployment frontend -n qawave --replicas=4

# Verify scaling
kubectl get pods -n qawave -l app.kubernetes.io/name=backend
```

### Temporarily Disable HPA

If you need to maintain a fixed replica count:

```bash
# Option 1: Set min=max in HPA
kubectl patch hpa qawave-backend-hpa -n qawave \
  --patch '{"spec":{"minReplicas":5,"maxReplicas":5}}'

# Option 2: Delete HPA temporarily (not recommended)
kubectl delete hpa qawave-backend-hpa -n qawave
# Remember to reapply later!
```

### Restore Auto-Scaling

```bash
# Reset HPA to normal values
kubectl patch hpa qawave-backend-hpa -n qawave \
  --patch '{"spec":{"minReplicas":2,"maxReplicas":10}}'

# Or reapply from source
kubectl apply -f infrastructure/kubernetes/base/autoscaling/backend-hpa.yaml
```

## Emergency Scaling

### High Traffic Emergency

**Symptoms**: P95 latency > 2s, error rate > 5%, HPA not scaling fast enough

**Immediate Actions**:

```bash
# 1. Scale backend immediately
kubectl scale deployment backend -n qawave --replicas=10

# 2. Scale frontend
kubectl scale deployment frontend -n qawave --replicas=6

# 3. Verify pods are running
kubectl get pods -n qawave -w

# 4. Check for pending pods
kubectl get pods -n qawave --field-selector=status.phase=Pending
```

### Resource Exhaustion

**Symptoms**: Pods stuck in Pending, node resources exhausted

**Immediate Actions**:

```bash
# 1. Check node resources
kubectl describe nodes | grep -A 10 "Allocated resources"

# 2. Check for resource quotas
kubectl describe resourcequota -n qawave

# 3. If node capacity is the issue, contact infrastructure
# for additional nodes (requires Terraform/Hetzner action)
```

### Database Connection Pool Exhaustion

**Symptoms**: Backend errors, connection timeouts

```bash
# 1. Check connection pool metrics
kubectl port-forward svc/backend -n qawave 8080:8080
curl localhost:8080/actuator/metrics/r2dbc.pool.acquired

# 2. Reduce backend replicas to lower connection count
kubectl scale deployment backend -n qawave --replicas=3

# 3. Check PostgreSQL connections
kubectl exec -it postgresql-0 -n qawave -- \
  psql -U qawave -c "SELECT count(*) FROM pg_stat_activity;"
```

## Capacity Planning

### Current Capacity Assessment

```bash
# Node capacity
kubectl describe nodes | grep -A 5 "Capacity"

# Current usage
kubectl top nodes
kubectl top pods -n qawave --containers
```

### Capacity Thresholds

| Metric | Warning | Critical | Action |
|--------|---------|----------|--------|
| Node CPU | 70% | 85% | Add worker node |
| Node Memory | 75% | 90% | Add worker node |
| Pod CPU | 70% | 85% | Scale horizontally |
| Pod Memory | 80% | 90% | Scale horizontally |
| PVC Usage | 75% | 90% | Expand volume |

### Adding Capacity

#### Add Worker Node (Terraform)

```bash
cd infrastructure/terraform/environments/production

# Update worker_count variable
terraform plan -var="worker_count=4"

# Apply if safe
terraform apply -var="worker_count=4"
```

#### Increase Pod Resources

Edit deployment or HPA:

```bash
# Edit deployment resources
kubectl edit deployment backend -n qawave

# Or patch
kubectl patch deployment backend -n qawave \
  --patch '{"spec":{"template":{"spec":{"containers":[{"name":"backend","resources":{"limits":{"cpu":"2","memory":"2Gi"}}}]}}}}'
```

## Troubleshooting

### HPA Not Scaling

1. **Check metrics availability**
   ```bash
   kubectl top pods -n qawave
   ```
   If empty, metrics server may be down.

2. **Check HPA conditions**
   ```bash
   kubectl describe hpa qawave-backend-hpa -n qawave | grep -A 20 "Conditions"
   ```

3. **Verify resource requests are set**
   ```bash
   kubectl get deployment backend -n qawave -o yaml | grep -A 10 resources
   ```

### Pods Not Starting

1. **Check events**
   ```bash
   kubectl get events -n qawave --sort-by='.lastTimestamp'
   ```

2. **Check resource availability**
   ```bash
   kubectl describe nodes | grep -A 10 "Non-terminated Pods"
   ```

3. **Check PodDisruptionBudget**
   ```bash
   kubectl get pdb -n qawave
   ```

### Scaling Too Slow

1. **Adjust scale-up policy**
   ```yaml
   behavior:
     scaleUp:
       stabilizationWindowSeconds: 30  # Reduce from 60
       policies:
         - type: Percent
           value: 200  # Increase from 100
           periodSeconds: 30
   ```

2. **Pre-scale before expected traffic**
   ```bash
   kubectl scale deployment backend -n qawave --replicas=6
   ```

## Monitoring

### Grafana Dashboards

- **QAWave Overview**: General health and scaling status
- **Kubernetes / Pods**: Pod resource usage

### Key Metrics

```promql
# Replica count
kube_deployment_status_replicas{namespace="qawave"}

# HPA desired replicas
kube_horizontalpodautoscaler_status_desired_replicas{namespace="qawave"}

# Current vs desired
kube_deployment_status_replicas_available{namespace="qawave"} /
kube_deployment_spec_replicas{namespace="qawave"}
```

## Escalation

If scaling issues persist after following this runbook:

1. **Page on-call engineer** if outside business hours
2. **Contact DevOps team lead** for capacity decisions
3. **Involve infrastructure team** for node additions

## Related Runbooks

- [Incident Response](./INCIDENT_RESPONSE.md)
- [Disaster Recovery](./DISASTER_RECOVERY.md)
- [Deploy to Production](./DEPLOY_TO_PRODUCTION.md)
