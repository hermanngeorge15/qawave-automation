import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { EmptyState } from './EmptyState'

describe('EmptyState', () => {
  it('renders title and description', () => {
    render(
      <EmptyState title="No items found" description="Try adjusting your filters" />
    )

    expect(screen.getByText('No items found')).toBeInTheDocument()
    expect(screen.getByText('Try adjusting your filters')).toBeInTheDocument()
  })

  it('renders with an action button when provided', () => {
    render(
      <EmptyState
        title="No packages"
        description="Create your first package"
        action={<button>Create Package</button>}
      />
    )

    expect(screen.getByRole('button', { name: 'Create Package' })).toBeInTheDocument()
  })

  it('does not render action when not provided', () => {
    render(
      <EmptyState title="Empty" description="Nothing here" />
    )

    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('renders with an icon when provided', () => {
    render(
      <EmptyState
        title="No data"
        description="No data available"
        icon={<svg data-testid="empty-icon" />}
      />
    )

    expect(screen.getByTestId('empty-icon')).toBeInTheDocument()
  })

  it('does not render icon container when icon not provided', () => {
    const { container } = render(
      <EmptyState title="No data" description="No data available" />
    )

    // Icon container has specific classes
    const iconContainer = container.querySelector('.h-12.w-12')
    expect(iconContainer).not.toBeInTheDocument()
  })

  it('renders with proper accessibility structure', () => {
    render(
      <EmptyState
        title="No packages yet"
        description="Get started by creating a package"
      />
    )

    // Title should be a heading
    expect(screen.getByRole('heading', { level: 3 })).toHaveTextContent('No packages yet')
  })

  it('centers content', () => {
    const { container } = render(
      <EmptyState title="Test" description="Test description" />
    )

    const wrapper = container.firstChild as HTMLElement
    expect(wrapper).toHaveClass('text-center')
  })
})
