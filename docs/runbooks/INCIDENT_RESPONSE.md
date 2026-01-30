# Incident Response Runbook

## Overview

This runbook defines the incident response process for QAWave production issues.

## Incident Severity Levels

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| **SEV1** | Complete service outage | Immediate | Site down, data loss |
| **SEV2** | Major feature broken | 15 minutes | Login failing, payments broken |
| **SEV3** | Degraded performance | 1 hour | Slow responses, intermittent errors |
| **SEV4** | Minor issue | 4 hours | UI glitch, non-critical bug |

## Incident Response Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Incident Response Flow                            │
└─────────────────────────────────────────────────────────────────────────┘

    Alert Triggered              Acknowledge                 Investigate
         │                           │                           │
         ▼                           ▼                           ▼
    ┌─────────┐               ┌─────────────┐             ┌─────────────┐
    │ Monitor │──────────────▶│  On-Call    │────────────▶│   Triage    │
    │ Detects │               │  Engineer   │             │   & Assess  │
    └─────────┘               └─────────────┘             └─────────────┘
                                                                 │
         ┌───────────────────────────────────────────────────────┤
         │                                                       │
         ▼                                                       ▼
    ┌─────────────┐                                       ┌─────────────┐
    │   Mitigate  │                                       │  Escalate   │
    │  (Rollback, │                                       │ (if needed) │
    │   Restart)  │                                       └─────────────┘
    └─────────────┘
         │
         ▼
    ┌─────────────┐             ┌─────────────┐             ┌─────────────┐
    │   Resolve   │────────────▶│  Verify &   │────────────▶│    Post-    │
    │             │             │  Monitor    │             │  Incident   │
    └─────────────┘             └─────────────┘             └─────────────┘
```

## Phase 1: Detection & Alerting

### Alert Sources

| Source | Type | Examples |
|--------|------|----------|
| Prometheus | Metrics | High error rate, high latency |
| Application logs | Errors | Exceptions, failures |
| Health checks | Availability | Endpoint failures |
| User reports | Manual | Support tickets |

### On-Call Responsibilities

1. Acknowledge alert within SLA
2. Assess severity
3. Begin investigation
4. Communicate status
5. Escalate if needed

## Phase 2: Triage & Assessment

### Initial Assessment (First 5 minutes)

```bash
# 1. Check service status
curl -s https://qawave.io/health

# 2. Check pods
export KUBECONFIG=~/.kube/config-qawave-prod
kubectl get pods -n production

# 3. Check recent events
kubectl get events -n production --sort-by='.lastTimestamp' | tail -20

# 4. Check logs
kubectl logs deployment/backend -n production --since=10m | tail -100

# 5. Check metrics
# Open Grafana dashboard
```

### Severity Classification

**SEV1 Indicators:**
- Site completely unavailable
- All users affected
- Data integrity at risk
- Security breach

**SEV2 Indicators:**
- Major feature unavailable
- >50% users affected
- No workaround available

**SEV3 Indicators:**
- Feature degraded but usable
- <50% users affected
- Workaround available

**SEV4 Indicators:**
- Minor inconvenience
- Few users affected
- Non-critical functionality

## Phase 3: Investigation

### Common Investigation Steps

#### High Error Rate

```bash
# Check error logs
kubectl logs deployment/backend -n production --since=30m | grep -i error

# Check error metrics
# Prometheus query: rate(http_requests_total{status=~"5.."}[5m])

# Check downstream dependencies
kubectl exec deployment/backend -n production -- curl -s http://postgresql:5432
kubectl exec deployment/backend -n production -- curl -s http://redis:6379
```

#### High Latency

```bash
# Check slow queries
kubectl exec -it postgresql-0 -n production -- psql -U qawave -c \
  "SELECT query, calls, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check resource usage
kubectl top pods -n production

# Check network
kubectl exec deployment/backend -n production -- ping -c 3 postgresql
```

#### Pod Crashes

```bash
# Check crash logs
kubectl logs <pod-name> -n production --previous

# Check events
kubectl describe pod <pod-name> -n production

# Check resource limits
kubectl get pod <pod-name> -n production -o yaml | grep -A 10 resources
```

#### Database Issues

```bash
# Check connections
kubectl exec -it postgresql-0 -n production -- psql -U qawave -c \
  "SELECT count(*) FROM pg_stat_activity;"

# Check locks
kubectl exec -it postgresql-0 -n production -- psql -U qawave -c \
  "SELECT * FROM pg_locks WHERE NOT granted;"

# Check disk space
kubectl exec -it postgresql-0 -n production -- df -h
```

## Phase 4: Mitigation

### Immediate Actions

| Action | When to Use | Command |
|--------|-------------|---------|
| Restart pods | Memory leak, stuck process | `kubectl rollout restart deployment/backend -n production` |
| Rollback | Bad deployment | `kubectl rollout undo deployment/backend -n production` |
| Scale up | Capacity issue | `kubectl scale deployment/backend --replicas=5 -n production` |
| Failover | Node failure | See Disaster Recovery |

### Mitigation Decision Tree

```
Is the issue caused by recent deployment?
├── Yes → Rollback
│         kubectl rollout undo deployment/backend -n production
│
└── No → Is it a resource issue?
         ├── Yes → Scale or increase limits
         │         kubectl scale deployment/backend --replicas=5 -n production
         │
         └── No → Is a single component failing?
                  ├── Yes → Restart that component
                  │         kubectl rollout restart deployment/<name> -n production
                  │
                  └── No → Is it a data issue?
                           ├── Yes → See Database Recovery
                           │
                           └── No → Escalate to senior engineer
```

## Phase 5: Communication

### Internal Communication

**Channel:** Slack #incidents

**Updates:**
- Initial: When incident acknowledged
- Every 30 min: During SEV1/SEV2
- Every 1 hour: During SEV3
- Resolution: When fixed

### External Communication (if needed)

**Status Page Updates:**

```
Title: [Service] experiencing issues

Investigating - We are investigating issues with [service].
Identified - The issue has been identified and we are working on a fix.
Monitoring - A fix has been deployed and we are monitoring.
Resolved - The issue has been resolved.
```

### Communication Templates

**Initial Alert:**
```
:rotating_light: INCIDENT: [Brief description]
Severity: SEV[1-4]
Status: Investigating
Impact: [Who is affected]
IC: @[name]
Thread: [link]
```

**Update:**
```
UPDATE: [Brief description]
Status: [Investigating/Identified/Mitigating/Monitoring]
Progress: [What we've done/found]
Next: [What we're doing next]
ETA: [If known]
```

**Resolution:**
```
:white_check_mark: RESOLVED: [Brief description]
Duration: [Start] - [End]
Impact: [Summary of impact]
Root Cause: [Brief cause]
Follow-up: [Tickets/reviews]
```

## Phase 6: Resolution & Verification

### Verification Checklist

- [ ] Primary symptoms no longer present
- [ ] Monitoring shows normal metrics
- [ ] Health checks passing
- [ ] No new errors in logs
- [ ] User-facing functionality working
- [ ] Related services healthy

### Resolution Steps

```bash
# 1. Verify fix deployed
kubectl get pods -n production
kubectl rollout status deployment/backend -n production

# 2. Check health
curl -s https://qawave.io/health

# 3. Check error rate (should be decreasing)
# Prometheus: rate(http_requests_total{status=~"5.."}[5m])

# 4. Monitor for 15-30 minutes
watch -n 30 'kubectl get pods -n production; echo "---"; curl -s https://qawave.io/health'
```

## Phase 7: Post-Incident

### Immediate (Within 24 hours)

1. Update incident ticket with timeline
2. Communicate resolution to stakeholders
3. Identify any urgent follow-ups

### Post-Incident Review (Within 1 week)

**Attendees:** IC, responders, relevant engineers

**Agenda:**
1. Timeline review
2. What went well
3. What could improve
4. Action items

**Document:**
- Incident summary
- Timeline
- Root cause
- Impact
- Lessons learned
- Action items

### Action Items

Create tickets for:
- [ ] Root cause fix (if not already fixed)
- [ ] Monitoring improvements
- [ ] Runbook updates
- [ ] Process improvements
- [ ] Training needs

## Escalation Matrix

| Severity | 15 min | 30 min | 1 hour | 2 hours |
|----------|--------|--------|--------|---------|
| SEV1 | On-call | DevOps Lead | Engineering Manager | CTO |
| SEV2 | On-call | DevOps Lead | Engineering Manager | - |
| SEV3 | On-call | DevOps Lead | - | - |
| SEV4 | On-call | - | - | - |

## Quick Reference

### Key Commands

```bash
# Health check
curl https://qawave.io/health

# Pods status
kubectl get pods -n production

# Recent logs
kubectl logs deployment/backend -n production --since=10m

# Events
kubectl get events -n production --sort-by='.lastTimestamp'

# Rollback
kubectl rollout undo deployment/backend -n production

# Restart
kubectl rollout restart deployment/backend -n production

# Scale
kubectl scale deployment/backend --replicas=5 -n production
```

### Key Dashboards

- Grafana: Application metrics
- ArgoCD: Deployment status
- Prometheus: Raw metrics

## Related Documentation

- [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md)
- [Disaster Recovery](./DISASTER_RECOVERY.md)
- [Database Backup](./DATABASE_BACKUP.md)
- [Production Environment](../environments/PRODUCTION.md)
