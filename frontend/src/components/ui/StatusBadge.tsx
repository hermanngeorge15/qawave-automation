import type { QaPackageStatus, ScenarioStatus } from '@/api/types'

type Status = QaPackageStatus | ScenarioStatus

interface StatusBadgeProps {
  status: Status
}

const statusConfig: Record<Status, { label: string; className: string }> = {
  // Package statuses
  DRAFT: { label: 'Draft', className: 'bg-secondary-500/10 text-secondary-400' },
  GENERATING: { label: 'Generating', className: 'bg-blue-500/10 text-blue-500' },
  READY: { label: 'Ready', className: 'bg-green-500/10 text-green-500' },
  RUNNING: { label: 'Running', className: 'bg-blue-500/10 text-blue-500' },
  COMPLETED: { label: 'Completed', className: 'bg-green-500/10 text-green-500' },
  FAILED: { label: 'Failed', className: 'bg-red-500/10 text-red-500' },
  // Scenario statuses
  PENDING: { label: 'Pending', className: 'bg-yellow-500/10 text-yellow-500' },
  PASSED: { label: 'Passed', className: 'bg-green-500/10 text-green-500' },
  SKIPPED: { label: 'Skipped', className: 'bg-secondary-500/10 text-secondary-400' },
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status]

  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  )
}
