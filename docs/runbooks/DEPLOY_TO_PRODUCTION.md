# Deploy to Production Runbook

## Overview

This runbook describes the process for deploying changes to the QAWave production environment.

**Environment:** qawave.io
**Branch:** main
**Deployment Method:** GitOps (ArgoCD) with manual approval

> **Warning:** Production deployments require careful planning and approval. Follow this runbook exactly.

## Pre-Deployment Checklist

### Required Approvals

- [ ] **QA Sign-off:** All staging E2E tests passing
- [ ] **Code Review:** At least 2 approvals on PR
- [ ] **Security Review:** (if security-related changes)
- [ ] **Database Review:** (if migration included)

### Verification

- [ ] Changes tested on staging for at least 24 hours
- [ ] No regression issues reported
- [ ] Rollback plan documented
- [ ] On-call team notified

### Communication

- [ ] Deployment window agreed with team
- [ ] Stakeholders notified
- [ ] Status page updated (if maintenance window)

## Deployment Process

### Phase 1: Preparation (30 min before)

#### 1.1 Verify Staging Health

```bash
export KUBECONFIG=~/.kube/config-qawave-staging

# Check all pods healthy
kubectl get pods -n staging

# Verify services responding
curl -s -o /dev/null -w "%{http_code}" http://46.224.232.46:30000

# Check recent logs for errors
kubectl logs --since=1h deployment/backend -n staging | grep -i error
```

#### 1.2 Review Changes

```bash
# List commits since last production deploy
git log main..develop --oneline

# Review changed files
git diff main..develop --stat
```

#### 1.3 Document Current State

```bash
export KUBECONFIG=~/.kube/config-qawave-prod

# Record current versions
kubectl get deployment backend -n production -o jsonpath='{.spec.template.spec.containers[0].image}' > /tmp/prod-backend-version.txt
kubectl get deployment frontend -n production -o jsonpath='{.spec.template.spec.containers[0].image}' > /tmp/prod-frontend-version.txt

echo "Pre-deployment versions:"
cat /tmp/prod-backend-version.txt
cat /tmp/prod-frontend-version.txt
```

### Phase 2: Deployment

#### 2.1 Merge to Main

```bash
# Create release PR
gh pr create --base main --head develop --title "Release: $(date +%Y-%m-%d)" --body "## Changes
[List of changes]

## Testing
- [ ] Staging E2E tests passed
- [ ] Manual testing completed

## Rollback Plan
kubectl rollout undo deployment/backend -n production
kubectl rollout undo deployment/frontend -n production
"

# After approvals, merge
gh pr merge --merge
```

#### 2.2 Monitor Build

```bash
# Watch GitHub Actions
gh run list --workflow="Build and Deploy" --branch main --limit 1
gh run watch <run-id>
```

#### 2.3 Trigger Production Sync

ArgoCD requires manual sync for production:

```bash
# Via ArgoCD CLI
argocd app sync qawave-production --prune

# Or via kubectl
kubectl patch application qawave-production -n argocd \
  --type merge -p '{"operation": {"sync": {"prune": true}}}'
```

#### 2.4 Monitor Rollout

```bash
export KUBECONFIG=~/.kube/config-qawave-prod

# Watch rollout
kubectl rollout status deployment/backend -n production
kubectl rollout status deployment/frontend -n production

# Watch pods
kubectl get pods -n production -w
```

### Phase 3: Verification (15 min after)

#### 3.1 Health Checks

```bash
# Frontend health
curl -s -o /dev/null -w "%{http_code}" https://qawave.io

# API health
curl -s https://api.qawave.io/actuator/health

# Check all pods running
kubectl get pods -n production
```

#### 3.2 Smoke Tests

```bash
# Run production smoke tests
cd e2e-tests
ENVIRONMENT=production npx playwright test tests/smoke/ --config=playwright.prod.config.ts
```

#### 3.3 Monitor Metrics

- Check Grafana dashboards for:
  - Request rate
  - Error rate
  - Response latency
  - Resource usage

### Phase 4: Post-Deployment

#### 4.1 Communication

- Update deployment ticket/issue
- Notify team of successful deployment
- Update status page (if was in maintenance)

#### 4.2 Monitoring (Next 24 hours)

- Watch error rates
- Monitor user feedback
- Check support channels

## Rollback Procedure

If issues occur, initiate rollback immediately.

### Quick Rollback

```bash
export KUBECONFIG=~/.kube/config-qawave-prod

# Rollback deployments
kubectl rollout undo deployment/backend -n production
kubectl rollout undo deployment/frontend -n production

# Verify rollback
kubectl rollout status deployment/backend -n production
kubectl rollout status deployment/frontend -n production
```

### ArgoCD Rollback

```bash
# List history
argocd app history qawave-production

# Rollback to specific revision
argocd app rollback qawave-production <revision>
```

See [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md) for detailed rollback procedures.

## Emergency Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| On-call Engineer | PagerDuty | Immediate |
| DevOps Lead | Slack/Phone | 15 min |
| Engineering Manager | Phone | 30 min |

## Deployment Windows

| Day | Time (UTC) | Notes |
|-----|------------|-------|
| Mon-Thu | 14:00-16:00 | Preferred |
| Friday | AVOID | No deploys |
| Weekend | AVOID | Emergency only |

## Troubleshooting

### Deployment Stuck

```bash
# Check events
kubectl get events -n production --sort-by='.lastTimestamp'

# Check deployment status
kubectl describe deployment backend -n production
```

### Database Migration Failed

```bash
# Check migration status
kubectl logs job/db-migration -n production

# Manual rollback if needed
kubectl exec -it postgresql-0 -n production -- psql -U qawave -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### Service Unavailable

```bash
# Check pods
kubectl get pods -n production

# Check endpoints
kubectl get endpoints -n production

# Check ingress
kubectl describe ingress -n production
```

## Related Documentation

- [Production Environment](../environments/PRODUCTION.md)
- [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md)
- [Incident Response](./INCIDENT_RESPONSE.md)
- [Disaster Recovery](./DISASTER_RECOVERY.md)
