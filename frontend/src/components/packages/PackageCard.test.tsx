import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PackageCard } from './PackageCard'
import { mockPackage } from '@/test/mocks'
import type { QaPackage } from '@/api/types'

// Mock TanStack Router Link
vi.mock('@tanstack/react-router', () => ({
  Link: ({ children, to, params }: { children: React.ReactNode; to: string; params: Record<string, string> }) => (
    <a href={to.replace('$packageId', params.packageId)} data-testid="link">
      {children}
    </a>
  ),
}))

describe('PackageCard', () => {
  it('renders package name', () => {
    render(<PackageCard pkg={mockPackage} />)
    expect(screen.getByText(mockPackage.name)).toBeInTheDocument()
  })

  it('renders package description when provided', () => {
    render(<PackageCard pkg={mockPackage} />)
    expect(screen.getByText(mockPackage.description ?? '')).toBeInTheDocument()
  })

  it('does not render description when null', () => {
    const pkgWithoutDesc: QaPackage = {
      ...mockPackage,
      description: null,
    }
    render(<PackageCard pkg={pkgWithoutDesc} />)
    // Should only show name and status, no description paragraph
    expect(screen.queryByText('A test package')).not.toBeInTheDocument()
  })

  it('renders status badge', () => {
    render(<PackageCard pkg={mockPackage} />)
    expect(screen.getByText('Ready')).toBeInTheDocument()
  })

  it('renders different status badges correctly', () => {
    const draftPackage: QaPackage = {
      ...mockPackage,
      status: 'DRAFT',
    }
    render(<PackageCard pkg={draftPackage} />)
    expect(screen.getByText('Draft')).toBeInTheDocument()
  })

  it('renders formatted date', () => {
    render(<PackageCard pkg={mockPackage} />)
    // Mock package updatedAt is '2026-01-15T10:00:00Z'
    // Should be formatted as 'Jan 15, 2026'
    expect(screen.getByText(/Updated Jan 15, 2026/)).toBeInTheDocument()
  })

  it('renders view details link with correct href', () => {
    render(<PackageCard pkg={mockPackage} />)
    const link = screen.getByRole('link', { name: 'View Details' })
    expect(link).toHaveAttribute('href', '/packages/pkg-1')
  })

  it('applies card styling', () => {
    const { container } = render(<PackageCard pkg={mockPackage} />)
    const card = container.firstChild as HTMLElement
    expect(card).toHaveClass('card')
  })

  it('truncates long package names', () => {
    const longNamePackage: QaPackage = {
      ...mockPackage,
      name: 'This is a very long package name that should be truncated in the UI',
    }
    render(<PackageCard pkg={longNamePackage} />)
    const heading = screen.getByRole('heading', { level: 3 })
    expect(heading).toHaveClass('truncate')
  })
})
