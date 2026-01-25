import { Link } from '@tanstack/react-router'
import type { QaPackage } from '@/api/types'
import { StatusBadge } from '@/components/ui'

interface PackageCardProps {
  pkg: QaPackage
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export function PackageCard({ pkg }: PackageCardProps) {
  return (
    <div className="card hover:border-primary-500 transition-colors">
      <div className="flex justify-between items-start mb-3">
        <h3 className="text-lg font-semibold text-white truncate pr-4">{pkg.name}</h3>
        <StatusBadge status={pkg.status} />
      </div>

      {pkg.description && (
        <p className="text-secondary-400 text-sm mb-4 line-clamp-2">{pkg.description}</p>
      )}

      <div className="flex justify-between items-center text-sm">
        <span className="text-secondary-500">Updated {formatDate(pkg.updatedAt)}</span>
        <Link
          to="/packages/$packageId"
          params={{ packageId: pkg.id }}
          className="btn btn-ghost text-sm py-1 px-3"
        >
          View Details
        </Link>
      </div>
    </div>
  )
}
