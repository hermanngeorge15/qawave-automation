# QAWave Error Detail View UI Design

> Comprehensive wireframes for viewing and analyzing test execution errors.

## Table of Contents

1. [Overview](#overview)
2. [Error Summary Card](#error-summary-card)
3. [Error Detail Page](#error-detail-page)
4. [Request/Response Viewer](#requestresponse-viewer)
5. [Assertion Failure Detail](#assertion-failure-detail)
6. [Error Timeline](#error-timeline)
7. [Similar Errors](#similar-errors)
8. [Responsive Layouts](#responsive-layouts)
9. [Component Specifications](#component-specifications)

---

## Overview

### Purpose

The Error Detail View helps users:
- Understand why a test scenario failed
- Inspect request/response data
- View assertion details and actual vs expected
- Track error history and patterns
- Debug and fix test issues

### Error Categories

| Category | Description | Icon |
|----------|-------------|------|
| Assertion | Assertion check failed | [✗] |
| Timeout | Request timed out | [⏱] |
| Network | Connection/network error | [⚡] |
| Auth | Authentication/authorization error | [🔒] |
| Server | 5xx server error | [☁] |
| Validation | Request validation failed | [!] |

---

## Error Summary Card

### Compact Error Card (in Run Results List)

```
+---------------------------------------------------------------+
| [✗] Assertion Failed                              Step 3 of 5  |
| GET /api/users/123 - Get user by ID                           |
+---------------------------------------------------------------+
|                                                                |
| Expected status 200, got 404                                   |
|                                                                |
| Response time: 234ms                                           |
|                                                                |
|                                          [View Error Details →]|
+---------------------------------------------------------------+
```

### Expanded Error Card

```
+---------------------------------------------------------------+
| [✗] Assertion Failed                              Step 3 of 5  |
| Scenario: Get user by ID - success case                        |
| Endpoint: GET /api/users/123                                   |
+---------------------------------------------------------------+
|                                                                |
| ASSERTION DETAILS                                              |
| +-----------------------------------------------------------+ |
| | Type: Status Code                                         | |
| | Expected: 200                                             | |
| | Actual: 404                                               | |
| +-----------------------------------------------------------+ |
|                                                                |
| RESPONSE PREVIEW                                               |
| +-----------------------------------------------------------+ |
| | {                                                         | |
| |   "error": "NOT_FOUND",                                   | |
| |   "message": "User with ID 123 not found"                 | |
| | }                                                         | |
| +-----------------------------------------------------------+ |
|                                                                |
| Response time: 234ms | Response size: 128 bytes               |
|                                                                |
| [Copy Response] [View Full Details →]                          |
+---------------------------------------------------------------+
```

---

## Error Detail Page

### Full Error Detail Layout

```
+------------------------------------------------------------------+
| [← Back to Run #a1b2c3d4]                           [Re-run Step]|
+------------------------------------------------------------------+
|                                                                   |
| +---------------------------------------------------------------+ |
| |                                                               | |
| |  [✗] Assertion Failed                                         | |
| |                                                               | |
| |  Scenario: Get user by ID - success case                      | |
| |  Step 3 of 5: HTTP Request                                    | |
| |                                                               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------+ +---------------+ +---------------+             |
| | STATUS        | | RESPONSE TIME | | RETRIES       |             |
| | 404 Not Found | | 234ms         | | 0 of 3        |             |
| +---------------+ +---------------+ +---------------+             |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| [Request] [Response] [Assertions] [Trace] [History]               |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Assertions (1 failed, 2 passed)                                   |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✗] Status Code                                               | |
| +---------------------------------------------------------------+ |
| |                                                               | |
| |  Expected        │  Actual                                    | |
| | ─────────────────┼─────────────────                           | |
| |  200             │  404                                       | |
| |  (OK)            │  (Not Found)                               | |
| |                                                               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✓] Response Time                                             | |
| +---------------------------------------------------------------+ |
| |  Expected: < 500ms  │  Actual: 234ms  │  Passed              | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✓] Content-Type Header                                       | |
| +---------------------------------------------------------------+ |
| |  Expected: application/json  │  Actual: application/json     | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| POSSIBLE CAUSES                                 [AI Suggestions] |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [i] The user with ID 123 may not exist in the test database  | |
| |                                                               | |
| |     • Check if test data setup created the user              | |
| |     • Verify the user ID is correct in the scenario          | |
| |     • Check if previous test deleted the user                | |
| |                                                               | |
| |                                    [Create Test Data Fix →]   | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Request/Response Viewer

### Request Tab

```
+------------------------------------------------------------------+
| Request                                                    [Copy] |
+------------------------------------------------------------------+
|                                                                   |
| [GET] /api/users/123                                              |
|                                                                   |
| Base URL: https://api.example.com                                 |
|                                                                   |
+------------------------------------------------------------------+
| Headers                                              [Show All ▼] |
+------------------------------------------------------------------+
| +---------------------------------------------------------------+ |
| | Authorization    │ Bearer eyJhbGciOiJIUzI1NiIs...            | |
| | Content-Type     │ application/json                          | |
| | Accept           │ application/json                          | |
| | X-Request-ID     │ req_abc123def456                          | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
| Query Parameters                                                  |
+------------------------------------------------------------------+
| +---------------------------------------------------------------+ |
| | include          │ profile,settings                          | |
| | fields           │ id,name,email                             | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
| Body                                                 (No body)    |
+------------------------------------------------------------------+
```

### Response Tab

```
+------------------------------------------------------------------+
| Response                                                   [Copy] |
+------------------------------------------------------------------+
|                                                                   |
| Status: [404 Not Found]         Time: 234ms        Size: 128 B   |
|                                                                   |
+------------------------------------------------------------------+
| Headers                                              [Show All ▼] |
+------------------------------------------------------------------+
| +---------------------------------------------------------------+ |
| | Content-Type     │ application/json                          | |
| | X-Request-ID     │ req_abc123def456                          | |
| | X-RateLimit-Rem  │ 99                                        | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
| Body                                           [Raw] [Formatted] |
+------------------------------------------------------------------+
| +---------------------------------------------------------------+ |
| | 1  │ {                                                        | |
| | 2  │   "error": "NOT_FOUND",                                  | |
| | 3  │   "message": "User with ID 123 not found",               | |
| | 4  │   "details": {                                           | |
| | 5  │     "resource": "User",                                  | |
| | 6  │     "identifier": "123"                                  | |
| | 7  │   },                                                     | |
| | 8  │   "timestamp": "2026-01-30T14:32:15Z"                    | |
| | 9  │ }                                                        | |
| +---------------------------------------------------------------+ |
|                                                                   |
| [Download Response] [Copy as cURL]                               |
+------------------------------------------------------------------+
```

### Split View (Request | Response)

```
+------------------------------------------------------------------+
| Request                     │ Response                            |
+-----------------------------+------------------------------------+
|                             │                                    |
| [GET] /api/users/123        │ Status: [404]         Time: 234ms  |
|                             │                                    |
| Headers                     │ Headers                            |
| +-------------------------+ │ +--------------------------------+ |
| | Authorization: Bearer...| │ | Content-Type: application/json | |
| | Accept: application/json| │ | X-Request-ID: req_abc123...    | |
| +-------------------------+ │ +--------------------------------+ |
|                             │                                    |
| Query Params                │ Body                               |
| +-------------------------+ │ +--------------------------------+ |
| | include: profile        | │ | {                              | |
| | fields: id,name,email   | │ |   "error": "NOT_FOUND",        | |
| +-------------------------+ │ |   "message": "User with..."    | |
|                             │ | }                              | |
| Body: (none)                │ +--------------------------------+ |
|                             │                                    |
+-----------------------------+------------------------------------+
```

---

## Assertion Failure Detail

### Comparison View

```
+------------------------------------------------------------------+
| Assertion: Status Code                                     [✗]    |
+------------------------------------------------------------------+
|                                                                   |
|          EXPECTED                    ACTUAL                       |
|  +---------------------+    +---------------------+               |
|  |                     |    |                     |               |
|  |        200          |    |        404          |               |
|  |        (OK)         |    |     (Not Found)     |               |
|  |                     |    |                     |               |
|  +---------------------+    +---------------------+               |
|                  ↑                     ↑                          |
|              Expected              Received                       |
|                                                                   |
+------------------------------------------------------------------+
```

### JSON Body Comparison

```
+------------------------------------------------------------------+
| Assertion: Response Body Match                             [✗]    |
+------------------------------------------------------------------+
|                                                                   |
|          EXPECTED                    ACTUAL                       |
|  +---------------------------+  +---------------------------+     |
|  | {                        |  | {                        |     |
|  |   "id": "123",           |  |   "id": "123",           |     |
|  |   "name": "John Doe",    |  |   "name": "Jane Doe",    | ← ! |
|  |   "email": "john@..."    |  |   "email": "jane@..."    | ← ! |
|  |   "status": "active"     |  |   "status": "active"     |     |
|  | }                        |  | }                        |     |
|  +---------------------------+  +---------------------------+     |
|                                                                   |
| Differences (2):                                                  |
| • Line 3: name - expected "John Doe", got "Jane Doe"             |
| • Line 4: email - expected "john@...", got "jane@..."            |
|                                                                   |
+------------------------------------------------------------------+
```

### JSONPath Assertion

```
+------------------------------------------------------------------+
| Assertion: JSONPath $.data.users[0].id                     [✗]    |
+------------------------------------------------------------------+
|                                                                   |
| Path: $.data.users[0].id                                          |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Response Body (highlighted path):                             | |
| | {                                                             | |
| |   "data": {                                                   | |
| |     "users": [                                                | |
| |       {                                                       | |
| |         "id": [→ "456" ←],  // Expected: "123"                | |
| |         "name": "Jane Doe"                                    | |
| |       }                                                       | |
| |     ]                                                         | |
| |   }                                                           | |
| | }                                                             | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Expected: "123"                                                   |
| Actual:   "456"                                                   |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Error Timeline

### Step Execution Timeline

```
+------------------------------------------------------------------+
| Execution Timeline                                                |
+------------------------------------------------------------------+
|                                                                   |
| 0ms        100ms       200ms       300ms       400ms       500ms |
| |-----------|-----------|-----------|-----------|-----------|    |
|                                                                   |
| Step 1: Setup                                                     |
| [▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 85ms ✓  |
|                                                                   |
| Step 2: Auth                                                      |
| [░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 120ms ✓ |
|                                                                   |
| Step 3: HTTP Request  ← Failed                                    |
| [░░░░░░░░░░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░] 234ms ✗ |
|                                                                   |
| Step 4: (not executed)                                            |
| [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] -- ○     |
|                                                                   |
| Step 5: (not executed)                                            |
| [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] -- ○     |
|                                                                   |
| Total: 439ms (3 of 5 steps executed)                              |
|                                                                   |
+------------------------------------------------------------------+
```

### Error History

```
+------------------------------------------------------------------+
| Error History (This scenario)                      [View Trends]  |
+------------------------------------------------------------------+
|                                                                   |
| Last 10 runs:  ✓ ✓ ✓ ✗ ✗ ✓ ✓ ✓ ✗ ✗                               |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Jan 30, 14:32 │ [✗] 404 Not Found │ Same error    [View]      | |
| | Jan 30, 12:15 │ [✗] 404 Not Found │ Same error    [View]      | |
| | Jan 30, 10:00 │ [✓] Passed        │ 200 OK        [View]      | |
| | Jan 29, 18:45 │ [✓] Passed        │ 200 OK        [View]      | |
| | Jan 29, 14:30 │ [✓] Passed        │ 200 OK        [View]      | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Pattern: Started failing after Jan 30, 12:00                      |
| [Compare with last passing run →]                                 |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Similar Errors

### Related Errors Panel

```
+------------------------------------------------------------------+
| Similar Errors in This Run                                        |
+------------------------------------------------------------------+
|                                                                   |
| 3 other scenarios have similar 404 errors:                        |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✗] Get user profile                                          | |
| |     GET /api/users/123/profile                                | |
| |     404 Not Found - Same user ID                              | |
| |                                              [View Details →] | |
| +---------------------------------------------------------------+ |
| | [✗] Update user settings                                      | |
| |     PUT /api/users/123/settings                               | |
| |     404 Not Found - Same user ID                              | |
| |                                              [View Details →] | |
| +---------------------------------------------------------------+ |
| | [✗] Delete user                                               | |
| |     DELETE /api/users/123                                     | |
| |     404 Not Found - Same user ID                              | |
| |                                              [View Details →] | |
| +---------------------------------------------------------------+ |
|                                                                   |
| [!] Root Cause: User ID 123 doesn't exist                        |
|                                                                   |
| Suggestion: Update test data setup to create user with ID 123    |
|                                                                   |
+------------------------------------------------------------------+
```

### Cross-Run Error Patterns

```
+------------------------------------------------------------------+
| Error Patterns (Last 7 days)                                      |
+------------------------------------------------------------------+
|                                                                   |
| This error has occurred 12 times across 5 runs:                   |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Run #a1b2c3d4 │ Jan 30, 14:32 │ 4 occurrences                 | |
| | Run #e5f6g7h8 │ Jan 30, 12:15 │ 3 occurrences                 | |
| | Run #i9j0k1l2 │ Jan 29, 18:45 │ 2 occurrences                 | |
| | Run #m3n4o5p6 │ Jan 28, 14:30 │ 2 occurrences                 | |
| | Run #q7r8s9t0 │ Jan 27, 10:00 │ 1 occurrence                  | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Trend: [▁▂▃▅█] Increasing                                        |
|                                                                   |
| [Create Issue from Pattern] [Mark as Known Issue]                |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Responsive Layouts

### Tablet (768px - 1023px)

```
+------------------------------------------+
| [←] Error Detail            [Re-run]     |
+------------------------------------------+
|                                          |
| [✗] Assertion Failed                     |
|                                          |
| Scenario: Get user by ID                 |
| Step 3: HTTP Request                     |
|                                          |
| +-------------+ +-------------+          |
| | Status: 404 | | Time: 234ms |          |
| +-------------+ +-------------+          |
|                                          |
+------------------------------------------+
| [Request] [Response] [Assertions]        |
+------------------------------------------+
|                                          |
| Assertions                               |
|                                          |
| +--------------------------------------+ |
| | [✗] Status Code                      | |
| | Expected: 200  |  Actual: 404        | |
| +--------------------------------------+ |
|                                          |
| +--------------------------------------+ |
| | [✓] Response Time < 500ms            | |
| | Actual: 234ms                        | |
| +--------------------------------------+ |
|                                          |
+------------------------------------------+
```

### Mobile (< 768px)

```
+------------------------+
| [←] Error       [...]  |
+------------------------+
|                        |
| [✗] Assertion Failed   |
|                        |
| Get user by ID         |
| Step 3 of 5            |
|                        |
| +--------------------+ |
| | Status  | 404      | |
| | Time    | 234ms    | |
| | Size    | 128 B    | |
| +--------------------+ |
|                        |
+------------------------+
| [Request] [Response]   |
| [Assertions] [History] |
+------------------------+
|                        |
| Expected vs Actual     |
|                        |
| +--------------------+ |
| |     EXPECTED       | |
| |       200          | |
| +--------------------+ |
|         ↓              |
| +--------------------+ |
| |      ACTUAL        | |
| |       404          | |
| +--------------------+ |
|                        |
+------------------------+
| Possible Causes        |
| +--------------------+ |
| | User ID 123 may    | |
| | not exist in test  | |
| | database.          | |
| |       [Fix →]      | |
| +--------------------+ |
+------------------------+
```

---

## Component Specifications

### ErrorSummaryCard

```typescript
interface ErrorSummaryCardProps {
  error: {
    type: ErrorCategory
    message: string
    step: number
    totalSteps: number
    endpoint?: {
      method: string
      path: string
    }
  }
  scenario: {
    name: string
  }
  response?: {
    status: number
    time: number
  }
  expanded?: boolean
  onViewDetails: () => void
}
```

### AssertionComparison

```typescript
interface AssertionComparisonProps {
  type: 'status' | 'body' | 'header' | 'jsonpath' | 'time'
  passed: boolean
  expected: unknown
  actual: unknown
  path?: string              // For JSONPath assertions
  showDiff?: boolean         // Show inline diff for objects
}
```

### RequestResponseViewer

```typescript
interface RequestResponseViewerProps {
  request: {
    method: string
    url: string
    headers: Record<string, string>
    queryParams?: Record<string, string>
    body?: unknown
  }
  response: {
    status: number
    statusText: string
    headers: Record<string, string>
    body: unknown
    time: number
    size: number
  }
  layout?: 'tabs' | 'split'
  onCopy: (type: 'request' | 'response' | 'curl') => void
}
```

### ErrorTimeline

```typescript
interface ErrorTimelineProps {
  steps: {
    name: string
    status: 'passed' | 'failed' | 'skipped'
    duration?: number
    error?: string
  }[]
  totalDuration: number
  failedStep?: number
}
```

---

## States

### Loading State

```
+------------------------------------------------------------------+
| [←] Error Detail                                                  |
+------------------------------------------------------------------+
|                                                                   |
| +---------------------------------------------------------------+ |
| | ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ | |
| | ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Loading error details...                                          |
+------------------------------------------------------------------+
```

### Retry In Progress

```
+------------------------------------------------------------------+
| [✗] Assertion Failed                              [Retrying...]   |
+------------------------------------------------------------------+
|                                                                   |
|                    [Spinner]                                      |
|                                                                   |
|              Re-running step 3...                                 |
|                                                                   |
|                    [Cancel]                                       |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Accessibility

### Screen Reader Support

```html
<!-- Error summary -->
<article
  role="alert"
  aria-label="Assertion failed on step 3"
>
  <h2>Assertion Failed</h2>
  <p>Expected status code 200, received 404</p>
</article>

<!-- Comparison view -->
<div role="group" aria-label="Expected versus actual comparison">
  <div aria-label="Expected value">200</div>
  <div aria-label="Actual value">404</div>
</div>
```

### Color Independence

Error states always include:
- Red color indicator
- [✗] icon
- "Failed" or "Error" text label

### Keyboard Navigation

- Tab through error sections
- Arrow keys to switch tabs
- Enter to expand details
- Escape to go back

---

*Last Updated: January 2026*
*Version: 1.0*
