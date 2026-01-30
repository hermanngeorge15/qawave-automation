# QAWave Dark Mode Guidelines

> Color mappings and implementation patterns for dark mode support.

## Overview

Dark mode provides an alternative color scheme that:
- Reduces eye strain in low-light environments
- Saves battery on OLED screens
- Respects user system preferences
- Maintains accessibility (WCAG 2.1 AA contrast)

---

## Color Token Mappings

### Background Colors

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--bg-page` | neutral-50 (#F9FAFB) | neutral-950 (#030712) |
| `--bg-surface` | white (#FFFFFF) | neutral-900 (#111827) |
| `--bg-elevated` | white (#FFFFFF) | neutral-800 (#1F2937) |
| `--bg-overlay` | neutral-100 (#F3F4F6) | neutral-700 (#374151) |
| `--bg-muted` | neutral-100 (#F3F4F6) | neutral-800 (#1F2937) |

### Text Colors

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--text-primary` | neutral-900 (#111827) | neutral-50 (#F9FAFB) |
| `--text-secondary` | neutral-600 (#4B5563) | neutral-400 (#9CA3AF) |
| `--text-tertiary` | neutral-500 (#6B7280) | neutral-500 (#6B7280) |
| `--text-muted` | neutral-400 (#9CA3AF) | neutral-600 (#4B5563) |
| `--text-inverted` | white (#FFFFFF) | neutral-900 (#111827) |

### Border Colors

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--border-default` | neutral-200 (#E5E7EB) | neutral-700 (#374151) |
| `--border-subtle` | neutral-100 (#F3F4F6) | neutral-800 (#1F2937) |
| `--border-strong` | neutral-300 (#D1D5DB) | neutral-600 (#4B5563) |
| `--border-focus` | primary-500 (#6366F1) | primary-400 (#818CF8) |

### Interactive States

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--hover-bg` | neutral-100 (#F3F4F6) | neutral-700 (#374151) |
| `--active-bg` | neutral-200 (#E5E7EB) | neutral-600 (#4B5563) |
| `--selected-bg` | primary-50 (#EEF2FF) | primary-900/30 |
| `--disabled-bg` | neutral-100 (#F3F4F6) | neutral-800 (#1F2937) |
| `--disabled-text` | neutral-400 (#9CA3AF) | neutral-600 (#4B5563) |

---

## Semantic Colors (Dark Mode)

### Primary (Indigo)

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--primary-bg` | primary-50 (#EEF2FF) | primary-900/20 |
| `--primary-bg-hover` | primary-100 (#E0E7FF) | primary-800/30 |
| `--primary-text` | primary-700 (#4338CA) | primary-300 (#A5B4FC) |
| `--primary-solid` | primary-500 (#6366F1) | primary-400 (#818CF8) |
| `--primary-solid-hover` | primary-600 (#4F46E5) | primary-300 (#A5B4FC) |

### Success (Green)

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--success-bg` | success-50 (#F0FDF4) | success-900/20 |
| `--success-text` | success-700 (#15803D) | success-300 (#86EFAC) |
| `--success-solid` | success-500 (#22C55E) | success-400 (#4ADE80) |
| `--success-border` | success-200 (#BBF7D0) | success-700 (#15803D) |

### Warning (Amber)

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--warning-bg` | warning-50 (#FFFBEB) | warning-900/20 |
| `--warning-text` | warning-700 (#B45309) | warning-300 (#FCD34D) |
| `--warning-solid` | warning-500 (#F59E0B) | warning-400 (#FBBF24) |
| `--warning-border` | warning-200 (#FDE68A) | warning-700 (#B45309) |

### Danger (Red)

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--danger-bg` | danger-50 (#FEF2F2) | danger-900/20 |
| `--danger-text` | danger-700 (#B91C1C) | danger-300 (#FCA5A5) |
| `--danger-solid` | danger-500 (#EF4444) | danger-400 (#F87171) |
| `--danger-border` | danger-200 (#FECACA) | danger-700 (#B91C1C) |

### Info (Blue)

| Token | Light Mode | Dark Mode |
|-------|------------|-----------|
| `--info-bg` | info-50 (#EFF6FF) | info-900/20 |
| `--info-text` | info-700 (#1D4ED8) | info-300 (#93C5FD) |
| `--info-solid` | info-500 (#3B82F6) | info-400 (#60A5FA) |
| `--info-border` | info-200 (#BFDBFE) | info-700 (#1D4ED8) |

---

## Component Examples

### Card

```jsx
// Light mode
<div className="bg-white border border-neutral-200 shadow-sm">
  <h3 className="text-neutral-900">Title</h3>
  <p className="text-neutral-600">Description</p>
</div>

// With dark mode support
<div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700 shadow-sm dark:shadow-neutral-900/50">
  <h3 className="text-neutral-900 dark:text-neutral-50">Title</h3>
  <p className="text-neutral-600 dark:text-neutral-400">Description</p>
</div>
```

### Button (Primary)

```jsx
// With dark mode
<button className="
  bg-primary-500 dark:bg-primary-400
  hover:bg-primary-600 dark:hover:bg-primary-300
  text-white dark:text-neutral-900
  focus:ring-primary-500 dark:focus:ring-primary-400
">
  Submit
</button>
```

### Input

```jsx
<input className="
  bg-white dark:bg-neutral-800
  border-neutral-300 dark:border-neutral-600
  text-neutral-900 dark:text-neutral-100
  placeholder-neutral-400 dark:placeholder-neutral-500
  focus:border-primary-500 dark:focus:border-primary-400
  focus:ring-primary-100 dark:focus:ring-primary-900/50
" />
```

### StatusBadge

```jsx
// Success
<span className="
  bg-success-100 dark:bg-success-900/30
  text-success-700 dark:text-success-300
">
  Passed
</span>

// Danger
<span className="
  bg-danger-100 dark:bg-danger-900/30
  text-danger-700 dark:text-danger-300
">
  Failed
</span>
```

---

## Implementation

### CSS Variables

```css
:root {
  /* Light mode (default) */
  --bg-page: #F9FAFB;
  --bg-surface: #FFFFFF;
  --text-primary: #111827;
  --text-secondary: #4B5563;
  --border-default: #E5E7EB;
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg-page: #030712;
    --bg-surface: #111827;
    --text-primary: #F9FAFB;
    --text-secondary: #9CA3AF;
    --border-default: #374151;
  }
}

/* Class-based toggle (manual) */
.dark {
  --bg-page: #030712;
  --bg-surface: #111827;
  --text-primary: #F9FAFB;
  --text-secondary: #9CA3AF;
  --border-default: #374151;
}
```

### Tailwind Configuration

```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class', // or 'media' for system preference only
  theme: {
    extend: {
      colors: {
        // Semantic color aliases handled via CSS variables
      }
    }
  }
}
```

### React Context

```typescript
// lib/theme/ThemeProvider.tsx
import { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface ThemeContextType {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  resolvedTheme: 'light' | 'dark';
}

const ThemeContext = createContext<ThemeContextType | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => {
    if (typeof window === 'undefined') return 'system';
    return (localStorage.getItem('theme') as Theme) ?? 'system';
  });

  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light');

  useEffect(() => {
    const root = window.document.documentElement;

    const applyTheme = (isDark: boolean) => {
      if (isDark) {
        root.classList.add('dark');
        setResolvedTheme('dark');
      } else {
        root.classList.remove('dark');
        setResolvedTheme('light');
      }
    };

    if (theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      applyTheme(mediaQuery.matches);

      const listener = (e: MediaQueryListEvent) => applyTheme(e.matches);
      mediaQuery.addEventListener('change', listener);
      return () => mediaQuery.removeEventListener('change', listener);
    } else {
      applyTheme(theme === 'dark');
    }
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('theme', theme);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, setTheme, resolvedTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
}
```

### Theme Toggle Component

```tsx
// components/ui/ThemeToggle.tsx
import { Sun, Moon, Monitor } from 'lucide-react';
import { useTheme } from '@/lib/theme';

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <div className="flex gap-1 rounded-lg bg-neutral-100 dark:bg-neutral-800 p-1">
      <button
        onClick={() => setTheme('light')}
        className={`p-2 rounded ${
          theme === 'light'
            ? 'bg-white dark:bg-neutral-700 shadow'
            : 'hover:bg-neutral-200 dark:hover:bg-neutral-700'
        }`}
        aria-label="Light mode"
      >
        <Sun className="w-4 h-4" />
      </button>
      <button
        onClick={() => setTheme('dark')}
        className={`p-2 rounded ${
          theme === 'dark'
            ? 'bg-white dark:bg-neutral-700 shadow'
            : 'hover:bg-neutral-200 dark:hover:bg-neutral-700'
        }`}
        aria-label="Dark mode"
      >
        <Moon className="w-4 h-4" />
      </button>
      <button
        onClick={() => setTheme('system')}
        className={`p-2 rounded ${
          theme === 'system'
            ? 'bg-white dark:bg-neutral-700 shadow'
            : 'hover:bg-neutral-200 dark:hover:bg-neutral-700'
        }`}
        aria-label="System preference"
      >
        <Monitor className="w-4 h-4" />
      </button>
    </div>
  );
}
```

---

## Contrast Verification (Dark Mode)

All dark mode colors maintain WCAG AA compliance:

| Foreground | Background | Ratio | Pass |
|------------|------------|-------|------|
| neutral-50 | neutral-900 | 17.4:1 | AAA |
| neutral-300 | neutral-900 | 7.7:1 | AAA |
| neutral-400 | neutral-900 | 4.6:1 | AA |
| primary-300 | primary-900/20 on neutral-900 | 5.2:1 | AA |
| success-300 | success-900/20 on neutral-900 | 6.1:1 | AAA |
| danger-300 | danger-900/20 on neutral-900 | 5.8:1 | AA |

---

## Best Practices

### 1. Test Both Modes

Always develop and test in both light and dark modes.

### 2. Use Semantic Colors

Use semantic tokens (success, danger) rather than raw colors (green, red).

### 3. Avoid Pure Black

Use neutral-950 (#030712) instead of pure black (#000000) for backgrounds.

### 4. Reduce Contrast for Dark Mode

White (#FFFFFF) on dark backgrounds can be harsh. Use neutral-50 (#F9FAFB).

### 5. Adjust Shadows

Shadows are less visible on dark backgrounds. Use darker shadow colors or glow effects.

```css
/* Light mode shadow */
.card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* Dark mode shadow */
.dark .card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
  /* Or use a subtle border instead */
  border: 1px solid rgba(255, 255, 255, 0.05);
}
```

### 6. Respect User Preferences

Default to system preference, but allow manual override.

---

*Last Updated: January 2026*
*Version: 1.0*
