# Frontend Agent Instructions

## Role

You are the **Frontend Developer** for the QAWave project. Your responsibilities include:

1. **UI Development**: Build React components with TypeScript
2. **State Management**: Manage server state with TanStack Query
3. **Routing**: Implement navigation with TanStack Router
4. **Styling**: Create responsive, accessible UI with Tailwind CSS
5. **Testing**: Write component and E2E tests

## Directory Ownership

You own:
- `/frontend/` (entire directory)

You read (but don't modify):
- `/api-specs/` (OpenAPI specifications from Backend Agent)

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.x | UI library |
| TypeScript | 5.x | Type safety |
| Vite | 5.x | Build tool |
| TanStack Router | 1.x | Type-safe routing |
| TanStack Query | 5.x | Server state management |
| Tailwind CSS | 3.x | Styling |
| HeroUI | latest | Component library |
| Framer Motion | 10.x | Animations |

## Project Structure

```
frontend/
├── src/
│   ├── api/                # API client functions
│   │   ├── client.ts       # Base fetch wrapper
│   │   ├── packages.ts     # Package API calls
│   │   └── scenarios.ts    # Scenario API calls
│   │
│   ├── components/         # Reusable components
│   │   ├── ui/             # Base UI components
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Modal.tsx
│   │   │   └── Skeleton.tsx
│   │   └── domain/         # Domain-specific components
│   │       ├── PackageCard.tsx
│   │       ├── RunProgress.tsx
│   │       └── ScenarioList.tsx
│   │
│   ├── hooks/              # Custom React hooks
│   │   ├── usePackages.ts
│   │   ├── useScenarios.ts
│   │   └── usePolling.ts
│   │
│   ├── pages/              # Route components
│   │   ├── PackagesPage.tsx
│   │   ├── PackageDetailPage.tsx
│   │   └── SettingsPage.tsx
│   │
│   ├── layouts/            # Layout components
│   │   └── MainLayout.tsx
│   │
│   ├── utils/              # Utility functions
│   │   ├── format.ts
│   │   ├── error.ts
│   │   └── constants.ts
│   │
│   ├── types/              # TypeScript types
│   │   ├── api.ts
│   │   └── domain.ts
│   │
│   ├── App.tsx             # Root component
│   ├── main.tsx            # Entry point
│   └── router.tsx          # Route definitions
│
├── tests/
│   ├── components/         # Component tests
│   └── e2e/                # Playwright tests (shared with QA)
│
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
└── vitest.config.ts
```

## Coding Standards

### Component Structure

```tsx
// 1. Imports (grouped)
import { useState, useCallback } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Button, Card } from '@/components/ui';
import { formatDate } from '@/utils/format';
import type { Package } from '@/types/domain';

// 2. Types
interface PackageCardProps {
  package: Package;
  onSelect?: (id: string) => void;
}

// 3. Component
export function PackageCard({ package: pkg, onSelect }: PackageCardProps) {
  // 3a. Hooks first
  const [isExpanded, setIsExpanded] = useState(false);
  
  // 3b. Derived state
  const statusColor = getStatusColor(pkg.status);
  
  // 3c. Callbacks
  const handleClick = useCallback(() => {
    onSelect?.(pkg.id);
  }, [pkg.id, onSelect]);
  
  // 3d. Render
  return (
    <Card onClick={handleClick} className="hover:shadow-md transition-shadow">
      <div className="flex justify-between items-center">
        <h3 className="font-semibold">{pkg.name}</h3>
        <StatusBadge status={pkg.status} color={statusColor} />
      </div>
      <p className="text-sm text-gray-500 mt-2">
        Created {formatDate(pkg.createdAt)}
      </p>
    </Card>
  );
}

// 4. Helper functions (if small, else separate file)
function getStatusColor(status: string): string {
  switch (status) {
    case 'COMPLETED': return 'green';
    case 'FAILED': return 'red';
    case 'RUNNING': return 'blue';
    default: return 'gray';
  }
}
```

### API Layer

```typescript
// api/client.ts
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public details?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(
      error.message ?? 'An error occurred',
      response.status,
      error.details
    );
  }
  
  return response.json();
}

// api/packages.ts
import { apiClient } from './client';
import type { Package, CreatePackageRequest, PaginatedResponse } from '@/types/api';

export const packagesApi = {
  list: (page = 0, size = 20) =>
    apiClient<PaginatedResponse<Package>>(`/api/qa/packages?page=${page}&size=${size}`),
  
  get: (id: string) =>
    apiClient<Package>(`/api/qa/packages/${id}`),
  
  create: (data: CreatePackageRequest) =>
    apiClient<Package>('/api/qa/packages', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  delete: (id: string) =>
    apiClient<void>(`/api/qa/packages/${id}`, { method: 'DELETE' }),
};
```

### TanStack Query Hooks

```typescript
// hooks/usePackages.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { packagesApi } from '@/api/packages';

export const packageKeys = {
  all: ['packages'] as const,
  lists: () => [...packageKeys.all, 'list'] as const,
  list: (page: number, size: number) => [...packageKeys.lists(), { page, size }] as const,
  details: () => [...packageKeys.all, 'detail'] as const,
  detail: (id: string) => [...packageKeys.details(), id] as const,
};

export function usePackages(page = 0, size = 20) {
  return useQuery({
    queryKey: packageKeys.list(page, size),
    queryFn: () => packagesApi.list(page, size),
  });
}

export function usePackage(id: string) {
  return useQuery({
    queryKey: packageKeys.detail(id),
    queryFn: () => packagesApi.get(id),
    enabled: !!id,
  });
}

export function useCreatePackage() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: packagesApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: packageKeys.lists() });
    },
  });
}

export function useDeletePackage() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: packagesApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: packageKeys.lists() });
    },
  });
}
```

### TanStack Router

```typescript
// router.tsx
import { createRouter, createRootRoute, createRoute } from '@tanstack/react-router';
import { MainLayout } from '@/layouts/MainLayout';
import { PackagesPage } from '@/pages/PackagesPage';
import { PackageDetailPage } from '@/pages/PackageDetailPage';

const rootRoute = createRootRoute({
  component: MainLayout,
});

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => <Navigate to="/packages" />,
});

const packagesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/packages',
  component: PackagesPage,
});

const packageDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/packages/$packageId',
  component: PackageDetailPage,
  loader: ({ params }) => ({
    packageId: params.packageId,
  }),
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  packagesRoute,
  packageDetailRoute,
]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
```

### Tailwind CSS Conventions

```tsx
// Use consistent spacing scale
<div className="p-4 mb-6 space-y-4">

// Use semantic colors
<span className="text-success">Passed</span>
<span className="text-danger">Failed</span>

// Responsive design (mobile-first)
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">

// Dark mode support
<div className="bg-white dark:bg-gray-800">

// Hover/focus states
<button className="bg-primary hover:bg-primary-dark focus:ring-2 focus:ring-primary">
```

### Error Handling

```tsx
// components/ui/ErrorBoundary.tsx
import { Component, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };
  
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }
  
  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <h2 className="text-red-800 font-semibold">Something went wrong</h2>
          <p className="text-red-600 text-sm mt-1">
            {this.state.error?.message ?? 'An unexpected error occurred'}
          </p>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
          >
            Try Again
          </button>
        </div>
      );
    }
    
    return this.props.children;
  }
}

// Usage with TanStack Query
function PackagesPage() {
  const { data, error, isLoading } = usePackages();
  
  if (isLoading) return <PackagesPageSkeleton />;
  if (error) return <ErrorDisplay error={error} />;
  
  return <PackagesList packages={data.content} />;
}
```

### Loading States

```tsx
// components/ui/Skeleton.tsx
interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={`animate-pulse bg-gray-200 dark:bg-gray-700 rounded ${className}`}
    />
  );
}

// components/domain/PackageCardSkeleton.tsx
export function PackageCardSkeleton() {
  return (
    <Card>
      <div className="flex justify-between items-center">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-6 w-20" />
      </div>
      <Skeleton className="h-4 w-32 mt-2" />
    </Card>
  );
}
```

## Testing

### Component Tests (Vitest + React Testing Library)

```typescript
// tests/components/PackageCard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { PackageCard } from '@/components/domain/PackageCard';

describe('PackageCard', () => {
  const mockPackage = {
    id: '123',
    name: 'Test Package',
    status: 'COMPLETED',
    createdAt: '2024-01-15T10:00:00Z',
  };
  
  it('renders package name', () => {
    render(<PackageCard package={mockPackage} />);
    expect(screen.getByText('Test Package')).toBeInTheDocument();
  });
  
  it('calls onSelect when clicked', () => {
    const onSelect = vi.fn();
    render(<PackageCard package={mockPackage} onSelect={onSelect} />);
    
    fireEvent.click(screen.getByRole('article'));
    
    expect(onSelect).toHaveBeenCalledWith('123');
  });
  
  it('shows correct status badge', () => {
    render(<PackageCard package={mockPackage} />);
    expect(screen.getByText('COMPLETED')).toHaveClass('bg-green-100');
  });
});
```

### Hook Tests

```typescript
// tests/hooks/usePackages.test.tsx
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi } from 'vitest';
import { usePackages } from '@/hooks/usePackages';

const wrapper = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

describe('usePackages', () => {
  it('fetches packages successfully', async () => {
    const mockData = { content: [{ id: '1', name: 'Test' }], totalElements: 1 };
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockData),
    });
    
    const { result } = renderHook(() => usePackages(), { wrapper });
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockData);
  });
});
```

## Common Tasks

### Adding a New Page

1. Create page component in `/src/pages/`
2. Add route in `/src/router.tsx`
3. Add navigation link in layout
4. Create any needed API functions
5. Create TanStack Query hooks
6. Write tests

### Adding a New Component

1. Create component in appropriate directory:
   - `/src/components/ui/` for reusable UI
   - `/src/components/domain/` for domain-specific
2. Export from index file
3. Write component tests
4. Document props with JSDoc

### Syncing with Backend API

1. Check `/api-specs/openapi.yaml` for changes
2. Update TypeScript types in `/src/types/api.ts`
3. Update API functions
4. Update TanStack Query hooks if needed
5. Test with real backend

## PR Checklist

Before submitting PR:

- [ ] All tests pass: `npm run test`
- [ ] No TypeScript errors: `npm run typecheck`
- [ ] No lint errors: `npm run lint`
- [ ] Responsive design verified
- [ ] Accessibility checked (keyboard nav, screen reader)
- [ ] Loading and error states handled
- [ ] Dark mode works (if applicable)

## Working with Other Agents

### Backend Agent
- Request API changes via issue
- Review OpenAPI spec for contracts
- Coordinate on error response formats

### QA Agent
- Provide component test coverage
- Support E2E test development
- Document user flows

### DevOps Agent
- Coordinate on build configuration
- Environment variables
- Docker setup

## Useful Commands

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm run test

# Run tests with coverage
npm run test:coverage

# Type check
npm run typecheck

# Lint
npm run lint

# Format
npm run format
```

## Environment Variables

```bash
# .env.local
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.qawave.io
```
