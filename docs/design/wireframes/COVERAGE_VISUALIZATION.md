# QAWave Coverage Visualization UI Design

> Comprehensive wireframes for API coverage visualization and reporting.

## Table of Contents

1. [Overview](#overview)
2. [Coverage Dashboard](#coverage-dashboard)
3. [Endpoint Coverage View](#endpoint-coverage-view)
4. [Coverage Heatmap](#coverage-heatmap)
5. [Coverage Trends](#coverage-trends)
6. [Gap Analysis](#gap-analysis)
7. [Coverage Detail Modal](#coverage-detail-modal)
8. [Responsive Layouts](#responsive-layouts)
9. [Component Specifications](#component-specifications)

---

## Overview

### Purpose

The coverage visualization UI helps users understand:
- Which API endpoints have test scenarios
- How thoroughly each endpoint is tested
- Coverage trends over time
- Gaps in test coverage that need attention

### Coverage Metrics

| Metric | Description |
|--------|-------------|
| Endpoint Coverage | % of endpoints with at least 1 scenario |
| Method Coverage | % of HTTP methods tested per endpoint |
| Parameter Coverage | % of parameters tested (path, query, body) |
| Response Coverage | % of response codes tested |
| Scenario Depth | Average scenarios per endpoint |

---

## Coverage Dashboard

### Desktop Layout (1024px+)

```
+------------------------------------------------------------------+
| Coverage Overview                               [Package ▼] [Sync]|
+------------------------------------------------------------------+
|                                                                   |
| +------------------+ +------------------+ +------------------+     |
| |    ENDPOINTS     | |     METHODS      | |    RESPONSES    |     |
| |                  | |                  | |                 |     |
| |   ████████░░     | |   ████████░░     | |   ██████░░░░    |     |
| |      78%         | |      82%         | |      64%        |     |
| |                  | |                  | |                 |     |
| | 156/200 covered  | | 328/400 tested   | | 48/75 codes     |     |
| +------------------+ +------------------+ +------------------+     |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Coverage by Category                                [View: Grid]  |
| +---------------------------------------------------------------+ |
| |                                                               | |
| | +---------------------------+ +---------------------------+   | |
| | | [Users API]               | | [Products API]            |   | |
| | |                           | |                           |   | |
| | | ██████████ 95%            | | ████████░░ 82%            |   | |
| | |                           | |                           |   | |
| | | 42 scenarios              | | 38 scenarios              |   | |
| | | 12 endpoints              | | 15 endpoints              |   | |
| | +---------------------------+ +---------------------------+   | |
| |                                                               | |
| | +---------------------------+ +---------------------------+   | |
| | | [Orders API]              | | [Payments API]            |   | |
| | |                           | |                           |   | |
| | | ███████░░░ 68%            | | ██████░░░░ 58%            |   | |
| | |                           | |                           |   | |
| | | 28 scenarios              | | 22 scenarios              |   | |
| | | 18 endpoints              | | 10 endpoints              |   | |
| | +---------------------------+ +---------------------------+   | |
| |                                                               | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Coverage Trend (Last 30 Days)                                     |
| +---------------------------------------------------------------+ |
| |  90%|                                              ___---      | |
| |     |                                    ___---^^^             | |
| |  75%|                          ___---^^^                       | |
| |     |                ___---^^^                                 | |
| |  60%|      ___---^^^                                          | |
| |     | ---^^                                                    | |
| |  45%+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ | |
| |     Jan 1   5     10    15    20    25    30                  | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

### Coverage Summary Cards

```
+---------------------------+
|      ENDPOINTS            |
|    [Icon: API]            |
|                           |
|  ┌─────────────────────┐  |
|  │██████████░░░░░░░░░░░│  |
|  └─────────────────────┘  |
|          78%              |
|                           |
|  156 of 200 endpoints     |
|  have test scenarios      |
|                           |
|  +12 from last week       |
|      [▲ green]            |
+---------------------------+
```

### Anatomy

1. **Metric Label** - What coverage type
2. **Progress Bar** - Visual fill
3. **Percentage** - Large numeric display
4. **Absolute Numbers** - X of Y covered
5. **Trend Indicator** - Change from previous period

---

## Endpoint Coverage View

### Endpoint List with Coverage Bars

```
+------------------------------------------------------------------+
| API Endpoints                                                     |
+------------------------------------------------------------------+
| Search: [________________________] [Filter ▼] [Sort: Coverage ▼]  |
+------------------------------------------------------------------+
|                                                                   |
| +---------------------------------------------------------------+ |
| | GET /api/users                                                 | |
| | Users API > Authentication                                     | |
| | +-----------------------------------------------------------+ | |
| | | Coverage: ███████████████████████████████░░░░░░░░░░ 75%   | | |
| | +-----------------------------------------------------------+ | |
| | | Methods   | Params    | Responses | Scenarios              | | |
| | | 1/1 (100%)| 5/8 (63%) | 3/5 (60%) | 8 total                | | |
| | +-----------------------------------------------------------+ | |
| |                                            [View Details →]    | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | POST /api/users                                                | |
| | Users API > User Management                                    | |
| | +-----------------------------------------------------------+ | |
| | | Coverage: █████████████████████████████████████████ 100%  | | |
| | +-----------------------------------------------------------+ | |
| | | Methods   | Params    | Responses | Scenarios              | | |
| | | 1/1 (100%)| 6/6 (100%)| 4/4 (100%)| 12 total               | | |
| | +-----------------------------------------------------------+ | |
| |                                            [View Details →]    | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | DELETE /api/users/{id}                                         | |
| | Users API > User Management                                    | |
| | +-----------------------------------------------------------+ | |
| | | Coverage: ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 25%   | | |
| | +-----------------------------------------------------------+ | |
| | | Methods   | Params    | Responses | Scenarios              | | |
| | | 1/1 (100%)| 1/2 (50%) | 1/4 (25%) | 2 total                | | |
| | +-----------------------------------------------------------+ | |
| |                              [!] Low coverage  [View Details →]| |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

### Filter Panel

```
+----------------------------------+
| Filter Endpoints                 |
+----------------------------------+
|                                  |
| Coverage Level:                  |
| [x] All                          |
| [ ] High (>80%)                  |
| [ ] Medium (50-80%)              |
| [ ] Low (<50%)                   |
| [ ] Uncovered (0%)               |
|                                  |
| HTTP Method:                     |
| [x] GET  [x] POST  [x] PUT       |
| [x] DELETE  [x] PATCH            |
|                                  |
| Category:                        |
| [All Categories           ▼]    |
|                                  |
| Tags:                            |
| [auth] [x]  [critical] [x]       |
| [deprecated] [ ]                 |
|                                  |
| +------------------------------+ |
| |   [Reset]    [Apply Filters] | |
| +------------------------------+ |
+----------------------------------+
```

---

## Coverage Heatmap

### Full Heatmap View

```
+------------------------------------------------------------------+
| Coverage Heatmap                        [View: Week ▼] [Export]   |
+------------------------------------------------------------------+
|                                                                   |
|           | Mon | Tue | Wed | Thu | Fri | Sat | Sun |             |
| +---------+-----+-----+-----+-----+-----+-----+-----+             |
| | Users   | ██  | ██  | ██  | ██  | ██  | ░░  | ░░  |             |
| | Products| ██  | ██  | ██  | ░░  | ░░  | ░░  | ░░  |             |
| | Orders  | ██  | ░░  | ░░  | ██  | ██  | ░░  | ░░  |             |
| | Payments| ░░  | ░░  | ██  | ██  | ██  | ░░  | ░░  |             |
| | Reports | ░░  | ░░  | ░░  | ░░  | ██  | ░░  | ░░  |             |
| +---------+-----+-----+-----+-----+-----+-----+-----+             |
|                                                                   |
| Legend:                                                           |
| ░░ No tests  ▒▒ Low (1-25%)  ▓▓ Med (26-75%)  ██ High (76-100%)  |
|                                                                   |
+------------------------------------------------------------------+
```

### Method x Endpoint Heatmap

```
+------------------------------------------------------------------+
| Method Coverage Matrix                                            |
+------------------------------------------------------------------+
|                                                                   |
|                   | GET | POST | PUT | PATCH | DELETE |           |
| +-----------------+-----+------+-----+-------+--------+           |
| | /users          | ██  |  ██  | ██  |  ░░   |   ██   |           |
| | /users/{id}     | ██  |  --  | ██  |  ▓▓   |   ▒▒   |           |
| | /products       | ██  |  ██  | --  |  --   |   --   |           |
| | /products/{id}  | ██  |  --  | ██  |  ██   |   ██   |           |
| | /orders         | ▓▓  |  ▓▓  | --  |  --   |   --   |           |
| | /orders/{id}    | ██  |  --  | ▒▒  |  ▒▒   |   ░░   |           |
| +-----------------+-----+------+-----+-------+--------+           |
|                                                                   |
| -- = Method not available for endpoint                            |
|                                                                   |
+------------------------------------------------------------------+
```

### Heatmap Cell States

| State | Visual | Meaning |
|-------|--------|---------|
| Not available | `--` | Endpoint doesn't support this method |
| No coverage | `░░` Light gray | 0% - No scenarios |
| Low coverage | `▒▒` Light green | 1-25% - Minimal testing |
| Medium coverage | `▓▓` Medium green | 26-75% - Partial testing |
| High coverage | `██` Dark green | 76-100% - Well tested |

---

## Coverage Trends

### Trend Chart

```
+------------------------------------------------------------------+
| Coverage Trends                                                   |
+------------------------------------------------------------------+
|                                                                   |
| Period: [Last 30 days ▼]      Compare: [Endpoint ▼] [Response ▼] |
|                                                                   |
| +---------------------------------------------------------------+ |
| |  100%|                                                        | |
| |      |                                          ___----___    | |
| |   80%|                             ___----^^~~~            ___| |
| |      |                   ___---^^^                   ___---   | |
| |   60%|         ___---^^^                      ___---          | |
| |      |   __--^^                          __---                | |
| |   40%|--^                           __--^                     | |
| |      |                         __--^                          | |
| |   20%|                    __--^                               | |
| |      |              __--^^                                    | |
| |    0%+----+----+----+----+----+----+----+----+----+----+----+ | |
| |      W1   W2   W3   W4   W5   W6   W7   W8   W9  W10  W11  W12| |
| |                                                               | |
| |  ─── Endpoint Coverage    --- Response Coverage               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Summary:                                                          |
| +------------------+ +------------------+ +------------------+     |
| | Endpoint +18%    | | Response +12%    | | Parameter +8%   |     |
| | from baseline    | | from baseline    | | from baseline   |     |
| +------------------+ +------------------+ +------------------+     |
|                                                                   |
+------------------------------------------------------------------+
```

### Milestone Markers

```
+---------------------------------------------------------------+
|                                                     [+] Add    |
|   |                                          ↓                |
|   |                         ↓ Sprint 5      Release v2.0       |
|   |            ↓ API v1.5                                      |
|   |                                    ↓                       |
|   | ↓ Sprint 3                        New Auth                 |
|   |                                                            |
| --+----+----+----+----+----+----+----+----+----+----+----+---  |
|   Jan  Feb  Mar  Apr  May  Jun  Jul  Aug  Sep  Oct  Nov  Dec  |
+---------------------------------------------------------------+
```

---

## Gap Analysis

### Uncovered Endpoints

```
+------------------------------------------------------------------+
| Coverage Gaps                                     [Generate Report]|
+------------------------------------------------------------------+
|                                                                   |
| [!] 44 endpoints need test coverage                               |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Priority: Critical (8)                                        | |
| +---------------------------------------------------------------+ |
| |                                                               | |
| | +--------------------------+  +--------------------------+    | |
| | | DELETE /api/users/{id}   |  | POST /api/payments       |    | |
| | | 0% coverage              |  | 0% coverage              |    | |
| | |                          |  |                          |    | |
| | | [!] Critical endpoint    |  | [!] Critical endpoint    |    | |
| | | No scenarios defined     |  | No scenarios defined     |    | |
| | |                          |  |                          |    | |
| | | [+ Add Scenario]         |  | [+ Add Scenario]         |    | |
| | +--------------------------+  +--------------------------+    | |
| |                                                               | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Priority: High (12)                                  [Expand] | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Priority: Medium (18)                                [Expand] | |
| +---------------------------------------------------------------+ |
|                                                                   |
| +---------------------------------------------------------------+ |
| | Priority: Low (6)                                    [Expand] | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

### Missing Test Cases Table

```
+------------------------------------------------------------------+
| Missing Response Codes                                            |
+------------------------------------------------------------------+
| Endpoint           | Missing Codes       | Impact    | Action     |
+--------------------+---------------------+-----------+------------+
| POST /users        | 409 Conflict        | High      | [+ Add]    |
| GET /products/{id} | 404 Not Found       | Critical  | [+ Add]    |
| PUT /orders/{id}   | 403 Forbidden       | Medium    | [+ Add]    |
|                    | 409 Conflict        | Medium    | [+ Add]    |
| DELETE /users/{id} | 401 Unauthorized    | High      | [+ Add]    |
|                    | 403 Forbidden       | High      | [+ Add]    |
|                    | 404 Not Found       | High      | [+ Add]    |
+------------------------------------------------------------------+
| Showing 7 of 23 missing cases            [1] [2] [3] ... [Next →] |
+------------------------------------------------------------------+
```

### Suggested Scenarios

```
+------------------------------------------------------------------+
| AI-Suggested Scenarios                              [Regenerate]  |
+------------------------------------------------------------------+
|                                                                   |
| Based on your OpenAPI spec and existing scenarios, we suggest:    |
|                                                                   |
| +---------------------------------------------------------------+ |
| | [i] DELETE /api/users/{id} - Error Handling                   | |
| +---------------------------------------------------------------+ |
| | Missing scenarios for common error cases:                     | |
| |                                                               | |
| | [ ] Test 401 when not authenticated                           | |
| |     Assert: status = 401, body contains "unauthorized"        | |
| |                                                               | |
| | [ ] Test 403 when user lacks permission                       | |
| |     Assert: status = 403, body contains "forbidden"           | |
| |                                                               | |
| | [ ] Test 404 when user doesn't exist                          | |
| |     Assert: status = 404, body contains "not found"           | |
| |                                                               | |
| |                          [Generate Selected] [Generate All]    | |
| +---------------------------------------------------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Coverage Detail Modal

### Endpoint Coverage Detail

```
+--------------------------------------------------------------+
|                        Endpoint Coverage                   [X]|
+--------------------------------------------------------------+
|                                                               |
| GET /api/users/{id}                                           |
| Category: Users API > User Management                         |
|                                                               |
| Overall Coverage: ████████████████████░░░░░░░░░░░░░░░░ 55%   |
|                                                               |
+--------------------------------------------------------------+
|                                                               |
| BREAKDOWN                                                     |
|                                                               |
| Parameters                                    Coverage: 67%   |
| +----------------------------------------------------------+ |
| | Parameter    | Type   | Required | Tested | Scenarios    | |
| +--------------+--------+----------+--------+--------------+ |
| | id           | path   | Yes      | [✓]    | 6            | |
| | include      | query  | No       | [✓]    | 3            | |
| | fields       | query  | No       | [ ]    | 0            | |
| +----------------------------------------------------------+ |
|                                                               |
| Response Codes                                Coverage: 50%   |
| +----------------------------------------------------------+ |
| | Code | Description        | Tested | Scenarios           | |
| +------+--------------------+--------+---------------------+ |
| | 200  | Success            | [✓]    | 6                   | |
| | 400  | Bad Request        | [ ]    | 0                   | |
| | 401  | Unauthorized       | [✓]    | 2                   | |
| | 404  | Not Found          | [ ]    | 0                   | |
| +----------------------------------------------------------+ |
|                                                               |
+--------------------------------------------------------------+
|                                                               |
| LINKED SCENARIOS (8 total)                                    |
|                                                               |
| +----------------------------------------------------------+ |
| | [✓] Get user by ID - success                              | |
| |     Tests: 200, id param                                  | |
| |     Last run: 2h ago - Passed                             | |
| +----------------------------------------------------------+ |
| | [✓] Get user with include parameter                       | |
| |     Tests: 200, id param, include param                   | |
| |     Last run: 2h ago - Passed                             | |
| +----------------------------------------------------------+ |
| | [✓] Get user - unauthorized                               | |
| |     Tests: 401                                            | |
| |     Last run: 2h ago - Passed                             | |
| +----------------------------------------------------------+ |
|                                            [View All →]       |
|                                                               |
+--------------------------------------------------------------+
|                                                               |
|        [+ Add Scenario]              [View in Swagger]        |
|                                                               |
+--------------------------------------------------------------+
```

---

## Responsive Layouts

### Tablet (768px - 1023px)

```
+------------------------------------------+
| Coverage Overview        [Package ▼]     |
+------------------------------------------+
|                                          |
| +------------+ +------------+            |
| | ENDPOINTS  | | METHODS    |            |
| | ████░░ 78% | | ████░░ 82% |            |
| +------------+ +------------+            |
|                                          |
| +------------+ +------------+            |
| | RESPONSES  | | DEPTH      |            |
| | ███░░░ 64% | | ████░░ 3.2 |            |
| +------------+ +------------+            |
|                                          |
+------------------------------------------+
| Coverage by Category                     |
| +--------------------------------------+ |
| | [Users API]           ██████████ 95% | |
| | 42 scenarios | 12 endpoints          | |
| +--------------------------------------+ |
| | [Products API]        ████████░░ 82% | |
| | 38 scenarios | 15 endpoints          | |
| +--------------------------------------+ |
+------------------------------------------+
```

### Mobile (< 768px)

```
+------------------------+
| Coverage     [Filter]  |
+------------------------+
|                        |
| Overall Coverage       |
| +--------------------+ |
| |                    | |
| |  ████████████░░░░  | |
| |       72%          | |
| |                    | |
| +--------------------+ |
|                        |
+------------------------+
| Breakdown              |
| +--------------------+ |
| | Endpoints    78%   | |
| | ████████░░░░       | |
| +--------------------+ |
| | Methods      82%   | |
| | ████████░░░░       | |
| +--------------------+ |
| | Responses    64%   | |
| | ██████░░░░░░       | |
| +--------------------+ |
+------------------------+
| Gaps (44)     [View →] |
+------------------------+
| +--------------------+ |
| | DELETE /users/{id} | |
| | 0% coverage        | |
| | [!] Critical       | |
| +--------------------+ |
| | POST /payments     | |
| | 0% coverage        | |
| | [!] Critical       | |
| +--------------------+ |
| [Show more...]        |
+------------------------+
```

---

## Component Specifications

### CoverageProgressBar

```typescript
interface CoverageProgressBarProps {
  value: number                // 0-100
  size?: 'sm' | 'md' | 'lg'   // Height variant
  showLabel?: boolean          // Show percentage text
  color?: 'auto' | 'primary' | 'success' | 'warning' | 'danger'
  animate?: boolean            // Animate on mount
  threshold?: {                // Color thresholds
    low: number               // Default: 25
    medium: number            // Default: 75
  }
}
```

### Visual Variants

```
Size Variants:
sm:  ██████░░░░░░░░░░░░░░  (h-1.5)
md:  ██████████░░░░░░░░░░  (h-2)
lg:  ████████████████░░░░  (h-3)

Color by Value (auto):
0-25%:   danger  (red)
26-75%:  warning (amber)
76-100%: success (green)
```

### CoverageHeatmapCell

```typescript
interface CoverageHeatmapCellProps {
  value: number | null        // null = not applicable
  onClick?: () => void
  tooltip?: string
  showValue?: boolean
}
```

### CoverageCard

```typescript
interface CoverageCardProps {
  title: string
  value: number               // Percentage
  total: number               // X of Y
  covered: number
  trend?: {
    value: number
    direction: 'up' | 'down' | 'neutral'
  }
  icon?: ReactNode
}
```

---

## Accessibility

### Color Independence

Coverage levels always include:
- Color coding
- Numeric percentage
- Text label or pattern

### Screen Reader Support

```html
<!-- Coverage bar -->
<div
  role="meter"
  aria-valuenow="78"
  aria-valuemin="0"
  aria-valuemax="100"
  aria-label="Endpoint coverage: 78 percent"
>
  <span class="sr-only">78% coverage (156 of 200 endpoints)</span>
</div>

<!-- Heatmap cell -->
<td
  role="gridcell"
  aria-label="Users API on Monday: 92% coverage"
  tabindex="0"
>
  <span aria-hidden="true">██</span>
</td>
```

### Keyboard Navigation

- Arrow keys navigate heatmap cells
- Enter/Space opens cell details
- Tab moves between interactive elements

---

## States

### Loading State

```
+------------------------------------------------------------------+
| Coverage Overview                                                 |
+------------------------------------------------------------------+
|                                                                   |
| +------------------+ +------------------+ +------------------+     |
| | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ |     |
| | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ |     |
| | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ | | ░░░░░░░░░░░░░░░░ |     |
| +------------------+ +------------------+ +------------------+     |
|                                                                   |
| Calculating coverage...                                           |
+------------------------------------------------------------------+
```

### Empty State (No Endpoints)

```
+------------------------------------------------------------------+
|                                                                   |
|                      [Icon: API endpoints]                        |
|                                                                   |
|                   No Endpoints Discovered                         |
|                                                                   |
|          Import your OpenAPI specification to start               |
|               tracking endpoint coverage.                         |
|                                                                   |
|                      [Import OpenAPI Spec]                        |
|                                                                   |
+------------------------------------------------------------------+
```

### Empty State (No Scenarios)

```
+------------------------------------------------------------------+
|                                                                   |
|                      [Icon: Test tubes]                           |
|                                                                   |
|                   No Scenarios Created                            |
|                                                                   |
|          Create test scenarios for your API endpoints             |
|              to start measuring coverage.                         |
|                                                                   |
|                      [+ Create Scenario]                          |
|                 or  [Generate from OpenAPI]                       |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Interactions

### Hover States

| Element | Hover Effect |
|---------|--------------|
| Coverage card | Subtle shadow lift |
| Heatmap cell | Highlight + tooltip |
| Endpoint row | Background highlight |
| Progress bar | Tooltip with details |

### Click Actions

| Element | Action |
|---------|--------|
| Coverage card | Navigate to filtered view |
| Heatmap cell | Open detail modal |
| Endpoint row | Navigate to endpoint detail |
| Gap card | Navigate to add scenario |

---

*Last Updated: January 2026*
*Version: 1.0*
