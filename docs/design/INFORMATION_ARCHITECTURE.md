# QAWave Information Architecture

> Defines the content organization, navigation structure, and user mental models.

## Table of Contents

1. [Site Map](#site-map)
2. [Navigation Structure](#navigation-structure)
3. [Page Hierarchy](#page-hierarchy)
4. [URL Structure](#url-structure)
5. [User Roles & Access](#user-roles--access)
6. [Content Organization](#content-organization)

---

## Site Map

### Complete Application Structure

```
QAWave Application
│
├── / (Dashboard - redirects to /packages)
│
├── /packages
│   ├── [List] All QA Packages
│   ├── /packages/new → Create Package Modal
│   └── /packages/:packageId
│       ├── [Detail] Package Overview
│       ├── Scenarios Tab
│       ├── Runs Tab
│       ├── Coverage Tab
│       └── Settings Tab
│
├── /runs
│   └── /runs/:runId
│       ├── [Detail] Run Results
│       ├── Step Details
│       └── AI Logs
│
├── /scenarios
│   ├── [List] All Scenarios (cross-package)
│   └── /scenarios/:scenarioId
│       └── [Detail] Scenario Definition
│
├── /settings
│   ├── General
│   ├── AI Provider
│   ├── Notifications
│   └── API Keys
│
├── /auth
│   ├── /login (Keycloak redirect)
│   ├── /logout
│   └── /unauthorized
│
└── /help
    ├── Getting Started
    ├── API Reference
    └── Keyboard Shortcuts
```

---

## Navigation Structure

### Primary Navigation (Sidebar)

```
┌─────────────────────────────┐
│  📦 Packages                │  ← Primary entity
├─────────────────────────────┤
│  📋 Scenarios               │  ← Cross-cutting view
├─────────────────────────────┤
│  ⚙️ Settings                │  ← Configuration
└─────────────────────────────┘
```

### Navigation Hierarchy

| Level | Location | Content |
|-------|----------|---------|
| **L1: Global** | Header | Logo, Search, User Menu |
| **L2: Primary** | Sidebar | Main sections (Packages, Scenarios, Settings) |
| **L3: Contextual** | Tabs | Within detail pages (Overview, Runs, Coverage) |
| **L4: Local** | In-page | Filters, sorting, pagination |

### Breadcrumb Structure

```
Packages > User API Tests > Run #42 > Step 3

┌─────────────────────────────────────────────────────────────────────────┐
│ Packages / User API Tests / Run #42                                     │
│ ─────────────────────────────────────────────────────────────────────── │
│ Run #42 Details...                                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

### Navigation Patterns

| Pattern | Use Case | Behavior |
|---------|----------|----------|
| **Click-through** | Packages → Package → Run | Drill down into detail |
| **Modal overlay** | Create Package, Edit Scenario | Overlay on current context |
| **Tab navigation** | Package detail sections | Stay on page, change content |
| **Back navigation** | Return from detail | Browser back or breadcrumb |

---

## Page Hierarchy

### Page Types

| Type | Description | Examples |
|------|-------------|----------|
| **List Page** | Shows collection of items | Packages, Scenarios |
| **Detail Page** | Shows single item with context | Package Detail, Run Detail |
| **Modal** | Overlays for focused tasks | Create Package, Export |
| **Settings Page** | Configuration interface | AI Provider, Notifications |

### Page Templates

#### List Page Template
```
┌─────────────────────────────────────────────────────────────────┐
│ Page Title                                    [Primary Action]  │
│ Description text                                                │
├─────────────────────────────────────────────────────────────────┤
│ [Search]                    [Filters]           [Sort]          │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Item Card / Row 1                                           │ │
│ ├─────────────────────────────────────────────────────────────┤ │
│ │ Item Card / Row 2                                           │ │
│ ├─────────────────────────────────────────────────────────────┤ │
│ │ Item Card / Row 3                                           │ │
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│ Pagination                                                      │
└─────────────────────────────────────────────────────────────────┘
```

#### Detail Page Template
```
┌─────────────────────────────────────────────────────────────────┐
│ ← Back to List                                                  │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Title                                          [Status]     │ │
│ │ Description                                                 │ │
│ │ Metadata: Created, Updated, Owner                           │ │
│ │                                                             │ │
│ │ [Action 1] [Action 2] [Action 3]                            │ │
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│ [Tab 1] [Tab 2] [Tab 3] [Tab 4]                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                    Tab Content Area                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## URL Structure

### URL Patterns

| Pattern | Example | Description |
|---------|---------|-------------|
| `/:entity` | `/packages` | List page |
| `/:entity/new` | `/packages/new` | Create new (modal trigger) |
| `/:entity/:id` | `/packages/abc-123` | Detail page |
| `/:entity/:id/:sub` | `/packages/abc-123/runs` | Detail sub-section |
| `/settings/:section` | `/settings/ai-provider` | Settings section |

### Route Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `:packageId` | UUID | Package identifier |
| `:runId` | UUID | Test run identifier |
| `:scenarioId` | UUID | Scenario identifier |

### Query Parameters

| Parameter | Used In | Description |
|-----------|---------|-------------|
| `page` | List pages | Pagination (1-indexed) |
| `size` | List pages | Items per page |
| `sort` | List pages | Sort field and direction |
| `status` | List pages | Filter by status |
| `search` | List pages | Search query |
| `tab` | Detail pages | Active tab |

### Example URLs

```
/packages                           → Package list
/packages?status=running            → Filtered list
/packages?page=2&size=20            → Paginated list
/packages/abc-123                   → Package detail (default tab)
/packages/abc-123?tab=runs          → Package detail, runs tab
/runs/xyz-789                       → Run detail
/scenarios?package=abc-123          → Scenarios for package
/settings/ai-provider               → AI settings
```

---

## User Roles & Access

### Role Definitions

| Role | Description | Access Level |
|------|-------------|--------------|
| **Admin** | Full system access | All features, settings, user management |
| **Tester** | Create and run tests | Packages, scenarios, runs (own and team) |
| **Viewer** | Read-only access | View results, export reports |

### Permission Matrix

| Feature | Admin | Tester | Viewer |
|---------|-------|--------|--------|
| View packages | ✓ | ✓ | ✓ |
| Create package | ✓ | ✓ | ✗ |
| Edit package | ✓ | ✓ (own) | ✗ |
| Delete package | ✓ | ✓ (own) | ✗ |
| Run tests | ✓ | ✓ | ✗ |
| View results | ✓ | ✓ | ✓ |
| Export results | ✓ | ✓ | ✓ |
| Configure AI | ✓ | ✗ | ✗ |
| Manage users | ✓ | ✗ | ✗ |

### Navigation by Role

```
Admin:
├── Packages (full CRUD)
├── Scenarios (full CRUD)
├── Settings (all sections)
└── Users (manage)

Tester:
├── Packages (CRUD own)
├── Scenarios (CRUD own)
└── Settings (limited)

Viewer:
├── Packages (read)
├── Scenarios (read)
└── (no Settings)
```

---

## Content Organization

### Entity Hierarchy

```
QA Package
├── Metadata
│   ├── Name
│   ├── Description
│   ├── OpenAPI Spec URL
│   ├── Base URL
│   └── Requirements
│
├── Scenarios (1:N)
│   ├── Name
│   ├── Description
│   ├── Steps (1:N)
│   │   ├── Method
│   │   ├── Endpoint
│   │   ├── Headers
│   │   ├── Body
│   │   ├── Expected
│   │   └── Extractions
│   └── Status
│
├── Runs (1:N)
│   ├── Timestamp
│   ├── Duration
│   ├── Status
│   └── Step Results (1:N)
│       ├── Status
│       ├── Request
│       ├── Response
│       └── Assertions
│
└── Coverage
    ├── Operations covered
    ├── Operations missing
    └── Pass rate
```

### Information Grouping

#### Package Detail Tabs

| Tab | Content | Purpose |
|-----|---------|---------|
| **Overview** | Summary stats, recent activity | Quick status check |
| **Scenarios** | List of test scenarios | Manage tests |
| **Runs** | Execution history | Review results |
| **Coverage** | API coverage report | Identify gaps |
| **Settings** | Package configuration | Configure |

#### Run Detail Sections

| Section | Content | Purpose |
|---------|---------|---------|
| **Summary** | Pass/fail, duration, metadata | Quick overview |
| **Scenarios** | Expandable list with steps | Detailed results |
| **AI Logs** | Generation interactions | Debug/audit |
| **Export** | Download options | Reporting |

### Search & Filter Strategy

#### Global Search
- Searches: Package names, scenario names, descriptions
- Returns: Mixed results with type indicators
- Keyboard: `Cmd/Ctrl + K` to open

#### List Filters

| Filter | Packages | Scenarios | Runs |
|--------|----------|-----------|------|
| Status | ✓ | ✓ | ✓ |
| Date range | ✓ | ✗ | ✓ |
| Package | N/A | ✓ | ✓ |
| Method | ✗ | ✓ | ✗ |

#### Sort Options

| Sort | Packages | Scenarios | Runs |
|------|----------|-----------|------|
| Name | ✓ | ✓ | ✗ |
| Created | ✓ | ✓ | ✓ |
| Updated | ✓ | ✓ | ✗ |
| Status | ✓ | ✓ | ✓ |
| Pass rate | ✓ | ✗ | ✗ |

---

## Keyboard Shortcuts

### Global Shortcuts

| Shortcut | Action |
|----------|--------|
| `Cmd/Ctrl + K` | Open search |
| `Cmd/Ctrl + /` | Show shortcuts |
| `G + P` | Go to Packages |
| `G + S` | Go to Scenarios |
| `G + E` | Go to Settings |

### List Page Shortcuts

| Shortcut | Action |
|----------|--------|
| `N` | New item |
| `J` | Next item |
| `K` | Previous item |
| `Enter` | Open selected |
| `/` | Focus search |

### Detail Page Shortcuts

| Shortcut | Action |
|----------|--------|
| `E` | Edit |
| `R` | Run tests |
| `D` | Delete (with confirmation) |
| `Esc` | Close modal/back |

---

## Error States

### Error Page Hierarchy

| Error | Page | Recovery |
|-------|------|----------|
| **404** | /404 | Go home, go back |
| **403** | /unauthorized | Login, contact admin |
| **500** | /error | Retry, contact support |
| **Offline** | Toast | Auto-retry on connection |

### Empty States

| Context | Message | Action |
|---------|---------|--------|
| No packages | "Create your first QA package" | Create Package |
| No scenarios | "Generate scenarios from spec" | Generate |
| No runs | "Run your first test" | Run Tests |
| No results | "No matching results" | Clear filters |

---

*Last Updated: January 2026*
*Version: 1.0*
