import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Skeleton, PackageCardSkeleton, PackagesListSkeleton } from './Skeleton'

describe('Skeleton', () => {
  it('renders with loading role and label', () => {
    render(<Skeleton />)
    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.getByLabelText('Loading')).toBeInTheDocument()
  })

  it('has pulse animation', () => {
    render(<Skeleton />)
    expect(screen.getByRole('status')).toHaveClass('animate-pulse')
  })

  it('applies custom className', () => {
    render(<Skeleton className="h-10 w-20" />)
    const skeleton = screen.getByRole('status')
    expect(skeleton).toHaveClass('h-10')
    expect(skeleton).toHaveClass('w-20')
  })

  it('has default background and rounded styling', () => {
    render(<Skeleton />)
    const skeleton = screen.getByRole('status')
    expect(skeleton).toHaveClass('bg-secondary-700')
    expect(skeleton).toHaveClass('rounded')
  })
})

describe('PackageCardSkeleton', () => {
  it('renders skeleton structure', () => {
    const { container } = render(<PackageCardSkeleton />)
    expect(container.querySelector('.card')).toBeInTheDocument()
  })

  it('renders multiple skeleton elements', () => {
    render(<PackageCardSkeleton />)
    // Should have 6 skeleton elements
    const skeletons = screen.getAllByRole('status')
    expect(skeletons.length).toBe(6)
  })

  it('has proper card styling', () => {
    const { container } = render(<PackageCardSkeleton />)
    expect(container.firstChild).toHaveClass('card')
  })
})

describe('PackagesListSkeleton', () => {
  it('renders 6 package card skeletons', () => {
    const { container } = render(<PackagesListSkeleton />)
    const cards = container.querySelectorAll('.card')
    expect(cards.length).toBe(6)
  })

  it('has grid layout', () => {
    const { container } = render(<PackagesListSkeleton />)
    const grid = container.firstChild as HTMLElement
    expect(grid).toHaveClass('grid')
    expect(grid).toHaveClass('gap-4')
  })

  it('has responsive grid columns', () => {
    const { container } = render(<PackagesListSkeleton />)
    const grid = container.firstChild as HTMLElement
    expect(grid).toHaveClass('md:grid-cols-2')
    expect(grid).toHaveClass('lg:grid-cols-3')
  })
})
