/* eslint-disable react-refresh/only-export-components */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryHistory, createRouter } from '@tanstack/react-router'
import { render, type RenderOptions } from '@testing-library/react'
import { type ReactElement, type ReactNode } from 'react'
import { routeTree } from '@/routeTree.gen'

// Create a fresh query client for each test
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  })
}

// Wrapper that provides QueryClient
interface QueryWrapperProps {
  children: ReactNode
  queryClient?: QueryClient
}

export function QueryWrapper({ children, queryClient }: QueryWrapperProps) {
  const client = queryClient ?? createTestQueryClient()
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

// Custom render with QueryClient
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  queryClient?: QueryClient
}

export function renderWithQuery(ui: ReactElement, options?: CustomRenderOptions) {
  const { queryClient, ...renderOptions } = options ?? {}

  return render(ui, {
    wrapper: ({ children }) => (
      <QueryWrapper queryClient={queryClient}>{children}</QueryWrapper>
    ),
    ...renderOptions,
  })
}

// Create a test router for page testing
export function createTestRouter(initialPath = '/') {
  const history = createMemoryHistory({
    initialEntries: [initialPath],
  })

  return createRouter({
    routeTree,
    history,
    defaultPreload: false,
  })
}

// Render with full router context
interface RouterRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialPath?: string
  queryClient?: QueryClient
}

export function renderWithRouter(options?: RouterRenderOptions) {
  const { initialPath = '/', queryClient, ...renderOptions } = options ?? {}
  const testRouter = createTestRouter(initialPath)
  const client = queryClient ?? createTestQueryClient()

  return {
    ...render(
      <QueryClientProvider client={client}>
        <RouterProvider router={testRouter} />
      </QueryClientProvider>,
      renderOptions
    ),
    router: testRouter,
    queryClient: client,
  }
}

// Re-export everything from testing-library
export * from '@testing-library/react'
export { default as userEvent } from '@testing-library/user-event'
