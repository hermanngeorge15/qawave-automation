# Rollback Deployment Runbook

## Overview

This runbook describes how to rollback a deployment when issues are detected after release.

## When to Rollback

Initiate rollback if you observe:
- Error rate > 5% for more than 5 minutes
- Response time degradation > 100% (doubled)
- Critical functionality broken
- Data corruption risk
- Security vulnerability discovered

## Decision Tree

```
Issue Detected
      │
      ▼
┌─────────────────────┐
│ Is service working  │───Yes───▶ Monitor closely
│ for most users?     │           Consider hotfix
└─────────────────────┘
      │ No
      ▼
┌─────────────────────┐
│ Is issue fixable    │───Yes───▶ Deploy hotfix
│ in < 30 minutes?    │           (if confident)
└─────────────────────┘
      │ No
      ▼
  ROLLBACK NOW
```

## Rollback Methods

### Method 1: Kubernetes Rollback (Fastest)

```bash
# Set environment
export KUBECONFIG=~/.kube/config-qawave-staging  # or -prod

# Rollback to previous revision
kubectl rollout undo deployment/backend -n staging
kubectl rollout undo deployment/frontend -n staging

# Verify rollback started
kubectl rollout status deployment/backend -n staging
kubectl rollout status deployment/frontend -n staging
```

**Rollback to specific revision:**
```bash
# View history
kubectl rollout history deployment/backend -n staging

# Rollback to specific revision
kubectl rollout undo deployment/backend -n staging --to-revision=5
```

### Method 2: ArgoCD Rollback

```bash
# View deployment history
argocd app history qawave-staging

# Example output:
# ID  DATE                    REVISION
# 0   2024-01-15 10:00:00     abc123
# 1   2024-01-15 14:00:00     def456  <-- Current (broken)

# Rollback to previous revision
argocd app rollback qawave-staging 0

# Force sync to previous state
argocd app sync qawave-staging --revision abc123
```

### Method 3: Git Revert (For GitOps)

```bash
# Find the breaking commit
git log --oneline -10

# Revert the breaking commit
git revert <commit-hash>

# Push to trigger deployment
git push origin develop  # or main for production

# ArgoCD will automatically sync
```

## Rollback Procedures by Component

### Backend Rollback

```bash
# Check current state
kubectl get deployment backend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'

# Rollback
kubectl rollout undo deployment/backend -n staging

# Verify
kubectl rollout status deployment/backend -n staging
kubectl get pods -l app=backend -n staging

# Check health
kubectl port-forward svc/backend 8080:8080 -n staging &
curl http://localhost:8080/actuator/health
```

### Frontend Rollback

```bash
# Check current state
kubectl get deployment frontend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'

# Rollback
kubectl rollout undo deployment/frontend -n staging

# Verify
kubectl rollout status deployment/frontend -n staging
curl -s -o /dev/null -w "%{http_code}" http://46.224.232.46:30000
```

### Database Migration Rollback

> **Warning:** Database rollbacks can cause data loss. Proceed with caution.

```bash
# Check current migration version
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -d qawave -c \
  "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# If Flyway undo migrations available:
kubectl exec -it <backend-pod> -n staging -- java -jar app.jar flyway:undo

# Manual SQL rollback (prepare script in advance):
kubectl exec -it postgresql-0 -n staging -- psql -U qawave -d qawave < rollback.sql
```

## Post-Rollback Actions

### 1. Verify Service Health

```bash
# Check all pods
kubectl get pods -n staging

# Check services
kubectl get svc -n staging

# Check endpoints
kubectl get endpoints -n staging

# Run smoke tests
cd e2e-tests && ./scripts/run-e2e-tests.sh --smoke
```

### 2. Communication

- Notify team of rollback
- Update incident ticket
- Update status page (if applicable)

### 3. Root Cause Analysis

- Gather logs from the broken deployment
- Document what went wrong
- Create follow-up ticket for fix

```bash
# Get logs from previous pod
kubectl logs <pod-name> -n staging --previous > broken-deployment-logs.txt

# Get events
kubectl get events -n staging --sort-by='.lastTimestamp' > events.txt
```

### 4. Prevent Re-deployment

If using ArgoCD, disable auto-sync temporarily:

```bash
argocd app set qawave-staging --sync-policy none
```

Or set deployment to manual:
```bash
kubectl patch application qawave-staging -n argocd \
  --type merge -p '{"spec": {"syncPolicy": null}}'
```

## Rollback Timelines

| Action | Target Time |
|--------|-------------|
| Issue detected → Decision to rollback | < 5 min |
| Start rollback command | < 1 min |
| Rollback complete | < 5 min |
| Verification complete | < 10 min |
| **Total** | **< 20 min** |

## Troubleshooting

### Rollback Command Fails

```bash
# Force delete stuck pods
kubectl delete pods -l app=backend -n staging --grace-period=0 --force

# Scale down then up
kubectl scale deployment backend --replicas=0 -n staging
kubectl scale deployment backend --replicas=2 -n staging
```

### No Previous Revision Available

```bash
# Check revision history
kubectl rollout history deployment/backend -n staging

# If no history, manually set image to known good version
kubectl set image deployment/backend backend=ghcr.io/org/backend:v1.2.3 -n staging
```

### ArgoCD Out of Sync

```bash
# Disable sync policy
argocd app set qawave-staging --sync-policy none

# Manual kubectl rollback
kubectl rollout undo deployment/backend -n staging

# Later, re-enable sync (after fix is ready)
argocd app set qawave-staging --sync-policy automated
```

## Rollback Verification Checklist

- [ ] All pods running and healthy
- [ ] No error spikes in logs
- [ ] Health endpoints returning 200
- [ ] Key functionality working (manual test)
- [ ] Smoke tests passing
- [ ] Error rate back to baseline
- [ ] Response time back to baseline

## Related Documentation

- [Deploy to Staging](./DEPLOY_TO_STAGING.md)
- [Deploy to Production](./DEPLOY_TO_PRODUCTION.md)
- [Incident Response](./INCIDENT_RESPONSE.md)
