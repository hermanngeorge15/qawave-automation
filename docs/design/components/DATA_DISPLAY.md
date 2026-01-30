# QAWave Data Display Component Specifications

> Detailed specifications for data presentation components.

## Table of Contents

1. [Data Table](#data-table)
2. [Pagination](#pagination)
3. [Toast / Notification](#toast--notification)
4. [Alert / Banner](#alert--banner)
5. [Progress Bar](#progress-bar)
6. [Tabs](#tabs)
7. [Badge / Tag](#badge--tag)

---

## Data Table

### Purpose
Display tabular data with sorting, filtering, and row actions.

### Anatomy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Table Header                                                                 │
│ ┌────────────────────────────────────────────────────────────────────────┐  │
│ │ [Search...]                              [Filter ▼] [Columns ▼]       │  │
│ └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│ ┌────────────────────────────────────────────────────────────────────────┐  │
│ │ ☐ │ Name ▲          │ Status    │ Created      │ Actions             │  │
│ ├────────────────────────────────────────────────────────────────────────┤  │
│ │ ☐ │ User API Tests  │ ✓ Passed  │ Jan 30, 2026 │ [Run] [View] [···] │  │
│ │ ☐ │ Auth Flow       │ ⏳ Running │ Jan 29, 2026 │ [Stop] [View] [···]│  │
│ │ ☐ │ Payment Tests   │ ✗ Failed  │ Jan 28, 2026 │ [Run] [View] [···] │  │
│ │ ☐ │ Order API       │ ○ Pending │ Jan 27, 2026 │ [Run] [View] [···] │  │
│ └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│ ┌────────────────────────────────────────────────────────────────────────┐  │
│ │ Showing 1-4 of 24                                  [◀] 1 2 3 ... 6 [▶]│  │
│ └────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| data | Array<T> | required | Table data |
| columns | Array<Column<T>> | required | Column definitions |
| selectable | boolean | false | Enable row selection |
| sortable | boolean | true | Enable column sorting |
| loading | boolean | false | Show loading state |
| emptyState | ReactNode | - | Empty state content |
| onRowClick | (row: T) => void | - | Row click handler |
| pagination | PaginationProps | - | Pagination config |
| stickyHeader | boolean | false | Sticky header on scroll |

### Column Definition

```typescript
interface Column<T> {
  key: keyof T | string;
  header: string | ReactNode;
  width?: string | number;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  render?: (value: any, row: T) => ReactNode;
}
```

### Visual Specs

```
Header Row:
  background: neutral-50
  font-weight: font-semibold
  font-size: text-xs
  text-transform: uppercase
  letter-spacing: 0.05em
  color: neutral-600
  padding: space-3 (12px) space-4 (16px)
  border-bottom: 1px solid neutral-200

Data Row:
  background: white
  font-size: text-sm
  color: neutral-700
  padding: space-3 (12px) space-4 (16px)
  border-bottom: 1px solid neutral-100

Row Hover:
  background: neutral-50

Row Selected:
  background: primary-50
  border-left: 2px solid primary-500

Sort Icon:
  size: 14px
  color: neutral-400 (inactive)
  color: neutral-700 (active)
  margin-left: space-1

Checkbox Column:
  width: 40px
  padding: space-2
```

### Loading State

```
┌────────────────────────────────────────────────────────────────────────────┐
│ ☐ │ Name              │ Status    │ Created      │ Actions              │
├────────────────────────────────────────────────────────────────────────────┤
│ ☐ │ ████████████      │ ████      │ ██████████   │ ████ ████            │
│ ☐ │ █████████████     │ █████     │ ██████████   │ ████ ████            │
│ ☐ │ ██████████        │ ████      │ ██████████   │ ████ ████            │
└────────────────────────────────────────────────────────────────────────────┘

Skeleton rows: Match expected data structure
Animation: Pulse animation
```

### Empty State

```
┌────────────────────────────────────────────────────────────────────────────┐
│ ☐ │ Name              │ Status    │ Created      │ Actions              │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│                         📋 No data found                                  │
│                                                                            │
│                    Try adjusting your filters                              │
│                    or create a new entry.                                  │
│                                                                            │
│                        [Clear Filters]                                     │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### Bulk Actions

```
When rows are selected:

┌────────────────────────────────────────────────────────────────────────────┐
│ 3 items selected           [Run Selected]  [Delete]  [Clear Selection]    │
└────────────────────────────────────────────────────────────────────────────┘

Bar:
  background: primary-50
  border: 1px solid primary-200
  padding: space-2 space-4
  border-radius: radius-md
```

### Accessibility

- Role: `table`, `thead`, `tbody`, `tr`, `th`, `td`
- Sortable headers have `aria-sort`: ascending/descending/none
- Row selection: `aria-selected`
- Loading: `aria-busy="true"`

---

## Pagination

### Purpose
Navigate through paginated data.

### Anatomy

```
Showing 1-10 of 245 results               [◀] [1] [2] [3] ... [25] [▶]
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| currentPage | number | 1 | Current page (1-indexed) |
| totalPages | number | required | Total number of pages |
| totalItems | number | - | Total item count |
| pageSize | number | 10 | Items per page |
| onPageChange | (page: number) => void | required | Page change callback |
| showItemCount | boolean | true | Show item count text |
| siblingCount | number | 1 | Pages to show around current |

### Visual Specs

```
Container:
  display: flex
  justify-content: space-between
  align-items: center
  padding: space-3 (12px) 0

Item count text:
  font-size: text-sm
  color: neutral-500

Page button:
  min-width: 32px
  height: 32px
  border-radius: radius-default (4px)
  font-size: text-sm

Page button (default):
  background: transparent
  color: neutral-600

Page button (hover):
  background: neutral-100

Page button (current):
  background: primary-500
  color: white

Page button (disabled):
  color: neutral-300
  cursor: not-allowed

Nav arrows (◀ ▶):
  Same as page buttons
  Icon size: 16px

Ellipsis:
  color: neutral-400
  cursor: default
  no hover effect
```

### Variants

#### Compact (mobile)
```
[◀] Page 3 of 25 [▶]
```

#### With Page Size Selector
```
Showing 1-10 of 245      [10 ▼] per page      [◀] [1] [2] [3] ... [▶]
```

---

## Toast / Notification

### Purpose
Temporary messages for feedback on actions.

### Anatomy

```
┌─────────────────────────────────────────────────────────┐
│ [Icon]  Message text                              [×]   │
│         Optional description                            │
│         [Action Button]                                 │
└─────────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| type | `'success' \| 'error' \| 'warning' \| 'info'` | 'info' | Toast type |
| title | string | required | Main message |
| description | string | - | Additional details |
| action | { label: string, onClick: () => void } | - | Action button |
| duration | number | 5000 | Auto-dismiss time (ms), 0 for persistent |
| position | `'top-right' \| 'top-center' \| 'bottom-right' \| 'bottom-center'` | 'bottom-right' | Screen position |
| closable | boolean | true | Show close button |

### Visual Specs

```
Container:
  min-width: 300px
  max-width: 400px
  padding: space-4 (16px)
  border-radius: radius-lg (8px)
  shadow: shadow-lg

Position from edge:
  margin: space-4 (16px)

Stack spacing (multiple toasts):
  gap: space-3 (12px)
```

### Type Variants

```
Success:
  background: success-50
  border-left: 4px solid success-500
  icon: CheckCircle (success-500)
  title color: success-700

Error:
  background: danger-50
  border-left: 4px solid danger-500
  icon: XCircle (danger-500)
  title color: danger-700

Warning:
  background: warning-50
  border-left: 4px solid warning-500
  icon: AlertTriangle (warning-500)
  title color: warning-700

Info:
  background: info-50
  border-left: 4px solid info-500
  icon: Info (info-500)
  title color: info-700
```

### Animation

```
Enter:
  from: translateX(100%) + opacity(0)
  to: translateX(0) + opacity(1)
  duration: 300ms
  easing: ease-out

Exit:
  from: opacity(1) + scale(1)
  to: opacity(0) + scale(0.95)
  duration: 200ms
  easing: ease-in

Progress bar (before auto-dismiss):
  height: 2px
  background: current type color
  width: 100% -> 0%
  duration: matches toast duration
```

### Accessibility

- Role: `alert` for errors, `status` for others
- `aria-live`: polite (or assertive for errors)
- Close button: `aria-label="Dismiss notification"`
- Focus management: Don't steal focus from user

---

## Alert / Banner

### Purpose
Persistent messages for important information or errors.

### Anatomy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Icon]  Alert title                                                    [×]  │
│         Description text with additional context about the alert.           │
│         [Action 1]  [Action 2]                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| type | `'success' \| 'error' \| 'warning' \| 'info'` | 'info' | Alert type |
| title | string | - | Alert title |
| children | ReactNode | required | Alert content |
| closable | boolean | false | Show dismiss button |
| onClose | () => void | - | Close callback |
| actions | Array<{ label, onClick, variant }> | - | Action buttons |
| variant | `'filled' \| 'outlined' \| 'subtle'` | 'subtle' | Visual style |

### Variants

#### Subtle (default)
```
background: type-50
border: 1px solid type-200
```

#### Filled
```
background: type-500
color: white
icon: white
```

#### Outlined
```
background: white
border: 1px solid type-500
```

### Visual Specs

```
Container:
  padding: space-4 (16px)
  border-radius: radius-md (6px)

Icon:
  size: 20px
  flex-shrink: 0
  margin-right: space-3 (12px)

Title:
  font-weight: font-semibold
  margin-bottom: space-1 (4px)

Content:
  font-size: text-sm
  color: type-700 (or white for filled)

Actions:
  margin-top: space-3 (12px)
  gap: space-2 (8px)
```

---

## Progress Bar

### Purpose
Show completion status of a process.

### Anatomy

```
Label                                                    75%
█████████████████████████████████████████░░░░░░░░░░░░░░░░
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| value | number | 0 | Progress value (0-100) |
| label | string | - | Label text |
| showValue | boolean | true | Show percentage |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Bar height |
| color | `'primary' \| 'success' \| 'warning' \| 'danger'` | 'primary' | Bar color |
| indeterminate | boolean | false | Unknown progress |
| striped | boolean | false | Striped pattern |
| animated | boolean | false | Animate stripes |

### Visual Specs

```
Size sm: height 4px
Size md: height 8px
Size lg: height 12px

Track:
  background: neutral-200
  border-radius: radius-full

Bar:
  border-radius: radius-full
  transition: width 300ms ease

Label/Value:
  font-size: text-sm
  font-weight: font-medium
  margin-bottom: space-1 (4px)
```

### Indeterminate Animation

```
Bar moves left to right continuously:
  width: 30%
  animation: indeterminate 1.5s infinite ease-in-out

@keyframes indeterminate {
  0% { left: -30%; }
  100% { left: 100%; }
}
```

### Striped Pattern

```
background-image: linear-gradient(
  45deg,
  rgba(255,255,255,0.15) 25%,
  transparent 25%,
  transparent 50%,
  rgba(255,255,255,0.15) 50%,
  rgba(255,255,255,0.15) 75%,
  transparent 75%,
  transparent
);
background-size: 1rem 1rem;

Animated:
  animation: progress-stripes 1s linear infinite;
```

---

## Tabs

### Purpose
Organize content into multiple panels, showing one at a time.

### Anatomy

```
┌─────────────┬─────────────┬─────────────┐
│  Tab 1      │  Tab 2 (2)  │  Tab 3      │
│  (active)   │             │             │
└─────────────┴─────────────┴─────────────┘
│                                          │
│            Tab content panel             │
│                                          │
└──────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| tabs | Array<{ key, label, badge?, icon?, content, disabled? }> | required | Tab definitions |
| activeKey | string | - | Controlled active tab |
| defaultActiveKey | string | - | Initial active tab |
| onChange | (key: string) => void | - | Tab change callback |
| variant | `'line' \| 'enclosed' \| 'pills'` | 'line' | Visual style |
| size | `'sm' \| 'md'` | 'md' | Tab size |
| fullWidth | boolean | false | Stretch tabs to fill |

### Variant: Line (default)

```
┌─────────────┬─────────────┬─────────────┐
│  Tab 1      │  Tab 2      │  Tab 3      │
├═════════════┼─────────────┴─────────────┘
│ ═══════════ │
└─────────────────────────────────────────┘

Active indicator:
  height: 2px
  background: primary-500
  position: bottom
  transition: left 200ms ease
```

### Variant: Enclosed

```
┌─────────────┐─────────────┐─────────────┐
│  Tab 1      │  Tab 2      │  Tab 3      │
│  (active)   │             │             │
└─────────────┘─────────────┴─────────────┴
│                                          │
│            Content with border           │
│                                          │
└──────────────────────────────────────────┘

Active tab:
  background: white
  border-bottom: none (merges with content)

Inactive:
  background: neutral-50
  border: 1px solid neutral-200
```

### Variant: Pills

```
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│   Tab 1       │ │   Tab 2       │ │   Tab 3       │
│  (selected)   │ │               │ │               │
└───────────────┘ └───────────────┘ └───────────────┘

Active:
  background: primary-500
  color: white
  border-radius: radius-full

Inactive:
  background: transparent
  color: neutral-600
  hover: neutral-100
```

### Visual Specs

```
Size sm:
  padding: space-2 (8px) space-3 (12px)
  font-size: text-sm

Size md:
  padding: space-2 (8px) space-4 (16px)
  font-size: text-sm

Tab gap: space-1 (4px) for pills, 0 for others

Badge:
  margin-left: space-2 (8px)
  padding: 0 space-2 (8px)
  height: 18px
  border-radius: radius-full
  font-size: text-xs
  background: neutral-100
  color: neutral-600
```

### Accessibility

- Role: `tablist` on container
- Role: `tab` on each tab button
- Role: `tabpanel` on content
- `aria-selected`: true for active tab
- `aria-controls`: links tab to panel
- Arrow keys navigate tabs
- Tab focuses active tab, not all tabs

---

## Badge / Tag

### Purpose
Labels for categorization and status indication.

### Anatomy

```
[Icon] Label [×]
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| label | string | required | Badge text |
| color | `'primary' \| 'success' \| 'warning' \| 'danger' \| 'neutral'` | 'neutral' | Color variant |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Badge size |
| variant | `'solid' \| 'subtle' \| 'outline'` | 'subtle' | Fill style |
| icon | ReactNode | - | Leading icon |
| removable | boolean | false | Show remove button |
| onRemove | () => void | - | Remove callback |

### Visual Specs

```
Size sm:
  height: 18px
  padding: 0 space-2 (8px)
  font-size: 11px

Size md:
  height: 22px
  padding: 0 space-2 (8px)
  font-size: 12px

Size lg:
  height: 26px
  padding: 0 space-3 (12px)
  font-size: 13px

Border-radius: radius-default (4px)
Font-weight: font-medium
```

### Variant: Solid

```
background: color-500
color: white
```

### Variant: Subtle

```
background: color-100
color: color-700
```

### Variant: Outline

```
background: transparent
border: 1px solid color-300
color: color-700
```

### Remove Button

```
margin-left: space-1 (4px)
padding: 2px
border-radius: radius-full
hover: background darkens slightly

Icon: × (X)
size: 12px
```

---

*Last Updated: January 2026*
*Version: 1.0*
