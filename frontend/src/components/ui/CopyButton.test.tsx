import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CopyButton } from './CopyButton'

describe('CopyButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders copy button', () => {
    render(<CopyButton text="test text" />)
    expect(screen.getByRole('button')).toBeInTheDocument()
  })

  it('shows "Copy to clipboard" tooltip by default', () => {
    render(<CopyButton text="test" />)
    expect(screen.getByTitle('Copy to clipboard')).toBeInTheDocument()
  })

  it('renders copy icon initially', () => {
    render(<CopyButton text="test" />)
    const svg = screen.getByRole('button').querySelector('svg')
    expect(svg).toHaveClass('text-secondary-400')
  })

  it('applies custom className', () => {
    render(<CopyButton text="test" className="custom-class" />)
    expect(screen.getByRole('button')).toHaveClass('custom-class')
  })

  it('has proper button styling', () => {
    render(<CopyButton text="test" />)
    const button = screen.getByRole('button')
    expect(button).toHaveClass('p-2')
    expect(button).toHaveClass('rounded')
    expect(button).toHaveClass('hover:bg-secondary-700')
  })

  it('button is clickable', async () => {
    const user = userEvent.setup()
    render(<CopyButton text="test" />)
    const button = screen.getByRole('button')

    // Button should be clickable without throwing
    await expect(user.click(button)).resolves.not.toThrow()
  })

  it('shows success state after clicking', async () => {
    const user = userEvent.setup()

    // Mock the clipboard API
    const mockWriteText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    })

    render(<CopyButton text="test" />)
    await user.click(screen.getByRole('button'))

    await waitFor(() => {
      expect(screen.getByTitle('Copied!')).toBeInTheDocument()
    })
  })

  it('changes to checkmark icon after clicking', async () => {
    const user = userEvent.setup()

    // Mock the clipboard API
    const mockWriteText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    })

    render(<CopyButton text="test" />)
    await user.click(screen.getByRole('button'))

    await waitFor(() => {
      const svg = screen.getByRole('button').querySelector('svg')
      expect(svg).toHaveClass('text-green-500')
    })
  })
})
