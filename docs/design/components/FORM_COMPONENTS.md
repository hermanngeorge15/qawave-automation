# QAWave Form Component Specifications

> Detailed specifications for form elements and input components.

## Table of Contents

1. [Input](#input)
2. [Textarea](#textarea)
3. [Select](#select)
4. [Checkbox](#checkbox)
5. [Radio Group](#radio-group)
6. [Toggle](#toggle)
7. [Form Field Wrapper](#form-field-wrapper)
8. [Form Validation](#form-validation)

---

## Input

### Purpose
Single-line text input for forms. Supports various types (text, email, password, url, number).

### Anatomy

```
Label (optional)
┌─────────────────────────────────────────────────┐
│ [Icon]  Placeholder text...              [Icon] │
└─────────────────────────────────────────────────┘
Helper text or error message (optional)
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| type | `'text' \| 'email' \| 'password' \| 'url' \| 'number' \| 'search'` | 'text' | Input type |
| label | string | - | Label text |
| placeholder | string | - | Placeholder text |
| helperText | string | - | Helper/hint text below input |
| error | string | - | Error message (replaces helper text) |
| leftIcon | ReactNode | - | Icon on left side |
| rightIcon | ReactNode | - | Icon on right side |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Input size |
| disabled | boolean | false | Disable input |
| required | boolean | false | Mark as required |
| fullWidth | boolean | true | Stretch to container width |

### Visual Specs

```
Size sm:
  height: 32px
  padding: 8px 12px
  font-size: 13px
  icon-size: 16px

Size md:
  height: 40px
  padding: 10px 14px
  font-size: 14px
  icon-size: 18px

Size lg:
  height: 48px
  padding: 12px 16px
  font-size: 16px
  icon-size: 20px

Border-radius: radius-md (6px)
Border-width: 1px
```

### States

#### Default
```
border: neutral-300
background: white
text: neutral-900
placeholder: neutral-400
```

#### Hover
```
border: neutral-400
```

#### Focus
```
border: primary-500
ring: 2px primary-100
```

#### Disabled
```
background: neutral-100
border: neutral-200
text: neutral-400
cursor: not-allowed
```

#### Error
```
border: danger-500
background: danger-50
ring (on focus): 2px danger-100
```

#### Success (validated)
```
border: success-500
rightIcon: CheckCircle (success-500)
```

### Label Styling

```
font-size: text-sm (14px)
font-weight: font-medium (500)
color: neutral-700
margin-bottom: space-1 (4px)

Required indicator:
  content: " *"
  color: danger-500
```

### Helper Text Styling

```
font-size: text-xs (12px)
color: neutral-500
margin-top: space-1 (4px)

Error state:
  color: danger-500
  icon: AlertCircle (optional)
```

### Accessibility

- `aria-invalid="true"` when in error state
- `aria-describedby` pointing to helper/error text ID
- `aria-required="true"` for required fields
- Label should use `htmlFor` linking to input ID
- Focus visible outline meeting WCAG requirements

### Password Input Variant

```
┌─────────────────────────────────────────────────┐
│ ••••••••••••••                        [👁] [X] │
└─────────────────────────────────────────────────┘

Toggle button: Shows/hides password
Clear button: Clears input (optional)
```

---

## Textarea

### Purpose
Multi-line text input for longer form content.

### Anatomy

```
Label
┌─────────────────────────────────────────────────┐
│ Text content goes here...                       │
│                                                 │
│                                                 │
│                                          [↕️]   │
└─────────────────────────────────────────────────┘
Helper text                            123/500 chars
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| label | string | - | Label text |
| placeholder | string | - | Placeholder text |
| rows | number | 3 | Initial visible rows |
| maxLength | number | - | Max character count |
| showCount | boolean | false | Show character counter |
| resize | `'none' \| 'vertical' \| 'horizontal' \| 'both'` | 'vertical' | Resize behavior |
| autoResize | boolean | false | Auto-grow with content |
| error | string | - | Error message |

### Visual Specs

```
min-height: 80px (3 rows)
padding: space-3 (12px)
font-size: text-sm (14px)
line-height: 1.5
border-radius: radius-md (6px)
```

### Character Counter

```
position: bottom-right
font-size: text-xs (12px)
color: neutral-400

When approaching limit (>80%):
  color: warning-500

When at/over limit:
  color: danger-500
```

---

## Select

### Purpose
Dropdown selection from a list of options.

### Anatomy

#### Closed
```
Label
┌─────────────────────────────────────────────────┐
│ Selected option                            [▼]  │
└─────────────────────────────────────────────────┘
```

#### Open
```
┌─────────────────────────────────────────────────┐
│ Selected option                            [▲]  │
├─────────────────────────────────────────────────┤
│ ✓ Option 1 (selected)                          │
│   Option 2                                      │
│   Option 3                                      │
│   ─────────────────────────                     │
│   Option 4 (grouped)                            │
│   Option 5                                      │
└─────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| options | Array<{ value, label, disabled?, group? }> | required | Option list |
| value | string \| string[] | - | Selected value(s) |
| onChange | (value) => void | - | Selection callback |
| multiple | boolean | false | Allow multiple selection |
| searchable | boolean | false | Enable search/filter |
| placeholder | string | 'Select...' | Placeholder text |
| disabled | boolean | false | Disable select |
| error | string | - | Error message |

### Visual Specs

```
Trigger (closed):
  Same as Input component
  Chevron icon rotates on open (180deg)

Dropdown:
  background: white
  border: 1px solid neutral-200
  border-radius: radius-md (6px)
  shadow: shadow-lg
  max-height: 280px (with scroll)
  z-index: z-dropdown (10)

Option:
  padding: space-2 (8px) space-3 (12px)
  font-size: text-sm (14px)

Option hover:
  background: neutral-100

Option selected:
  background: primary-50
  color: primary-700
  checkmark: primary-500

Option disabled:
  color: neutral-400
  cursor: not-allowed

Group divider:
  border-top: 1px solid neutral-200
  margin: space-1 (4px) 0
```

### Searchable Variant

```
┌─────────────────────────────────────────────────┐
│ [🔍] Type to search...                          │
├─────────────────────────────────────────────────┤
│   Matching Option 1                             │
│   Matching Option 2                             │
│   ─────────────────────────────────────────     │
│   No matches found (when empty)                 │
└─────────────────────────────────────────────────┘
```

### Multiple Selection

```
┌─────────────────────────────────────────────────┐
│ [Option 1 ×] [Option 2 ×]              [▼]     │
└─────────────────────────────────────────────────┘

Tags:
  background: primary-100
  color: primary-700
  border-radius: radius-default (4px)
  padding: 2px 6px
  font-size: text-xs

Remove button (×):
  hover: background darkens
```

### Accessibility

- Role: `combobox` (trigger), `listbox` (dropdown)
- `aria-expanded`: true/false
- `aria-activedescendant`: points to focused option
- Keyboard: Arrow keys to navigate, Enter to select, Escape to close
- Options have `role="option"` with `aria-selected`

---

## Checkbox

### Purpose
Boolean selection or multi-select from a list.

### Anatomy

```
┌───┐
│ ✓ │  Checkbox label
└───┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| checked | boolean | false | Checked state |
| onChange | (checked: boolean) => void | - | Change callback |
| label | string \| ReactNode | - | Label content |
| indeterminate | boolean | false | Partial selection state |
| disabled | boolean | false | Disable checkbox |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Checkbox size |
| error | boolean | false | Error state |

### Visual Specs

```
Size sm:
  box: 14px × 14px
  icon: 10px
  gap: space-2 (8px)
  label font-size: text-xs

Size md:
  box: 18px × 18px
  icon: 12px
  gap: space-2 (8px)
  label font-size: text-sm

Size lg:
  box: 22px × 22px
  icon: 14px
  gap: space-3 (12px)
  label font-size: text-base

Border-radius: radius-sm (2px)
Border-width: 2px
```

### States

#### Unchecked
```
border: neutral-300
background: white
```

#### Unchecked Hover
```
border: neutral-400
background: neutral-50
```

#### Checked
```
border: primary-500
background: primary-500
icon: white checkmark
```

#### Checked Hover
```
background: primary-600
```

#### Indeterminate
```
border: primary-500
background: primary-500
icon: white horizontal line (minus)
```

#### Disabled
```
background: neutral-100
border: neutral-200
opacity: 0.6
cursor: not-allowed
```

#### Focus
```
ring: 2px primary-100
outline-offset: 2px
```

### Accessibility

- Role: `checkbox`
- `aria-checked`: true/false/mixed (for indeterminate)
- Keyboard: Space to toggle
- Label is clickable

---

## Radio Group

### Purpose
Single selection from a list of mutually exclusive options.

### Anatomy

```
Group Label

( ) Option 1
(●) Option 2 (selected)
( ) Option 3
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| value | string | - | Selected value |
| onChange | (value: string) => void | - | Selection callback |
| options | Array<{ value, label, disabled? }> | required | Options list |
| orientation | `'horizontal' \| 'vertical'` | 'vertical' | Layout direction |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Radio size |
| disabled | boolean | false | Disable all options |
| error | string | - | Error message |

### Visual Specs

```
Radio button (sizes same as Checkbox):
  border-radius: radius-full
  inner dot: 40% of radio size

Selected:
  border: primary-500
  inner dot: primary-500

Gap between options:
  vertical: space-3 (12px)
  horizontal: space-6 (24px)
```

### Accessibility

- Role: `radiogroup` on container
- Role: `radio` on each option
- `aria-checked`: true/false
- Arrow keys navigate within group
- Only selected radio is in tab order

---

## Toggle

### Purpose
Binary on/off switch, often for settings.

### Anatomy

```
Label                    ┌─────────────┐
Description text         │ ────────(●) │  ON
                         └─────────────┘

                         ┌─────────────┐
                         │ (●)──────── │  OFF
                         └─────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| checked | boolean | false | On/off state |
| onChange | (checked: boolean) => void | - | Change callback |
| label | string | - | Label text |
| description | string | - | Description text |
| size | `'sm' \| 'md' \| 'lg'` | 'md' | Toggle size |
| disabled | boolean | false | Disable toggle |
| labelPosition | `'left' \| 'right'` | 'left' | Label position |

### Visual Specs

```
Size sm:
  track: 32px × 16px
  thumb: 12px

Size md:
  track: 40px × 20px
  thumb: 16px

Size lg:
  track: 48px × 24px
  thumb: 20px

Track border-radius: radius-full
Thumb border-radius: radius-full
Transition: 200ms ease-in-out
```

### States

#### Off
```
track: neutral-300
thumb: white
thumb position: left
```

#### On
```
track: primary-500
thumb: white
thumb position: right
```

#### Hover (off)
```
track: neutral-400
```

#### Hover (on)
```
track: primary-600
```

#### Disabled
```
track: neutral-200
thumb: neutral-100
opacity: 0.6
```

### Accessibility

- Role: `switch`
- `aria-checked`: true/false
- Space/Enter to toggle
- Label is associated

---

## Form Field Wrapper

### Purpose
Consistent wrapper for all form fields, handling label, helper text, and error states.

### Anatomy

```
┌─────────────────────────────────────────────────────────┐
│ Label *                                                  │
│                                                          │
│ ┌────────────────────────────────────────────────────┐  │
│ │ [Form Control Component]                           │  │
│ └────────────────────────────────────────────────────┘  │
│                                                          │
│ Helper text or error message                             │
└─────────────────────────────────────────────────────────┘
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| label | string | - | Field label |
| required | boolean | false | Show required indicator |
| helperText | string | - | Helper text |
| error | string | - | Error message |
| children | ReactNode | required | Form control |
| htmlFor | string | - | Associate label with input |

### Spacing

```
label to input: space-1 (4px)
input to helper/error: space-1 (4px)
between form fields: space-4 (16px) or space-6 (24px)
```

---

## Form Validation

### Validation Timing

| Trigger | Behavior |
|---------|----------|
| On blur | Validate field when losing focus |
| On change | Validate after first blur (not before) |
| On submit | Validate all fields, focus first error |

### Error Display

```
Inline (per-field):
┌─────────────────────────────────────────────────────────┐
│ Email *                                                  │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ invalid@email                                  [red]│ │
│ └─────────────────────────────────────────────────────┘ │
│ ⚠ Please enter a valid email address                    │
└─────────────────────────────────────────────────────────┘

Summary (top of form):
┌─────────────────────────────────────────────────────────┐
│ ⚠ Please fix the following errors:                      │
│   • Email address is invalid                            │
│   • Password is too short                               │
└─────────────────────────────────────────────────────────┘
```

### Common Validations

| Field Type | Validation Rules |
|------------|-----------------|
| Email | Valid email format |
| URL | Valid URL with protocol |
| Password | Min 8 chars, complexity rules |
| Required | Non-empty, not just whitespace |
| Number | Valid number, min/max range |
| Date | Valid date, min/max date |

### Accessibility for Errors

```html
<div role="alert" aria-live="polite">
  <!-- Error summary appears here -->
</div>

<input
  aria-invalid="true"
  aria-describedby="email-error"
/>
<div id="email-error" role="alert">
  Please enter a valid email address
</div>
```

---

## Form Layout Patterns

### Single Column (Default)

```
┌─────────────────────────────────────────────────────────┐
│ Name *                                                   │
│ ┌─────────────────────────────────────────────────────┐ │
│ │                                                     │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                          │
│ Email *                                                  │
│ ┌─────────────────────────────────────────────────────┐ │
│ │                                                     │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                          │
│ Message                                                  │
│ ┌─────────────────────────────────────────────────────┐ │
│ │                                                     │ │
│ │                                                     │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                          │
│                              [Cancel]  [Submit]         │
└─────────────────────────────────────────────────────────┘
```

### Two Column (Desktop)

```
┌─────────────────────────────────────────────────────────┐
│ First Name *              Last Name *                    │
│ ┌────────────────────┐   ┌────────────────────┐         │
│ │                    │   │                    │         │
│ └────────────────────┘   └────────────────────┘         │
│                                                          │
│ Email *                   Phone                          │
│ ┌────────────────────┐   ┌────────────────────┐         │
│ │                    │   │                    │         │
│ └────────────────────┘   └────────────────────┘         │
│                                                          │
│ Address (full width)                                     │
│ ┌─────────────────────────────────────────────────────┐ │
│ │                                                     │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

gap between columns: space-4 (16px)
Collapses to single column on mobile
```

### Section Groups

```
─── Account Information ───────────────────────────────────

Username *
┌─────────────────────────────────────────────────────────┐
│                                                          │
└─────────────────────────────────────────────────────────┘

Email *
┌─────────────────────────────────────────────────────────┐
│                                                          │
└─────────────────────────────────────────────────────────┘

─── Security ──────────────────────────────────────────────

Password *
┌─────────────────────────────────────────────────────────┐
│                                                          │
└─────────────────────────────────────────────────────────┘

Section divider:
  border-top: 1px solid neutral-200
  padding-top: space-6 (24px)
  margin-top: space-6 (24px)

Section title:
  font-size: text-sm
  font-weight: font-semibold
  color: neutral-500
  text-transform: uppercase
  letter-spacing: 0.05em
```

---

*Last Updated: January 2026*
*Version: 1.0*
