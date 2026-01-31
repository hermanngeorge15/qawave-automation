import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RetryFailedDialog } from './RetryFailedDialog'
import type { ScenarioResult } from '@/api/types'

const mockFailedScenarios: ScenarioResult[] = [
  {
    scenarioId: 'scenario-1',
    scenarioName: 'User Login',
    status: 'FAILED',
    stepResults: [],
    duration: 1500,
    error: 'Authentication failed',
  },
  {
    scenarioId: 'scenario-2',
    scenarioName: 'Create Order',
    status: 'FAILED',
    stepResults: [],
    duration: 2500,
    error: 'Validation error',
  },
  {
    scenarioId: 'scenario-3',
    scenarioName: 'Get Profile',
    status: 'FAILED',
    stepResults: [],
    duration: 500,
    error: null,
  },
]

describe('RetryFailedDialog', () => {
  const mockOnClose = vi.fn()
  const mockOnConfirm = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockOnConfirm.mockResolvedValue(undefined)
  })

  it('renders when open', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByText('Retry Failed Scenarios')).toBeInTheDocument()
  })

  it('does not render when closed', () => {
    render(
      <RetryFailedDialog
        isOpen={false}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.queryByText('Retry Failed Scenarios')).not.toBeInTheDocument()
  })

  it('shows all failed scenarios', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByText('User Login')).toBeInTheDocument()
    expect(screen.getByText('Create Order')).toBeInTheDocument()
    expect(screen.getByText('Get Profile')).toBeInTheDocument()
  })

  it('shows error messages for scenarios with errors', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByText('Authentication failed')).toBeInTheDocument()
    expect(screen.getByText('Validation error')).toBeInTheDocument()
  })

  it('shows all scenarios selected by default', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    const checkboxes = screen.getAllByRole('checkbox')
    expect(checkboxes).toHaveLength(3)
    checkboxes.forEach((checkbox) => {
      expect(checkbox).toBeChecked()
    })
  })

  it('shows selection count', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByText('3 of 3 selected')).toBeInTheDocument()
  })

  it('toggles individual scenario selection', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    const checkbox = screen.getAllByRole('checkbox')[0] as HTMLElement
    await user.click(checkbox)

    expect(checkbox).not.toBeChecked()
    expect(screen.getByText('2 of 3 selected')).toBeInTheDocument()
  })

  it('deselects all when clicking Deselect All', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    await user.click(screen.getByText('Deselect All'))

    const checkboxes = screen.getAllByRole('checkbox')
    checkboxes.forEach((checkbox) => {
      expect(checkbox).not.toBeChecked()
    })
    expect(screen.getByText('0 of 3 selected')).toBeInTheDocument()
  })

  it('selects all when clicking Select All', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    // First deselect all
    await user.click(screen.getByText('Deselect All'))

    // Then select all
    await user.click(screen.getByText('Select All'))

    const checkboxes = screen.getAllByRole('checkbox')
    checkboxes.forEach((checkbox) => {
      expect(checkbox).toBeChecked()
    })
  })

  it('calls onConfirm with selected scenario IDs', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    // Deselect the first scenario
    await user.click(screen.getAllByRole('checkbox')[0] as HTMLElement)

    // Click retry button
    await user.click(screen.getByRole('button', { name: /retry 2 scenarios/i }))

    await waitFor(() => {
      expect(mockOnConfirm).toHaveBeenCalledWith(['scenario-2', 'scenario-3'])
    })
  })

  it('calls onClose when Cancel is clicked', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(mockOnClose).toHaveBeenCalled()
  })

  it('disables retry button when no scenarios selected', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    await user.click(screen.getByText('Deselect All'))

    const retryButton = screen.getByRole('button', { name: /retry 0 scenarios/i })
    expect(retryButton).toBeDisabled()
  })

  it('shows loading state during submission', async () => {
    const user = userEvent.setup()
    mockOnConfirm.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    )

    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    await user.click(screen.getByRole('button', { name: /retry 3 scenarios/i }))

    // Button should show loading state
    const retryButton = screen.getByRole('button', { name: /retry 3 scenarios/i })
    expect(retryButton).toBeDisabled()

    await waitFor(() => {
      expect(retryButton).not.toBeDisabled()
    })
  })

  it('shows scenario durations', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByText('1500ms')).toBeInTheDocument()
    expect(screen.getByText('2500ms')).toBeInTheDocument()
    expect(screen.getByText('500ms')).toBeInTheDocument()
  })

  it('updates button text based on selection count', async () => {
    const user = userEvent.setup()
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    expect(screen.getByRole('button', { name: /retry 3 scenarios/i })).toBeInTheDocument()

    await user.click(screen.getAllByRole('checkbox')[0] as HTMLElement)
    expect(screen.getByRole('button', { name: /retry 2 scenarios/i })).toBeInTheDocument()

    await user.click(screen.getAllByRole('checkbox')[1] as HTMLElement)
    expect(screen.getByRole('button', { name: /retry 1 scenario$/i })).toBeInTheDocument()
  })

  it('handles confirm error gracefully', async () => {
    const user = userEvent.setup()
    mockOnConfirm.mockRejectedValue(new Error('Retry failed'))

    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
      />
    )

    await user.click(screen.getByRole('button', { name: /retry 3 scenarios/i }))

    // Should recover from error (button re-enabled)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /retry 3 scenarios/i })).not.toBeDisabled()
    })
  })

  it('disables buttons when isLoading is true', () => {
    render(
      <RetryFailedDialog
        isOpen={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        failedScenarios={mockFailedScenarios}
        isLoading={true}
      />
    )

    expect(screen.getByRole('button', { name: /retry 3 scenarios/i })).toBeDisabled()
  })
})
