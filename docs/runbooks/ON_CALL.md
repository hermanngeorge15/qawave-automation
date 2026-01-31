# On-Call Procedures

This document outlines on-call responsibilities, escalation paths, and procedures for QAWave operations.

## Table of Contents

1. [On-Call Rotation](#on-call-rotation)
2. [Responsibilities](#responsibilities)
3. [Alert Response](#alert-response)
4. [Escalation Procedures](#escalation-procedures)
5. [Communication](#communication)
6. [Tools and Access](#tools-and-access)
7. [Handoff Procedures](#handoff-procedures)

## On-Call Rotation

### Schedule

- **Primary On-Call**: Responds to all alerts
- **Secondary On-Call**: Backup if primary is unavailable
- **Rotation Period**: Weekly, Monday 9:00 AM to Monday 9:00 AM
- **Schedule Location**: PagerDuty / OpsGenie

### Expectations

- Response time: 15 minutes for Critical, 30 minutes for Warning
- Availability: Must have laptop and internet access
- Handoff: Complete handoff document before rotation ends

## Responsibilities

### During On-Call Shift

1. **Monitor Alerts**
   - Acknowledge alerts within SLA
   - Investigate and resolve or escalate
   - Document actions taken

2. **System Health**
   - Check Grafana dashboards daily
   - Review deployment status
   - Monitor error rates and latency

3. **Respond to Incidents**
   - Follow incident response runbook
   - Communicate status updates
   - Create post-mortems for major incidents

4. **Handoff**
   - Document ongoing issues
   - Update runbooks if needed
   - Brief incoming on-call

## Alert Response

### Alert Severity Levels

| Severity | Response Time | Examples |
|----------|---------------|----------|
| Critical (P1) | 15 min | Service down, data loss risk |
| High (P2) | 30 min | Degraded performance, high error rate |
| Medium (P3) | 2 hours | Non-critical service issues |
| Low (P4) | Next business day | Warnings, capacity alerts |

### Initial Response Steps

1. **Acknowledge the alert**
   ```
   Acknowledge in PagerDuty/OpsGenie immediately
   ```

2. **Assess severity**
   - Is the service down?
   - Are users impacted?
   - Is data at risk?

3. **Check dashboards**
   - Grafana: https://grafana.qawave.io
   - ArgoCD: https://argocd.qawave.io
   - Prometheus: https://prometheus.qawave.io

4. **Gather information**
   ```bash
   # Quick status check
   kubectl get pods -n qawave
   kubectl get events -n qawave --sort-by='.lastTimestamp' | tail -20
   kubectl logs -n qawave -l app.kubernetes.io/name=backend --tail=50
   ```

5. **Follow relevant runbook**
   - [Incident Response](./INCIDENT_RESPONSE.md)
   - [Scaling](./SCALING.md)
   - [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md)

### Common Alerts and Actions

#### QAWaveBackendDown
```bash
# Check pod status
kubectl get pods -n qawave -l app.kubernetes.io/name=backend

# Check logs
kubectl logs -n qawave -l app.kubernetes.io/name=backend --tail=100

# Check events
kubectl describe pod -n qawave -l app.kubernetes.io/name=backend

# If pods are crashing, consider rollback
kubectl rollout undo deployment/backend -n qawave
```

#### QAWaveBackendHighErrorRate
```bash
# Check recent deployments
kubectl rollout history deployment/backend -n qawave

# Check application logs for errors
kubectl logs -n qawave -l app.kubernetes.io/name=backend | grep ERROR | tail -50

# Check database connectivity
kubectl exec -it postgresql-0 -n qawave -- pg_isready
```

#### PostgreSQLDown
```bash
# Check PostgreSQL pod
kubectl get pods -n qawave -l app.kubernetes.io/name=postgresql

# Check PVC status
kubectl get pvc -n qawave

# Attempt restart
kubectl delete pod postgresql-0 -n qawave
```

## Escalation Procedures

### Escalation Matrix

| Condition | Escalate To | Contact Method |
|-----------|-------------|----------------|
| Cannot resolve in 30 min | Secondary On-Call | PagerDuty |
| Service down > 1 hour | Engineering Lead | Phone/Slack |
| Data loss suspected | CTO | Phone immediately |
| Security incident | Security Team | Dedicated channel |

### When to Escalate

- Issue beyond your expertise
- Resolution taking longer than expected
- Multiple services affected
- Customer impact significant
- Security implications

### How to Escalate

1. **Document what you've tried**
   - Actions taken
   - Error messages
   - Relevant logs

2. **Page the escalation target**
   - Use PagerDuty escalation policy
   - Or call directly for Critical issues

3. **Brief the escalation**
   - What triggered the alert
   - What you've investigated
   - Current status
   - What help you need

## Communication

### Status Updates

During an incident, post updates every 15-30 minutes:

**Template**:
```
[TIMESTAMP] QAWave Incident Update

Status: Investigating / Identified / Monitoring / Resolved
Impact: [Description of user impact]
Current Actions: [What we're doing]
Next Update: [Time]
```

### Communication Channels

| Channel | Use For |
|---------|---------|
| #qawave-incidents | Real-time incident discussion |
| #qawave-ops | General operations |
| Status Page | Customer-facing updates |
| Email | Post-mortems, summaries |

### Stakeholder Communication

For P1/P2 incidents affecting customers:

1. Notify Customer Success within 30 minutes
2. Update status page
3. Prepare customer communication if needed

## Tools and Access

### Required Access

Before going on-call, ensure you have:

- [ ] Kubernetes cluster access (kubectl configured)
- [ ] Grafana login
- [ ] ArgoCD login
- [ ] PagerDuty/OpsGenie account
- [ ] Slack access to incident channels
- [ ] VPN access (if required)
- [ ] SSH keys for emergency access

### Access Commands

```bash
# Set up kubectl context
export KUBECONFIG=~/.kube/qawave-prod-config

# Verify access
kubectl get nodes

# Port forward Grafana (if ingress down)
kubectl port-forward svc/kube-prometheus-stack-grafana -n monitoring 3000:80
```

### Emergency Access

If normal access is unavailable:

1. SSH to control plane: `ssh root@<control-plane-ip>`
2. Use local kubectl: `k0s kubectl get pods -A`

## Handoff Procedures

### End of Shift Handoff

Complete before rotation ends:

1. **Update handoff document**
   - Ongoing issues
   - Recent incidents
   - Scheduled maintenance
   - Anything unusual

2. **Brief incoming on-call**
   - 15-minute sync call or async update
   - Walk through open issues
   - Highlight anything to watch

3. **Clear your alerts**
   - Resolve or hand off acknowledged alerts
   - Update alert notes with context

### Handoff Document Template

```markdown
## On-Call Handoff: [Date]

### Ongoing Issues
- [Issue description, status, next steps]

### Recent Incidents
- [Brief summary of incidents in past week]

### Scheduled Changes
- [Upcoming deployments, maintenance windows]

### Notes
- [Anything else incoming on-call should know]

### Open Alerts
- [List of acknowledged but unresolved alerts]
```

## Self-Care

On-call can be stressful. Remember:

- Take breaks when possible
- Hand off if you're overwhelmed
- Don't skip meals or sleep for non-critical issues
- Debrief after major incidents
- Provide feedback to improve processes

## Related Documents

- [Incident Response](./INCIDENT_RESPONSE.md)
- [Scaling](./SCALING.md)
- [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md)
- [Disaster Recovery](./DISASTER_RECOVERY.md)
