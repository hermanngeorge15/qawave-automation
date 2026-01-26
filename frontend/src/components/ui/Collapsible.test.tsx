import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Collapsible } from './Collapsible'

describe('Collapsible', () => {
  it('renders title', () => {
    render(
      <Collapsible title="Section Title">
        <p>Content</p>
      </Collapsible>
    )
    expect(screen.getByText('Section Title')).toBeInTheDocument()
  })

  it('hides content by default', () => {
    render(
      <Collapsible title="Title">
        <p>Hidden content</p>
      </Collapsible>
    )
    expect(screen.queryByText('Hidden content')).not.toBeInTheDocument()
  })

  it('shows content when defaultOpen is true', () => {
    render(
      <Collapsible title="Title" defaultOpen>
        <p>Visible content</p>
      </Collapsible>
    )
    expect(screen.getByText('Visible content')).toBeInTheDocument()
  })

  it('toggles content visibility when clicked', async () => {
    const user = userEvent.setup()

    render(
      <Collapsible title="Click me">
        <p>Toggle content</p>
      </Collapsible>
    )

    // Initially hidden
    expect(screen.queryByText('Toggle content')).not.toBeInTheDocument()

    // Click to open
    await user.click(screen.getByRole('button'))
    expect(screen.getByText('Toggle content')).toBeInTheDocument()

    // Click to close
    await user.click(screen.getByRole('button'))
    expect(screen.queryByText('Toggle content')).not.toBeInTheDocument()
  })

  it('applies custom className', () => {
    const { container } = render(
      <Collapsible title="Title" className="custom-class">
        <p>Content</p>
      </Collapsible>
    )
    expect(container.firstChild).toHaveClass('custom-class')
  })

  it('renders title as ReactNode', () => {
    render(
      <Collapsible title={<span data-testid="custom-title">Custom Title</span>}>
        <p>Content</p>
      </Collapsible>
    )
    expect(screen.getByTestId('custom-title')).toBeInTheDocument()
  })

  it('rotates chevron icon when open', async () => {
    const user = userEvent.setup()

    render(
      <Collapsible title="Title">
        <p>Content</p>
      </Collapsible>
    )

    const svg = screen.getByRole('button').querySelector('svg')

    // Initially not rotated
    expect(svg).not.toHaveClass('rotate-180')

    // After click, rotated
    await user.click(screen.getByRole('button'))
    expect(svg).toHaveClass('rotate-180')
  })

  it('button has full width and text-left alignment', () => {
    render(
      <Collapsible title="Title">
        <p>Content</p>
      </Collapsible>
    )

    const button = screen.getByRole('button')
    expect(button).toHaveClass('w-full')
    expect(button).toHaveClass('text-left')
  })
})
