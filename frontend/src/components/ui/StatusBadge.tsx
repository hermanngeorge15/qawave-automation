import type { QaPackageStatus, ScenarioStatus, TestRunStatus, StepResultStatus } from '@/api/types'

type Status = QaPackageStatus | ScenarioStatus | TestRunStatus | StepResultStatus

interface StatusBadgeProps {
  status: Status
}

const statusConfig: Record<Status, { label: string; className: string }> = {
  // Package statuses
  DRAFT: {
    label: 'Draft',
    className: 'bg-secondary-700 text-secondary-200',
  },
  GENERATING: {
    label: 'Generating',
    className: 'bg-primary-600 text-white animate-pulse',
  },
  READY: {
    label: 'Ready',
    className: 'bg-success-600 text-white',
  },
  // Shared statuses
  RUNNING: {
    label: 'Running',
    className: 'bg-primary-500 text-white animate-pulse',
  },
  COMPLETED: {
    label: 'Completed',
    className: 'bg-success-700 text-white',
  },
  FAILED: {
    label: 'Failed',
    className: 'bg-danger-600 text-white',
  },
  // Scenario statuses
  PENDING: {
    label: 'Pending',
    className: 'bg-yellow-500/10 text-yellow-500',
  },
  PASSED: {
    label: 'Passed',
    className: 'bg-success-600 text-white',
  },
  SKIPPED: {
    label: 'Skipped',
    className: 'bg-secondary-500/10 text-secondary-400',
  },
  // Run status
  CANCELLED: {
    label: 'Cancelled',
    className: 'bg-secondary-500/10 text-secondary-400',
  },
  // Step status
  ERROR: {
    label: 'Error',
    className: 'bg-danger-600 text-white',
  },
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status] ?? { label: status, className: 'bg-secondary-500/10 text-secondary-400' }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  )
}
