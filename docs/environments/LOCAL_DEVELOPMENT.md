# Local Development Environment

## Overview

The local development environment allows you to run QAWave on your machine for development and testing.

| Property | Value |
|----------|-------|
| **Frontend URL** | http://localhost:5173 |
| **API URL** | http://localhost:8080 |
| **Database** | localhost:5432 |
| **Cost** | €0 |

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Docker | 24.0+ | Container runtime |
| Docker Compose | 2.20+ | Multi-container orchestration |
| Node.js | 20 LTS | Frontend development |
| JDK | 21 | Backend development |
| Git | 2.40+ | Version control |

### Optional Software

| Software | Purpose |
|----------|---------|
| IntelliJ IDEA | Kotlin/Spring development |
| VS Code | Frontend development |
| DBeaver | Database management |
| Redis Insight | Redis debugging |

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/your-org/qawave.git
cd qawave
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, Kafka
docker compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379
- Kafka on port 9092
- Zookeeper on port 2181

### 3. Start Backend

```bash
cd backend

# Run with Gradle
./gradlew bootRun

# Or with specific profile
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Backend starts on http://localhost:8080

### 4. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend starts on http://localhost:5173

## Docker Compose Configuration

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: qawave
      POSTGRES_PASSWORD: qawave-local
      POSTGRES_DB: qawave
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass redis-local

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

volumes:
  postgres_data:
```

### Useful Docker Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v

# Restart specific service
docker compose restart postgres
```

## Environment Variables

### Backend (application-local.yml)

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/qawave
    username: qawave
    password: qawave-local

  redis:
    host: localhost
    port: 6379
    password: redis-local

  kafka:
    bootstrap-servers: localhost:9092
```

### Frontend (.env.local)

```bash
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

## Database Setup

### Initialize Schema

Flyway migrations run automatically on backend startup.

### Manual Database Access

```bash
# Via Docker
docker exec -it qawave-postgres-1 psql -U qawave -d qawave

# Via local psql
psql -h localhost -U qawave -d qawave
```

### Reset Database

```bash
# Drop and recreate
docker compose down
docker volume rm qawave_postgres_data
docker compose up -d
```

## Development Workflow

### Backend Development

```bash
cd backend

# Run tests
./gradlew test

# Run with hot reload
./gradlew bootRun --continuous

# Build
./gradlew build

# Format code
./gradlew ktlintFormat
```

### Frontend Development

```bash
cd frontend

# Development server with hot reload
npm run dev

# Run tests
npm test

# Run tests in watch mode
npm test -- --watch

# Type checking
npm run type-check

# Linting
npm run lint

# Build for production
npm run build
```

### Running E2E Tests Locally

```bash
cd e2e-tests

# Install Playwright browsers
npx playwright install

# Run tests against local environment
npm test

# Run with UI mode
npx playwright test --ui

# Run specific test file
npx playwright test tests/login.spec.ts
```

## IDE Setup

### IntelliJ IDEA (Backend)

1. Import project as Gradle project
2. Set JDK to 21
3. Install Kotlin plugin
4. Enable annotation processing for Lombok

### VS Code (Frontend)

Recommended extensions:
- ESLint
- Prettier
- TypeScript Vue Plugin (Volar)
- Tailwind CSS IntelliSense

`.vscode/settings.json`:
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "typescript.preferences.importModuleSpecifier": "relative"
}
```

## Debugging

### Backend Debugging

IntelliJ IDEA:
1. Create Run/Debug configuration
2. Set breakpoints
3. Run in debug mode (Shift+F9)

VS Code:
```json
// .vscode/launch.json
{
  "configurations": [
    {
      "type": "java",
      "name": "Debug Backend",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

Start backend with debug port:
```bash
./gradlew bootRun --debug-jvm
```

### Frontend Debugging

Use browser DevTools (F12):
- Sources tab for breakpoints
- Network tab for API calls
- React DevTools extension

## Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :8080
lsof -i :5432

# Kill process
kill -9 <PID>
```

### Docker Issues

```bash
# Reset Docker
docker system prune -af
docker volume prune -f

# Restart Docker daemon
# On macOS: Restart Docker Desktop
# On Linux: sudo systemctl restart docker
```

### Database Connection Failed

1. Check Docker is running: `docker ps`
2. Check PostgreSQL logs: `docker compose logs postgres`
3. Verify port is open: `nc -zv localhost 5432`

### Gradle Build Failed

```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Rebuild
./gradlew build --refresh-dependencies
```

### npm Install Failed

```bash
# Clear npm cache
npm cache clean --force
rm -rf node_modules package-lock.json

# Reinstall
npm install
```

## Related Documentation

- [Getting Started Guide](../setup/GETTING_STARTED.md)
- [Technology Stack](../TECH_STACK.md)
- [Backend Development](../agents/BACKEND.md)
- [Frontend Development](../agents/FRONTEND.md)
