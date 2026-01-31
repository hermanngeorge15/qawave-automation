# QAWave AI Logs Viewer UI Design

> Comprehensive wireframes for viewing and analyzing AI agent logs and decisions.

## Table of Contents

1. [Overview](#overview)
2. [Logs List View](#logs-list-view)
3. [Log Detail View](#log-detail-view)
4. [Conversation Thread View](#conversation-thread-view)
5. [Decision Trace View](#decision-trace-view)
6. [Filter and Search](#filter-and-search)
7. [Responsive Layouts](#responsive-layouts)
8. [Component Specifications](#component-specifications)

---

## Overview

### Purpose

The AI Logs Viewer helps users:
- Debug AI agent decisions during scenario generation
- Understand why certain test cases were created or skipped
- Review prompt/response pairs for fine-tuning
- Track token usage and costs
- Audit AI behavior for compliance

### Log Types

| Type | Description | Icon |
|------|-------------|------|
| Generation | Scenario/step generation | [Wand] |
| Analysis | API analysis decisions | [Magnifier] |
| Validation | Test result validation | [Check] |
| Suggestion | Improvement suggestions | [Lightbulb] |
| Error | Processing errors | [Alert] |

---

## Logs List View

### Desktop Layout (1024px+)

```
+------------------------------------------------------------------+
| AI Logs                                            [Export] [?]   |
+------------------------------------------------------------------+
| Search: [_________________________] [Filter ▼] [Date Range ▼]     |
+------------------------------------------------------------------+
|                                                                   |
| Today (24 logs)                                                   |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [Wand] Scenario Generation                              3m ago | |
| | Package: Payment API Tests                                     | |
| | Generated 5 scenarios for POST /api/payments                   | |
| |                                                                | |
| | Tokens: 2,450 in / 1,890 out    Duration: 4.2s    Cost: $0.02 | |
| |                                                                | |
| | [Success]                                       [View Details →]| |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [Magnifier] API Analysis                               15m ago | |
| | Package: User Management                                       | |
| | Analyzed OpenAPI spec: 24 endpoints discovered                 | |
| |                                                                | |
| | Tokens: 8,120 in / 3,450 out    Duration: 8.7s    Cost: $0.05 | |
| |                                                                | |
| | [Success]                                       [View Details →]| |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [Alert] Generation Error                                1h ago | |
| | Package: Orders API                                            | |
| | Failed to generate scenarios: Rate limit exceeded              | |
| |                                                                | |
| | Tokens: 500 in / 0 out          Duration: 0.3s    Cost: $0.00 | |
| |                                                                | |
| | [Error]                                         [View Details →]| |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [Check] Result Validation                              2h ago  | |
| | Run: #a1b2c3d4                                                 | |
| | Validated 18 scenarios, 2 flaky tests detected                 | |
| |                                                                | |
| | Tokens: 1,200 in / 800 out      Duration: 2.1s    Cost: $0.01 | |
| |                                                                | |
| | [Warning]                                       [View Details →]| |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Yesterday (42 logs)                                      [Expand] |
|                                                                   |
| Last Week (186 logs)                                     [Expand] |
|                                                                   |
+------------------------------------------------------------------+
| Page 1 of 12              [← Previous] [1] [2] [3] ... [Next →]   |
+------------------------------------------------------------------+
```

### Log Entry Card

```
+---------------------------------------------------------------+
| [Icon] {Log Type}                               {Relative Time}|
| {Context: Package/Run/Scenario}                                |
| {Summary description of what happened}                         |
|                                                                |
| +------------------+ +------------------+ +------------------+ |
| | Tokens           | | Duration         | | Cost             | |
| | 2,450 ↓ / 1,890 ↑| | 4.2s             | | $0.02            | |
| +------------------+ +------------------+ +------------------+ |
|                                                                |
| [{Status Badge}]                              [View Details →] |
+---------------------------------------------------------------+
```

### Status Badge Variants

| Status | Color | Description |
|--------|-------|-------------|
| Success | Green | Completed successfully |
| Warning | Amber | Completed with warnings |
| Error | Red | Failed with error |
| Pending | Blue | In progress |

---

## Log Detail View

### Full Log Detail Page

```
+------------------------------------------------------------------+
| [← Back to Logs]                                                  |
+------------------------------------------------------------------+
| Scenario Generation                                               |
| Payment API Tests > POST /api/payments                            |
+------------------------------------------------------------------+
|                                                                   |
| +------------------+ +------------------+ +------------------+     |
| | STATUS           | | TIMESTAMP        | | DURATION         |     |
| | [✓ Success]      | | Jan 30, 2026     | | 4.2 seconds      |     |
| |                  | | 14:32:15 UTC     | |                  |     |
| +------------------+ +------------------+ +------------------+     |
|                                                                   |
| +------------------+ +------------------+ +------------------+     |
| | MODEL            | | TOKENS           | | ESTIMATED COST   |     |
| | claude-3-opus    | | 2,450 in         | | $0.02            |     |
| |                  | | 1,890 out        | |                  |     |
| +------------------+ +------------------+ +------------------+     |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| [Conversation] [Decision Trace] [Generated Output] [Raw JSON]     |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Conversation Thread                                               |
|                                                                   |
| +---------------------------------------------------------------+ |
| | SYSTEM                                                        | |
| | You are a QA engineer specialized in API testing. Given an    | |
| | OpenAPI specification, generate comprehensive test scenarios  | |
| | covering happy paths, edge cases, and error conditions...     | |
| |                                                               | |
| | [Expand full prompt...]                                       | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | USER                                          [Copy] [Expand] | |
| | Generate test scenarios for the following endpoint:           | |
| |                                                               | |
| | ```json                                                       | |
| | {                                                             | |
| |   "path": "/api/payments",                                    | |
| |   "method": "POST",                                           | |
| |   "summary": "Create a payment",                              | |
| |   "parameters": [...],                                        | |
| |   "requestBody": {...},                                       | |
| |   "responses": {...}                                          | |
| | }                                                             | |
| | ```                                                           | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | ASSISTANT                                     [Copy] [Expand] | |
| | Based on the OpenAPI specification, I'll generate 5 test      | |
| | scenarios for POST /api/payments:                             | |
| |                                                               | |
| | 1. **Happy Path - Successful Payment**                        | |
| |    - Valid payment with all required fields                   | |
| |    - Expected: 201 Created                                    | |
| |                                                               | |
| | 2. **Validation Error - Missing Amount**                      | |
| |    - Payment without amount field                             | |
| |    - Expected: 400 Bad Request                                | |
| |                                                               | |
| | [Show more scenarios...]                                      | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

### Tabs Content

**Conversation Tab**: Shows prompt/response thread
**Decision Trace Tab**: Shows reasoning steps
**Generated Output Tab**: Shows final scenarios created
**Raw JSON Tab**: Shows raw API request/response

---

## Conversation Thread View

### Message Bubbles

```
+---------------------------------------------------------------+
| SYSTEM                                                         |
+---------------------------------------------------------------+
|                                                                |
| You are a QA engineer specialized in API testing.             |
|                                                                |
| ## Your Task                                                   |
| Generate comprehensive test scenarios for API endpoints.       |
|                                                                |
| ## Guidelines                                                  |
| - Cover happy paths (successful operations)                    |
| - Cover edge cases (boundary values, empty inputs)             |
| - Cover error conditions (4xx, 5xx responses)                  |
| - Include authentication scenarios if applicable               |
|                                                                |
| ## Output Format                                               |
| Return scenarios as JSON array with the following structure... |
|                                                                |
| [Token count: 450]                                             |
|                                                                |
+---------------------------------------------------------------+

+---------------------------------------------------------------+
| USER                                                    [Copy] |
+---------------------------------------------------------------+
|                                                                |
| Generate test scenarios for:                                   |
|                                                                |
| ```json                                                        |
| {                                                              |
|   "path": "/api/payments",                                     |
|   "method": "POST",                                            |
|   "operationId": "createPayment",                              |
|   "summary": "Create a new payment",                           |
|   "requestBody": {                                             |
|     "required": true,                                          |
|     "content": {                                               |
|       "application/json": {                                    |
|         "schema": {                                            |
|           "type": "object",                                    |
|           "required": ["amount", "currency", "recipient"],     |
|           "properties": {                                      |
|             "amount": { "type": "number", "minimum": 0.01 },   |
|             "currency": { "type": "string", "enum": [...] },   |
|             "recipient": { "type": "string" }                  |
|           }                                                    |
|         }                                                      |
|       }                                                        |
|     }                                                          |
|   }                                                            |
| }                                                              |
| ```                                                            |
|                                                                |
| [Token count: 2,000]                                           |
|                                                                |
+---------------------------------------------------------------+

+---------------------------------------------------------------+
| ASSISTANT                                               [Copy] |
+---------------------------------------------------------------+
|                                                                |
| I'll generate comprehensive test scenarios for POST            |
| /api/payments based on the specification.                      |
|                                                                |
| ## Analysis                                                    |
|                                                                |
| **Required Fields**: amount, currency, recipient               |
| **Validation Rules**: amount >= 0.01, currency enum            |
| **Expected Responses**: 201, 400, 401, 403, 500                |
|                                                                |
| ## Generated Scenarios                                         |
|                                                                |
| ```json                                                        |
| [                                                              |
|   {                                                            |
|     "name": "Create payment - success",                        |
|     "steps": [                                                 |
|       {                                                        |
|         "type": "http",                                        |
|         "method": "POST",                                      |
|         "url": "/api/payments",                                |
|         "body": {                                              |
|           "amount": 100.00,                                    |
|           "currency": "USD",                                   |
|           "recipient": "user@example.com"                      |
|         },                                                     |
|         "assertions": [                                        |
|           { "type": "status", "expected": 201 }                |
|         ]                                                      |
|       }                                                        |
|     ]                                                          |
|   },                                                           |
|   ...                                                          |
| ]                                                              |
| ```                                                            |
|                                                                |
| [Token count: 1,890]                                           |
|                                                                |
+---------------------------------------------------------------+
```

### Message Actions

```
+-----------------------------------------------+
| ASSISTANT                        [Actions ▼]  |
+-----------------------------------------------+

Actions Dropdown:
+------------------+
| [Copy] Copy Text |
| [<>] View Raw    |
| [↗] Export       |
| [Flag] Report    |
+------------------+
```

---

## Decision Trace View

### Reasoning Steps

```
+------------------------------------------------------------------+
| Decision Trace                                                    |
+------------------------------------------------------------------+
|                                                                   |
| Step 1: Parse OpenAPI Specification                               |
| +---------------------------------------------------------------+ |
| | [✓] Extracted endpoint: POST /api/payments                    | |
| | [✓] Identified 3 required parameters                          | |
| | [✓] Found 5 possible response codes                           | |
| |                                                                | |
| | Input:  OpenAPI JSON (2,000 tokens)                           | |
| | Output: Parsed endpoint structure                              | |
| | Time:   0.2s                                                   | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Step 2: Analyze Parameter Constraints                             |
| +---------------------------------------------------------------+ |
| | [✓] amount: number, min 0.01 → boundary tests needed          | |
| | [✓] currency: enum ["USD", "EUR", "GBP"] → each value test    | |
| | [✓] recipient: string, email format → validation tests        | |
| |                                                                | |
| | Decision: Generate 8 parameter-focused scenarios               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Step 3: Map Response Codes                                        |
| +---------------------------------------------------------------+ |
| | [✓] 201 Created → happy path scenario                         | |
| | [✓] 400 Bad Request → validation error scenarios              | |
| | [✓] 401 Unauthorized → auth error scenario                    | |
| | [✓] 403 Forbidden → permission error scenario                 | |
| | [!] 500 Internal Error → skipped (not in spec)                | |
| |                                                                | |
| | Decision: Generate 4 response-code scenarios                   | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Step 4: Deduplicate & Prioritize                                  |
| +---------------------------------------------------------------+ |
| | [✓] Combined overlapping scenarios                            | |
| | [✓] Prioritized by coverage impact                            | |
| |                                                                | |
| | Input:  12 candidate scenarios                                 | |
| | Output: 5 optimized scenarios                                  | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Final Output: 5 scenarios generated                               |
|                                                                   |
+------------------------------------------------------------------+
```

### Step Card Anatomy

```
+---------------------------------------------------------------+
| Step {N}: {Step Title}                           {Status Icon} |
+---------------------------------------------------------------+
| {List of decisions/actions taken}                              |
|                                                                |
| +--------------------+ +--------------------+                  |
| | Input              | | Output             |                  |
| | {Input summary}    | | {Output summary}   |                  |
| +--------------------+ +--------------------+                  |
|                                                                |
| Time: {duration}                            [View Details →]   |
+---------------------------------------------------------------+
```

---

## Filter and Search

### Filter Panel

```
+----------------------------------+
| Filters                   [Clear]|
+----------------------------------+
|                                  |
| Log Type:                        |
| [x] All                          |
| [ ] Generation                   |
| [ ] Analysis                     |
| [ ] Validation                   |
| [ ] Suggestion                   |
| [ ] Error                        |
|                                  |
| Status:                          |
| [x] All                          |
| [ ] Success                      |
| [ ] Warning                      |
| [ ] Error                        |
|                                  |
| Package:                         |
| [All Packages           ▼]       |
|                                  |
| Model:                           |
| [x] claude-3-opus                |
| [x] claude-3-sonnet              |
| [ ] claude-3-haiku               |
|                                  |
| Token Range:                     |
| Min: [____] Max: [____]          |
|                                  |
| Cost Range:                      |
| Min: [$___] Max: [$___]          |
|                                  |
+----------------------------------+
|       [Apply Filters]            |
+----------------------------------+
```

### Search Interface

```
+------------------------------------------------------------------+
| Search Logs                                                       |
+------------------------------------------------------------------+
| [Search prompts, responses, or errors..._______________] [Search] |
|                                                                   |
| Search in:                                                        |
| [x] Prompts  [x] Responses  [ ] Errors only  [ ] Include JSON    |
|                                                                   |
| Recent searches:                                                  |
| "payment validation" | "rate limit" | "unauthorized"             |
+------------------------------------------------------------------+
```

### Date Range Picker

```
+----------------------------------+
| Date Range                       |
+----------------------------------+
| Quick Select:                    |
| [Today] [Yesterday] [Last 7d]    |
| [Last 30d] [This month] [Custom] |
|                                  |
| Custom Range:                    |
| From: [01/20/2026    ] [📅]      |
| To:   [01/30/2026    ] [📅]      |
|                                  |
|              [Apply]             |
+----------------------------------+
```

---

## Responsive Layouts

### Tablet (768px - 1023px)

```
+------------------------------------------+
| AI Logs                    [Filter] [↗]  |
+------------------------------------------+
| [Search...                       ] [🔍]  |
+------------------------------------------+
|                                          |
| Today (24)                               |
|                                          |
| +--------------------------------------+ |
| | [Wand] Scenario Generation    3m ago | |
| | Payment API Tests                    | |
| | Generated 5 scenarios                | |
| |                                      | |
| | Tokens: 4,340  Duration: 4.2s        | |
| | [Success]              [Details →]   | |
| +--------------------------------------+ |
|                                          |
| +--------------------------------------+ |
| | [Alert] Generation Error      1h ago | |
| | Orders API                           | |
| | Rate limit exceeded                  | |
| |                                      | |
| | Tokens: 500    Duration: 0.3s        | |
| | [Error]                [Details →]   | |
| +--------------------------------------+ |
|                                          |
+------------------------------------------+
```

### Mobile (< 768px)

```
+------------------------+
| AI Logs     [≡] [↗]   |
+------------------------+
| [Search logs...    🔍] |
+------------------------+
|                        |
| Today                  |
|                        |
| +--------------------+ |
| | [Wand]      3m ago | |
| | Scenario Gen       | |
| | Payment API Tests  | |
| |                    | |
| | 4,340 tokens       | |
| | [Success]          | |
| +--------------------+ |
|         ↓              |
| +--------------------+ |
| | [Alert]     1h ago | |
| | Error              | |
| | Orders API         | |
| |                    | |
| | Rate limit         | |
| | [Error]            | |
| +--------------------+ |
|                        |
| [Load more...]         |
+------------------------+
```

### Mobile Log Detail

```
+------------------------+
| [←] Log Detail         |
+------------------------+
|                        |
| Scenario Generation    |
| Payment API Tests      |
|                        |
| +--------------------+ |
| | Status   | Success | |
| | Time     | 14:32   | |
| | Duration | 4.2s    | |
| | Tokens   | 4,340   | |
| | Cost     | $0.02   | |
| +--------------------+ |
|                        |
+------------------------+
| [Conversation] [Trace] |
+------------------------+
|                        |
| SYSTEM                 |
| +--------------------+ |
| | You are a QA       | |
| | engineer...        | |
| |            [More ↓]| |
| +--------------------+ |
|                        |
| USER                   |
| +--------------------+ |
| | Generate tests for | |
| | POST /api/payments | |
| |            [More ↓]| |
| +--------------------+ |
|                        |
| ASSISTANT              |
| +--------------------+ |
| | I'll generate 5    | |
| | scenarios...       | |
| |            [More ↓]| |
| +--------------------+ |
|                        |
+------------------------+
```

---

## Component Specifications

### LogEntryCard

```typescript
interface LogEntryCardProps {
  id: string
  type: 'generation' | 'analysis' | 'validation' | 'suggestion' | 'error'
  status: 'success' | 'warning' | 'error' | 'pending'
  title: string
  context: string              // Package/Run name
  summary: string
  timestamp: Date
  metrics: {
    tokensIn: number
    tokensOut: number
    duration: number          // seconds
    cost: number              // dollars
  }
  onClick: () => void
}
```

### ConversationMessage

```typescript
interface ConversationMessageProps {
  role: 'system' | 'user' | 'assistant'
  content: string
  tokenCount?: number
  timestamp?: Date
  onCopy: () => void
  onExpand: () => void
  isExpanded?: boolean
}
```

### DecisionTraceStep

```typescript
interface DecisionTraceStepProps {
  stepNumber: number
  title: string
  decisions: {
    status: 'success' | 'warning' | 'skipped'
    description: string
  }[]
  input?: string
  output?: string
  duration?: number           // seconds
  onViewDetails?: () => void
}
```

---

## States

### Loading State

```
+------------------------------------------------------------------+
| AI Logs                                                           |
+------------------------------------------------------------------+
|                                                                   |
| +---------------------------------------------------------------+ |
| | ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ | |
| | ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ | |
| | ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Loading logs...                                                   |
+------------------------------------------------------------------+
```

### Empty State (No Logs)

```
+------------------------------------------------------------------+
|                                                                   |
|                      [Icon: AI Brain]                             |
|                                                                   |
|                     No AI Logs Yet                                |
|                                                                   |
|          AI logs will appear here when you generate               |
|           scenarios or run AI-powered analysis.                   |
|                                                                   |
|                   [Generate Scenarios]                            |
|                                                                   |
+------------------------------------------------------------------+
```

### Empty State (No Results)

```
+------------------------------------------------------------------+
|                                                                   |
|                      [Icon: Search]                               |
|                                                                   |
|                   No Logs Found                                   |
|                                                                   |
|          No logs match your search criteria.                      |
|          Try adjusting your filters.                              |
|                                                                   |
|                    [Clear Filters]                                |
|                                                                   |
+------------------------------------------------------------------+
```

### Error State

```
+------------------------------------------------------------------+
|                                                                   |
|                      [Icon: Alert]                                |
|                                                                   |
|                 Failed to Load Logs                               |
|                                                                   |
|          There was an error fetching AI logs.                     |
|          Please try again.                                        |
|                                                                   |
|                       [Retry]                                     |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Accessibility

### Screen Reader Support

```html
<!-- Log entry -->
<article
  role="article"
  aria-label="Scenario generation log, success, 3 minutes ago"
>
  <h3>Scenario Generation</h3>
  <p>Generated 5 scenarios for POST /api/payments</p>
</article>

<!-- Conversation message -->
<div
  role="log"
  aria-label="Conversation thread"
>
  <div role="listitem" aria-label="System prompt">
    ...
  </div>
  <div role="listitem" aria-label="User message">
    ...
  </div>
  <div role="listitem" aria-label="Assistant response">
    ...
  </div>
</div>
```

### Keyboard Navigation

- Tab through log entries
- Enter to open log detail
- Arrow keys to navigate within conversation
- Escape to close detail view

---

## Interactions

### Expand/Collapse

Long content (prompts, responses) are collapsed by default with "Show more" toggle.

### Copy Actions

- Copy individual messages
- Copy entire conversation thread
- Copy raw JSON

### Export

- Export single log as JSON
- Export filtered logs as CSV
- Export conversation as Markdown

---

*Last Updated: January 2026*
*Version: 1.0*
