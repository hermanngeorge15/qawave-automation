# ADR-016: Data Flow and State Management

## Status
Accepted

## Date
2026-01-30

## Context

QAWave requires a coherent data flow architecture to manage:
- Frontend state across components
- Backend service layer coordination
- Real-time updates during test execution
- Optimistic updates for better UX
- Cache management and invalidation

## Decision

We implement a **layered state management architecture** with TanStack Query for server state and React Context for UI state.

### Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA FLOW ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  FRONTEND                                                                    │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                                                                        │  │
│  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐              │  │
│  │   │ Server State│    │  UI State   │    │ Form State  │              │  │
│  │   │ (TanStack   │    │  (React     │    │ (React Hook │              │  │
│  │   │  Query)     │    │  Context)   │    │  Form)      │              │  │
│  │   └──────┬──────┘    └──────┬──────┘    └──────┬──────┘              │  │
│  │          │                  │                  │                      │  │
│  │          └──────────────────┼──────────────────┘                      │  │
│  │                             │                                         │  │
│  │                     ┌───────▼───────┐                                │  │
│  │                     │   Components  │                                │  │
│  │                     └───────┬───────┘                                │  │
│  └─────────────────────────────┼─────────────────────────────────────────┘  │
│                                │                                             │
│  ════════════════════════════════════════════════════════════════════════   │
│                                │ HTTP/SSE                                    │
│  ════════════════════════════════════════════════════════════════════════   │
│                                │                                             │
│  BACKEND                       ▼                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐              │  │
│  │   │ Controllers │───►│  Services   │───►│ Repositories│              │  │
│  │   │ (WebFlux)   │    │ (Coroutines)│    │ (R2DBC)     │              │  │
│  │   └─────────────┘    └──────┬──────┘    └─────────────┘              │  │
│  │                             │                                         │  │
│  │                     ┌───────▼───────┐                                │  │
│  │                     │  Event Bus    │                                │  │
│  │                     │  (Kafka)      │                                │  │
│  │                     └───────────────┘                                │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Frontend State Categories

| State Type | Tool | Scope | Examples |
|------------|------|-------|----------|
| Server State | TanStack Query | Global | Packages, Scenarios, Results |
| UI State | React Context | App-wide | Theme, Sidebar, Modals |
| Form State | React Hook Form | Component | Create/Edit forms |
| URL State | TanStack Router | Global | Filters, Pagination |

### TanStack Query Configuration

```typescript
// src/lib/query-client.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minute
      gcTime: 1000 * 60 * 5, // 5 minutes (formerly cacheTime)
      retry: 3,
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 1,
      onError: (error) => {
        console.error('Mutation error:', error);
      },
    },
  },
});

// Query key factory for type safety
export const queryKeys = {
  packages: {
    all: ['packages'] as const,
    list: (filters: PackageFilters) => [...queryKeys.packages.all, 'list', filters] as const,
    detail: (id: string) => [...queryKeys.packages.all, 'detail', id] as const,
    scenarios: (id: string) => [...queryKeys.packages.all, id, 'scenarios'] as const,
  },
  runs: {
    all: ['runs'] as const,
    list: (packageId: string) => [...queryKeys.runs.all, 'list', packageId] as const,
    detail: (id: string) => [...queryKeys.runs.all, 'detail', id] as const,
    results: (id: string) => [...queryKeys.runs.all, id, 'results'] as const,
  },
} as const;
```

### Query Hooks Pattern

```typescript
// src/features/packages/hooks/usePackages.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/query-client';
import { packageApi } from '../api/packageApi';

export function usePackages(filters: PackageFilters) {
  return useQuery({
    queryKey: queryKeys.packages.list(filters),
    queryFn: () => packageApi.list(filters),
    staleTime: 1000 * 30, // 30 seconds for list
  });
}

export function usePackage(id: string) {
  return useQuery({
    queryKey: queryKeys.packages.detail(id),
    queryFn: () => packageApi.get(id),
    enabled: !!id,
  });
}

export function useCreatePackage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: packageApi.create,
    onSuccess: (newPackage) => {
      // Invalidate list queries
      queryClient.invalidateQueries({
        queryKey: queryKeys.packages.all,
      });
      // Optionally set the new package in cache
      queryClient.setQueryData(
        queryKeys.packages.detail(newPackage.id),
        newPackage
      );
    },
  });
}

export function useUpdatePackage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePackageRequest }) =>
      packageApi.update(id, data),
    onMutate: async ({ id, data }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({
        queryKey: queryKeys.packages.detail(id),
      });

      // Snapshot previous value
      const previousPackage = queryClient.getQueryData(
        queryKeys.packages.detail(id)
      );

      // Optimistically update
      queryClient.setQueryData(
        queryKeys.packages.detail(id),
        (old: Package) => ({ ...old, ...data })
      );

      return { previousPackage };
    },
    onError: (err, { id }, context) => {
      // Rollback on error
      if (context?.previousPackage) {
        queryClient.setQueryData(
          queryKeys.packages.detail(id),
          context.previousPackage
        );
      }
    },
    onSettled: (_, __, { id }) => {
      // Refetch after mutation
      queryClient.invalidateQueries({
        queryKey: queryKeys.packages.detail(id),
      });
    },
  });
}
```

### Real-Time Updates with SSE

```typescript
// src/features/runs/hooks/useRunEvents.ts
import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/query-client';

export function useRunEvents(runId: string) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!runId) return;

    const eventSource = new EventSource(`/api/runs/${runId}/events`);

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);

      switch (data.type) {
        case 'STEP_STARTED':
        case 'STEP_COMPLETED':
          // Update step results incrementally
          queryClient.setQueryData(
            queryKeys.runs.results(runId),
            (old: StepResult[]) => updateStepResults(old, data.payload)
          );
          break;

        case 'RUN_COMPLETED':
          // Invalidate run detail to refetch final state
          queryClient.invalidateQueries({
            queryKey: queryKeys.runs.detail(runId),
          });
          break;
      }
    };

    eventSource.onerror = () => {
      eventSource.close();
      // Fallback to polling
      queryClient.invalidateQueries({
        queryKey: queryKeys.runs.detail(runId),
      });
    };

    return () => eventSource.close();
  }, [runId, queryClient]);
}
```

### UI State with Context

```typescript
// src/contexts/UIContext.tsx
import { createContext, useContext, useReducer, ReactNode } from 'react';

interface UIState {
  sidebarCollapsed: boolean;
  theme: 'light' | 'dark' | 'system';
  activeModal: string | null;
  notifications: Notification[];
}

type UIAction =
  | { type: 'TOGGLE_SIDEBAR' }
  | { type: 'SET_THEME'; payload: UIState['theme'] }
  | { type: 'OPEN_MODAL'; payload: string }
  | { type: 'CLOSE_MODAL' }
  | { type: 'ADD_NOTIFICATION'; payload: Notification }
  | { type: 'REMOVE_NOTIFICATION'; payload: string };

const initialState: UIState = {
  sidebarCollapsed: false,
  theme: 'system',
  activeModal: null,
  notifications: [],
};

function uiReducer(state: UIState, action: UIAction): UIState {
  switch (action.type) {
    case 'TOGGLE_SIDEBAR':
      return { ...state, sidebarCollapsed: !state.sidebarCollapsed };
    case 'SET_THEME':
      return { ...state, theme: action.payload };
    case 'OPEN_MODAL':
      return { ...state, activeModal: action.payload };
    case 'CLOSE_MODAL':
      return { ...state, activeModal: null };
    case 'ADD_NOTIFICATION':
      return { ...state, notifications: [...state.notifications, action.payload] };
    case 'REMOVE_NOTIFICATION':
      return {
        ...state,
        notifications: state.notifications.filter((n) => n.id !== action.payload),
      };
    default:
      return state;
  }
}

const UIContext = createContext<{
  state: UIState;
  dispatch: React.Dispatch<UIAction>;
} | null>(null);

export function UIProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(uiReducer, initialState);
  return (
    <UIContext.Provider value={{ state, dispatch }}>
      {children}
    </UIContext.Provider>
  );
}

export function useUI() {
  const context = useContext(UIContext);
  if (!context) throw new Error('useUI must be used within UIProvider');
  return context;
}
```

### Backend Service Layer

```kotlin
// Application Service orchestrating domain operations
@Service
class QaPackageApplicationService(
    private val qaPackageRepository: QaPackageRepository,
    private val scenarioRepository: TestScenarioRepository,
    private val eventPublisher: DomainEventPublisher,
    private val transactionTemplate: TransactionalOperator
) {
    suspend fun createPackage(command: CreateQaPackageCommand): QaPackage {
        return transactionTemplate.executeAndAwait {
            val package = QaPackage.create(command)
            val saved = qaPackageRepository.save(package)

            // Publish domain events
            package.pullDomainEvents().forEach { event ->
                eventPublisher.publish(event)
            }

            saved
        }
    }

    suspend fun runPackage(packageId: QaPackageId): TestRun {
        return transactionTemplate.executeAndAwait {
            val package = qaPackageRepository.findById(packageId)
                ?: throw PackageNotFoundException(packageId)

            val startedPackage = package.start()
            qaPackageRepository.save(startedPackage)

            val scenarios = scenarioRepository.findByPackageId(packageId)
            val run = TestRun.create(packageId, scenarios)

            startedPackage.pullDomainEvents().forEach { event ->
                eventPublisher.publish(event)
            }

            run
        }
    }
}
```

### Cache Invalidation Strategy

| Trigger | Invalidated Keys | Strategy |
|---------|------------------|----------|
| Create Package | `packages.all` | Invalidate list |
| Update Package | `packages.detail(id)` | Optimistic + refetch |
| Delete Package | `packages.all`, `packages.detail(id)` | Invalidate both |
| Run Complete | `runs.detail(id)`, `packages.detail(pkgId)` | Invalidate related |
| SSE Event | `runs.results(id)` | Incremental update |

### Data Synchronization Patterns

```typescript
// Optimistic update with rollback
const updatePackageMutation = useMutation({
  mutationFn: updatePackage,
  onMutate: async (newData) => {
    // 1. Cancel in-flight queries
    await queryClient.cancelQueries({ queryKey: ['package', id] });

    // 2. Snapshot current state
    const previous = queryClient.getQueryData(['package', id]);

    // 3. Optimistically update
    queryClient.setQueryData(['package', id], (old) => ({
      ...old,
      ...newData,
    }));

    // 4. Return rollback context
    return { previous };
  },
  onError: (err, newData, context) => {
    // 5. Rollback on error
    queryClient.setQueryData(['package', id], context.previous);
    toast.error('Update failed. Changes reverted.');
  },
  onSettled: () => {
    // 6. Refetch to ensure consistency
    queryClient.invalidateQueries({ queryKey: ['package', id] });
  },
});
```

## Consequences

### Positive
- Clear separation of server vs UI state
- Automatic caching and background refetching
- Optimistic updates improve perceived performance
- Real-time updates via SSE

### Negative
- Learning curve for TanStack Query
- Cache invalidation complexity
- SSE connection management overhead
- Multiple state stores to coordinate

## References

- [TanStack Query Documentation](https://tanstack.com/query/latest)
- [React Hook Form](https://react-hook-form.com/)
- [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
