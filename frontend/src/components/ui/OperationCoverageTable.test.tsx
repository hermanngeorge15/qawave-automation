import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OperationCoverageTable } from './OperationCoverageTable'
import type { Scenario } from '@/api/types'

const createScenario = (overrides: Partial<Scenario> = {}): Scenario => ({
  id: 'scenario-1',
  packageId: 'pkg-1',
  name: 'Test Scenario',
  description: null,
  steps: [
    {
      id: 'step-1',
      order: 1,
      method: 'GET',
      endpoint: '/api/users',
      headers: {},
      body: null,
      expectedStatus: 200,
      assertions: [],
      extractors: [],
      timeoutMs: 5000,
    },
  ],
  status: 'PASSED',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  ...overrides,
})

const mockScenarios: Scenario[] = [
  createScenario({
    id: 'scenario-1',
    name: 'Get Users',
    status: 'PASSED',
    steps: [
      {
        id: 'step-1',
        order: 1,
        method: 'GET',
        endpoint: '/api/users',
        headers: {},
        body: null,
        expectedStatus: 200,
        assertions: [],
        extractors: [],
        timeoutMs: 5000,
      },
    ],
  }),
  createScenario({
    id: 'scenario-2',
    name: 'Create User',
    status: 'PASSED',
    steps: [
      {
        id: 'step-2',
        order: 1,
        method: 'POST',
        endpoint: '/api/users',
        headers: {},
        body: '{}',
        expectedStatus: 201,
        assertions: [],
        extractors: [],
        timeoutMs: 5000,
      },
    ],
  }),
  createScenario({
    id: 'scenario-3',
    name: 'Delete User',
    status: 'FAILED',
    steps: [
      {
        id: 'step-3',
        order: 1,
        method: 'DELETE',
        endpoint: '/api/users/{id}',
        headers: {},
        body: null,
        expectedStatus: 204,
        assertions: [],
        extractors: [],
        timeoutMs: 5000,
      },
    ],
  }),
  createScenario({
    id: 'scenario-4',
    name: 'Get Products',
    status: 'PENDING',
    steps: [
      {
        id: 'step-4',
        order: 1,
        method: 'GET',
        endpoint: '/api/products',
        headers: {},
        body: null,
        expectedStatus: 200,
        assertions: [],
        extractors: [],
        timeoutMs: 5000,
      },
    ],
  }),
]

describe('OperationCoverageTable', () => {
  it('renders loading state', () => {
    render(<OperationCoverageTable scenarios={[]} isLoading={true} />)
    expect(screen.getByTestId('coverage-loading')).toBeInTheDocument()
  })

  it('renders empty state when no scenarios', () => {
    render(<OperationCoverageTable scenarios={[]} />)
    expect(screen.getByTestId('coverage-empty')).toBeInTheDocument()
    expect(screen.getByText('No coverage data')).toBeInTheDocument()
  })

  it('renders coverage table with scenarios', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    expect(screen.getByTestId('coverage-table')).toBeInTheDocument()
  })

  it('displays correct coverage percentage', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    // 2 covered (GET /api/users, POST /api/users), 1 failing, 1 untested = 50% covered
    expect(screen.getByTestId('coverage-percentage')).toHaveTextContent('50%')
  })

  it('displays correct status counts', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    expect(screen.getByTestId('covered-count')).toHaveTextContent('2')
    expect(screen.getByTestId('failing-count')).toHaveTextContent('1')
    expect(screen.getByTestId('untested-count')).toHaveTextContent('1')
  })

  it('shows all operations', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    // There are 2 /api/users entries (GET and POST)
    expect(screen.getAllByText('/api/users')).toHaveLength(2)
    expect(screen.getByText('/api/users/{id}')).toBeInTheDocument()
    expect(screen.getByText('/api/products')).toBeInTheDocument()
  })

  it('displays HTTP method badges', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    // There are 2 GET operations (GET /api/users and GET /api/products)
    expect(screen.getAllByText('GET')).toHaveLength(2)
    expect(screen.getByText('POST')).toBeInTheDocument()
    expect(screen.getByText('DELETE')).toBeInTheDocument()
  })

  it('displays coverage status badges', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)
    // "Covered" appears: 1 in stats card label + 2 in operation badges = 3
    expect(screen.getAllByText('Covered')).toHaveLength(3)
    // "Failing" appears: 1 in stats card label + 1 in operation badge = 2
    expect(screen.getAllByText('Failing')).toHaveLength(2)
    // "Untested" appears: 1 in stats card label + 1 in operation badge = 2
    expect(screen.getAllByText('Untested')).toHaveLength(2)
  })

  it('filters by covered status', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const coveredFilter = screen.getByRole('button', { name: /covered.*2/i })
    await user.click(coveredFilter)

    // Should show only 2 covered operations
    expect(screen.getByText('(2 of 4)')).toBeInTheDocument()
    expect(screen.queryByText('/api/users/{id}')).not.toBeInTheDocument()
    expect(screen.queryByText('/api/products')).not.toBeInTheDocument()
  })

  it('filters by failing status', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const failingFilter = screen.getByRole('button', { name: /failing.*1/i })
    await user.click(failingFilter)

    // Should show only failing operation
    expect(screen.getByText('(1 of 4)')).toBeInTheDocument()
    expect(screen.getByText('/api/users/{id}')).toBeInTheDocument()
    expect(screen.queryByText('/api/products')).not.toBeInTheDocument()
  })

  it('filters by untested status', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const untestedFilter = screen.getByRole('button', { name: /untested.*1/i })
    await user.click(untestedFilter)

    // Should show only untested operation
    expect(screen.getByText('(1 of 4)')).toBeInTheDocument()
    expect(screen.getByText('/api/products')).toBeInTheDocument()
  })

  it('shows all when All filter is clicked', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    // First filter to covered
    const coveredFilter = screen.getByRole('button', { name: /covered.*2/i })
    await user.click(coveredFilter)

    // Then click All
    const allFilter = screen.getByRole('button', { name: /all.*4/i })
    await user.click(allFilter)

    // Should show all operations
    expect(screen.getByText('(4 of 4)')).toBeInTheDocument()
  })

  it('shows no results message when filter has no matches', async () => {
    const user = userEvent.setup()
    const scenariosWithNoCovered = [
      createScenario({
        id: 'scenario-1',
        status: 'FAILED',
        steps: [
          {
            id: 'step-1',
            order: 1,
            method: 'GET',
            endpoint: '/api/test',
            headers: {},
            body: null,
            expectedStatus: 200,
            assertions: [],
            extractors: [],
            timeoutMs: 5000,
          },
        ],
      }),
    ]
    render(<OperationCoverageTable scenarios={scenariosWithNoCovered} />)

    const coveredFilter = screen.getByRole('button', { name: /covered.*0/i })
    await user.click(coveredFilter)

    expect(screen.getByTestId('no-results')).toBeInTheDocument()
    expect(screen.getByText('No operations match the current filter')).toBeInTheDocument()
  })

  it('expands operation to show linked scenarios', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    // Click on an operation to expand
    const operationButton = screen.getByRole('button', {
      name: /GET \/api\/users/i,
    })
    await user.click(operationButton)

    // Should show linked scenarios
    expect(screen.getByTestId('scenario-list')).toBeInTheDocument()
    expect(screen.getByText('Get Users')).toBeInTheDocument()
  })

  it('collapses operation when clicked again', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const operationButton = screen.getByRole('button', {
      name: /GET \/api\/users/i,
    })

    // Expand
    await user.click(operationButton)
    expect(screen.getByTestId('scenario-list')).toBeInTheDocument()

    // Collapse
    await user.click(operationButton)
    expect(screen.queryByTestId('scenario-list')).not.toBeInTheDocument()
  })

  it('only one operation expanded at a time', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    // Expand first operation
    const firstOperation = screen.getByRole('button', {
      name: /GET \/api\/users/i,
    })
    await user.click(firstOperation)
    expect(screen.getByText('Get Users')).toBeInTheDocument()

    // Expand second operation
    const secondOperation = screen.getByRole('button', {
      name: /POST \/api\/users/i,
    })
    await user.click(secondOperation)

    // First should be collapsed, second should be expanded
    expect(screen.queryByText('Get Users')).not.toBeInTheDocument()
    expect(screen.getByText('Create User')).toBeInTheDocument()
  })

  it('renders export button when onExport is provided', () => {
    const mockExport = vi.fn()
    render(<OperationCoverageTable scenarios={mockScenarios} onExport={mockExport} />)

    expect(screen.getByRole('button', { name: /export coverage report/i })).toBeInTheDocument()
  })

  it('does not render export button when onExport is not provided', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    expect(screen.queryByRole('button', { name: /export coverage report/i })).not.toBeInTheDocument()
  })

  it('calls onExport when export button is clicked', async () => {
    const user = userEvent.setup()
    const mockExport = vi.fn()
    render(<OperationCoverageTable scenarios={mockScenarios} onExport={mockExport} />)

    await user.click(screen.getByRole('button', { name: /export coverage report/i }))

    expect(mockExport).toHaveBeenCalledTimes(1)
  })

  it('shows loading state on export button when isExporting', () => {
    const mockExport = vi.fn()
    render(
      <OperationCoverageTable
        scenarios={mockScenarios}
        onExport={mockExport}
        isExporting={true}
      />
    )

    expect(screen.getByText('Exporting...')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /export coverage report/i })).toBeDisabled()
  })

  it('displays progress bar with correct aria attributes', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const progressBar = screen.getByRole('meter')
    expect(progressBar).toHaveAttribute('aria-valuenow', '50')
    expect(progressBar).toHaveAttribute('aria-valuemin', '0')
    expect(progressBar).toHaveAttribute('aria-valuemax', '100')
  })

  it('displays scenario count in operation row', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    // All 4 operations have 1 scenario each
    expect(screen.getAllByText('1 scenario')).toHaveLength(4)
  })

  it('groups multiple scenarios for same endpoint', () => {
    const scenariosWithDuplicateEndpoint = [
      ...mockScenarios,
      createScenario({
        id: 'scenario-5',
        name: 'Get Users - Pagination',
        status: 'PASSED',
        steps: [
          {
            id: 'step-5',
            order: 1,
            method: 'GET',
            endpoint: '/api/users',
            headers: {},
            body: null,
            expectedStatus: 200,
            assertions: [],
            extractors: [],
            timeoutMs: 5000,
          },
        ],
      }),
    ]

    render(<OperationCoverageTable scenarios={scenariosWithDuplicateEndpoint} />)

    // GET /api/users should now have 2 scenarios
    expect(screen.getByText('2 scenarios')).toBeInTheDocument()
  })

  it('shows legend with all status colors', () => {
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    expect(screen.getByText('Covered (passing tests)')).toBeInTheDocument()
    expect(screen.getByText('Failing (tests exist)')).toBeInTheDocument()
    expect(screen.getByText('Untested (no status)')).toBeInTheDocument()
  })

  it('filter buttons have aria-pressed attribute', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const allFilter = screen.getByRole('button', { name: /all.*4/i })
    expect(allFilter).toHaveAttribute('aria-pressed', 'true')

    const coveredFilter = screen.getByRole('button', { name: /covered.*2/i })
    expect(coveredFilter).toHaveAttribute('aria-pressed', 'false')

    await user.click(coveredFilter)
    expect(allFilter).toHaveAttribute('aria-pressed', 'false')
    expect(coveredFilter).toHaveAttribute('aria-pressed', 'true')
  })

  it('operation buttons have aria-expanded attribute', async () => {
    const user = userEvent.setup()
    render(<OperationCoverageTable scenarios={mockScenarios} />)

    const operationButton = screen.getByRole('button', {
      name: /GET \/api\/users/i,
    })

    expect(operationButton).toHaveAttribute('aria-expanded', 'false')

    await user.click(operationButton)
    expect(operationButton).toHaveAttribute('aria-expanded', 'true')
  })
})
