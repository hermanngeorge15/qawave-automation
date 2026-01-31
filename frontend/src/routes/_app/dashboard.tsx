import { createFileRoute, Link } from '@tanstack/react-router'
import { usePackages, useScenarios } from '@/hooks'
import { StatusBadge, Skeleton } from '@/components/ui'
import type { QaPackage } from '@/api/types'

export const Route = createFileRoute('/_app/dashboard')({
  component: DashboardPage,
})

function DashboardPage() {
  const { data: packages, isLoading: packagesLoading } = usePackages(0, 100)
  const { data: scenarios, isLoading: scenariosLoading } = useScenarios({ size: 100 })

  const isLoading = packagesLoading || scenariosLoading

  // Calculate stats from packages
  const stats = calculateStats(packages?.content ?? [])

  return (
    <div className="dashboard-page">
      {/* Header */}
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="text-secondary-400">Overview of your QA automation status</p>
        </div>
        <Link to="/packages/new" className="btn btn-primary">
          + New Package
        </Link>
      </header>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <SummaryCard
          title="Packages"
          value={packages?.totalElements ?? 0}
          subtitle={`${String(stats.activePackages)} active`}
          isLoading={isLoading}
          color="primary"
        />
        <SummaryCard
          title="Scenarios"
          value={scenarios?.totalElements ?? 0}
          subtitle="test scenarios"
          isLoading={isLoading}
          color="secondary"
        />
        <SummaryCard
          title="Total Runs"
          value={stats.totalRuns}
          subtitle="test executions"
          isLoading={isLoading}
          color="info"
        />
        <SummaryCard
          title="Pass Rate"
          value={`${stats.passRate.toFixed(1)}%`}
          subtitle={stats.passRate >= 80 ? '↑ Good' : '↓ Needs attention'}
          isLoading={isLoading}
          color={stats.passRate >= 80 ? 'success' : 'warning'}
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Recent Packages - Takes 2 columns */}
        <div className="lg:col-span-2">
          <RecentPackages packages={packages?.content ?? []} isLoading={isLoading} />
        </div>

        {/* Quick Actions - Takes 1 column */}
        <div className="space-y-6">
          <QuickActions />
          <PackageStatusBreakdown packages={packages?.content ?? []} isLoading={isLoading} />
        </div>
      </div>
    </div>
  )
}

interface SummaryCardProps {
  title: string
  value: string | number
  subtitle: string
  isLoading: boolean
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'info'
}

function SummaryCard({ title, value, subtitle, isLoading, color }: SummaryCardProps) {
  const colorClasses = {
    primary: 'border-primary-500/30 bg-primary-500/5',
    secondary: 'border-secondary-500/30 bg-secondary-500/5',
    success: 'border-green-500/30 bg-green-500/5',
    warning: 'border-yellow-500/30 bg-yellow-500/5',
    info: 'border-blue-500/30 bg-blue-500/5',
  }

  const valueColors = {
    primary: 'text-primary-400',
    secondary: 'text-secondary-300',
    success: 'text-green-400',
    warning: 'text-yellow-400',
    info: 'text-blue-400',
  }

  return (
    <div className={`card ${colorClasses[color]} border`}>
      <p className="text-sm text-secondary-400 mb-1">{title}</p>
      {isLoading ? (
        <Skeleton className="h-8 w-20 mb-1" />
      ) : (
        <p className={`text-3xl font-bold ${valueColors[color]}`}>{value}</p>
      )}
      <p className="text-xs text-secondary-500 mt-1">{subtitle}</p>
    </div>
  )
}

interface RecentPackagesProps {
  packages: QaPackage[]
  isLoading: boolean
}

function RecentPackages({ packages, isLoading }: RecentPackagesProps) {
  const recentPackages = packages
    .slice()
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 5)

  return (
    <div className="card">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold text-white">Recent Packages</h2>
        <Link to="/packages" className="text-sm text-primary-400 hover:text-primary-300">
          View All →
        </Link>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      ) : recentPackages.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-secondary-400 mb-4">No packages yet</p>
          <Link to="/packages/new" className="btn btn-primary">
            Create Your First Package
          </Link>
        </div>
      ) : (
        <div className="space-y-3">
          {recentPackages.map((pkg) => (
            <Link
              key={pkg.id}
              to="/packages/$packageId"
              params={{ packageId: pkg.id }}
              className="block p-3 rounded-lg bg-secondary-800/50 hover:bg-secondary-800 transition-colors"
            >
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium text-white truncate">{pkg.name}</h3>
                    <StatusBadge status={pkg.status} />
                  </div>
                  {pkg.description && (
                    <p className="text-sm text-secondary-400 truncate mt-1">{pkg.description}</p>
                  )}
                </div>
                <div className="text-xs text-secondary-500 ml-4 shrink-0">
                  {formatRelativeTime(pkg.updatedAt)}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

function QuickActions() {
  return (
    <div className="card">
      <h2 className="text-lg font-semibold text-white mb-4">Quick Actions</h2>
      <div className="space-y-2">
        <Link
          to="/packages/new"
          className="flex items-center gap-3 p-3 rounded-lg bg-secondary-800/50 hover:bg-secondary-800 transition-colors"
        >
          <span className="w-8 h-8 rounded-lg bg-primary-500/20 flex items-center justify-center text-primary-400">
            +
          </span>
          <span className="text-white">Create Package</span>
        </Link>
        <Link
          to="/packages"
          className="flex items-center gap-3 p-3 rounded-lg bg-secondary-800/50 hover:bg-secondary-800 transition-colors"
        >
          <span className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center text-blue-400">
            📦
          </span>
          <span className="text-white">View Packages</span>
        </Link>
        <Link
          to="/scenarios"
          className="flex items-center gap-3 p-3 rounded-lg bg-secondary-800/50 hover:bg-secondary-800 transition-colors"
        >
          <span className="w-8 h-8 rounded-lg bg-green-500/20 flex items-center justify-center text-green-400">
            📋
          </span>
          <span className="text-white">View Scenarios</span>
        </Link>
        <Link
          to="/settings"
          className="flex items-center gap-3 p-3 rounded-lg bg-secondary-800/50 hover:bg-secondary-800 transition-colors"
        >
          <span className="w-8 h-8 rounded-lg bg-secondary-500/20 flex items-center justify-center text-secondary-400">
            ⚙️
          </span>
          <span className="text-white">Settings</span>
        </Link>
      </div>
    </div>
  )
}

interface PackageStatusBreakdownProps {
  packages: QaPackage[]
  isLoading: boolean
}

function PackageStatusBreakdown({ packages, isLoading }: PackageStatusBreakdownProps) {
  const statusCounts = packages.reduce(
    (acc, pkg) => {
      const status = pkg.status.toLowerCase()
      if (status === 'completed' || status === 'ready') {
        acc.passing++
      } else if (status === 'failed' || status === 'error') {
        acc.failing++
      } else {
        acc.pending++
      }
      return acc
    },
    { passing: 0, failing: 0, pending: 0 }
  )

  const total = packages.length || 1

  return (
    <div className="card">
      <h2 className="text-lg font-semibold text-white mb-4">Packages by Status</h2>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : (
        <div className="space-y-3">
          <StatusBar
            label="Passing"
            count={statusCounts.passing}
            percentage={(statusCounts.passing / total) * 100}
            color="bg-green-500"
          />
          <StatusBar
            label="Failing"
            count={statusCounts.failing}
            percentage={(statusCounts.failing / total) * 100}
            color="bg-red-500"
          />
          <StatusBar
            label="Pending"
            count={statusCounts.pending}
            percentage={(statusCounts.pending / total) * 100}
            color="bg-yellow-500"
          />
        </div>
      )}
    </div>
  )
}

interface StatusBarProps {
  label: string
  count: number
  percentage: number
  color: string
}

function StatusBar({ label, count, percentage }: StatusBarProps) {
  const colorClass =
    label === 'Passing'
      ? 'bg-green-500'
      : label === 'Failing'
        ? 'bg-red-500'
        : 'bg-yellow-500'

  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span className="text-secondary-400">{label}</span>
        <span className="text-white">{count}</span>
      </div>
      <div className="h-2 bg-secondary-700 rounded-full overflow-hidden">
        <div
          className={`h-full ${colorClass} rounded-full transition-all duration-300`}
          style={{ width: `${String(Math.max(percentage, 2))}%` }}
        />
      </div>
    </div>
  )
}

function calculateStats(packages: QaPackage[]) {
  let totalRuns = 0
  let passedRuns = 0
  let activePackages = 0

  packages.forEach((pkg) => {
    if (pkg.status === 'READY' || pkg.status === 'RUNNING') {
      activePackages++
    }
    // Note: We'd need run data to calculate accurate totals
    // For now, estimate based on package status
    if (pkg.status === 'COMPLETED') {
      totalRuns += 1
      passedRuns += 1
    } else if (pkg.status === 'FAILED') {
      totalRuns += 1
    }
  })

  const passRate = totalRuns > 0 ? (passedRuns / totalRuns) * 100 : 0

  return {
    totalRuns,
    passRate,
    activePackages,
  }
}

function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMins < 1) return 'just now'
  if (diffMins < 60) return `${String(diffMins)}m ago`
  if (diffHours < 24) return `${String(diffHours)}h ago`
  if (diffDays < 7) return `${String(diffDays)}d ago`
  return date.toLocaleDateString()
}
