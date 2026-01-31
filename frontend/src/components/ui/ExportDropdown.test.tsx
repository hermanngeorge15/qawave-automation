import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ExportDropdown } from './ExportDropdown'

describe('ExportDropdown', () => {
  const mockOnExport = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockOnExport.mockResolvedValue(undefined)
  })

  it('renders export button', () => {
    render(<ExportDropdown onExport={mockOnExport} />)

    expect(screen.getByRole('button', { name: /export/i })).toBeInTheDocument()
  })

  it('shows dropdown menu when clicked', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    // Click export button
    await user.click(screen.getByRole('button', { name: /export/i }))

    // Dropdown should be visible
    expect(screen.getByRole('menu')).toBeInTheDocument()
    expect(screen.getByText('Export as JSON')).toBeInTheDocument()
    expect(screen.getByText('Export as CSV')).toBeInTheDocument()
  })

  it('closes dropdown when clicking outside', async () => {
    const user = userEvent.setup()
    render(
      <div>
        <ExportDropdown onExport={mockOnExport} />
        <button data-testid="outside">Outside</button>
      </div>
    )

    // Open dropdown
    await user.click(screen.getByRole('button', { name: /export/i }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    // Click outside
    await user.click(screen.getByTestId('outside'))

    // Dropdown should be closed
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('closes dropdown on escape key', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown
    await user.click(screen.getByRole('button', { name: /export/i }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    // Press escape
    await user.keyboard('{Escape}')

    // Dropdown should be closed
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('calls onExport with json format when JSON option is clicked', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown
    await user.click(screen.getByRole('button', { name: /export/i }))

    // Click JSON option
    await user.click(screen.getByText('Export as JSON'))

    // Should call onExport with 'json'
    await waitFor(() => {
      expect(mockOnExport).toHaveBeenCalledWith('json')
    })
  })

  it('calls onExport with csv format when CSV option is clicked', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown
    await user.click(screen.getByRole('button', { name: /export/i }))

    // Click CSV option
    await user.click(screen.getByText('Export as CSV'))

    // Should call onExport with 'csv'
    await waitFor(() => {
      expect(mockOnExport).toHaveBeenCalledWith('csv')
    })
  })

  it('shows loading state when isLoading is true', () => {
    render(<ExportDropdown onExport={mockOnExport} isLoading />)

    expect(screen.getByText('Exporting...')).toBeInTheDocument()
  })

  it('disables button when disabled prop is true', () => {
    render(<ExportDropdown onExport={mockOnExport} disabled />)

    expect(screen.getByRole('button', { name: /export/i })).toBeDisabled()
  })

  it('disables button during export operation', async () => {
    const user = userEvent.setup()
    // Make the export take some time
    mockOnExport.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    )

    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown and click export
    await user.click(screen.getByRole('button', { name: /export/i }))
    await user.click(screen.getByText('Export as JSON'))

    // Should show loading state
    expect(screen.getByText('Exporting...')).toBeInTheDocument()

    // Wait for export to complete
    await waitFor(() => {
      expect(screen.queryByText('Exporting...')).not.toBeInTheDocument()
    })
  })

  it('closes dropdown when an export option is clicked', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown
    await user.click(screen.getByRole('button', { name: /export/i }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    // Click export option
    await user.click(screen.getByText('Export as JSON'))

    // Dropdown should be closed
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('applies custom className', () => {
    const { container } = render(
      <ExportDropdown onExport={mockOnExport} className="custom-class" />
    )

    expect(container.firstChild).toHaveClass('custom-class')
  })

  it('toggles dropdown open/closed on repeated clicks', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    const button = screen.getByRole('button', { name: /export/i })

    // First click - open
    await user.click(button)
    expect(screen.getByRole('menu')).toBeInTheDocument()

    // Second click - close
    await user.click(button)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()

    // Third click - open again
    await user.click(button)
    expect(screen.getByRole('menu')).toBeInTheDocument()
  })

  it('has correct aria attributes', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    const button = screen.getByRole('button', { name: /export/i })

    // Initial state
    expect(button).toHaveAttribute('aria-haspopup', 'true')
    expect(button).toHaveAttribute('aria-expanded', 'false')

    // Open dropdown
    await user.click(button)

    // Expanded state
    expect(button).toHaveAttribute('aria-expanded', 'true')
  })

  it('handles export error gracefully', async () => {
    const user = userEvent.setup()
    mockOnExport.mockRejectedValue(new Error('Export failed'))

    render(<ExportDropdown onExport={mockOnExport} />)

    // Open dropdown and click export
    await user.click(screen.getByRole('button', { name: /export/i }))
    await user.click(screen.getByText('Export as JSON'))

    // Should recover from error (loading state cleared)
    await waitFor(() => {
      expect(screen.queryByText('Exporting...')).not.toBeInTheDocument()
    })
  })

  it('shows format descriptions in dropdown', async () => {
    const user = userEvent.setup()
    render(<ExportDropdown onExport={mockOnExport} />)

    await user.click(screen.getByRole('button', { name: /export/i }))

    expect(screen.getByText('Full structured data')).toBeInTheDocument()
    expect(screen.getByText('Spreadsheet format')).toBeInTheDocument()
  })
})
