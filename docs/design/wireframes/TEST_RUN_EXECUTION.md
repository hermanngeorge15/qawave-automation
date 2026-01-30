# QAWave Test Run Execution UI Design

> Design specifications for test execution, progress tracking, and results display.

## Run Initiation

### Run Configuration Modal

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│         ┌────────────────────────────────────────────────────────┐          │
│         │                                                    [X] │          │
│         │  Run Tests                                             │          │
│         │  ────────────────────────────────────────────────────  │          │
│         │                                                        │          │
│         │  Package: User API Tests                               │          │
│         │  Scenarios: 10 scenarios selected                      │          │
│         │                                                        │          │
│         │  Base URL *                                            │          │
│         │  ┌──────────────────────────────────────────────────┐  │          │
│         │  │ https://api.staging.example.com                  │  │          │
│         │  └──────────────────────────────────────────────────┘  │          │
│         │  [Use default] [Use production] [Use staging]          │          │
│         │                                                        │          │
│         │  Options                                               │          │
│         │  ┌──────────────────────────────────────────────────┐  │          │
│         │  │ ☑ Stop on first failure                         │  │          │
│         │  │ ☐ Run scenarios in parallel                      │  │          │
│         │  │ ☐ Include AI regeneration                        │  │          │
│         │  └──────────────────────────────────────────────────┘  │          │
│         │                                                        │          │
│         │  ────────────────────────────────────────────────────  │          │
│         │                                                        │          │
│         │                         [Cancel]  [▶ Start Run]        │          │
│         │                                                        │          │
│         └────────────────────────────────────────────────────────┘          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Run Progress View

### Live Progress Display

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  ← Back to Package                                                           │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                        │ │
│  │  Run #42                                                 [RUNNING]     │ │
│  │  User API Tests                                                        │ │
│  │  ─────────────────────────────────────────────────────────────────     │ │
│  │                                                                        │ │
│  │  Started: Jan 30, 2026 14:30:00                                        │ │
│  │  Elapsed: 00:01:23                                                     │ │
│  │  Base URL: https://api.staging.example.com                             │ │
│  │                                                                        │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │ │
│  │  │                                                                 │  │ │
│  │  │  Progress: 6/10 scenarios                                       │  │ │
│  │  │  ████████████████████████████████████████░░░░░░░░░░░░░░░░  60%  │  │ │
│  │  │                                                                 │  │ │
│  │  │  ✓ Passed: 5    ✗ Failed: 1    ⏳ Running: 1    ○ Pending: 3   │  │ │
│  │  │                                                                 │  │ │
│  │  └─────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                        │ │
│  │  [■ Stop Run]                                                          │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  LIVE PROGRESS                                                               │
│  ─────────────                                                               │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ✓  Create User                                               245ms    │ │
│  │    POST /api/users → 201 Created                                      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ✓  Get User by ID                                             89ms    │ │
│  │    GET /api/users/{id} → 200 OK                                       │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ✗  Update User Email                                          1.2s    │ │
│  │    PATCH /api/users/{id} → Expected 200, got 500                      │ │
│  │    ⚠ Response: {"error": "ValidationError", "message": "..."}         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ⏳ Delete User                                              Running... │ │
│  │    DELETE /api/users/{id}                                             │ │
│  │    ████████████████████░░░░░░░░░░ Step 2/3                           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ○  List Users                                               Pending   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ○  Search Users                                             Pending   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Progress Components

### Overall Progress Bar

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Progress: 6/10 scenarios                                       │
│  ███████████████████████████████░░░░░░░░░░░░░░░░░░░  60%       │
│                                                                 │
│  Estimated remaining: ~2 minutes                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Status Counters

```
┌───────────────────────────────────────────────────────────────────────┐
│                                                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │     ✓       │  │     ✗       │  │     ⏳      │  │     ○       │  │
│  │     5       │  │     1       │  │     1       │  │     3       │  │
│  │   Passed    │  │   Failed    │  │   Running   │  │   Pending   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### Scenario Progress Item

```
Running state:
┌────────────────────────────────────────────────────────────────────────┐
│ ⏳ Delete User                                              Running... │
│    DELETE /api/users/{id}                                              │
│    ████████████████████░░░░░░░░░░ Step 2/3                            │
│                                                                        │
│    Current: Executing DELETE request...                                │
└────────────────────────────────────────────────────────────────────────┘

Completed (success):
┌────────────────────────────────────────────────────────────────────────┐
│ ✓ Create User                                                   245ms │
│   POST /api/users → 201 Created                                        │
└────────────────────────────────────────────────────────────────────────┘

Completed (failed):
┌────────────────────────────────────────────────────────────────────────┐
│ ✗ Update User Email                                             1.2s │
│   PATCH /api/users/{id} → Expected 200, got 500                        │
│   ⚠ Response: {"error": "ValidationError", "message": "..."}           │
│                                                                        │
│   [View Details] [Copy cURL]                                           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Run Completion

### Success State

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                        │ │
│  │                            ✓                                           │ │
│  │                                                                        │ │
│  │                   Run Completed Successfully                           │ │
│  │                                                                        │ │
│  │                   8/10 scenarios passed (80%)                          │ │
│  │                   Duration: 45.2 seconds                               │ │
│  │                                                                        │ │
│  │         [View Results]    [Run Again]    [Export Report]               │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Failure State

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                        │ │
│  │                            ✗                                           │ │
│  │                                                                        │ │
│  │                   Run Completed with Failures                          │ │
│  │                                                                        │ │
│  │                   3/10 scenarios passed (30%)                          │ │
│  │                   7 scenarios failed                                   │ │
│  │                                                                        │ │
│  │         [View Failures]   [Retry Failed]   [Export Report]             │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  TOP FAILURES                                                                │
│  ────────────                                                                │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ✗ Update User Email                                                    │ │
│  │   Expected: 200 OK                                                     │ │
│  │   Actual: 500 Internal Server Error                                    │ │
│  │   Message: ValidationError - Email format invalid                      │ │
│  │                                                      [View Details →]  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ ✗ Delete User                                                          │ │
│  │   Expected: 204 No Content                                             │ │
│  │   Actual: 403 Forbidden                                                │ │
│  │   Message: Insufficient permissions                                    │ │
│  │                                                      [View Details →]  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Step Detail View

### Expandable Step Details

```
┌────────────────────────────────────────────────────────────────────────────┐
│ [▼] Step 2: PATCH /api/users/{id}                               ✗ Failed │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Request                              Response                             │
│  ───────                              ────────                             │
│                                                                            │
│  PATCH /api/users/abc-123             HTTP 500 Internal Server Error       │
│                                                                            │
│  Headers:                             Headers:                             │
│  ┌──────────────────────────┐        ┌──────────────────────────────┐     │
│  │ Content-Type: app/json  │        │ Content-Type: application/json│     │
│  │ Authorization: Bearer...│        │ X-Request-Id: xyz-789        │     │
│  └──────────────────────────┘        └──────────────────────────────┘     │
│                                                                            │
│  Body:                                Body:                                │
│  ┌──────────────────────────┐        ┌──────────────────────────────┐     │
│  │ {                        │        │ {                            │     │
│  │   "email": "new@test.com"│        │   "error": "ValidationError",│     │
│  │ }                        │        │   "message": "Email format   │     │
│  │                    [Copy]│        │              invalid",       │     │
│  └──────────────────────────┘        │   "path": "/email"           │     │
│                                      │ }                    [Copy] │     │
│                                      └──────────────────────────────┘     │
│                                                                            │
│  Assertions                                                                │
│  ──────────                                                                │
│  ✗ Status code equals 200                                                  │
│    Expected: 200                                                           │
│    Actual: 500                                                             │
│                                                                            │
│  ─ Body field "email" equals "new@test.com"                               │
│    Skipped: Previous assertion failed                                      │
│                                                                            │
│  Timing                                                                    │
│  ──────                                                                    │
│  Duration: 1.2s                                                            │
│  DNS Lookup: 12ms                                                          │
│  TCP Connect: 45ms                                                         │
│  TLS Handshake: 89ms                                                       │
│  First Byte: 1.1s                                                          │
│                                                                            │
│  [Copy cURL]  [Copy Request]  [Copy Response]  [Report Bug]                │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Real-time Updates

### WebSocket/Polling Events

| Event | UI Update |
|-------|-----------|
| `scenario_started` | Move to "Running" state with progress |
| `step_completed` | Update step status inline |
| `scenario_completed` | Update scenario status and counters |
| `run_completed` | Show completion state |
| `run_failed` | Show failure state with details |

### Optimistic Updates

```typescript
// Scenario state progression
interface ScenarioProgress {
  status: 'pending' | 'running' | 'passed' | 'failed';
  currentStep?: number;
  totalSteps: number;
  duration?: number;
  error?: string;
}
```

---

## Stop Run Confirmation

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                [X] │
│  Stop Run?                                                         │
│  ─────────────────────────────────────────────────────────────     │
│                                                                    │
│  Are you sure you want to stop this run?                           │
│                                                                    │
│  • 6 scenarios have completed                                      │
│  • 4 scenarios will be cancelled                                   │
│  • Results will be saved for completed scenarios                   │
│                                                                    │
│                               [Keep Running]  [Stop Run]           │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## Retry Failed Scenarios

### Retry Selection

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                [X] │
│  Retry Failed Scenarios                                            │
│  ─────────────────────────────────────────────────────────────     │
│                                                                    │
│  Select scenarios to retry:                                        │
│                                                                    │
│  ☑ Update User Email (failed: 500 error)                          │
│  ☑ Delete User (failed: 403 forbidden)                            │
│  ☐ Bulk Update (failed: timeout)                                   │
│                                                                    │
│  ─────────────────────────────────────────────────────────────     │
│                                                                    │
│  Base URL:                                                         │
│  ┌──────────────────────────────────────────────────────────┐     │
│  │ https://api.staging.example.com                          │     │
│  └──────────────────────────────────────────────────────────┘     │
│                                                                    │
│  ☐ Use same extracted values from original run                     │
│                                                                    │
│                              [Cancel]  [▶ Retry 2 Scenarios]       │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## Visual Specifications

### Status Colors

| Status | Background | Text | Border | Icon |
|--------|------------|------|--------|------|
| Pending | neutral-50 | neutral-500 | neutral-200 | Circle |
| Running | info-50 | info-700 | info-200 | Loader (animated) |
| Passed | success-50 | success-700 | success-200 | CheckCircle |
| Failed | danger-50 | danger-700 | danger-200 | XCircle |

### Animation Specs

```css
/* Progress bar animation */
.progress-bar {
  transition: width 500ms ease-out;
}

/* Running spinner */
.spinner {
  animation: spin 1s linear infinite;
}

/* New item fade-in */
.scenario-item {
  animation: fadeInUp 300ms ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

---

*Last Updated: January 2026*
*Version: 1.0*
