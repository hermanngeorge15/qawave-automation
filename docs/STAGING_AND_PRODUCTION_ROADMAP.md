# QAWave Staging & Production Roadmap

## Table of Contents
- [On-Demand Staging Environment](#on-demand-staging-environment)
- [Production Readiness TODOs](#production-readiness-todos)

---

## On-Demand Staging Environment

### Goal
Spin up a complete staging environment on Hetzner VPS with a single command (or minimal commands).

### Prerequisites

Before running the setup, ensure you have:

- [ ] Hetzner Cloud account with API token
- [ ] GitHub account with PAT (scopes: `read:packages`, `write:packages`)
- [ ] SSH key pair for VPS access
- [ ] Domain (optional, for DNS setup)
- [ ] Local tools installed:
  - `terraform` >= 1.5
  - `kubectl` >= 1.28
  - `helm` >= 3.12
  - `hcloud` CLI (optional)

### Quick Start

```bash
# 1. Set environment variables
export HCLOUD_TOKEN="your-hetzner-api-token"
export GITHUB_TOKEN="your-github-pat"
export GITHUB_USERNAME="hermanngeorge15"

# 2. Run the staging setup script
./scripts/setup-staging.sh
```

### What the Setup Script Does

1. **Provision Infrastructure** (Terraform)
   - Creates Hetzner VPS (CX21 or CX31)
   - Configures firewall rules
   - Sets up SSH access

2. **Install Kubernetes** (K0s)
   - Single-node K0s cluster
   - Configures kubectl context

3. **Deploy Core Services** (Helm/kubectl)
   - PostgreSQL database
   - Redis cache
   - Kafka message broker

4. **Configure GitOps** (ArgoCD)
   - Installs ArgoCD
   - Configures application manifests
   - Sets up auto-sync

5. **Setup Image Pull Secrets**
   - Creates GHCR authentication secret

6. **Deploy Applications**
   - Backend (Spring Boot)
   - Frontend (React/Nginx)

### Manual Step-by-Step Setup

If you prefer manual control, follow these steps:

#### Step 1: Provision VPS with Terraform

```bash
cd infrastructure/terraform/environments/staging

# Initialize Terraform
terraform init

# Review the plan
terraform plan -var="hcloud_token=$HCLOUD_TOKEN"

# Apply
terraform apply -var="hcloud_token=$HCLOUD_TOKEN"

# Get the VPS IP
export VPS_IP=$(terraform output -raw vps_ip)
echo "VPS IP: $VPS_IP"
```

#### Step 2: Install K0s on VPS

```bash
# SSH into VPS
ssh root@$VPS_IP

# Install K0s (on VPS)
curl -sSLf https://get.k0s.sh | sudo sh
sudo k0s install controller --single
sudo k0s start

# Get kubeconfig (on VPS)
sudo k0s kubeconfig admin > /root/kubeconfig

# Exit and copy kubeconfig locally
exit
scp root@$VPS_IP:/root/kubeconfig ~/.kube/config-staging

# Set context
export KUBECONFIG=~/.kube/config-staging
kubectl get nodes
```

#### Step 3: Create Staging Namespace

```bash
kubectl create namespace staging
```

#### Step 4: Deploy Infrastructure Services

```bash
# PostgreSQL
kubectl apply -f gitops/envs/staging/postgresql/

# Redis
kubectl apply -f gitops/envs/staging/redis/

# Kafka (optional - if needed)
kubectl apply -f gitops/envs/staging/kafka/
```

#### Step 5: Create Image Pull Secret

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=$GITHUB_USERNAME \
  --docker-password=$GITHUB_TOKEN \
  -n staging
```

#### Step 6: Deploy Applications

```bash
# Deploy backend
kubectl apply -f gitops/envs/staging/backend/

# Deploy frontend
kubectl apply -f gitops/envs/staging/frontend/
```

#### Step 7: Verify Deployment

```bash
# Check all pods
kubectl get pods -n staging

# Check services
kubectl get svc -n staging

# Test frontend (NodePort 30000)
curl http://$VPS_IP:30000

# Test backend health
kubectl exec -it deploy/backend -n staging -- wget -qO- http://localhost:8080/actuator/health
```

### Teardown Staging

```bash
# Option 1: Delete applications only (keep infrastructure)
kubectl delete -f gitops/envs/staging/backend/
kubectl delete -f gitops/envs/staging/frontend/

# Option 2: Destroy entire VPS
cd infrastructure/terraform/environments/staging
terraform destroy -var="hcloud_token=$HCLOUD_TOKEN"
```

### Staging Environment Endpoints

| Service | URL | Notes |
|---------|-----|-------|
| Frontend | `http://<VPS_IP>:30000` | React app |
| Backend API | `http://<VPS_IP>:30080` | Spring Boot (if exposed) |
| ArgoCD UI | `http://<VPS_IP>:30443` | GitOps dashboard |

---

## Production Readiness TODOs

### Priority: Critical (P0)

#### Security
- [ ] **TLS/HTTPS Setup**
  - Install cert-manager
  - Configure Let's Encrypt ClusterIssuer
  - Create Ingress with TLS termination
  - Redirect HTTP to HTTPS

- [ ] **Secrets Management**
  - Install Sealed Secrets or External Secrets Operator
  - Encrypt all secrets in Git
  - Rotate database passwords
  - Rotate API tokens

- [ ] **Network Policies**
  - Restrict pod-to-pod communication
  - Allow only necessary ingress/egress
  - Isolate namespaces

- [ ] **RBAC Configuration**
  - Create service accounts per application
  - Limit permissions to least privilege
  - Disable default service account auto-mount

#### Reliability
- [ ] **Database Backups**
  - Configure automated PostgreSQL backups
  - Test restore procedure
  - Off-site backup storage (S3-compatible)
  - Backup retention policy (30 days)

- [ ] **High Availability**
  - Multi-replica deployments (backend: 2+, frontend: 2+)
  - Pod Disruption Budgets
  - Anti-affinity rules
  - Database replication (if budget allows)

- [ ] **Resource Limits**
  - Set CPU/memory requests and limits for all pods
  - Configure LimitRange for namespace
  - Set ResourceQuota for namespace

### Priority: High (P1)

#### Observability
- [ ] **Monitoring Stack**
  - Install Prometheus (or VictoriaMetrics)
  - Install Grafana with dashboards
  - Configure alerting rules
  - Setup PagerDuty/Slack integration

- [ ] **Logging Stack**
  - Install Loki or ELK stack
  - Configure log aggregation
  - Set log retention policy
  - Create log-based alerts

- [ ] **Tracing**
  - Install Jaeger or Tempo
  - Configure OpenTelemetry in backend
  - Trace sampling configuration

- [ ] **Application Metrics**
  - Expose Prometheus metrics from backend
  - Create SLI/SLO dashboards
  - Error rate alerting
  - Latency percentile tracking

#### CI/CD Improvements
- [ ] **Image Tagging Strategy**
  - Use semantic versioning (not `latest`)
  - Tag with git SHA
  - Implement image promotion (staging -> prod)

- [ ] **Deployment Strategy**
  - Configure rolling updates
  - Set maxSurge and maxUnavailable
  - Implement canary deployments (optional)
  - Add rollback automation

- [ ] **Environment Promotion**
  - Create production overlay
  - Separate ArgoCD applications per env
  - Manual approval gate for production

### Priority: Medium (P2)

#### Infrastructure
- [ ] **DNS Configuration**
  - Register domain
  - Configure A/CNAME records
  - Setup external-dns for automatic DNS

- [ ] **CDN Setup**
  - Configure Cloudflare or similar
  - Cache static assets
  - DDoS protection

- [ ] **Load Balancer**
  - Replace NodePort with LoadBalancer or Ingress
  - Configure health checks
  - Session affinity (if needed)

#### Operations
- [ ] **Documentation**
  - Runbook for common issues
  - Architecture diagrams
  - Incident response procedures
  - On-call rotation setup

- [ ] **Disaster Recovery**
  - Document RTO/RPO requirements
  - Create DR plan
  - Test failover procedures
  - Multi-region consideration

- [ ] **Cost Optimization**
  - Right-size VPS instances
  - Implement auto-scaling (if needed)
  - Review resource utilization monthly

### Priority: Low (P3)

#### Nice to Have
- [ ] **GitOps Enhancements**
  - ArgoCD notifications
  - Slack deployment notifications
  - Automated diff comments on PRs

- [ ] **Developer Experience**
  - Preview environments per PR
  - Local development with Tilt/Skaffold
  - Database seeding scripts

- [ ] **Compliance**
  - Security scanning in CI (Trivy)
  - Dependency vulnerability scanning
  - License compliance checking
  - Audit logging

---

## Implementation Timeline Suggestion

| Phase | Items | Estimated Effort |
|-------|-------|------------------|
| Phase 1 | TLS, Secrets, Backups | 1-2 weeks |
| Phase 2 | Monitoring, Logging | 1 week |
| Phase 3 | HA, Network Policies | 1 week |
| Phase 4 | CI/CD Improvements | 1 week |
| Phase 5 | Documentation, DR | Ongoing |

---

## Scripts to Create

The following scripts should be created in `/scripts/`:

| Script | Purpose |
|--------|---------|
| `setup-staging.sh` | One-command staging environment setup |
| `teardown-staging.sh` | Destroy staging environment |
| `backup-db.sh` | Manual database backup |
| `restore-db.sh` | Database restore from backup |
| `rotate-secrets.sh` | Rotate all secrets |
| `health-check.sh` | Verify all services are healthy |

---

## References

- [K0s Documentation](https://docs.k0sproject.io/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Hetzner Cloud API](https://docs.hetzner.cloud/)
- [cert-manager](https://cert-manager.io/docs/)
- [Sealed Secrets](https://sealed-secrets.netlify.app/)
