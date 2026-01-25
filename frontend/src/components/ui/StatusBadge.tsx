import type { QaPackageStatus } from '@/api/types'

interface StatusBadgeProps {
  status: QaPackageStatus
}

const statusConfig: Record<QaPackageStatus, { label: string; className: string }> = {
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
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status]

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  )
}
