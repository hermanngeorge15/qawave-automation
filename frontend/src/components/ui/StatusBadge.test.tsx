import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from './StatusBadge'
import type {
  QaPackageStatus,
  ScenarioStatus,
  TestRunStatus,
  StepResultStatus,
} from '@/api/types'

describe('StatusBadge', () => {
  describe('Package statuses', () => {
    it('renders DRAFT status correctly', () => {
      render(<StatusBadge status="DRAFT" />)
      expect(screen.getByText('Draft')).toBeInTheDocument()
    })

    it('renders GENERATING status with pulse animation', () => {
      render(<StatusBadge status="GENERATING" />)
      const badge = screen.getByText('Generating')
      expect(badge).toBeInTheDocument()
      expect(badge).toHaveClass('animate-pulse')
    })

    it('renders READY status correctly', () => {
      render(<StatusBadge status="READY" />)
      expect(screen.getByText('Ready')).toBeInTheDocument()
    })
  })

  describe('Shared statuses', () => {
    it('renders RUNNING status with pulse animation', () => {
      render(<StatusBadge status="RUNNING" />)
      const badge = screen.getByText('Running')
      expect(badge).toBeInTheDocument()
      expect(badge).toHaveClass('animate-pulse')
    })

    it('renders COMPLETED status correctly', () => {
      render(<StatusBadge status="COMPLETED" />)
      expect(screen.getByText('Completed')).toBeInTheDocument()
    })

    it('renders FAILED status correctly', () => {
      render(<StatusBadge status="FAILED" />)
      expect(screen.getByText('Failed')).toBeInTheDocument()
    })
  })

  describe('Scenario statuses', () => {
    it('renders PENDING status correctly', () => {
      render(<StatusBadge status="PENDING" />)
      expect(screen.getByText('Pending')).toBeInTheDocument()
    })

    it('renders PASSED status correctly', () => {
      render(<StatusBadge status="PASSED" />)
      expect(screen.getByText('Passed')).toBeInTheDocument()
    })

    it('renders SKIPPED status correctly', () => {
      render(<StatusBadge status="SKIPPED" />)
      expect(screen.getByText('Skipped')).toBeInTheDocument()
    })
  })

  describe('Test run statuses', () => {
    it('renders CANCELLED status correctly', () => {
      render(<StatusBadge status="CANCELLED" />)
      expect(screen.getByText('Cancelled')).toBeInTheDocument()
    })
  })

  describe('Step result statuses', () => {
    it('renders ERROR status correctly', () => {
      render(<StatusBadge status="ERROR" />)
      expect(screen.getByText('Error')).toBeInTheDocument()
    })
  })

  it('renders with badge styling', () => {
    render(<StatusBadge status="READY" />)
    const badge = screen.getByText('Ready')
    expect(badge).toHaveClass('rounded-full')
    expect(badge).toHaveClass('text-xs')
    expect(badge).toHaveClass('font-medium')
  })

  it.each<[QaPackageStatus | ScenarioStatus | TestRunStatus | StepResultStatus, string]>([
    ['DRAFT', 'Draft'],
    ['GENERATING', 'Generating'],
    ['READY', 'Ready'],
    ['RUNNING', 'Running'],
    ['COMPLETED', 'Completed'],
    ['FAILED', 'Failed'],
    ['PENDING', 'Pending'],
    ['PASSED', 'Passed'],
    ['SKIPPED', 'Skipped'],
    ['CANCELLED', 'Cancelled'],
    ['ERROR', 'Error'],
  ])('renders %s status as %s', (status, expectedLabel) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByText(expectedLabel)).toBeInTheDocument()
  })
})
