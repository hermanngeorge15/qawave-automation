# Contributing to QAWave

Thank you for your interest in contributing to QAWave! This document provides guidelines and best practices for contributing to this multi-agent development project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Multi-Agent Coordination](#multi-agent-coordination)

---

## Code of Conduct

This project follows a professional and respectful code of conduct. Please:

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what's best for the project
- Accept feedback gracefully

---

## Getting Started

### Prerequisites

Ensure you have the required tools installed:

```bash
make check-deps
```

See [README.md](./README.md) for detailed prerequisites.

### Setup

```bash
# Clone the repository
git clone https://github.com/your-org/qawave.git
cd qawave

# First-time setup
make setup

# Start development
make up
```

### Verify Setup

```bash
# Check all services are healthy
make health

# Run tests
make test
```

---

## Development Workflow

### 1. Find or Create an Issue

Before starting work:

1. Check existing issues for the work you want to do
2. If no issue exists, create one with:
   - Clear title describing the change
   - Detailed description of the problem/feature
   - Acceptance criteria
   - Relevant labels (especially agent labels)

### 2. Create a Feature Branch

Branch naming convention:

```
feature/{agent}-{short-description}
```

Examples:
- `feature/backend-add-user-auth`
- `feature/frontend-dashboard-widgets`
- `feature/devops-ci-caching`

```bash
git checkout main
git pull origin main
git checkout -b feature/backend-add-user-auth
```

### 3. Make Your Changes

- Write clean, well-documented code
- Follow the coding standards for your language
- Add tests for new functionality
- Update documentation as needed

### 4. Test Your Changes

```bash
# Run all tests
make test

# For specific component tests
make backend-test    # Backend tests
make frontend-test   # Frontend tests
make test-e2e        # E2E tests (requires running services)
```

### 5. Commit Your Changes

Follow the [commit guidelines](#commit-guidelines) below.

### 6. Create a Pull Request

Push your branch and create a PR:

```bash
git push -u origin feature/backend-add-user-auth
```

Then create a PR on GitHub with:
- Clear title following PR format
- Description explaining the changes
- Reference to the issue(s) it addresses
- Test plan

---

## Coding Standards

### Kotlin (Backend)

```kotlin
// Use suspend functions, not Mono/Flux
suspend fun getUser(id: UserId): User

// Use value classes for domain IDs
@JvmInline
value class UserId(val value: String)

// Prefer immutable data classes
data class User(
    val id: UserId,
    val email: String,
    val name: String
)
```

Run linting:
```bash
make backend-lint
make backend-lint-fix  # Auto-fix issues
```

### TypeScript (Frontend)

```typescript
// Strict TypeScript mode
interface User {
  id: string;
  email: string;
  name: string;
}

// Functional components with hooks
function UserProfile({ userId }: { userId: string }) {
  const { data: user } = useUser(userId);
  return <div>{user?.name}</div>;
}
```

Run linting:
```bash
make frontend-lint
make frontend-lint-fix  # Auto-fix issues
```

### General Guidelines

- Keep functions small and focused
- Use meaningful variable and function names
- Write self-documenting code
- Add comments only when necessary (explain "why", not "what")
- Handle errors appropriately
- Don't commit commented-out code

---

## Commit Guidelines

### Format

```
type(scope): description

[optional body]

Refs: #issue-number
Agent: agent-name
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no code change |
| `refactor` | Code refactoring |
| `test` | Adding tests |
| `chore` | Maintenance tasks |
| `ci` | CI/CD changes |

### Examples

```
feat(backend): add user authentication endpoint

Implement JWT-based authentication with refresh tokens.
Includes password hashing with bcrypt.

Refs: #42
Agent: backend
```

```
fix(frontend): resolve dashboard loading state

Dashboard now shows skeleton loader while data is fetching.
Prevents flash of empty state.

Refs: #87
Agent: frontend
```

```
chore(devops): update Docker base images

Upgrade to Node 20 and Java 21 base images for security patches.

Refs: #123
Agent: devops
```

---

## Pull Request Process

### PR Title Format

```
[Agent] type: description (#issue)
```

Examples:
- `[Backend] feat: implement scenario generation service (#42)`
- `[Frontend] fix: resolve dashboard loading state (#87)`
- `[DevOps] chore: update Docker base images (#123)`

### PR Description Template

```markdown
## Summary

Brief description of changes.

## Changes

- List of specific changes made
- Another change
- And another

## Test Plan

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Documentation updated

Closes #123
```

### Review Process

1. **CI Checks**: All automated checks must pass
2. **Code Review**: At least one approval required
3. **QA Agent Review**: Required for all code changes
4. **Security Agent Review**: Required for security-related changes
5. **Merge**: Squash and merge when approved

### Merge Requirements

- All CI checks passing
- Required approvals received
- No merge conflicts
- Branch up-to-date with main

---

## Multi-Agent Coordination

This project uses specialized AI agents for development. Understanding agent responsibilities helps with effective collaboration.

### Agent Ownership

| Agent | Owned Directories | GitHub Label |
|-------|------------------|--------------|
| Orchestrator | `/docs/` | `agent:orchestrator` |
| Backend | `/backend/` | `agent:backend` |
| Frontend | `/frontend/` | `agent:frontend` |
| DevOps | `/infrastructure/`, `/.github/` | `agent:devops` |
| QA | `/e2e-tests/` | `agent:qa` |
| Security | `/security/` | `agent:security` |
| Database | `/backend/src/main/resources/db/` | `agent:database` |

### Cross-Agent Communication

When your work affects multiple agents' domains:

1. Create issues for each affected agent
2. Reference related issues in PRs
3. Use PR comments for coordination
4. Wait for all relevant agent approvals

### Agent-Specific Guidelines

See detailed instructions in:
- [Orchestrator Instructions](./docs/agents/ORCHESTRATOR.md)
- [Backend Instructions](./docs/agents/BACKEND.md)
- [Frontend Instructions](./docs/agents/FRONTEND.md)
- [DevOps Instructions](./docs/agents/DEVOPS.md)
- [QA Instructions](./docs/agents/QA.md)
- [Security Instructions](./docs/agents/SECURITY.md)
- [Database Instructions](./docs/agents/DATABASE.md)

---

## Questions?

- Check the [README.md](./README.md) for setup help
- Review [CLAUDE.md](./CLAUDE.md) for coordination protocol
- Create an issue with label `agent:orchestrator` for project questions

Thank you for contributing to QAWave!
