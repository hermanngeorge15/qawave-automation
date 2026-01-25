# Agent Coordination Protocol

This document defines how the AI agents communicate and coordinate when developing QAWave.

## Communication Channels

### 1. GitHub Issues
- **Purpose**: Task assignment, bug reports, feature requests
- **Format**: Use issue templates with agent labels

### 2. Pull Request Comments
- **Purpose**: Code review, agent-to-agent communication
- **Format**: Start comments with `@agent:{name}` for directed messages

### 3. Commit Messages
- **Purpose**: Track changes and agent attribution
- **Format**: Include `Agent: {name}` in commit body

## Issue Templates

### Task Assignment Template
```markdown
---
name: Task Assignment
labels: ['agent:{assigned-agent}', 'priority:{level}']
---

## Task Description
[Clear description of what needs to be done]

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Technical Notes
[Any relevant technical context]

## Dependencies
- Depends on: #{issue-number} (if any)
- Blocks: #{issue-number} (if any)

## Assigned Agent
**Primary**: {agent-name}
**Reviewers**: {agent-names}

## Deadline
{date or sprint number}
```

### Bug Report Template
```markdown
---
name: Bug Report
labels: ['bug', 'triage']
---

## Bug Description
[What's happening vs what should happen]

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
[What should happen]

## Actual Behavior
[What actually happens]

## Environment
- Browser/OS:
- Backend version:
- Frontend version:

## Logs/Screenshots
[Attach relevant logs or screenshots]
```

## PR Review Protocol

### PR Title Format
```
[{Agent}] {type}: {description} (#{issue})
```

Examples:
- `[Backend] feat: add scenario generation endpoint (#42)`
- `[Frontend] fix: correct date formatting in run list (#51)`
- `[DevOps] chore: update Terraform provider versions (#60)`

### PR Description Template
```markdown
## Summary
[Brief description of changes]

## Changes
- Change 1
- Change 2
- Change 3

## Related Issue
Closes #{issue-number}

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No security vulnerabilities introduced

## Agent
**Author**: {agent-name}
**Reviewers Needed**: @qa-agent, @{other-agent}
```

### Review Requirements by Change Type

| Change Type | Required Reviewers |
|-------------|-------------------|
| Backend API | QA Agent, Security Agent |
| Frontend UI | QA Agent |
| Database Schema | Backend Agent, Security Agent |
| Infrastructure | Security Agent |
| Security Config | Backend Agent, DevOps Agent |
| CI/CD Workflow | Security Agent |

## Merge Criteria

A PR can be merged when:

1. **CI Checks**: All GitHub Actions workflows pass
2. **QA Approval**: QA Agent has approved
3. **Security Approval**: Security Agent has approved (if applicable)
4. **No Conflicts**: PR has no merge conflicts
5. **Coverage**: Test coverage hasn't decreased

## Deployment Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Deployment Pipeline                          │
└─────────────────────────────────────────────────────────────────┘

  PR Created          PR Merged           E2E Pass           Release
      │                   │                   │                  │
      ▼                   ▼                   ▼                  ▼
  ┌───────┐          ┌───────┐          ┌───────┐          ┌───────┐
  │  CI   │────────▶ │Staging│────────▶ │  QA   │────────▶ │  Prod │
  │ Tests │          │Deploy │          │ Tests │          │Deploy │
  └───────┘          └───────┘          └───────┘          └───────┘
      │                   │                   │                  │
      ▼                   ▼                   ▼                  ▼
  All agents         ArgoCD auto         QA Agent           Orchestrator
  via CI             syncs               runs E2E           creates tag
```

## Agent Status Updates

Agents should update issue status by:

1. **Picking up task**: Comment "Starting work on this" + move to "In Progress"
2. **Blocked**: Comment with blocker + add `status:blocked` label
3. **PR Ready**: Comment with PR link + add `status:in-review` label
4. **Completed**: PR merged + close issue

## Conflict Resolution

When multiple agents need to modify the same file:

1. **Prevention**: Follow CODEOWNERS - each agent owns specific directories
2. **Detection**: CI will fail if PR conflicts with main
3. **Resolution**:
   - Lower priority agent rebases on main
   - If both critical, Orchestrator decides priority
   - Document decision in PR comments

## Emergency Procedures

### Critical Bug in Production

1. QA Agent creates issue with `priority:critical` label
2. Orchestrator immediately assigns to relevant agent
3. Agent creates hotfix branch from main
4. Fast-track review (single reviewer OK)
5. Merge and deploy immediately
6. Post-mortem issue created

### Failed Deployment

1. DevOps Agent triggers ArgoCD rollback
2. Creates incident issue
3. Notifies all agents via issue comment
4. Root cause analysis assigned

## Daily Sync

Orchestrator creates daily status issue:

```markdown
# Daily Sync - {Date}

## Completed
- [x] #{issue} - {description} (@{agent})

## In Progress
- [ ] #{issue} - {description} (@{agent}) - {status}

## Blocked
- ⚠️ #{issue} - {reason} - needs @{agent}

## Today's Priorities
1. #{issue} - @{agent}
2. #{issue} - @{agent}

## Notes
{Any important announcements}
```

## Sprint Ceremonies

### Sprint Planning (Orchestrator leads)
- Review backlog
- Assign issues to agents
- Set sprint goals
- Create sprint milestone

### Sprint Review (All agents)
- Demo completed work
- Discuss blockers
- Update documentation

### Retrospective (All agents)
- What went well
- What to improve
- Action items

## Labels Reference

| Label | Description |
|-------|-------------|
| `agent:orchestrator` | Orchestrator Agent task |
| `agent:backend` | Backend Agent task |
| `agent:frontend` | Frontend Agent task |
| `agent:devops` | DevOps Agent task |
| `agent:qa` | QA Agent task |
| `agent:security` | Security Agent task |
| `agent:database` | Database Agent task |
| `priority:critical` | Immediate attention |
| `priority:high` | This sprint |
| `priority:medium` | Backlog |
| `priority:low` | Nice to have |
| `status:blocked` | Cannot proceed |
| `status:in-review` | PR submitted |
| `status:approved` | Ready to merge |
| `type:bug` | Bug fix |
| `type:feature` | New feature |
| `type:docs` | Documentation |
| `type:refactor` | Code improvement |
