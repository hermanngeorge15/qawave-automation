# QAWave Dashboard Design

> Design specifications for the main dashboard and overview pages.

## Overview

The dashboard provides at-a-glance visibility into QA package status, recent activity, and key metrics.

---

## Dashboard Layout

### Desktop (> 1024px)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ← Sidebar                                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Welcome back, John!                                    [+ New Package]      │
│  ─────────────────────────────────────────────────────────────────────────── │
│                                                                              │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ │
│  │   PACKAGES     │ │   SCENARIOS    │ │    RUNS        │ │   PASS RATE    │ │
│  │      12        │ │      156       │ │     48         │ │     87.5%      │ │
│  │   +2 this week │ │   +24 this week│ │   +12 today    │ │   ↑ 3.2%       │ │
│  └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘ │
│                                                                              │
│  ┌─────────────────────────────────────────┐ ┌──────────────────────────────┐│
│  │ RECENT RUNS                             │ │ QUICK ACTIONS                ││
│  │ ─────────────────────────────────────── │ │ ────────────────────────────  ││
│  │                                         │ │                              ││
│  │ ┌─────────────────────────────────────┐ │ │ [▶ Run All Tests]            ││
│  │ │ ✓ User API Tests     2 min ago     │ │ │                              ││
│  │ │   8/10 passed                      │ │ │ [+ Create Package]           ││
│  │ └─────────────────────────────────────┘ │ │                              ││
│  │ ┌─────────────────────────────────────┐ │ │ [📄 View Reports]            ││
│  │ │ ✗ Payment Tests      15 min ago    │ │ │                              ││
│  │ │   3/8 passed - 5 failed            │ │ │ [⚙️ Settings]                 ││
│  │ └─────────────────────────────────────┘ │ │                              ││
│  │ ┌─────────────────────────────────────┐ │ └──────────────────────────────┘│
│  │ │ ⏳ Auth Flow          Running...    │ │                                │
│  │ │   5/8 completed (62%)              │ │ ┌──────────────────────────────┐│
│  │ └─────────────────────────────────────┘ │ │ COVERAGE OVERVIEW            ││
│  │                                         │ │ ────────────────────────────  ││
│  │ [View All Runs →]                       │ │                              ││
│  └─────────────────────────────────────────┘ │ │ User API: ██████████ 95%   ││
│                                              │ │ Payment:  ████████░░ 80%   ││
│  ┌───────────────────────────────────────────┘ │ Auth:     ██████░░░░ 60%   ││
│  │ PACKAGES BY STATUS                          │ Orders:   ████░░░░░░ 40%   ││
│  │ ──────────────────                          │ │                              ││
│  │                                             │ │ [View Full Report →]         ││
│  │ ┌───────────┐ ┌───────────┐ ┌───────────┐   │ └──────────────────────────────┘│
│  │ │ 8 Passing │ │ 2 Failing │ │ 2 Pending │   │                                │
│  │ │    66%    │ │    17%    │ │    17%    │   │                                │
│  │ └───────────┘ └───────────┘ └───────────┘   │                                │
│  │                                             │                                │
│  └─────────────────────────────────────────────┘                                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Mobile (< 768px)

```
┌─────────────────────────────────┐
│  Welcome, John!       [+ New]   │
├─────────────────────────────────┤
│                                 │
│  ┌──────────┐ ┌──────────┐     │
│  │ PACKAGES │ │ PASS RATE│     │
│  │    12    │ │   87.5%  │     │
│  └──────────┘ └──────────┘     │
│                                 │
│  Recent Runs                    │
│  ─────────────────────────────  │
│                                 │
│  ┌─────────────────────────────┐│
│  │ ✓ User API Tests            ││
│  │   8/10 passed • 2 min ago   ││
│  └─────────────────────────────┘│
│                                 │
│  ┌─────────────────────────────┐│
│  │ ✗ Payment Tests             ││
│  │   3/8 passed • 15 min ago   ││
│  └─────────────────────────────┘│
│                                 │
│  [View All →]                   │
│                                 │
└─────────────────────────────────┘
```

---

## Summary Cards

### Card Specifications

```
┌─────────────────────────────────┐
│  METRIC LABEL           [icon] │
│  ──────────────────────────────│
│        42                      │
│                                │
│  +12 this week        ↑ 15%   │
└─────────────────────────────────┘
```

### Card Props

| Prop | Type | Description |
|------|------|-------------|
| label | string | Metric name |
| value | number/string | Primary value |
| change | { value, direction } | Period comparison |
| icon | ReactNode | Optional icon |
| href | string | Optional link |

### Visual Specs

```
Card:
  background: white (dark: neutral-800)
  border: 1px solid neutral-200
  border-radius: radius-lg (8px)
  padding: space-5 (20px)
  min-width: 180px

Label:
  font-size: text-xs (12px)
  font-weight: font-medium
  color: neutral-500
  text-transform: uppercase
  letter-spacing: 0.05em

Value:
  font-size: text-3xl (30px)
  font-weight: font-bold
  color: neutral-900

Change:
  font-size: text-sm
  color: success-600 (positive) / danger-600 (negative)

Icon:
  position: top-right
  size: 24px
  color: primary-500
```

---

## Recent Runs Section

### Run Item Component

```
┌─────────────────────────────────────────────────────────────────────┐
│ [Status Icon]  Package Name                           Time ago     │
│                Results summary                        [View →]     │
└─────────────────────────────────────────────────────────────────────┘
```

### Status Icons

| Status | Icon | Color |
|--------|------|-------|
| Passed | ✓ CheckCircle | success-500 |
| Failed | ✗ XCircle | danger-500 |
| Running | ⏳ Loader (animated) | info-500 |
| Pending | ○ Circle | neutral-400 |

### Visual Specs

```
Container:
  border: 1px solid neutral-100
  border-radius: radius-md
  padding: space-4

  hover:
    background: neutral-50
    border-color: neutral-200

Status Icon:
  size: 20px
  margin-right: space-3

Package Name:
  font-weight: font-medium
  color: neutral-900

Time:
  font-size: text-sm
  color: neutral-500

Results:
  font-size: text-sm
  color: neutral-600
```

### Progress Bar (Running)

```
┌─────────────────────────────────────────────────────────────────────┐
│ ⏳ Auth Flow Tests                                    Running...    │
│    5/8 completed                                                    │
│    ████████████████████░░░░░░░░░░░ 62%                             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Actions Section

### Button Stack

```
┌─────────────────────────────────┐
│ QUICK ACTIONS                   │
│ ─────────────────────────────── │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ▶  Run All Tests            │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ +  Create Package           │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📄 View Reports             │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ⚙️  Settings                │ │
│ └─────────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

### Action Button Specs

```
Button:
  width: 100%
  padding: space-3 space-4
  border: 1px solid neutral-200
  border-radius: radius-md
  background: white
  text-align: left

  hover:
    background: neutral-50
    border-color: neutral-300

Icon:
  size: 18px
  margin-right: space-3
  color: primary-500

Text:
  font-size: text-sm
  font-weight: font-medium
  color: neutral-700
```

---

## Coverage Overview

### Mini Coverage Bars

```
┌─────────────────────────────────┐
│ COVERAGE OVERVIEW               │
│ ─────────────────────────────── │
│                                 │
│ User API    ██████████░ 95%     │
│ Payment     ████████░░░ 80%     │
│ Auth Flow   ██████░░░░░ 60%     │
│ Orders      ████░░░░░░░ 40%     │
│                                 │
│ [View Full Report →]            │
└─────────────────────────────────┘
```

### Visual Specs

```
Row:
  display: flex
  align-items: center
  margin-bottom: space-2

Label:
  width: 80px
  font-size: text-sm
  color: neutral-600

Bar Container:
  flex: 1
  height: 8px
  background: neutral-200
  border-radius: radius-full
  overflow: hidden

Bar Fill:
  height: 100%
  background: primary-500
  border-radius: radius-full

  // Color by percentage
  > 80%: success-500
  > 50%: primary-500
  > 25%: warning-500
  <= 25%: danger-500

Percentage:
  width: 40px
  text-align: right
  font-size: text-sm
  font-weight: font-medium
  color: neutral-700
```

---

## Packages by Status

### Status Distribution

```
┌───────────────────────────────────────────────────────────────────┐
│ PACKAGES BY STATUS                                                │
│ ─────────────────                                                 │
│                                                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │                 │  │                 │  │                 │   │
│  │     8           │  │     2           │  │     2           │   │
│  │   Passing       │  │   Failing       │  │   Pending       │   │
│  │    66%          │  │    17%          │  │    17%          │   │
│  │                 │  │                 │  │                 │   │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘   │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### Status Card Specs

```
Card:
  flex: 1
  min-width: 120px
  padding: space-4
  border-radius: radius-md
  text-align: center
  cursor: pointer

  Passing:
    background: success-50
    border: 1px solid success-200

  Failing:
    background: danger-50
    border: 1px solid danger-200

  Pending:
    background: neutral-100
    border: 1px solid neutral-200

Count:
  font-size: text-2xl
  font-weight: font-bold
  color: respective status color (700 shade)

Label:
  font-size: text-sm
  color: respective status color (600 shade)

Percentage:
  font-size: text-xs
  color: neutral-500
```

---

## Empty State

When no packages exist:

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                                                  │
│                         📦                                       │
│                                                                  │
│              Welcome to QAWave!                                  │
│                                                                  │
│     Get started by creating your first QA package.              │
│     Upload an OpenAPI spec and let AI generate tests.           │
│                                                                  │
│              [Create Your First Package]                         │
│                                                                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Loading State

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  Welcome back!                                                   │
│                                                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │ ████████████ │ │ ████████████ │ │ ████████████ │             │
│  │ ██████       │ │ ██████       │ │ ██████       │             │
│  └──────────────┘ └──────────────┘ └──────────────┘             │
│                                                                  │
│  ┌────────────────────────────────────────────────┐             │
│  │ ██████████████████████████████████████████    │             │
│  │ ████████████████████████                      │             │
│  │ ██████████████████████████████████████████    │             │
│  └────────────────────────────────────────────────┘             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

Skeleton animation: pulse
```

---

## Responsive Breakpoints

| Breakpoint | Layout Changes |
|------------|----------------|
| `< 640px` | Single column, stacked cards |
| `640px - 768px` | 2 summary cards per row |
| `768px - 1024px` | 4 summary cards, stacked sections |
| `> 1024px` | Full 2-column layout |

---

*Last Updated: January 2026*
*Version: 1.0*
