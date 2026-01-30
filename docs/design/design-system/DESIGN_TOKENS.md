# QAWave Design Tokens

> This document defines the visual language of QAWave. All frontend components should use these tokens for consistency.

## Color Palette

### Brand Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `primary-50` | #EEF2FF | Primary backgrounds, hover states |
| `primary-100` | #E0E7FF | Selected states, borders |
| `primary-500` | #6366F1 | Primary actions, links |
| `primary-600` | #4F46E5 | Hover state for primary |
| `primary-700` | #4338CA | Active/pressed state |

### Semantic Colors

#### Success (Test Passed)
| Token | Hex | Usage |
|-------|-----|-------|
| `success-50` | #F0FDF4 | Success background |
| `success-100` | #DCFCE7 | Success border |
| `success-500` | #22C55E | Success text, icons |
| `success-700` | #15803D | Success emphasis |

#### Warning (In Progress)
| Token | Hex | Usage |
|-------|-----|-------|
| `warning-50` | #FFFBEB | Warning background |
| `warning-100` | #FEF3C7 | Warning border |
| `warning-500` | #F59E0B | Warning text, icons |
| `warning-700` | #B45309 | Warning emphasis |

#### Danger (Test Failed)
| Token | Hex | Usage |
|-------|-----|-------|
| `danger-50` | #FEF2F2 | Error background |
| `danger-100` | #FEE2E2 | Error border |
| `danger-500` | #EF4444 | Error text, icons |
| `danger-700` | #B91C1C | Error emphasis |

#### Info (Running)
| Token | Hex | Usage |
|-------|-----|-------|
| `info-50` | #EFF6FF | Info background |
| `info-100` | #DBEAFE | Info border |
| `info-500` | #3B82F6 | Info text, icons |
| `info-700` | #1D4ED8 | Info emphasis |

### Neutral Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `neutral-50` | #F9FAFB | Page background |
| `neutral-100` | #F3F4F6 | Card background, hover |
| `neutral-200` | #E5E7EB | Borders, dividers |
| `neutral-300` | #D1D5DB | Disabled borders |
| `neutral-400` | #9CA3AF | Placeholder text |
| `neutral-500` | #6B7280 | Secondary text |
| `neutral-600` | #4B5563 | Body text |
| `neutral-700` | #374151 | Heading text |
| `neutral-800` | #1F2937 | Dark text |
| `neutral-900` | #111827 | Black text |

### Dark Mode Mapping

| Light Mode | Dark Mode |
|------------|-----------|
| `neutral-50` (bg) | `neutral-900` |
| `neutral-100` (card) | `neutral-800` |
| `neutral-200` (border) | `neutral-700` |
| `neutral-600` (text) | `neutral-300` |
| `neutral-900` (heading) | `neutral-50` |

## Typography

### Font Stack

```css
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
font-family-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

### Type Scale

| Token | Size | Line Height | Weight | Usage |
|-------|------|-------------|--------|-------|
| `text-xs` | 12px | 16px (1.33) | 400 | Labels, badges |
| `text-sm` | 14px | 20px (1.43) | 400 | Secondary text, table cells |
| `text-base` | 16px | 24px (1.5) | 400 | Body text |
| `text-lg` | 18px | 28px (1.56) | 500 | Large body |
| `text-xl` | 20px | 28px (1.4) | 600 | H4 heading |
| `text-2xl` | 24px | 32px (1.33) | 600 | H3 heading |
| `text-3xl` | 30px | 36px (1.2) | 700 | H2 heading |
| `text-4xl` | 36px | 40px (1.11) | 700 | H1 heading |

### Font Weights

| Token | Value | Usage |
|-------|-------|-------|
| `font-normal` | 400 | Body text |
| `font-medium` | 500 | Emphasized text, labels |
| `font-semibold` | 600 | Subheadings |
| `font-bold` | 700 | Headings |

## Spacing

### Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `space-0` | 0px | None |
| `space-1` | 4px | Tight spacing, icons |
| `space-2` | 8px | Inside components |
| `space-3` | 12px | Component padding |
| `space-4` | 16px | Default gap |
| `space-5` | 20px | Section padding |
| `space-6` | 24px | Card padding |
| `space-8` | 32px | Section gaps |
| `space-10` | 40px | Large sections |
| `space-12` | 48px | Page sections |
| `space-16` | 64px | Major sections |

### Common Patterns

```
Card padding: space-6 (24px)
Card gap: space-4 (16px)
List item gap: space-3 (12px)
Button padding: space-2 (8px) horizontal, space-3 (12px) vertical
Form field gap: space-4 (16px)
Page margin: space-6 (24px) mobile, space-8 (32px) desktop
```

## Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius-none` | 0px | No rounding |
| `radius-sm` | 2px | Inputs |
| `radius-default` | 4px | Buttons, badges |
| `radius-md` | 6px | Cards, modals |
| `radius-lg` | 8px | Large cards |
| `radius-xl` | 12px | Floating panels |
| `radius-2xl` | 16px | Hero cards |
| `radius-full` | 9999px | Pills, avatars |

## Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `shadow-sm` | 0 1px 2px rgba(0,0,0,0.05) | Subtle elevation |
| `shadow-default` | 0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06) | Cards |
| `shadow-md` | 0 4px 6px rgba(0,0,0,0.1) | Dropdowns |
| `shadow-lg` | 0 10px 15px rgba(0,0,0,0.1) | Modals |
| `shadow-xl` | 0 20px 25px rgba(0,0,0,0.15) | Popovers |

## Breakpoints

| Token | Value | Description |
|-------|-------|-------------|
| `sm` | 640px | Mobile landscape |
| `md` | 768px | Tablet portrait |
| `lg` | 1024px | Tablet landscape / Small desktop |
| `xl` | 1280px | Desktop |
| `2xl` | 1536px | Large desktop |

### Responsive Behavior

```
Mobile (< 640px):
- Single column layouts
- Stacked navigation
- Full-width cards
- Collapsible sidebars

Tablet (640px - 1024px):
- 2-column grids
- Visible sidebar (collapsed)
- Card grids

Desktop (> 1024px):
- 3+ column grids
- Full sidebar navigation
- Side-by-side layouts
```

## Animation

### Durations

| Token | Value | Usage |
|-------|-------|-------|
| `duration-fast` | 100ms | Micro-interactions (hover) |
| `duration-normal` | 200ms | Standard transitions |
| `duration-slow` | 300ms | Page transitions |
| `duration-slower` | 500ms | Complex animations |

### Easing

| Token | Value | Usage |
|-------|-------|-------|
| `ease-in` | cubic-bezier(0.4, 0, 1, 1) | Elements leaving |
| `ease-out` | cubic-bezier(0, 0, 0.2, 1) | Elements entering |
| `ease-in-out` | cubic-bezier(0.4, 0, 0.2, 1) | Default transitions |

## Z-Index Scale

| Token | Value | Usage |
|-------|-------|-------|
| `z-base` | 0 | Base layer |
| `z-dropdown` | 10 | Dropdowns |
| `z-sticky` | 20 | Sticky headers |
| `z-fixed` | 30 | Fixed elements |
| `z-modal-backdrop` | 40 | Modal backdrop |
| `z-modal` | 50 | Modals |
| `z-popover` | 60 | Popovers |
| `z-tooltip` | 70 | Tooltips |
| `z-toast` | 80 | Toast notifications |

## Icons

### Size Scale

| Token | Value | Usage |
|-------|-------|-------|
| `icon-xs` | 12px | Inline with small text |
| `icon-sm` | 16px | Inline with body text |
| `icon-md` | 20px | Default buttons |
| `icon-lg` | 24px | Feature icons |
| `icon-xl` | 32px | Empty states |

### Recommended Icon Set

Use **Lucide Icons** for consistency:
- Consistent 24px grid
- 1.5px stroke weight
- Rounded caps and joins

## Implementation

### Tailwind CSS Config

```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#EEF2FF',
          100: '#E0E7FF',
          500: '#6366F1',
          600: '#4F46E5',
          700: '#4338CA',
        },
        success: {
          50: '#F0FDF4',
          500: '#22C55E',
          700: '#15803D',
        },
        warning: {
          50: '#FFFBEB',
          500: '#F59E0B',
          700: '#B45309',
        },
        danger: {
          50: '#FEF2F2',
          500: '#EF4444',
          700: '#B91C1C',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
};
```

---

*Last Updated: January 2026*
*Version: 1.0*
