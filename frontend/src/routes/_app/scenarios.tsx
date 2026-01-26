import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useScenarios, usePackages } from '@/hooks'
import { StatusBadge, EmptyState, Skeleton } from '@/components/ui'
import type { Scenario, ScenarioStatus } from '@/api/types'

export const Route = createFileRoute('/_app/scenarios')({
  component: ScenariosPage,
})

function ScenariosListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="card flex items-center gap-4">
          <Skeleton className="h-5 w-48" />
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-5 w-16" />
          <Skeleton className="h-6 w-20 rounded-full" />
          <div className="ml-auto">
            <Skeleton className="h-8 w-24 rounded-md" />
          </div>
        </div>
      ))}
    </div>
  )
}

interface ScenarioRowProps {
  scenario: Scenario
  packageName: string | undefined
}

function ScenarioRow({ scenario, packageName }: ScenarioRowProps) {
  return (
    <div className="card hover:border-primary-500 transition-colors">
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-semibold text-white truncate">{scenario.name}</h3>
          {scenario.description && (
            <p className="text-secondary-400 text-sm line-clamp-1 mt-1">{scenario.description}</p>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-4 text-sm">
          <div className="flex items-center gap-2 text-secondary-400">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
              />
            </svg>
            <span className="truncate max-w-[120px]" title={packageName}>
              {packageName ?? 'Unknown Package'}
            </span>
          </div>

          <div className="flex items-center gap-2 text-secondary-400">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
              />
            </svg>
            <span>{scenario.steps.length} steps</span>
          </div>

          <StatusBadge status={scenario.status} />
        </div>

        <Link
          to="/packages/$packageId"
          params={{ packageId: scenario.packageId }}
          className="btn btn-ghost text-sm py-1 px-3 shrink-0"
        >
          View Package
        </Link>
      </div>
    </div>
  )
}

const STATUS_OPTIONS: { value: ScenarioStatus | ''; label: string }[] = [
  { value: '', label: 'All Statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'RUNNING', label: 'Running' },
  { value: 'PASSED', label: 'Passed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'SKIPPED', label: 'Skipped' },
]

function ScenariosPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [selectedPackage, setSelectedPackage] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<ScenarioStatus | ''>('')
  const pageSize = 20

  // Fetch packages for the filter dropdown
  const { data: packagesData } = usePackages(0, 100)

  // Fetch scenarios with filters
  const { data, isLoading, isError, error } = useScenarios({
    page,
    size: pageSize,
    packageId: selectedPackage || undefined,
    status: selectedStatus || undefined,
    search: search || undefined,
  })

  // Create a map of package IDs to names for display
  const packageNameMap = useMemo(() => {
    const map = new Map<string, string>()
    packagesData?.content.forEach((pkg) => {
      map.set(pkg.id, pkg.name)
    })
    return map
  }, [packagesData])

  // Reset to first page when filters change
  const handleSearchChange = (value: string) => {
    setSearch(value)
    setPage(0)
  }

  const handlePackageChange = (value: string) => {
    setSelectedPackage(value)
    setPage(0)
  }

  const handleStatusChange = (value: string) => {
    setSelectedStatus(value as ScenarioStatus | '')
    setPage(0)
  }

  const scenarios = data?.content ?? []

  return (
    <div className="scenarios-page">
      <header className="mb-8">
        <h1 className="page-title">Test Scenarios</h1>
        <p className="text-secondary-400">View and manage test scenarios across all packages</p>
      </header>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        <input
          type="text"
          placeholder="Search scenarios..."
          value={search}
          onChange={(e) => {
            handleSearchChange(e.target.value)
          }}
          className="w-full sm:w-80 px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors"
        />

        <select
          value={selectedPackage}
          onChange={(e) => {
            handlePackageChange(e.target.value)
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

        <select
          value={selectedStatus}
          onChange={(e) => {
            handleStatusChange(e.target.value)
          }}
          className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {/* Content */}
      {isLoading ? (
        <ScenariosListSkeleton />
      ) : isError ? (
        <div className="error-state">
          <h2>Error loading scenarios</h2>
          <p>{error.message}</p>
          <button
            onClick={() => {
              window.location.reload()
            }}
            className="btn btn-primary mt-4"
          >
            Retry
          </button>
        </div>
      ) : scenarios.length === 0 ? (
        <EmptyState
          title={search || selectedPackage || selectedStatus ? 'No scenarios found' : 'No scenarios yet'}
          description={
            search || selectedPackage || selectedStatus
              ? 'Try adjusting your filters'
              : 'Scenarios will appear here after generating them from a QA package'
          }
          action={
            !(search || selectedPackage || selectedStatus) && (
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
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
              />
            </svg>
          }
        />
      ) : (
        <>
          <div className="space-y-3">
            {scenarios.map((scenario) => (
              <ScenarioRow
                key={scenario.id}
                scenario={scenario}
                packageName={packageNameMap.get(scenario.packageId)}
              />
            ))}
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-4 mt-8">
              <button
                onClick={() => {
                  setPage((p) => Math.max(0, p - 1))
                }}
                disabled={data.first}
                className="btn btn-ghost disabled:opacity-50"
              >
                Previous
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
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
