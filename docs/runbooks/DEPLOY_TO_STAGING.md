# Deploy to Staging Runbook

## Overview

This runbook describes how to deploy changes to the QAWave staging environment.

**Environment:** staging.qawave.io
**Branch:** develop
**Deployment Method:** GitOps (ArgoCD)

## Pre-Deployment Checklist

- [ ] All CI tests passing on the PR
- [ ] Code review approved
- [ ] No blocking issues in GitHub
- [ ] Staging environment is accessible

## Automatic Deployment (Preferred)

### How It Works

1. PR merged to `develop` branch
2. GitHub Actions triggers build workflow
3. Docker images built and pushed to GHCR
4. ArgoCD detects new images (within 3 minutes)
5. ArgoCD syncs application automatically

### Monitor Automatic Deployment

```bash
# Watch GitHub Actions
gh run list --workflow="Build and Deploy to Staging" --limit 5

# Check ArgoCD sync status
export KUBECONFIG=~/.kube/config-qawave-staging
kubectl get applications -n argocd

# Watch pods
kubectl get pods -n staging -w
```

## Manual Deployment

### Trigger GitHub Actions Manually

```bash
gh workflow run "Build and Deploy to Staging" \
  --ref develop \
  -f deploy_backend=true \
  -f deploy_frontend=true
```

### Force ArgoCD Sync

```bash
# Via kubectl
kubectl patch application qawave-staging -n argocd \
  --type merge -p '{"operation": {"sync": {}}}'

# Via ArgoCD CLI
argocd app sync qawave-staging
```

### Direct kubectl Apply (Emergency Only)

```bash
# Only if ArgoCD is not working
kubectl apply -f gitops/envs/staging/backend/
kubectl apply -f gitops/envs/staging/frontend/
```

## Deployment Steps

### Step 1: Verify Pre-Deployment State

```bash
# Set kubeconfig
export KUBECONFIG=~/.kube/config-qawave-staging

# Check current pod status
kubectl get pods -n staging

# Check current image versions
kubectl get deployment backend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'
kubectl get deployment frontend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'
```

### Step 2: Trigger Deployment

**Option A: Merge PR**
1. Merge approved PR to `develop` branch
2. GitHub Actions starts automatically

**Option B: Manual Trigger**
```bash
gh workflow run "Build and Deploy to Staging" --ref develop
```

### Step 3: Monitor Build

```bash
# Get run ID
gh run list --workflow="Build and Deploy to Staging" --limit 1

# Watch run
gh run watch <run-id>
```

### Step 4: Monitor Rollout

```bash
# Watch pods
kubectl get pods -n staging -w

# Check rollout status
kubectl rollout status deployment/backend -n staging
kubectl rollout status deployment/frontend -n staging
```

### Step 5: Verify Deployment

```bash
# Check new image versions
kubectl get deployment backend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'
kubectl get deployment frontend -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'

# Check pod health
kubectl get pods -n staging

# Check service endpoints
kubectl get endpoints -n staging
```

### Step 6: Run Smoke Tests

```bash
# Health check
curl -s http://46.224.232.46:30000 | head -20

# Backend health (via port-forward)
kubectl port-forward svc/backend 8080:8080 -n staging &
curl http://localhost:8080/actuator/health

# Or run E2E smoke tests
cd e2e-tests
./scripts/run-e2e-tests.sh --smoke
```

## Post-Deployment

### Notify Team

- Update GitHub Issue with deployment status
- Post in team Slack channel if significant changes

### Monitor for Issues

Watch for 15-30 minutes after deployment:
- Error rates in logs
- Pod restarts
- Unusual resource usage

```bash
# Watch logs
kubectl logs -f deployment/backend -n staging

# Watch pod events
kubectl get events -n staging -w
```

## Troubleshooting

### Pods Not Starting

**ImagePullBackOff:**
```bash
# Check image exists
docker pull ghcr.io/hermanngeorge15/qawave-backend:latest

# Check pull secret
kubectl get secret ghcr-secret -n staging

# Recreate if needed
kubectl delete secret ghcr-secret -n staging
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=$GITHUB_USERNAME \
  --docker-password=$GITHUB_TOKEN \
  -n staging
```

**CrashLoopBackOff:**
```bash
# Check logs
kubectl logs <pod-name> -n staging --previous

# Check events
kubectl describe pod <pod-name> -n staging
```

### Deployment Stuck

```bash
# Check rollout status
kubectl rollout status deployment/backend -n staging --timeout=60s

# Describe deployment
kubectl describe deployment backend -n staging

# Check if blocked by PDB
kubectl get pdb -n staging
```

### ArgoCD Not Syncing

```bash
# Check ArgoCD application status
kubectl describe application qawave-staging -n argocd

# Force hard refresh
kubectl patch application qawave-staging -n argocd \
  --type merge -p '{"metadata": {"annotations": {"argocd.argoproj.io/refresh": "hard"}}}'
```

## Rollback

If deployment causes issues, see [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md).

**Quick Rollback:**
```bash
# Rollback to previous version
kubectl rollout undo deployment/backend -n staging
kubectl rollout undo deployment/frontend -n staging
```

## Related Documentation

- [Staging Environment](../environments/STAGING.md)
- [Rollback Deployment](./ROLLBACK_DEPLOYMENT.md)
- [GitHub Actions Workflow](.github/workflows/build-and-deploy.yml)
