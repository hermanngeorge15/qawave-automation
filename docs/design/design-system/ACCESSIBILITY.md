# QAWave Accessibility Guidelines

> WCAG 2.1 AA compliance requirements and implementation patterns.

## Table of Contents

1. [Overview](#overview)
2. [Color & Contrast](#color--contrast)
3. [Keyboard Navigation](#keyboard-navigation)
4. [Screen Reader Support](#screen-reader-support)
5. [Focus Management](#focus-management)
6. [Forms & Validation](#forms--validation)
7. [Component Checklist](#component-checklist)
8. [Testing Guidelines](#testing-guidelines)

---

## Overview

QAWave is committed to WCAG 2.1 Level AA compliance. This means our UI must be:

- **Perceivable**: Information must be presentable in ways all users can perceive
- **Operable**: UI components must be operable by all users
- **Understandable**: Information and UI operation must be understandable
- **Robust**: Content must be robust enough for assistive technologies

### Key Requirements

| Requirement | Standard | Status |
|-------------|----------|--------|
| Color contrast (text) | 4.5:1 minimum | Required |
| Color contrast (large text) | 3:1 minimum | Required |
| Color contrast (UI components) | 3:1 minimum | Required |
| Keyboard accessibility | All interactive elements | Required |
| Focus visible | 2px outline minimum | Required |
| Screen reader support | Semantic HTML + ARIA | Required |
| Motion reduction | Respect prefers-reduced-motion | Required |

---

## Color & Contrast

### Minimum Contrast Ratios

| Element Type | Minimum Ratio | Notes |
|--------------|---------------|-------|
| Normal text (< 18px) | 4.5:1 | Against background |
| Large text (>= 18px bold or >= 24px) | 3:1 | Against background |
| UI components (buttons, inputs) | 3:1 | Border/background contrast |
| Icons (informational) | 3:1 | When conveying meaning |
| Disabled elements | No requirement | But should be visually distinct |

### Verified Color Combinations

#### Text on Light Backgrounds

| Foreground | Background | Ratio | Pass |
|------------|------------|-------|------|
| neutral-900 (#111827) | neutral-50 (#F9FAFB) | 17.4:1 | AAA |
| neutral-700 (#374151) | neutral-50 (#F9FAFB) | 9.8:1 | AAA |
| neutral-600 (#4B5563) | neutral-50 (#F9FAFB) | 7.0:1 | AAA |
| neutral-500 (#6B7280) | neutral-50 (#F9FAFB) | 4.7:1 | AA |
| primary-700 (#4338CA) | white | 8.6:1 | AAA |
| primary-600 (#4F46E5) | white | 6.3:1 | AAA |
| danger-700 (#B91C1C) | white | 5.8:1 | AA |
| success-700 (#15803D) | white | 5.1:1 | AA |

#### Text on Dark Backgrounds

| Foreground | Background | Ratio | Pass |
|------------|------------|-------|------|
| white | primary-500 (#6366F1) | 4.6:1 | AA |
| white | primary-600 (#4F46E5) | 6.3:1 | AAA |
| white | danger-500 (#EF4444) | 4.5:1 | AA |
| white | success-500 (#22C55E) | 3.0:1 | AA (large only) |
| neutral-50 (#F9FAFB) | neutral-900 (#111827) | 17.4:1 | AAA |

### Color Independence

Never rely on color alone to convey information:

```
❌ Bad: Red border means error
✅ Good: Red border + error icon + error text

❌ Bad: Green means passed, red means failed
✅ Good: ✓ Passed (green) vs ✗ Failed (red) - icon + text + color
```

### Dark Mode Considerations

```css
/* Light mode */
--text-primary: var(--neutral-900);
--text-secondary: var(--neutral-600);
--bg-primary: var(--neutral-50);
--bg-secondary: white;

/* Dark mode */
--text-primary: var(--neutral-50);
--text-secondary: var(--neutral-300);
--bg-primary: var(--neutral-900);
--bg-secondary: var(--neutral-800);
```

---

## Keyboard Navigation

### Tab Order

- Tab order follows visual left-to-right, top-to-bottom flow
- Skip links available for main content
- No keyboard traps (user can always navigate away)

### Focus Order Example

```
[1] Skip to main content link
[2] Logo/home link
[3] Navigation items (in order)
[4] Main content area
    [4.1] First interactive element
    [4.2] Second interactive element
    ...
[5] Footer links
```

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| Tab | Move focus forward |
| Shift + Tab | Move focus backward |
| Enter | Activate buttons/links |
| Space | Activate buttons, check checkboxes |
| Escape | Close modals/dropdowns |
| Arrow keys | Navigate within components (menus, tabs, radio groups) |
| Home/End | Jump to first/last item in list |

### Component-Specific Navigation

#### Modal
```
1. Focus moves to modal on open
2. Focus is trapped inside modal
3. Escape closes modal
4. Focus returns to trigger on close
```

#### Dropdown/Menu
```
1. Enter/Space opens menu
2. Arrow keys navigate items
3. Enter selects item
4. Escape closes without selection
5. Focus returns to trigger
```

#### Tabs
```
1. Tab focuses the tab list
2. Arrow keys switch tabs
3. Tab moves to panel content
```

---

## Screen Reader Support

### Semantic HTML

Use native elements when possible:

```html
<!-- Good: Native elements -->
<button>Submit</button>
<nav><a href="/">Home</a></nav>
<input type="checkbox" />
<select><option>Choose</option></select>

<!-- Bad: Non-semantic -->
<div onclick="submit()">Submit</div>
<span class="link">Home</span>
<div class="checkbox"></div>
```

### ARIA Landmarks

```html
<header role="banner">
  <!-- Site header -->
</header>

<nav role="navigation" aria-label="Main">
  <!-- Navigation links -->
</nav>

<main role="main">
  <!-- Main content -->
</main>

<aside role="complementary">
  <!-- Sidebar -->
</aside>

<footer role="contentinfo">
  <!-- Footer -->
</footer>
```

### Required ARIA Attributes

#### Buttons with Icons Only
```html
<button aria-label="Close modal">
  <svg aria-hidden="true"><!-- X icon --></svg>
</button>
```

#### Loading States
```html
<button aria-busy="true" aria-disabled="true">
  <span className="sr-only">Loading...</span>
  <Spinner aria-hidden="true" />
  Submit
</button>
```

#### Status Messages
```html
<div role="status" aria-live="polite">
  Package created successfully!
</div>

<div role="alert" aria-live="assertive">
  Error: Failed to save changes.
</div>
```

#### Form Errors
```html
<input
  id="email"
  aria-invalid="true"
  aria-describedby="email-error"
/>
<div id="email-error" role="alert">
  Please enter a valid email address.
</div>
```

### Screen Reader Only Text

```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

---

## Focus Management

### Focus Visibility

All interactive elements must have a visible focus indicator:

```css
/* Default focus style */
:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
}

/* Custom focus for specific components */
.button:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}
```

### Focus States by Component

| Component | Focus Style |
|-----------|-------------|
| Button | 2px outline + offset |
| Input | Ring + border color change |
| Link | Underline + outline |
| Card (interactive) | Shadow + outline |
| Tab | Background change + underline |

### Focus Trapping (Modals)

```typescript
// Focus trap implementation
function trapFocus(element: HTMLElement) {
  const focusableElements = element.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );

  const firstElement = focusableElements[0];
  const lastElement = focusableElements[focusableElements.length - 1];

  element.addEventListener('keydown', (e) => {
    if (e.key === 'Tab') {
      if (e.shiftKey && document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      } else if (!e.shiftKey && document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    }
  });
}
```

---

## Forms & Validation

### Labels

Every form field must have a visible label:

```html
<!-- Good: Visible label -->
<label for="email">Email Address</label>
<input id="email" type="email" />

<!-- Good: Floating label (still accessible) -->
<div class="floating-label">
  <input id="email" type="email" placeholder=" " />
  <label for="email">Email Address</label>
</div>

<!-- Bad: Placeholder only -->
<input type="email" placeholder="Email Address" />
```

### Required Fields

```html
<label for="email">
  Email Address
  <span aria-hidden="true">*</span>
</label>
<input
  id="email"
  type="email"
  aria-required="true"
  required
/>
```

### Error Messages

```html
<div class="form-field">
  <label for="email">Email Address</label>
  <input
    id="email"
    type="email"
    aria-invalid="true"
    aria-describedby="email-error email-hint"
  />
  <p id="email-hint" class="hint">
    We'll never share your email.
  </p>
  <p id="email-error" class="error" role="alert">
    Please enter a valid email address.
  </p>
</div>
```

### Form Submission Feedback

```html
<!-- Loading state -->
<button type="submit" aria-busy="true" aria-disabled="true">
  <span aria-hidden="true">Submitting...</span>
  <span class="sr-only">Form is being submitted, please wait.</span>
</button>

<!-- Success state -->
<div role="status" aria-live="polite">
  Form submitted successfully!
</div>

<!-- Error state -->
<div role="alert" aria-live="assertive">
  Failed to submit form. Please try again.
</div>
```

---

## Component Checklist

Use this checklist when implementing any component:

### General

- [ ] Uses semantic HTML elements
- [ ] Has visible focus indicator
- [ ] Keyboard accessible (Tab, Enter, Space, Escape)
- [ ] Color contrast meets 4.5:1 (text) or 3:1 (UI)
- [ ] Information not conveyed by color alone
- [ ] Respects prefers-reduced-motion

### Interactive Elements

- [ ] Has accessible name (label, aria-label, or aria-labelledby)
- [ ] State changes announced (aria-pressed, aria-expanded, etc.)
- [ ] Disabled state indicated (aria-disabled)
- [ ] Loading state indicated (aria-busy)

### Forms

- [ ] Labels associated with inputs (htmlFor/id)
- [ ] Required fields marked (aria-required)
- [ ] Error messages linked (aria-describedby)
- [ ] Invalid state indicated (aria-invalid)
- [ ] Form errors announced to screen readers

### Dynamic Content

- [ ] Status changes use aria-live
- [ ] Errors use role="alert"
- [ ] Focus managed after content changes
- [ ] New content announced appropriately

### Images & Icons

- [ ] Decorative images have alt="" or aria-hidden="true"
- [ ] Informative images have descriptive alt text
- [ ] Icon buttons have aria-label
- [ ] SVGs have title or aria-label

---

## Testing Guidelines

### Automated Testing

Tools to use:

| Tool | Purpose |
|------|---------|
| axe DevTools | Browser extension for WCAG testing |
| Lighthouse | Accessibility audit in Chrome DevTools |
| jest-axe | Automated a11y tests in Jest |
| eslint-plugin-jsx-a11y | Lint rules for React |

### Manual Testing

1. **Keyboard-only navigation**
   - Unplug your mouse
   - Navigate through entire app using only keyboard
   - Ensure all functionality is accessible

2. **Screen reader testing**
   - VoiceOver (macOS): Cmd + F5
   - NVDA (Windows): Free download
   - Test all pages and interactions

3. **Color contrast check**
   - Use browser dev tools
   - Check all text against backgrounds
   - Verify in both light and dark modes

4. **Zoom testing**
   - Zoom to 200%
   - Verify layout doesn't break
   - Ensure all content is readable

### Testing Checklist

- [ ] Tab through all interactive elements
- [ ] Verify focus order is logical
- [ ] Test all modals/dropdowns with keyboard
- [ ] Run axe DevTools scan
- [ ] Test with screen reader
- [ ] Verify at 200% zoom
- [ ] Check prefers-reduced-motion

---

## Reduced Motion

Respect user preferences for reduced motion:

```css
/* Default: Include animations */
.element {
  transition: transform 300ms ease;
}

/* Reduced motion: Remove or simplify animations */
@media (prefers-reduced-motion: reduce) {
  .element {
    transition: none;
  }

  /* Or use instant transitions */
  .element {
    transition: transform 0.01ms;
  }
}
```

### React Implementation

```typescript
function useReducedMotion(): boolean {
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    setReducedMotion(mediaQuery.matches);

    const listener = (e: MediaQueryListEvent) => setReducedMotion(e.matches);
    mediaQuery.addEventListener('change', listener);

    return () => mediaQuery.removeEventListener('change', listener);
  }, []);

  return reducedMotion;
}
```

---

*Last Updated: January 2026*
*Version: 1.0*
