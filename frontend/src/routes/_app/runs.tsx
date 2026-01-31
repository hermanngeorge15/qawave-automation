import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useRuns, usePackages } from '@/hooks'
import { StatusBadge, EmptyState, Skeleton } from '@/components/ui'
import type { TestRun, TestRunStatus } from '@/api/types'

export const Route = createFileRoute('/_app/runs')({
  component: RunsListPage,
})

type SortOption = 'started' | 'status' | 'duration'

const STATUS_OPTIONS: { value: TestRunStatus | ''; label: string }[] = [
  { value: '', label: 'All Statuses' },
  { value: 'RUNNING', label: 'Running' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'started', label: 'Most Recent' },
  { value: 'status', label: 'Status' },
  { value: 'duration', label: 'Duration' },
]

const PAGE_SIZE_OPTIONS = [10, 20, 50]

function RunsListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="card">
          <div className="flex items-center gap-4">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-5 w-24 rounded-full" />
            <div className="flex-1" />
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-8 w-20" />
          </div>
        </div>
      ))}
    </div>
  )
}

interface RunCardProps {
  run: TestRun
  packageName: string | undefined
}

function RunCard({ run, packageName }: RunCardProps) {
  const isRunning = run.status === 'PENDING' || run.status === 'RUNNING'
  const progressPercent = run.summary.totalScenarios > 0
    ? ((run.summary.passedScenarios + run.summary.failedScenarios) / run.summary.totalScenarios) * 100
    : 0

  const duration = run.completedAt && run.startedAt
    ? Math.round((new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime()) / 1000)
    : null

  return (
    <Link
      to="/runs/$runId"
      params={{ runId: run.id }}
      className="card hover:border-primary-500 transition-colors block"
    >
      <div className="flex flex-col lg:flex-row lg:items-center gap-4">
        {/* Main Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3">
            <span className="font-medium text-white">Run #{run.id.slice(0, 8)}</span>
            <StatusBadge status={run.status} />
          </div>
          <div className="flex items-center gap-2 mt-1 text-sm text-secondary-400">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
              />
            </svg>
            <span className="truncate">{packageName ?? 'Unknown Package'}</span>
          </div>
        </div>

        {/* Progress (for running) */}
        {isRunning && (
          <div className="w-48">
            <div className="flex items-center gap-2 text-sm text-secondary-400 mb-1">
              <span>
                {run.summary.passedScenarios + run.summary.failedScenarios}/{run.summary.totalScenarios}
              </span>
              <span className="text-secondary-500">scenarios</span>
            </div>
            <div className="h-2 bg-secondary-700 rounded-full overflow-hidden">
              <div
                className="h-full bg-primary-500 rounded-full transition-all duration-300"
                style={{ width: `${String(Math.max(progressPercent, 2))}%` }}
              />
            </div>
          </div>
        )}

        {/* Summary Stats */}
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-1">
            <span className="text-green-500 font-medium">{run.summary.passedScenarios}</span>
            <span className="text-secondary-500">passed</span>
          </div>
          <div className="flex items-center gap-1">
            <span className="text-red-500 font-medium">{run.summary.failedScenarios}</span>
            <span className="text-secondary-500">failed</span>
          </div>
          {duration !== null && (
            <div className="flex items-center gap-1 text-secondary-400">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span>{duration}s</span>
            </div>
          )}
        </div>

        {/* Timestamp */}
        <div className="text-sm text-secondary-500 shrink-0">
          {run.startedAt ? formatRelativeTime(run.startedAt) : 'Not started'}
        </div>

        <div className="text-secondary-400 shrink-0">→</div>
      </div>
    </Link>
  )
}

function RunsListPage() {
  const [page, setPage] = useState(0)
  const [selectedPackage, setSelectedPackage] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<TestRunStatus | ''>('')
  const [sortBy, setSortBy] = useState<SortOption>('started')
  const [pageSize, setPageSize] = useState(20)

  // Fetch packages for filter dropdown
  const { data: packagesData } = usePackages(0, 100)

  // Fetch runs
  const { data, isLoading, isError, error, refetch } = useRuns({
    page,
    size: pageSize,
    packageId: selectedPackage || undefined,
    status: selectedStatus || undefined,
  })

  // Create a map of package IDs to names
  const packageNameMap = useMemo(() => {
    const map = new Map<string, string>()
    packagesData?.content.forEach((pkg) => {
      map.set(pkg.id, pkg.name)
    })
    return map
  }, [packagesData])

  // Sort runs (client-side)
  const sortedRuns = useMemo(() => {
    if (!data?.content) return []

    const runs = [...data.content]
    runs.sort((a, b) => {
      switch (sortBy) {
        case 'started':
          return new Date(b.startedAt ?? 0).getTime() - new Date(a.startedAt ?? 0).getTime()
        case 'status':
          return a.status.localeCompare(b.status)
        case 'duration': {
          const durationA = a.completedAt && a.startedAt
            ? new Date(a.completedAt).getTime() - new Date(a.startedAt).getTime()
            : 0
          const durationB = b.completedAt && b.startedAt
            ? new Date(b.completedAt).getTime() - new Date(b.startedAt).getTime()
            : 0
          return durationB - durationA
        }
        default:
          return 0
      }
    })
    return runs
  }, [data?.content, sortBy])

  // Reset page when filters change
  const handleFilterChange = <T,>(setter: React.Dispatch<React.SetStateAction<T>>) => {
    return (value: T) => {
      setter(value)
      setPage(0)
    }
  }

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize)
    setPage(0)
  }

  // Count running runs
  const runningCount = sortedRuns.filter(r => r.status === 'RUNNING' || r.status === 'PENDING').length

  return (
    <div className="runs-page">
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="page-title">Test Runs</h1>
          <p className="text-secondary-400">
            View all test executions across packages
            {runningCount > 0 && (
              <span className="ml-2 text-primary-400">
                ({runningCount} running)
              </span>
            )}
          </p>
        </div>
        <button
          onClick={() => { void refetch() }}
          className="btn btn-ghost"
        >
          ↻ Refresh
        </button>
      </header>

      {/* Filters */}
      <div className="bg-secondary-800/50 rounded-lg p-4 mb-6">
        <div className="flex flex-col lg:flex-row gap-4">
          {/* Package Filter */}
          <select
            value={selectedPackage}
            onChange={(e) => {
              handleFilterChange(setSelectedPackage)(e.target.value)
            }}
            className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
          >
            <option value="">All Packages</option>
            {packagesData?.content.map((pkg) => (
              <option key={pkg.id} value={pkg.id}>
                {pkg.name}
              </option>
            ))}
          </select>

          {/* Status Filter */}
          <select
            value={selectedStatus}
            onChange={(e) => {
              handleFilterChange(setSelectedStatus)(e.target.value as TestRunStatus | '')
            }}
            className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          {/* Sort */}
          <select
            value={sortBy}
            onChange={(e) => {
              setSortBy(e.target.value as SortOption)
            }}
            className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {/* Results count */}
        {!isLoading && data && (
          <div className="mt-3 text-sm text-secondary-400">
            Showing {sortedRuns.length} of {data.totalElements} runs
            {(selectedPackage || selectedStatus) && ' (filtered)'}
          </div>
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <RunsListSkeleton />
      ) : isError ? (
        <div className="error-state">
          <h2>Error loading runs</h2>
          <p>{error.message}</p>
          <button
            onClick={() => { void refetch() }}
            className="btn btn-primary mt-4"
          >
            Retry
          </button>
        </div>
      ) : sortedRuns.length === 0 ? (
        <EmptyState
          title={selectedPackage || selectedStatus ? 'No runs found' : 'No test runs yet'}
          description={
            selectedPackage || selectedStatus
              ? 'Try adjusting your filters'
              : 'Test runs will appear here after you run tests from a package'
          }
          action={
            !(selectedPackage || selectedStatus) && (
              <Link to="/packages" className="btn btn-primary">
                View Packages
              </Link>
            )
          }
          icon={
            <svg className="w-full h-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          }
        />
      ) : (
        <div className="space-y-3">
          {sortedRuns.map((run) => (
            <RunCard
              key={run.id}
              run={run}
              packageName={packageNameMap.get(run.packageId)}
            />
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mt-8 pt-4 border-t border-secondary-700">
          <div className="flex items-center gap-2">
            <span className="text-sm text-secondary-400">Show:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                handlePageSizeChange(Number(e.target.value))
              }}
              className="px-3 py-1 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500"
            >
              {PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
            <span className="text-sm text-secondary-400">per page</span>
          </div>

          <div className="flex items-center gap-4">
            <button
              onClick={() => {
                setPage((p) => Math.max(0, p - 1))
              }}
              disabled={data.first}
              className="btn btn-ghost disabled:opacity-50"
            >
              ← Previous
            </button>
            <span className="text-secondary-400">
              Page {page + 1} of {data.totalPages}
            </span>
            <button
              onClick={() => {
                setPage((p) => p + 1)
              }}
              disabled={data.last}
              className="btn btn-ghost disabled:opacity-50"
            >
              Next →
            </button>
          </div>
        </div>
      )}
    </div>
  )
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
