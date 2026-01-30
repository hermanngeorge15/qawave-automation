# QAWave Component Specifications

> Detailed specifications for all UI components in the QAWave platform.

## Table of Contents

1. [StatusBadge](#statusbadge)
2. [PackageCard](#packagecard)
3. [Button](#button)
4. [Modal](#modal)
5. [EmptyState](#emptystate)
6. [Skeleton](#skeleton)
7. [Card](#card)
8. [Collapsible](#collapsible)
9. [JsonViewer](#jsonviewer)
10. [CopyButton](#copybutton)

---

## StatusBadge

### Purpose
Display the current status of test runs, scenarios, or packages with appropriate color coding.

### Anatomy

```
┌──────────────────┐
│ [Icon] Status    │
└──────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| status | `'pending' \| 'running' \| 'passed' \| 'failed' \| 'completed' \| 'error'` | required | Status to display |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Badge size |
| showIcon | boolean | true | Show status icon |

### Status Mapping

| Status | Color | Icon | Label |
|--------|-------|------|-------|
| pending | neutral-100/500 | Clock | Pending |
| running | info-100/500 | Spinner | Running |
| passed | success-100/500 | CheckCircle | Passed |
| failed | danger-100/500 | XCircle | Failed |
| completed | success-100/500 | CheckCircle | Completed |
| error | danger-100/500 | AlertTriangle | Error |

### Visual Specs

```
Size sm:  height: 20px, padding: 4px 8px, font-size: 12px
Size md:  height: 24px, padding: 4px 10px, font-size: 13px
Size lg:  height: 28px, padding: 6px 12px, font-size: 14px

Border-radius: radius-full (pill shape)
Font-weight: font-medium (500)
```

### States

- **Default**: Base background + text color
- **Hover**: Slight darkening of background (optional)
- **Animating**: (running status) Icon rotates continuously

### Accessibility

- Role: `status`
- `aria-live="polite"` for dynamic updates
- Icon has `aria-hidden="true"`
- Status text is readable by screen readers

---

## PackageCard

### Purpose
Display a QA package summary with key metrics and actions.

### Anatomy

```
┌─────────────────────────────────────────────────────┐
│  Package Name                        [StatusBadge]  │
├─────────────────────────────────────────────────────┤
│  Description text (truncated to 2 lines)...        │
│                                                     │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐               │
│  │ Passed  │ │ Failed  │ │ Total   │               │
│  │   12    │ │    3    │ │   15    │               │
│  └─────────┘ └─────────┘ └─────────┘               │
│                                                     │
│  Created: Jan 30, 2026              [Run] [View]   │
└─────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| package | Package | required | Package data object |
| onRun | () => void | - | Callback for run action |
| onView | () => void | - | Callback for view action |
| onDelete | () => void | - | Callback for delete action |
| isRunning | boolean | false | Shows loading state |

### Visual Specs

```
Card:
  background: white (dark: neutral-800)
  border: 1px solid neutral-200 (dark: neutral-700)
  border-radius: radius-lg (8px)
  padding: space-6 (24px)
  shadow: shadow-default

Hover:
  shadow: shadow-md
  border-color: primary-200

Title:
  font-size: text-lg (18px)
  font-weight: font-semibold (600)
  color: neutral-900 (dark: neutral-50)

Description:
  font-size: text-sm (14px)
  color: neutral-500
  line-clamp: 2

Metrics:
  font-size: text-2xl (24px) for numbers
  font-size: text-xs (12px) for labels
  gap: space-4 (16px) between metrics

Timestamp:
  font-size: text-xs (12px)
  color: neutral-400
```

### States

- **Default**: Standard card appearance
- **Hover**: Elevated shadow, primary border tint
- **Loading**: Run button shows spinner, other actions disabled
- **Disabled**: Reduced opacity (0.5), no interactions

### Metrics Display

```tsx
// Passed: Green background/text
<div className="bg-success-50 text-success-700">
  <span className="text-2xl font-bold">{passed}</span>
  <span className="text-xs">Passed</span>
</div>

// Failed: Red background/text
<div className="bg-danger-50 text-danger-700">
  <span className="text-2xl font-bold">{failed}</span>
  <span className="text-xs">Failed</span>
</div>

// Total: Neutral background/text
<div className="bg-neutral-100 text-neutral-700">
  <span className="text-2xl font-bold">{total}</span>
  <span className="text-xs">Total</span>
</div>
```

### Accessibility

- Role: `article`
- `aria-label`: Package name + status
- Actions are keyboard accessible
- Focus outline on card when navigating

---

## Button

### Purpose
Primary interactive element for actions.

### Anatomy

```
┌─────────────────────────────┐
│  [Icon] Label [Icon]        │
└─────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| variant | `'primary' \| 'secondary' \| 'outline' \| 'ghost' \| 'danger'` | 'primary' | Button style |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Button size |
| leftIcon | ReactNode | - | Icon before label |
| rightIcon | ReactNode | - | Icon after label |
| isLoading | boolean | false | Show loading spinner |
| disabled | boolean | false | Disable interactions |
| fullWidth | boolean | false | Stretch to container |

### Variants

#### Primary
```
default:  bg-primary-500, text-white
hover:    bg-primary-600
active:   bg-primary-700
disabled: bg-primary-200, cursor-not-allowed
```

#### Secondary
```
default:  bg-neutral-100, text-neutral-700
hover:    bg-neutral-200
active:   bg-neutral-300
disabled: bg-neutral-50, text-neutral-300
```

#### Outline
```
default:  bg-transparent, border-neutral-300, text-neutral-700
hover:    bg-neutral-50
active:   bg-neutral-100
disabled: border-neutral-200, text-neutral-300
```

#### Ghost
```
default:  bg-transparent, text-neutral-700
hover:    bg-neutral-100
active:   bg-neutral-200
disabled: text-neutral-300
```

#### Danger
```
default:  bg-danger-500, text-white
hover:    bg-danger-600
active:   bg-danger-700
disabled: bg-danger-200
```

### Sizes

```
sm:  height: 32px, padding: 8px 12px, font-size: 13px, icon: 16px
md:  height: 40px, padding: 10px 16px, font-size: 14px, icon: 20px
lg:  height: 48px, padding: 12px 24px, font-size: 16px, icon: 24px
```

### States

- **Default**: Base colors per variant
- **Hover**: Darker shade
- **Active/Pressed**: Darkest shade
- **Focus**: Ring outline (2px, primary-500, offset 2px)
- **Loading**: Spinner replaces left icon, text opacity reduced
- **Disabled**: Reduced colors, `cursor-not-allowed`

### Accessibility

- Role: `button`
- `aria-disabled` when disabled
- `aria-busy` when loading
- Minimum touch target: 44x44px (achieved via padding)
- Visible focus state

---

## Modal

### Purpose
Display overlay dialogs for forms, confirmations, and detail views.

### Anatomy

```
┌──────────────────────────────────────────────────────┐
│                    BACKDROP (dimmed)                  │
│                                                       │
│        ┌─────────────────────────────────────┐       │
│        │  Title                          [X] │       │
│        ├─────────────────────────────────────┤       │
│        │                                     │       │
│        │           Content Area              │       │
│        │                                     │       │
│        ├─────────────────────────────────────┤       │
│        │              [Cancel] [Confirm]     │       │
│        └─────────────────────────────────────┘       │
│                                                       │
└──────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| isOpen | boolean | required | Controls visibility |
| onClose | () => void | required | Close callback |
| title | string | - | Modal title |
| size | `'sm' \| 'md' \| 'lg' \| 'xl' \| 'full'` | 'md' | Modal width |
| showCloseButton | boolean | true | Show X button |
| closeOnOverlayClick | boolean | true | Close on backdrop click |
| closeOnEsc | boolean | true | Close on Escape key |
| children | ReactNode | required | Modal content |
| footer | ReactNode | - | Footer content |

### Sizes

```
sm:   max-width: 400px
md:   max-width: 540px
lg:   max-width: 720px
xl:   max-width: 900px
full: max-width: calc(100vw - 48px)
```

### Visual Specs

```
Backdrop:
  background: rgba(0, 0, 0, 0.5)
  backdrop-filter: blur(4px)

Modal:
  background: white (dark: neutral-800)
  border-radius: radius-xl (12px)
  shadow: shadow-xl
  margin: 24px (ensures spacing from viewport edges)

Header:
  padding: space-6 (24px)
  border-bottom: 1px solid neutral-200
  font-size: text-xl
  font-weight: font-semibold

Content:
  padding: space-6 (24px)
  max-height: calc(100vh - 200px)
  overflow-y: auto

Footer:
  padding: space-4 (16px) space-6 (24px)
  border-top: 1px solid neutral-200
  display: flex
  justify-content: flex-end
  gap: space-3 (12px)
```

### Animation

```
Enter:
  backdrop: fade-in 200ms ease-out
  modal: scale(0.95) + fade-in -> scale(1) 200ms ease-out

Exit:
  backdrop: fade-out 150ms ease-in
  modal: scale(1) -> scale(0.95) + fade-out 150ms ease-in
```

### Accessibility

- Role: `dialog`
- `aria-modal="true"`
- `aria-labelledby` pointing to title
- Focus trapped inside modal
- Focus returns to trigger on close
- Escape key closes modal
- Backdrop click closes (configurable)

---

## EmptyState

### Purpose
Display when no data is available, guiding users to take action.

### Anatomy

```
┌─────────────────────────────────────────────┐
│                                             │
│              [Illustration]                 │
│                                             │
│            No packages found                │
│                                             │
│     Create your first QA package to         │
│     start testing your APIs.                │
│                                             │
│           [Create Package]                  │
│                                             │
└─────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| title | string | required | Main heading |
| description | string | - | Supporting text |
| icon | ReactNode | - | Icon or illustration |
| action | { label: string, onClick: () => void } | - | CTA button |
| variant | `'default' \| 'compact'` | 'default' | Size variant |

### Visual Specs

```
Container:
  text-align: center
  padding: space-12 (48px) space-6 (24px)

Icon/Illustration:
  size: 64px (default) or 48px (compact)
  color: neutral-300
  margin-bottom: space-4 (16px)

Title:
  font-size: text-xl (20px)
  font-weight: font-semibold
  color: neutral-700
  margin-bottom: space-2 (8px)

Description:
  font-size: text-sm (14px)
  color: neutral-500
  max-width: 360px
  margin: 0 auto space-6 (24px)

Action Button:
  variant: primary
  size: md
```

### Variants

- **Default**: Full-size with illustration
- **Compact**: Smaller icon, reduced padding (for inline empty states)

---

## Skeleton

### Purpose
Placeholder content while data is loading.

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| variant | `'text' \| 'circular' \| 'rectangular'` | 'text' | Shape |
| width | string \| number | '100%' | Element width |
| height | string \| number | - | Element height |
| lines | number | 1 | Number of text lines |

### Visual Specs

```
Background: neutral-200 (dark: neutral-700)
Animation: pulse (opacity 1 -> 0.4 -> 1, 1.5s infinite)

Text:
  height: 16px (matches text-sm line height)
  border-radius: radius-default (4px)
  Last line: 60% width (if multiple lines)

Circular:
  border-radius: radius-full

Rectangular:
  border-radius: radius-md (6px)
```

### Common Patterns

```tsx
// Card skeleton
<Card>
  <Skeleton variant="text" width="60%" />
  <Skeleton variant="text" lines={2} />
  <div className="flex gap-4 mt-4">
    <Skeleton variant="rectangular" width={80} height={60} />
    <Skeleton variant="rectangular" width={80} height={60} />
    <Skeleton variant="rectangular" width={80} height={60} />
  </div>
</Card>

// Avatar + text
<div className="flex items-center gap-3">
  <Skeleton variant="circular" width={40} height={40} />
  <div>
    <Skeleton variant="text" width={120} />
    <Skeleton variant="text" width={80} />
  </div>
</div>
```

---

## Card

### Purpose
Container for grouped content with consistent styling.

### Anatomy

```
┌─────────────────────────────────────┐
│  Header (optional)                  │
├─────────────────────────────────────┤
│                                     │
│  Content                            │
│                                     │
├─────────────────────────────────────┤
│  Footer (optional)                  │
└─────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| variant | `'default' \| 'elevated' \| 'outlined'` | 'default' | Visual style |
| padding | `'none' \| 'sm' \| 'md' \| 'lg'` | 'md' | Internal padding |
| interactive | boolean | false | Adds hover effect |
| header | ReactNode | - | Card header |
| footer | ReactNode | - | Card footer |

### Variants

```
default:
  background: white (dark: neutral-800)
  border: 1px solid neutral-200 (dark: neutral-700)
  shadow: none

elevated:
  background: white (dark: neutral-800)
  border: none
  shadow: shadow-default

outlined:
  background: transparent
  border: 1px solid neutral-300
  shadow: none
```

### Padding

```
none: 0
sm:   space-3 (12px)
md:   space-4 (16px)
lg:   space-6 (24px)
```

---

## Collapsible

### Purpose
Expandable section for showing/hiding content.

### Anatomy

```
Collapsed:
┌─────────────────────────────────────┐
│  [>] Section Title                  │
└─────────────────────────────────────┘

Expanded:
┌─────────────────────────────────────┐
│  [v] Section Title                  │
├─────────────────────────────────────┤
│  Content goes here                  │
│  More content...                    │
└─────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| title | string \| ReactNode | required | Header content |
| defaultOpen | boolean | false | Initial state |
| onToggle | (open: boolean) => void | - | Toggle callback |
| disabled | boolean | false | Prevent toggling |
| children | ReactNode | required | Collapsible content |

### Visual Specs

```
Header:
  padding: space-4 (16px)
  cursor: pointer
  background: transparent
  hover: bg-neutral-50

Icon:
  ChevronRight (collapsed) / ChevronDown (expanded)
  transition: transform 200ms ease

Content:
  padding: space-4 (16px)
  padding-top: 0
  animation: slideDown 200ms ease-out
```

### Accessibility

- Role: `button` on header
- `aria-expanded`: true/false
- `aria-controls`: content id
- Enter/Space toggles state

---

## JsonViewer

### Purpose
Display formatted JSON with syntax highlighting and collapsible nodes.

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| data | object | required | JSON data to display |
| collapsed | boolean \| number | false | Initial collapse level |
| theme | `'light' \| 'dark'` | 'light' | Color theme |
| copyable | boolean | true | Show copy button |

### Visual Specs

```
Font: font-mono (JetBrains Mono)
Font-size: text-sm (14px)
Line-height: 1.5
Padding: space-4 (16px)
Background: neutral-50 (dark: neutral-900)
Border-radius: radius-md (6px)

Syntax Colors (Light):
  string: #22863a (green)
  number: #005cc5 (blue)
  boolean: #d73a49 (red)
  null: #6f42c1 (purple)
  key: #24292e (dark gray)
  bracket: #586069 (gray)

Syntax Colors (Dark):
  string: #9ecbff
  number: #79b8ff
  boolean: #f97583
  null: #b392f0
  key: #e1e4e8
  bracket: #959da5
```

---

## CopyButton

### Purpose
Copy content to clipboard with visual feedback.

### Anatomy

```
Default:    [Copy icon]
Copied:     [Check icon]
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| text | string | required | Text to copy |
| size | `'sm' \| 'md'` | 'md' | Button size |
| onCopy | () => void | - | Callback after copy |

### Visual Specs

```
Size sm: 24px x 24px, icon 14px
Size md: 32px x 32px, icon 18px

Default state:
  icon: Copy (Lucide)
  color: neutral-400
  hover: neutral-600

Copied state (2s duration):
  icon: Check (Lucide)
  color: success-500
  animation: scale up briefly
```

### Behavior

1. Click triggers copy to clipboard
2. Icon changes to checkmark
3. After 2 seconds, reverts to copy icon
4. Optional toast notification

---

## Implementation Notes

### Component File Structure

```
components/
├── ui/
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── index.ts
│   ├── Card/
│   ├── Modal/
│   └── ...
└── packages/
    ├── PackageCard/
    │   ├── PackageCard.tsx
    │   ├── PackageCard.test.tsx
    │   ├── PackageCardSkeleton.tsx
    │   └── index.ts
    └── ...
```

### Export Pattern

```typescript
// components/ui/index.ts
export { Button } from './Button';
export { Card } from './Card';
export { Modal } from './Modal';
// ... etc
```

---

*Last Updated: January 2026*
*Version: 1.0*
