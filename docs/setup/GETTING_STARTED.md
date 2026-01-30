# Getting Started with QAWave Development

Welcome to QAWave! This guide will help you set up your development environment and make your first contribution.

## Prerequisites

Before you begin, ensure you have:

| Requirement | Minimum Version | Check Command |
|-------------|-----------------|---------------|
| Git | 2.40+ | `git --version` |
| Docker | 24.0+ | `docker --version` |
| Docker Compose | 2.20+ | `docker compose version` |
| Node.js | 20 LTS | `node --version` |
| npm | 10+ | `npm --version` |
| JDK | 21 | `java --version` |

## Quick Setup (5 minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/qawave.git
cd qawave
```

### 2. Start Infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, and Kafka.

### 3. Start Backend

```bash
cd backend
./gradlew bootRun
```

Backend runs on http://localhost:8080

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173

### 5. Verify Setup

Open http://localhost:5173 in your browser. You should see the QAWave login page.

## Project Structure

```
qawave/
├── backend/              # Kotlin/Spring WebFlux API
│   ├── src/main/kotlin/  # Application code
│   ├── src/test/         # Unit tests
│   └── build.gradle.kts  # Gradle config
│
├── frontend/             # React/TypeScript SPA
│   ├── src/              # Application code
│   ├── tests/            # Component tests
│   └── package.json      # npm config
│
├── e2e-tests/            # Playwright E2E tests
│   ├── tests/            # Test files
│   └── playwright.config.ts
│
├── infrastructure/       # DevOps configs
│   ├── terraform/        # IaC for Hetzner
│   └── kubernetes/       # K8s manifests
│
├── docs/                 # Documentation
│   ├── agents/           # Agent instructions
│   ├── environments/     # Env documentation
│   ├── setup/            # Setup guides
│   └── runbooks/         # Operational runbooks
│
└── docker-compose.yml    # Local development services
```

## Development Workflow

### 1. Create a Branch

```bash
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Backend: Edit files in `backend/src/main/kotlin/`
- Frontend: Edit files in `frontend/src/`
- Tests: Add tests alongside your changes

### 3. Run Tests

```bash
# Backend tests
cd backend && ./gradlew test

# Frontend tests
cd frontend && npm test

# E2E tests (requires running services)
cd e2e-tests && npm test
```

### 4. Commit Changes

```bash
git add .
git commit -m "feat(scope): description of changes

Refs: #issue-number
Agent: your-name"
```

### 5. Push and Create PR

```bash
git push -u origin feature/your-feature-name
gh pr create --title "[Agent] type: description" --body "Closes #issue"
```

## Commit Message Format

```
type(scope): description

[optional body]

Refs: #issue-number
Agent: agent-name
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance
- `ci` - CI/CD changes

**Examples:**
```
feat(backend): add user authentication endpoint

Refs: #42
Agent: backend

fix(frontend): resolve login button not clickable

Closes #55
Agent: frontend
```

## Running Specific Services

### Backend Only

```bash
# Start dependencies
docker compose up -d postgres redis

# Run backend
cd backend && ./gradlew bootRun
```

### Frontend Only (Mock API)

```bash
cd frontend
VITE_MOCK_API=true npm run dev
```

### Database Only

```bash
docker compose up -d postgres

# Connect
psql -h localhost -U qawave -d qawave
```

## Environment Variables

### Backend

Create `backend/src/main/resources/application-local.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/qawave
    username: qawave
    password: qawave-local
```

### Frontend

Create `frontend/.env.local`:

```bash
VITE_API_URL=http://localhost:8080
```

## IDE Setup

### IntelliJ IDEA (Recommended for Backend)

1. File → Open → Select `backend` folder
2. Import as Gradle project
3. Set Project SDK to JDK 21
4. Install Kotlin plugin

### VS Code (Recommended for Frontend)

1. File → Open Folder → Select `frontend`
2. Install recommended extensions when prompted
3. Run `npm install` in terminal

## Common Issues

### Docker Not Running

```bash
# macOS: Start Docker Desktop
# Linux:
sudo systemctl start docker
```

### Port Already in Use

```bash
# Find and kill process on port
lsof -i :8080
kill -9 <PID>
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
docker compose ps

# View logs
docker compose logs postgres
```

### npm Install Fails

```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

## Next Steps

1. **Read the architecture docs**: [TECH_STACK.md](../TECH_STACK.md)
2. **Understand the agents**: [docs/agents/](../agents/)
3. **Pick up an issue**: Check GitHub Issues
4. **Join discussions**: GitHub Discussions

## Getting Help

- **Documentation issues**: Create issue with `docs` label
- **Setup problems**: Ask in GitHub Discussions
- **Bug reports**: Create issue with reproduction steps

## Related Documentation

- [Local Development](../environments/LOCAL_DEVELOPMENT.md)
- [Technology Stack](../TECH_STACK.md)
- [Coordination Protocol](../COORDINATION_PROTOCOL.md)
