# Disaster Recovery Runbook

## Overview

This runbook covers disaster recovery procedures for QAWave infrastructure.

## Recovery Objectives

| Metric | Target | Maximum |
|--------|--------|---------|
| **RTO** (Recovery Time Objective) | 2 hours | 4 hours |
| **RPO** (Recovery Point Objective) | 1 hour | 4 hours |

## Disaster Scenarios

| Scenario | Severity | RTO | Procedure |
|----------|----------|-----|-----------|
| Single pod failure | Low | Auto-heal | Kubernetes handles |
| Single node failure | Medium | 15 min | Node replacement |
| Control plane failure | High | 1 hour | CP restoration |
| Full cluster failure | Critical | 2 hours | Full rebuild |
| Data corruption | Critical | 2-4 hours | PITR |
| Datacenter outage | Critical | 4 hours | Failover |

## Incident Classification

### Severity Levels

| Level | Description | Response Time |
|-------|-------------|---------------|
| SEV1 | Complete outage | Immediate |
| SEV2 | Major degradation | < 15 min |
| SEV3 | Partial degradation | < 1 hour |
| SEV4 | Minor issue | Next business day |

## Recovery Procedures

### Scenario 1: Single Node Failure

**Detection:**
```bash
kubectl get nodes
# Node shows NotReady status
```

**Recovery:**
```bash
# 1. Cordon the failing node
kubectl cordon <node-name>

# 2. Drain workloads
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# 3. Replace node via Terraform
cd infrastructure/terraform/environments/production
terraform apply -target=hcloud_server.worker[X]

# 4. Join new node to cluster
# (automated via cloud-init or manual k0s join)

# 5. Uncordon and verify
kubectl uncordon <new-node-name>
kubectl get nodes
```

### Scenario 2: Control Plane Failure

**Detection:**
```bash
kubectl get nodes
# Connection refused or timeout
```

**Recovery:**
```bash
# 1. SSH to control plane
ssh -i ~/.ssh/qawave-prod root@<cp-ip>

# 2. Check K0s status
k0s status
systemctl status k0scontroller

# 3. Restart K0s
systemctl restart k0scontroller

# 4. If K0s corrupted, restore from backup
# Stop K0s
systemctl stop k0scontroller

# Restore etcd
k0s restore /backups/etcd-snapshot-latest.db

# Start K0s
systemctl start k0scontroller

# 5. Verify cluster
kubectl get nodes
kubectl get pods -A
```

### Scenario 3: Full Cluster Rebuild

**When needed:**
- Complete infrastructure loss
- Unrecoverable corruption
- Security compromise requiring full rebuild

**Recovery Steps:**

```bash
# 1. Provision new infrastructure
cd infrastructure/terraform/environments/production
terraform init
terraform apply

# 2. Get new node IPs
CP_IP=$(terraform output -raw control_plane_ip)

# 3. Bootstrap K0s
ssh root@$CP_IP 'curl -sSLf https://get.k0s.sh | sudo sh'
ssh root@$CP_IP 'k0s install controller --single'
ssh root@$CP_IP 'k0s start'

# 4. Get kubeconfig
ssh root@$CP_IP 'k0s kubeconfig admin' | sed "s/10.0.1.10/$CP_IP/" > kubeconfig
export KUBECONFIG=$(pwd)/kubeconfig

# 5. Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 6. Configure ArgoCD with Git repo
argocd app create qawave-production \
  --repo https://github.com/org/qawave.git \
  --path gitops/envs/production \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace production

# 7. Sync applications
argocd app sync qawave-production

# 8. Restore database from backup
kubectl exec -i postgresql-0 -n production -- psql -U qawave -d qawave < /backups/latest.sql

# 9. Verify
kubectl get pods -A
curl https://qawave.io/health
```

### Scenario 4: Database Recovery

**Point-in-Time Recovery (PITR):**

```bash
# 1. Stop application writes
kubectl scale deployment backend --replicas=0 -n production

# 2. Connect to PostgreSQL
kubectl exec -it postgresql-0 -n production -- bash

# 3. Stop PostgreSQL
pg_ctl stop -D /var/lib/postgresql/data

# 4. Restore base backup
rm -rf /var/lib/postgresql/data/*
tar -xzf /backups/base-backup-latest.tar.gz -C /var/lib/postgresql/data

# 5. Configure recovery
cat > /var/lib/postgresql/data/recovery.signal << EOF
EOF

cat >> /var/lib/postgresql/data/postgresql.conf << EOF
restore_command = 'cp /backups/wal/%f %p'
recovery_target_time = '2024-01-15 14:30:00 UTC'
EOF

# 6. Start PostgreSQL
pg_ctl start -D /var/lib/postgresql/data

# 7. Wait for recovery
tail -f /var/lib/postgresql/data/log/postgresql.log

# 8. After recovery complete, restart application
kubectl scale deployment backend --replicas=3 -n production
```

### Scenario 5: Datacenter Failover

> Note: Requires multi-region setup (future enhancement)

**Current single-region recovery:**

1. Create new cluster in different datacenter
2. Restore from latest backup
3. Update DNS to new IPs
4. Verify functionality

## Pre-Disaster Preparation

### Daily Checklist

- [ ] Verify backup completed successfully
- [ ] Check backup integrity (automated)
- [ ] Verify monitoring is operational
- [ ] Check alert configurations

### Weekly Checklist

- [ ] Test backup restoration (to test env)
- [ ] Verify all runbooks are current
- [ ] Check infrastructure state matches Terraform
- [ ] Review access credentials

### Monthly Checklist

- [ ] Full disaster recovery drill
- [ ] Update emergency contact list
- [ ] Review and update this runbook
- [ ] Test communication channels

## Emergency Contacts

| Role | Primary | Backup |
|------|---------|--------|
| On-call Engineer | PagerDuty | Slack |
| DevOps Lead | [Phone] | [Email] |
| CTO | [Phone] | [Email] |

## Communication Templates

### Initial Incident

```
INCIDENT: [Brief description]
SEVERITY: [SEV1-4]
STATUS: Investigating
IMPACT: [User impact description]
NEXT UPDATE: [Time]
```

### Status Update

```
INCIDENT UPDATE: [Brief description]
STATUS: [Investigating/Identified/Monitoring/Resolved]
CURRENT STATE: [What's happening now]
NEXT STEPS: [What we're doing]
NEXT UPDATE: [Time]
```

### Resolution

```
INCIDENT RESOLVED: [Brief description]
DURATION: [Start time] - [End time]
ROOT CAUSE: [Brief cause]
FOLLOW-UP: Post-incident review scheduled for [Date]
```

## Recovery Verification Checklist

- [ ] All nodes showing Ready
- [ ] All pods running (no CrashLoopBackOff)
- [ ] All services have endpoints
- [ ] Health checks passing
- [ ] External access working
- [ ] Database connections working
- [ ] Cache (Redis) operational
- [ ] Message queue (Kafka) operational
- [ ] Monitoring receiving data
- [ ] Alerts configured and working
- [ ] Smoke tests passing

## Post-Incident Actions

1. **Immediate (within 24 hours)**
   - Document timeline of events
   - Identify root cause
   - Implement immediate fixes
   - Communicate to stakeholders

2. **Short-term (within 1 week)**
   - Conduct post-incident review
   - Create follow-up tickets
   - Update runbooks if needed
   - Share learnings with team

3. **Long-term (within 1 month)**
   - Implement preventive measures
   - Update monitoring/alerting
   - Conduct additional DR drills
   - Review and improve process

## Related Documentation

- [Database Backup](./DATABASE_BACKUP.md)
- [Incident Response](./INCIDENT_RESPONSE.md)
- [Production Environment](../environments/PRODUCTION.md)
- [Terraform Guide](../setup/TERRAFORM_GUIDE.md)
