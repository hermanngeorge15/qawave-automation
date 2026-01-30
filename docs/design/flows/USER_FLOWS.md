# QAWave User Flows

> Diagrams showing key user journeys through the application.

## Table of Contents

1. [Create and Run Package Flow](#create-and-run-package-flow)
2. [Review Run Results Flow](#review-run-results-flow)
3. [Replay Failed Scenarios Flow](#replay-failed-scenarios-flow)
4. [Export Test Results Flow](#export-test-results-flow)

---

## Create and Run Package Flow

### Overview
Primary flow for creating a new QA package and generating test scenarios.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌─────────┐                                                                │
│  │  START  │                                                                │
│  └────┬────┘                                                                │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────┐                                                        │
│  │ User clicks     │                                                        │
│  │ "New Package"   │                                                        │
│  └────────┬────────┘                                                        │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        CREATE PACKAGE MODAL                          │   │
│  │                                                                      │   │
│  │  ┌─────────────────┐                                                 │   │
│  │  │ Enter package   │                                                 │   │
│  │  │ name & desc     │                                                 │   │
│  │  └────────┬────────┘                                                 │   │
│  │           │                                                          │   │
│  │           ▼                                                          │   │
│  │  ┌─────────────────┐    ┌──────────────────┐                        │   │
│  │  │ Enter OpenAPI   │───▶│ Validate URL     │                        │   │
│  │  │ URL or Upload   │    │ (async fetch)    │                        │   │
│  │  └─────────────────┘    └────────┬─────────┘                        │   │
│  │                                  │                                   │   │
│  │                         ┌────────┴────────┐                         │   │
│  │                         │                 │                         │   │
│  │                    ✓ Valid           ✗ Invalid                      │   │
│  │                         │                 │                         │   │
│  │                         ▼                 ▼                         │   │
│  │                   ┌──────────┐    ┌──────────────┐                  │   │
│  │                   │ Show     │    │ Show error   │                  │   │
│  │                   │ endpoint │    │ "Invalid URL │                  │   │
│  │                   │ count    │    │ or spec"     │                  │   │
│  │                   └────┬─────┘    └──────────────┘                  │   │
│  │                        │                 ▲                          │   │
│  │                        │                 │ (retry)                  │   │
│  │                        ▼                 │                          │   │
│  │                   ┌──────────┐           │                          │   │
│  │                   │ Enter    │───────────┘                          │   │
│  │                   │ Base URL │                                      │   │
│  │                   └────┬─────┘                                      │   │
│  │                        │                                            │   │
│  │                        ▼                                            │   │
│  │                   ┌──────────────────┐                              │   │
│  │                   │ Enter optional   │                              │   │
│  │                   │ requirements     │                              │   │
│  │                   └────────┬─────────┘                              │   │
│  │                            │                                        │   │
│  │                            ▼                                        │   │
│  │            ┌───────────────────────────────────┐                    │   │
│  │            │ ☐ Generate scenarios immediately? │                    │   │
│  │            └───────────────┬───────────────────┘                    │   │
│  │                            │                                        │   │
│  │                            ▼                                        │   │
│  │                   [Cancel]  [Create Package]                        │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                            │                                                │
│          ┌─────────────────┴─────────────────┐                              │
│          │                                   │                              │
│     [Checked]                           [Unchecked]                         │
│          │                                   │                              │
│          ▼                                   ▼                              │
│  ┌───────────────────┐               ┌───────────────────┐                  │
│  │ Navigate to       │               │ Navigate to       │                  │
│  │ Package Detail    │               │ Package Detail    │                  │
│  │ (generating)      │               │ (empty)           │                  │
│  └─────────┬─────────┘               └─────────┬─────────┘                  │
│            │                                   │                            │
│            ▼                                   │                            │
│  ┌───────────────────┐                         │                            │
│  │ Show progress:    │                         │                            │
│  │ "Generating       │                         │                            │
│  │ scenarios..."     │                         │                            │
│  │                   │                         │                            │
│  │ ████████░░░ 75%   │                         │                            │
│  │                   │                         │                            │
│  │ ✓ Analyzed spec   │                         │                            │
│  │ ✓ Created 8/10    │                         │                            │
│  │ ⏳ Creating...    │                         │                            │
│  └─────────┬─────────┘                         │                            │
│            │                                   │                            │
│            ▼                                   │                            │
│  ┌───────────────────┐                         │                            │
│  │ Generation        │◀────────────────────────┘                            │
│  │ complete!         │                                                      │
│  │                   │                                                      │
│  │ 10 scenarios      │                                                      │
│  │ created           │                                                      │
│  │                   │                                                      │
│  │ [Run Tests Now]   │                                                      │
│  │ [Review First]    │                                                      │
│  └─────────┬─────────┘                                                      │
│            │                                                                │
│  ┌─────────┴─────────┐                                                      │
│  │                   │                                                      │
│  ▼                   ▼                                                      │
│ [Run]            [Review]                                                   │
│  │                   │                                                      │
│  ▼                   ▼                                                      │
│ (See Run      (See Package                                                  │
│  Flow)         Detail)                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Interactions

| Step | Component | Behavior |
|------|-----------|----------|
| Modal Open | Modal | Focus on first input, trap focus |
| URL Validation | Input | Async validation with loading indicator |
| Submit | Button | Loading state, disabled during creation |
| Progress | Progress Bar | Real-time updates via polling |
| Completion | Toast + Redirect | Success toast, navigate to detail |

---

## Review Run Results Flow

### Overview
Flow for reviewing test run results and debugging failures.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌─────────────┐                                                            │
│  │ Run Detail  │                                                            │
│  │ Page        │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                   │
│         ▼                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        RUN SUMMARY HEADER                            │   │
│  │                                                                      │   │
│  │  Run #42  [COMPLETED]                                                │   │
│  │  8/10 passed (80%)                                                   │   │
│  │                                                                      │   │
│  │  [Replay Run]  [Export]  [View AI Logs]                              │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│         │                                                                   │
│         ▼                                                                   │
│  ┌──────────────────────────────────────────┐                              │
│  │ Filter: [All ▼] [Show Failed ▼]          │                              │
│  └──────────────────────────────────────────┘                              │
│         │                                                                   │
│         ▼                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  SCENARIO LIST (Collapsed by default)                                │   │
│  │                                                                      │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │ [▶] ✓ Create User                                       245ms  │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │ [▶] ✓ Get User by ID                                     89ms  │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │ [▼] ✗ Update User Email                                  1.2s  │◀──── (auto-expand failed)
│  │  ├─────────────────────────────────────────────────────────────────┤ │   │
│  │  │                                                                 │ │   │
│  │  │  STEP 1: GET /api/users/{id}                            ✓      │ │   │
│  │  │  ──────────────────────────────────────────────────────        │ │   │
│  │  │  Status: 200 OK (expected: 200)                                │ │   │
│  │  │  Duration: 45ms                                                │ │   │
│  │  │  [View Details]                                                │ │   │
│  │  │                                                                 │ │   │
│  │  │  STEP 2: PATCH /api/users/{id}                          ✗      │ │   │
│  │  │  ──────────────────────────────────────────────────────        │ │   │
│  │  │                                                                 │ │   │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │ │   │
│  │  │  │ ⚠ FAILURE                                              │   │ │   │
│  │  │  │                                                         │   │ │   │
│  │  │  │ Expected: HTTP 200 OK                                   │   │ │   │
│  │  │  │ Actual:   HTTP 500 Internal Server Error                │   │ │   │
│  │  │  │                                                         │   │ │   │
│  │  │  │ Response:                                               │   │ │   │
│  │  │  │ {                                                       │   │ │   │
│  │  │  │   "error": "ValidationError",                           │   │ │   │
│  │  │  │   "message": "Email format invalid"                     │   │ │   │
│  │  │  │ }                                                       │   │ │   │
│  │  │  └─────────────────────────────────────────────────────────┘   │ │   │
│  │  │                                                                 │ │   │
│  │  │  Actions:                                                      │ │   │
│  │  │  [Copy cURL]  [Copy Request]  [Copy Response]  [Report Bug]   │ │   │
│  │  │                                                                 │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│         │                                                                   │
│         │                                                                   │
│  ┌──────┴──────────────────────────────────────────────────────────────┐   │
│  │                                                                      │   │
│  │  USER ACTIONS                                                        │   │
│  │                                                                      │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────────┐ │   │
│  │  │ Copy cURL  │  │ Export     │  │ Replay Run │  │ View AI Logs   │ │   │
│  │  │            │  │ Results    │  │            │  │                │ │   │
│  │  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └────────┬───────┘ │   │
│  │         │               │               │                 │         │   │
│  │         ▼               ▼               ▼                 ▼         │   │
│  │  ┌──────────┐   ┌──────────────┐ ┌───────────┐   ┌──────────────┐  │   │
│  │  │ Clipboard│   │ Download     │ │ New Run   │   │ AI Logs Page │  │   │
│  │  │ + Toast  │   │ JSON/CSV     │ │ Created   │   │              │  │   │
│  │  └──────────┘   └──────────────┘ └───────────┘   └──────────────┘  │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### User Decision Tree

```
User reviews run results
         │
         ▼
   All tests passed?
         │
    ┌────┴────┐
    │         │
   YES        NO
    │         │
    ▼         ▼
 [Done]   Expand failed
          scenarios
              │
              ▼
        Understand
        failure reason
              │
     ┌────────┴────────┐
     │                 │
  Test bug        API bug
     │                 │
     ▼                 ▼
 Edit scenario    Report bug
 and re-run       (copy cURL)
```

---

## Replay Failed Scenarios Flow

### Overview
Flow for re-running only failed scenarios after a bug fix.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌──────────────────┐                                                       │
│  │ Run Detail Page  │                                                       │
│  │ (with failures)  │                                                       │
│  └────────┬─────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                                                                    │    │
│  │  Run #42: 8/10 passed                                              │    │
│  │                                                                    │    │
│  │  ┌────────────────────────────────────────────────────────────┐    │    │
│  │  │ ✓ Create User          │ ✓ List Users     │ ✗ Update User │    │    │
│  │  │ ✓ Get User             │ ✓ Search Users   │ ✗ Delete User │    │    │
│  │  └────────────────────────────────────────────────────────────┘    │    │
│  │                                                                    │    │
│  │  [Replay All]  [Replay Failed Only]  [Export]                      │    │
│  │                                                                    │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                                                 │
│           │ Click "Replay Failed Only"                                      │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                    CONFIRMATION MODAL                               │    │
│  │                                                                     │    │
│  │  Replay 2 failed scenarios?                                         │    │
│  │  ───────────────────────────                                        │    │
│  │                                                                     │    │
│  │  The following scenarios will be re-executed:                       │    │
│  │                                                                     │    │
│  │  • Update User Email (4 steps)                                      │    │
│  │  • Delete User (2 steps)                                            │    │
│  │                                                                     │    │
│  │  Base URL: https://api.staging.example.com                          │    │
│  │  [Change URL]                                                       │    │
│  │                                                                     │    │
│  │                              [Cancel]  [Replay Now]                 │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                                                 │
│           │ Confirm                                                         │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                     NEW RUN CREATED                                  │    │
│  │                                                                     │    │
│  │  Run #43: Replaying 2 scenarios...                                  │    │
│  │                                                                     │    │
│  │  ┌────────────────────────────────────────────────────────────┐    │    │
│  │  │ ⏳ Update User Email                          [Running...]  │    │    │
│  │  │ ⏳ Delete User                                [Pending]     │    │    │
│  │  └────────────────────────────────────────────────────────────┘    │    │
│  │                                                                     │    │
│  │  Progress: ██████████░░░░░░░░░░ 50%                                │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                                                 │
│           │ Completion                                                      │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                      RUN COMPLETE                                   │    │
│  │                                                                     │    │
│  │  Run #43: 2/2 passed ✓                                              │    │
│  │                                                                     │    │
│  │  ┌────────────────────────────────────────────────────────────┐    │    │
│  │  │ ✓ Update User Email                         245ms          │    │    │
│  │  │ ✓ Delete User                               156ms          │    │    │
│  │  └────────────────────────────────────────────────────────────┘    │    │
│  │                                                                     │    │
│  │  All previously failed scenarios now pass!                          │    │
│  │                                                                     │    │
│  │  [View Full Results]  [Compare with Previous]                       │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Export Test Results Flow

### Overview
Flow for exporting test results to various formats.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌──────────────────┐                                                       │
│  │ Run Detail Page  │                                                       │
│  │ or Package Page  │                                                       │
│  └────────┬─────────┘                                                       │
│           │                                                                 │
│           │ Click "Export"                                                  │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                    EXPORT OPTIONS MODAL                             │    │
│  │                                                                     │    │
│  │  Export Test Results                                                │    │
│  │  ────────────────────                                               │    │
│  │                                                                     │    │
│  │  Format                                                             │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ (●) JSON Report                                              │   │    │
│  │  │     Full results with all details                            │   │    │
│  │  │                                                              │   │    │
│  │  │ ( ) CSV Summary                                              │   │    │
│  │  │     Spreadsheet-friendly summary                             │   │    │
│  │  │                                                              │   │    │
│  │  │ ( ) Markdown Report                                          │   │    │
│  │  │     Human-readable report                                    │   │    │
│  │  │                                                              │   │    │
│  │  │ ( ) Playwright Tests                                         │   │    │
│  │  │     Exportable TypeScript test file                          │   │    │
│  │  │                                                              │   │    │
│  │  │ ( ) JUnit XML                                                │   │    │
│  │  │     CI/CD compatible format                                  │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Options                                                            │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ ☑ Include request/response bodies                           │   │    │
│  │  │ ☐ Include AI interaction logs                                │   │    │
│  │  │ ☐ Redact sensitive headers (Authorization, cookies)         │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │                              [Cancel]  [Download]                   │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                                                 │
│           │ Click "Download"                                                │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                    GENERATING EXPORT                                │    │
│  │                                                                     │    │
│  │  ████████████░░░░░░░░  Generating export...                        │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                                                 │
│           │ Complete                                                        │
│           ▼                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                                                                     │    │
│  │  ✓ Downloaded: run-42-results.json (245 KB)                        │    │
│  │                                                                     │    │
│  │  ─────────────────────────────────────────────── (toast + file)    │    │
│  │                                                                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Export Format Decision Tree

```
User wants to export
         │
         ▼
    What's the goal?
         │
    ┌────┴────────────┬────────────────┬───────────────┐
    │                 │                │               │
  Share with      Run in CI       Analyze in      Generate
  stakeholder     pipeline        spreadsheet     code tests
    │                 │                │               │
    ▼                 ▼                ▼               ▼
  Markdown        JUnit XML         CSV           Playwright/
  Report                            Summary       RestAssured
```

---

## Error Handling Flows

### Network Error During Run

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Run in progress...                                             │
│  ████████░░░░░░░░░░ 42%                                         │
│                                                                 │
│         ┌──────────────────────────────────────────┐            │
│         │ ⚠ Network Error                         │            │
│         │                                          │            │
│         │ Unable to reach target API.              │            │
│         │ Check if the server is running.          │            │
│         │                                          │            │
│         │ [Retry]  [Abort Run]                     │            │
│         └──────────────────────────────────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### AI Generation Failure

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Generating scenarios...                                        │
│  ████████░░░░░░░░░░ 40%                                         │
│                                                                 │
│  ✓ Parsed 15 operations                                         │
│  ✓ Generated 6/15 scenarios                                     │
│         ┌──────────────────────────────────────────┐            │
│         │ ⚠ AI Rate Limit Exceeded                │            │
│         │                                          │            │
│         │ Too many requests to AI provider.        │            │
│         │ Waiting 60 seconds before retry...       │            │
│         │                                          │            │
│         │ ⏱ 45s remaining                          │            │
│         │                                          │            │
│         │ [Cancel]                                 │            │
│         └──────────────────────────────────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 2026*
*Version: 1.0*
