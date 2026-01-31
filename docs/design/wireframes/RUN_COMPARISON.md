# QAWave Run Comparison UI Design

> Comprehensive wireframes for comparing two test runs side-by-side.

## Table of Contents

1. [Overview](#overview)
2. [Comparison Selection](#comparison-selection)
3. [Summary Comparison](#summary-comparison)
4. [Scenario-by-Scenario Comparison](#scenario-by-scenario-comparison)
5. [Diff View](#diff-view)
6. [Performance Comparison](#performance-comparison)
7. [Regression Detection](#regression-detection)
8. [Responsive Layouts](#responsive-layouts)
9. [Component Specifications](#component-specifications)

---

## Overview

### Purpose

The Run Comparison View helps users:
- Compare results between two test runs
- Identify regressions (new failures)
- Spot fixes (previously failing, now passing)
- Analyze performance changes
- Understand the impact of code changes

### Comparison Types

| Type | Description |
|------|-------------|
| Status Changes | Scenarios that changed pass/fail status |
| New Failures | Previously passing, now failing |
| Fixes | Previously failing, now passing |
| Flaky | Inconsistent results between runs |
| Performance | Response time changes |

---

## Comparison Selection

### Select Runs to Compare

```
+------------------------------------------------------------------+
| Compare Runs                                                      |
+------------------------------------------------------------------+
|                                                                   |
| Select two runs to compare:                                       |
|                                                                   |
| +-----------------------------+  +-----------------------------+  |
| |       BASELINE (A)          |  |       COMPARISON (B)        |  |
| +-----------------------------+  +-----------------------------+  |
| |                             |  |                             |  |
| | [Select a run...        ▼]  |  | [Select a run...        ▼]  |  |
| |                             |  |                             |  |
| | Recent runs:                |  | Recent runs:                |  |
| | ○ Run #a1b2 - Jan 30 14:32  |  | ● Run #e5f6 - Jan 30 16:45  |  |
| |   18/20 passed              |  |   16/20 passed              |  |
| | ○ Run #c3d4 - Jan 30 12:15  |  | ○ Run #g7h8 - Jan 30 18:00  |  |
| |   20/20 passed              |  |   17/20 passed              |  |
| | ○ Run #i9j0 - Jan 29 18:45  |  | ○ Run #k1l2 - Jan 29 20:00  |  |
| |   19/20 passed              |  |   19/20 passed              |  |
| |                             |  |                             |  |
| +-----------------------------+  +-----------------------------+  |
|                                                                   |
|                       [Compare Runs]                              |
|                                                                   |
+------------------------------------------------------------------+
```

### Quick Compare Actions

```
+------------------------------------------------------------------+
| Run #a1b2c3d4                                                     |
+------------------------------------------------------------------+
|                                                                   |
| Compare with:                                                     |
|                                                                   |
| [Previous Run] [Latest on main] [Pick specific run...]           |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Summary Comparison

### Comparison Overview

```
+------------------------------------------------------------------+
| Run Comparison                                    [Export] [Share]|
+------------------------------------------------------------------+
|                                                                   |
| +-----------------------------+  vs  +-----------------------------+
| |       RUN A (Baseline)      |      |    RUN B (Comparison)      |
| +-----------------------------+      +-----------------------------+
| | #a1b2c3d4                   |      | #e5f6g7h8                   |
| | Jan 30, 2026 14:32          |      | Jan 30, 2026 16:45          |
| | Package: Payment API        |      | Package: Payment API        |
| |                             |      |                             |
| | [████████████░░░░] 90%      |      | [████████░░░░░░░░] 80%      |
| | 18 passed / 2 failed        |      | 16 passed / 4 failed        |
| |                             |      |                             |
| | Duration: 2m 34s            |      | Duration: 2m 48s            |
| +-----------------------------+      +-----------------------------+
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| SUMMARY OF CHANGES                                                |
|                                                                   |
| +---------------+ +---------------+ +---------------+             |
| | [↓] REGRESSIONS | [↑] FIXES     | [~] FLAKY       |             |
| |      2          |      0        |      1          |             |
| |   new failures  |   now passing |   inconsistent  |             |
| +---------------+ +---------------+ +---------------+             |
|                                                                   |
| +---------------+ +---------------+ +---------------+             |
| | [=] UNCHANGED | | [+] NEW       | | [-] REMOVED   |             |
| |     15        | |      2        | |      0        |             |
| |   same status | | new scenarios | | deleted       |             |
| +---------------+ +---------------+ +---------------+             |
|                                                                   |
+------------------------------------------------------------------+
```

### Visual Status Comparison

```
+------------------------------------------------------------------+
| Pass Rate Comparison                                              |
+------------------------------------------------------------------+
|                                                                   |
|   Run A (Baseline)              Run B (Comparison)                |
|                                                                   |
|   ████████████████░░░░          ████████████████░░░░░░░░          |
|         90%                            80%                        |
|                                                                   |
|                    [▼ 10% decrease]                               |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Scenario-by-Scenario Comparison

### Comparison Table

```
+------------------------------------------------------------------+
| Scenario Comparison                              [Filter ▼] [↕]   |
+------------------------------------------------------------------+
| Show: [All] [Regressions] [Fixes] [Unchanged] [New] [Removed]     |
+------------------------------------------------------------------+
|                                                                   |
| Scenario Name                    │  Run A   │  Run B   │ Change  |
| ─────────────────────────────────┼──────────┼──────────┼─────────|
| [!] Create payment - success     │  [✓]     │  [✗]     │ ↓ Regr  |
| [!] Create payment - validation  │  [✓]     │  [✗]     │ ↓ Regr  |
| [~] Get payment - auth           │  [✗]     │  [✓]     │ ~ Flaky |
| [=] Get payment - success        │  [✓]     │  [✓]     │ = Same  |
| [=] Get payment - not found      │  [✓]     │  [✓]     │ = Same  |
| [=] List payments - empty        │  [✓]     │  [✓]     │ = Same  |
| [=] List payments - pagination   │  [✓]     │  [✓]     │ = Same  |
| [=] Update payment - success     │  [✓]     │  [✓]     │ = Same  |
| [=] Delete payment - success     │  [✓]     │  [✓]     │ = Same  |
| [+] Refund payment - success     │   --     │  [✓]     │ + New   |
| [+] Refund payment - failed      │   --     │  [✗]     │ + New   |
| ...                                                               |
|                                                                   |
+------------------------------------------------------------------+
| Showing 20 of 22 scenarios              [1] [2] [Next →]          |
+------------------------------------------------------------------+
```

### Status Icons Legend

| Icon | Status | Description |
|------|--------|-------------|
| [✓] | Pass | Scenario passed |
| [✗] | Fail | Scenario failed |
| [○] | Skip | Scenario skipped |
| -- | N/A | Not in this run |

### Change Indicators

| Indicator | Change Type | Color |
|-----------|-------------|-------|
| ↓ Regr | Regression | Red |
| ↑ Fix | Fix | Green |
| ~ Flaky | Flaky | Amber |
| = Same | Unchanged | Gray |
| + New | New | Blue |
| - Removed | Removed | Gray |

---

## Diff View

### Scenario Diff Panel

```
+------------------------------------------------------------------+
| [!] Create payment - success                        [View Details]|
+------------------------------------------------------------------+
|                                                                   |
| Status: [✓ Pass] → [✗ Fail]                    REGRESSION         |
|                                                                   |
+------------------------------------------------------------------+
|       Run A (Baseline)          │       Run B (Comparison)        |
+--------------------------------+----------------------------------+
|                                │                                  |
| Status: 201 Created            │ Status: 400 Bad Request          |
| Duration: 234ms                │ Duration: 89ms                   |
|                                │                                  |
| Response:                      │ Response:                        |
| {                              │ {                                |
|   "id": "pay_123",             │   "error": "VALIDATION_ERROR",   |
|   "amount": 100.00,            │   "message": "Invalid amount",   |
|   "status": "completed"        │   "field": "amount"              |
| }                              │ }                                |
|                                │                                  |
+--------------------------------+----------------------------------+
|                                                                   |
| ANALYSIS                                                          |
| +---------------------------------------------------------------+ |
| | The request body changed between runs. The 'amount' field     | |
| | now contains an invalid value causing validation to fail.     | |
| |                                                               | |
| | Request body diff:                                            | |
| | - "amount": 100.00                                            | |
| | + "amount": -50.00   ← Invalid value                          | |
| +---------------------------------------------------------------+ |
|                                                                   |
| [View Full Error] [Compare Requests] [Compare Responses]          |
|                                                                   |
+------------------------------------------------------------------+
```

### Inline Diff (JSON)

```
+------------------------------------------------------------------+
| Request Body Comparison                                           |
+------------------------------------------------------------------+
|                                                                   |
|   {                                                               |
|     "payment": {                                                  |
| -     "amount": 100.00,                           ← Removed       |
| +     "amount": -50.00,                           ← Added         |
|       "currency": "USD",                                          |
|       "recipient": {                                              |
|         "email": "user@example.com",                              |
| -       "name": "John Doe"                        ← Removed       |
| +       "name": "Jane Doe"                        ← Added         |
|       }                                                           |
|     }                                                             |
|   }                                                               |
|                                                                   |
| Legend: [-] Removed  [+] Added  [ ] Unchanged                     |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Performance Comparison

### Response Time Comparison

```
+------------------------------------------------------------------+
| Performance Comparison                                            |
+------------------------------------------------------------------+
|                                                                   |
| Overall                                                           |
| +---------------------------------------------------------------+ |
| |                                                               | |
| |  Run A: ████████████████░░░░░░░░░░░░░░░░░░░░░░░  Avg: 245ms  | |
| |  Run B: ████████████████████░░░░░░░░░░░░░░░░░░░  Avg: 312ms  | |
| |                                                               | |
| |                          [+27% slower]                         | |
| |                                                               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| By Scenario                                                       |
| +---------------------------------------------------------------+ |
| | Scenario                      │ Run A   │ Run B   │ Change   | |
| | ──────────────────────────────┼─────────┼─────────┼───────── | |
| | Create payment - success      │ 234ms   │ 89ms    │ ↓ 62%    | |
| | Get payment - success         │ 156ms   │ 412ms   │ ↑ 164%   | |
| | List payments - pagination    │ 523ms   │ 534ms   │ ↑ 2%     | |
| | Update payment - success      │ 189ms   │ 201ms   │ ↑ 6%     | |
| | Delete payment - success      │ 167ms   │ 178ms   │ ↑ 7%     | |
| +---------------------------------------------------------------+ |
|                                                                   |
| [!] 1 scenario exceeded threshold (>50% slower)                   |
|                                                                   |
+------------------------------------------------------------------+
```

### Performance Chart

```
+------------------------------------------------------------------+
| Response Time Distribution                                        |
+------------------------------------------------------------------+
|                                                                   |
|  600ms |                                                          |
|        |                               ▓▓                         |
|  400ms |               ▓▓              ▓▓                         |
|        |     ░░        ▓▓    ░░        ▓▓                         |
|  200ms |     ░░  ░░    ▓▓    ░░  ░░    ▓▓    ░░  ░░               |
|        |  ░░ ░░  ░░ ░░ ▓▓ ▓▓ ░░  ░░ ▓▓ ▓▓    ░░  ░░               |
|    0ms +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+          |
|          S1 S2 S3 S4 S5 S6 S7 S8 S9 S10                           |
|                                                                   |
|  ░░ Run A (Baseline)    ▓▓ Run B (Comparison)                     |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Regression Detection

### Regression Alert

```
+------------------------------------------------------------------+
| [!] REGRESSIONS DETECTED                                          |
+------------------------------------------------------------------+
|                                                                   |
| 2 scenarios that were passing in Run A are now failing in Run B.  |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✗] Create payment - success                                  | |
| |                                                               | |
| |     Run A: 201 Created (passed)                               | |
| |     Run B: 400 Bad Request (failed)                           | |
| |                                                               | |
| |     Error: Invalid amount value                               | |
| |                                                               | |
| |     [View Diff] [View Run A] [View Run B]                     | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [✗] Create payment - validation                               | |
| |                                                               | |
| |     Run A: 400 Bad Request (passed - expected)                | |
| |     Run B: 500 Internal Server Error (failed)                 | |
| |                                                               | |
| |     Error: Unexpected server error                            | |
| |                                                               | |
| |     [View Diff] [View Run A] [View Run B]                     | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Actions:                                                          |
| [Create Issue for Regressions] [Notify Team] [Mark as Known]     |
|                                                                   |
+------------------------------------------------------------------+
```

### Flaky Test Detection

```
+------------------------------------------------------------------+
| [~] FLAKY TESTS DETECTED                                          |
+------------------------------------------------------------------+
|                                                                   |
| 1 scenario has inconsistent results (different outcome each run). |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [~] Get payment - auth                                        | |
| |                                                               | |
| |     History (last 10 runs):                                   | |
| |     ✗ ✓ ✗ ✓ ✓ ✗ ✓ ✗ ✓ ✗                                       | |
| |                                                               | |
| |     Pass rate: 50% (flaky threshold: 80%)                     | |
| |                                                               | |
| |     Possible causes:                                          | |
| |     • Race condition in auth token refresh                    | |
| |     • Timing-dependent assertion                              | |
| |                                                               | |
| |     [Analyze Flakiness] [Quarantine Test] [View History]      | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Responsive Layouts

### Tablet (768px - 1023px)

```
+------------------------------------------+
| Compare Runs              [Export]       |
+------------------------------------------+
|                                          |
| Run A              vs         Run B      |
| #a1b2c3d4                    #e5f6g7h8   |
|                                          |
| +----------------+ +----------------+    |
| | Jan 30, 14:32  | | Jan 30, 16:45  |    |
| | 18/20 (90%)    | | 16/20 (80%)    |    |
| +----------------+ +----------------+    |
|                                          |
| [▼ 10% decrease]                         |
|                                          |
+------------------------------------------+
| Changes                                  |
| +--------------------------------------+ |
| | Regressions: 2  |  Fixes: 0         | |
| | Flaky: 1        |  Unchanged: 15    | |
| +--------------------------------------+ |
+------------------------------------------+
| Scenario Comparison     [Regressions ▼] |
+------------------------------------------+
| +--------------------------------------+ |
| | [!] Create payment - success         | |
| | [✓] → [✗]            ↓ Regression    | |
| +--------------------------------------+ |
| | [!] Create payment - validation      | |
| | [✓] → [✗]            ↓ Regression    | |
| +--------------------------------------+ |
+------------------------------------------+
```

### Mobile (< 768px)

```
+------------------------+
| Compare      [Export]  |
+------------------------+
|                        |
| +--------------------+ |
| | Run A: #a1b2       | |
| | Jan 30, 14:32      | |
| | 18/20 passed (90%) | |
| +--------------------+ |
|          vs            |
| +--------------------+ |
| | Run B: #e5f6       | |
| | Jan 30, 16:45      | |
| | 16/20 passed (80%) | |
| +--------------------+ |
|                        |
| [▼ 10% decrease]       |
|                        |
+------------------------+
| Summary                |
| +--------------------+ |
| | Regressions    2   | |
| | Fixes          0   | |
| | Flaky          1   | |
| | Unchanged     15   | |
| +--------------------+ |
+------------------------+
| Filter: [All       ▼] |
+------------------------+
| +--------------------+ |
| | [!] Create payment | |
| | success            | |
| | [✓] → [✗] Regr.   | |
| |       [Details →] | |
| +--------------------+ |
| +--------------------+ |
| | [!] Create payment | |
| | validation         | |
| | [✓] → [✗] Regr.   | |
| |       [Details →] | |
| +--------------------+ |
+------------------------+
```

### Mobile Diff View

```
+------------------------+
| [←] Scenario Diff      |
+------------------------+
|                        |
| Create payment         |
| [✓] Pass → [✗] Fail    |
|                        |
+------------------------+
| [Run A] [Run B]        |
+------------------------+
|                        |
| Run B Result:          |
| +--------------------+ |
| | Status: 400        | |
| | Duration: 89ms     | |
| +--------------------+ |
|                        |
| Response:              |
| +--------------------+ |
| | {                  | |
| |   "error":         | |
| |   "VALIDATION..."  | |
| | }                  | |
| +--------------------+ |
|                        |
| [View Full Response]   |
| [Compare with Run A]   |
|                        |
+------------------------+
```

---

## Component Specifications

### RunSelector

```typescript
interface RunSelectorProps {
  label: 'baseline' | 'comparison'
  selectedRun?: TestRun
  availableRuns: TestRun[]
  onSelect: (run: TestRun) => void
  excludeRunId?: string        // To exclude already-selected run
}
```

### ComparisonSummary

```typescript
interface ComparisonSummaryProps {
  baseline: {
    run: TestRun
    stats: RunStats
  }
  comparison: {
    run: TestRun
    stats: RunStats
  }
  changes: {
    regressions: number
    fixes: number
    flaky: number
    unchanged: number
    new: number
    removed: number
  }
}
```

### ScenarioComparisonRow

```typescript
interface ScenarioComparisonRowProps {
  scenario: {
    id: string
    name: string
  }
  baselineResult: 'pass' | 'fail' | 'skip' | null
  comparisonResult: 'pass' | 'fail' | 'skip' | null
  changeType: 'regression' | 'fix' | 'flaky' | 'unchanged' | 'new' | 'removed'
  onViewDiff: () => void
}
```

### DiffViewer

```typescript
interface DiffViewerProps {
  left: {
    label: string
    content: unknown
  }
  right: {
    label: string
    content: unknown
  }
  format?: 'json' | 'text' | 'side-by-side' | 'inline'
  highlightDiff?: boolean
}
```

---

## States

### Loading State

```
+------------------------------------------------------------------+
| Run Comparison                                                    |
+------------------------------------------------------------------+
|                                                                   |
| +---------------------------------------------------------------+ |
| | Comparing runs...                                             | |
| |                                                               | |
| |                    [Spinner]                                  | |
| |                                                               | |
| | Analyzing 22 scenarios                                        | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

### No Differences State

```
+------------------------------------------------------------------+
|                                                                   |
|                    [Icon: Check Circle]                           |
|                                                                   |
|               Runs are Identical                                  |
|                                                                   |
|          Both runs have the same results for all                  |
|               22 scenarios.                                       |
|                                                                   |
|                   [View Full Results]                             |
|                                                                   |
+------------------------------------------------------------------+
```

### Incompatible Runs State

```
+------------------------------------------------------------------+
|                                                                   |
|                    [Icon: Warning]                                |
|                                                                   |
|             Runs Cannot Be Compared                               |
|                                                                   |
|          These runs are from different packages and               |
|               have different scenario sets.                       |
|                                                                   |
|          Run A: Payment API (22 scenarios)                        |
|          Run B: User API (15 scenarios)                           |
|                                                                   |
|                [Select Different Runs]                            |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Accessibility

### Screen Reader Support

```html
<!-- Comparison summary -->
<section aria-label="Run comparison summary">
  <h2>Comparison: Run A versus Run B</h2>
  <p>
    Run A: 18 of 20 scenarios passed (90 percent)
    Run B: 16 of 20 scenarios passed (80 percent)
    Change: 10 percent decrease, 2 regressions detected
  </p>
</section>

<!-- Change indicator -->
<span
  role="status"
  aria-label="Regression: previously passing, now failing"
  class="change-indicator regression"
>
  ↓ Regression
</span>
```

### Color Independence

Change indicators always include:
- Color coding (red/green/amber/gray)
- Icon (↓/↑/~/=)
- Text label ("Regr", "Fix", "Flaky", "Same")

### Keyboard Navigation

- Tab through run selectors
- Arrow keys to navigate comparison table
- Enter to view scenario diff
- Escape to close diff panel

---

*Last Updated: January 2026*
*Version: 1.0*
