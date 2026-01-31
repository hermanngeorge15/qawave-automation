import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useScenarios, usePackages } from '@/hooks'
import { StatusBadge, EmptyState, Skeleton } from '@/components/ui'
import type { Scenario, ScenarioStatus, HttpMethod } from '@/api/types'

export const Route = createFileRoute('/_app/scenarios')({
  component: ScenariosPage,
})

type SortOption = 'name' | 'status' | 'steps' | 'updated'
type ViewMode = 'flat' | 'grouped'

const STATUS_OPTIONS: { value: ScenarioStatus | ''; label: string }[] = [
  { value: '', label: 'All Statuses' },
  { value: 'PASSED', label: 'Passed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'RUNNING', label: 'Running' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'SKIPPED', label: 'Skipped' },
]

const METHOD_OPTIONS: { value: HttpMethod | ''; label: string }[] = [
  { value: '', label: 'All Methods' },
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'PATCH', label: 'PATCH' },
  { value: 'DELETE', label: 'DELETE' },
]

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'name', label: 'Name (A-Z)' },
  { value: 'status', label: 'Status' },
  { value: 'steps', label: 'Steps Count' },
  { value: 'updated', label: 'Recently Updated' },
]

const PAGE_SIZE_OPTIONS = [10, 20, 50]

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
  // Get primary method and endpoint from first step
  const primaryStep = scenario.steps[0]
  const primaryMethod = primaryStep?.method ?? 'GET'
  const primaryEndpoint = primaryStep?.endpoint ?? ''

  return (
    <div className="card hover:border-primary-500 transition-colors">
      <div className="flex flex-col lg:flex-row lg:items-center gap-4">
        {/* Main info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-white truncate">{scenario.name}</h3>
            <StatusBadge status={scenario.status} />
          </div>
          <div className="flex items-center gap-2 mt-1">
            <MethodBadge method={primaryMethod} />
            <span className="text-secondary-400 text-sm truncate">{primaryEndpoint}</span>
          </div>
          {scenario.description && (
            <p className="text-secondary-500 text-sm line-clamp-1 mt-1">{scenario.description}</p>
          )}
        </div>

        {/* Meta info */}
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
              {packageName ?? 'Unknown'}
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
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 shrink-0">
          <Link
            to="/packages/$packageId"
            params={{ packageId: scenario.packageId }}
            className="btn btn-ghost text-sm py-1 px-3"
          >
            View Package
          </Link>
        </div>
      </div>
    </div>
  )
}

// Method badge with color coding
function MethodBadge({ method }: { method: HttpMethod }) {
  const colors: Record<HttpMethod, string> = {
    GET: 'bg-green-500/20 text-green-400 border-green-500/30',
    POST: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    PUT: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
    PATCH: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    DELETE: 'bg-red-500/20 text-red-400 border-red-500/30',
  }

  return (
    <span className={`px-2 py-0.5 text-xs font-medium rounded border ${colors[method]}`}>
      {method}
    </span>
  )
}

// Grouped view - scenarios organized by package
interface GroupedViewProps {
  scenarios: Scenario[]
  packageNameMap: Map<string, string>
}

function GroupedView({ scenarios, packageNameMap }: GroupedViewProps) {
  // Group scenarios by package
  const groupedScenarios = useMemo(() => {
    const groups = new Map<string, Scenario[]>()
    scenarios.forEach((scenario) => {
      const existing = groups.get(scenario.packageId) ?? []
      groups.set(scenario.packageId, [...existing, scenario])
    })
    return groups
  }, [scenarios])

  return (
    <div className="space-y-6">
      {Array.from(groupedScenarios.entries()).map(([packageId, pkgScenarios]) => (
        <div key={packageId} className="border border-secondary-700 rounded-lg overflow-hidden">
          <div className="bg-secondary-800/50 px-4 py-3 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-lg">📦</span>
              <h3 className="font-semibold text-white">
                {packageNameMap.get(packageId) ?? 'Unknown Package'}
              </h3>
              <span className="text-secondary-400 text-sm">
                ({pkgScenarios.length} scenarios)
              </span>
            </div>
            <Link
              to="/packages/$packageId"
              params={{ packageId }}
              className="btn btn-ghost text-sm py-1 px-3"
            >
              View Package →
            </Link>
          </div>
          <div className="divide-y divide-secondary-700">
            {pkgScenarios.map((scenario) => (
              <GroupedScenarioRow key={scenario.id} scenario={scenario} />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function GroupedScenarioRow({ scenario }: { scenario: Scenario }) {
  const primaryStep = scenario.steps[0]
  const primaryMethod = primaryStep?.method ?? 'GET'
  const primaryEndpoint = primaryStep?.endpoint ?? ''

  return (
    <div className="px-4 py-3 hover:bg-secondary-800/30 transition-colors flex items-center gap-4">
      <StatusIcon status={scenario.status} />
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="font-medium text-white truncate">{scenario.name}</span>
          <span className="text-secondary-400 text-sm">{scenario.steps.length} steps</span>
        </div>
        <div className="flex items-center gap-2 mt-0.5">
          <MethodBadge method={primaryMethod} />
          <span className="text-secondary-500 text-sm truncate">{primaryEndpoint}</span>
        </div>
      </div>
      <StatusBadge status={scenario.status} />
    </div>
  )
}

function StatusIcon({ status }: { status: ScenarioStatus }) {
  const icons: Record<ScenarioStatus, { icon: string; color: string }> = {
    PASSED: { icon: '✓', color: 'text-green-400' },
    FAILED: { icon: '✗', color: 'text-red-400' },
    RUNNING: { icon: '◌', color: 'text-blue-400' },
    PENDING: { icon: '○', color: 'text-secondary-400' },
    SKIPPED: { icon: '⊘', color: 'text-yellow-400' },
  }

  const { icon, color } = icons[status]

  return <span className={`text-lg ${color}`}>{icon}</span>
}

function ScenariosPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [selectedPackage, setSelectedPackage] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<ScenarioStatus | ''>('')
  const [selectedMethod, setSelectedMethod] = useState<HttpMethod | ''>('')
  const [sortBy, setSortBy] = useState<SortOption>('name')
  const [viewMode, setViewMode] = useState<ViewMode>('flat')
  const [pageSize, setPageSize] = useState(20)

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

  // Filter by method and sort (client-side for method since API might not support it)
  const filteredAndSortedScenarios = useMemo(() => {
    if (!data?.content) return []

    let result = [...data.content]

    // Filter by method (check first step)
    if (selectedMethod) {
      result = result.filter((scenario) => {
        const primaryMethod = scenario.steps[0]?.method
        return primaryMethod === selectedMethod
      })
    }

    // Sort
    result.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name)
        case 'status':
          return a.status.localeCompare(b.status)
        case 'steps':
          return b.steps.length - a.steps.length
        case 'updated':
          return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        default:
          return 0
      }
    })

    return result
  }, [data?.content, selectedMethod, sortBy])

  // Reset to first page when filters change
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

  return (
    <div className="scenarios-page">
      <header className="mb-8">
        <h1 className="page-title">Test Scenarios</h1>
        <p className="text-secondary-400">View and manage test scenarios across all packages</p>
      </header>

      {/* Filters */}
      <div className="bg-secondary-800/50 rounded-lg p-4 mb-6">
        <div className="flex flex-col lg:flex-row gap-4">
          {/* Search */}
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search scenarios..."
              value={search}
              onChange={(e) => {
                handleFilterChange(setSearch)(e.target.value)
              }}
              className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors"
            />
          </div>

          {/* Filters */}
          <div className="flex flex-wrap gap-2">
            <select
              value={selectedPackage}
              onChange={(e) => {
                handleFilterChange(setSelectedPackage)(e.target.value)
              }}
              className="px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500 transition-colors"
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
                handleFilterChange(setSelectedStatus)(e.target.value as ScenarioStatus | '')
              }}
              className="px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500 transition-colors"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <select
              value={selectedMethod}
              onChange={(e) => {
                handleFilterChange(setSelectedMethod)(e.target.value as HttpMethod | '')
              }}
              className="px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500 transition-colors"
            >
              {METHOD_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <select
              value={sortBy}
              onChange={(e) => {
                setSortBy(e.target.value as SortOption)
              }}
              className="px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500 transition-colors"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            {/* View Toggle */}
            <div className="flex bg-secondary-800 border border-secondary-700 rounded-lg overflow-hidden">
              <button
                onClick={() => {
                  setViewMode('flat')
                }}
                className={`px-3 py-2 text-sm ${viewMode === 'flat' ? 'bg-primary-600 text-white' : 'text-secondary-400 hover:text-white'}`}
                title="Flat list"
              >
                List
              </button>
              <button
                onClick={() => {
                  setViewMode('grouped')
                }}
                className={`px-3 py-2 text-sm ${viewMode === 'grouped' ? 'bg-primary-600 text-white' : 'text-secondary-400 hover:text-white'}`}
                title="Grouped by package"
              >
                Grouped
              </button>
            </div>
          </div>
        </div>

        {/* Results count */}
        {!isLoading && data && (
          <div className="mt-3 text-sm text-secondary-400">
            Showing {filteredAndSortedScenarios.length} of {data.totalElements} scenarios
            {(search || selectedPackage || selectedStatus || selectedMethod) && ' (filtered)'}
          </div>
        )}
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
      ) : filteredAndSortedScenarios.length === 0 ? (
        <EmptyState
          title={search || selectedPackage || selectedStatus || selectedMethod ? 'No scenarios found' : 'No scenarios yet'}
          description={
            search || selectedPackage || selectedStatus || selectedMethod
              ? 'Try adjusting your filters'
              : 'Scenarios will appear here after generating them from a QA package'
          }
          action={
            !(search || selectedPackage || selectedStatus || selectedMethod) && (
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
      ) : viewMode === 'flat' ? (
        <div className="space-y-3">
          {filteredAndSortedScenarios.map((scenario) => (
            <ScenarioRow
              key={scenario.id}
              scenario={scenario}
              packageName={packageNameMap.get(scenario.packageId)}
            />
          ))}
        </div>
      ) : (
        <GroupedView scenarios={filteredAndSortedScenarios} packageNameMap={packageNameMap} />
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
