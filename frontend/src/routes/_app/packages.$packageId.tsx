import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { usePackage, usePackageScenarios, usePackageRuns, useStartRun, useGenerateScenarios } from '@/hooks'
import { StatusBadge, Skeleton, EmptyState } from '@/components/ui'
import type { Scenario, TestRun, HttpMethod } from '@/api/types'

export const Route = createFileRoute('/_app/packages/$packageId')({
  component: PackageDetailPage,
})

type TabId = 'scenarios' | 'runs' | 'coverage'

function PackageDetailPage() {
  const { packageId } = Route.useParams()
  const [activeTab, setActiveTab] = useState<TabId>('scenarios')

  const { data: pkg, isLoading, isError, error } = usePackage(packageId)
  const { data: scenarios, isLoading: scenariosLoading } = usePackageScenarios(packageId)
  const { data: runs, isLoading: runsLoading } = usePackageRuns(packageId)
  const startRun = useStartRun()
  const generateScenarios = useGenerateScenarios()

  if (isLoading) {
    return <PackageDetailSkeleton />
  }

  if (isError || !pkg) {
    return (
      <div className="error-state">
        <h2>Error loading package</h2>
        <p>{error?.message ?? 'Package not found'}</p>
        <Link to="/packages" className="btn btn-primary mt-4">
          Back to Packages
        </Link>
      </div>
    )
  }

  const handleStartRun = () => {
    startRun.mutate(packageId)
  }

  const handleGenerateScenarios = () => {
    generateScenarios.mutate(packageId)
  }

  const canStartRun = pkg.status === 'READY' || pkg.status === 'COMPLETED' || pkg.status === 'FAILED'
  const canGenerateScenarios = pkg.status === 'DRAFT'

  return (
    <div className="package-detail-page">
      {/* Header */}
      <header className="mb-8">
        <div className="flex items-center gap-2 text-secondary-400 text-sm mb-2">
          <Link to="/packages" className="hover:text-white transition-colors">
            Packages
          </Link>
          <span>/</span>
          <span className="text-white">{pkg.name}</span>
        </div>

        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-white">{pkg.name}</h1>
              <StatusBadge status={pkg.status} />
            </div>
            {pkg.description && (
              <p className="text-secondary-400 mt-1">{pkg.description}</p>
            )}
          </div>

          <div className="flex gap-2">
            {canGenerateScenarios && (
              <button
                onClick={handleGenerateScenarios}
                disabled={generateScenarios.isPending}
                className="btn btn-secondary disabled:opacity-50"
              >
                {generateScenarios.isPending ? 'Generating...' : 'Generate Scenarios'}
              </button>
            )}
            {canStartRun && (
              <button
                onClick={handleStartRun}
                disabled={startRun.isPending}
                className="btn btn-primary disabled:opacity-50"
              >
                {startRun.isPending ? 'Starting...' : 'Start Run'}
              </button>
            )}
            <Link
              to="/packages/$packageId"
              params={{ packageId }}
              className="btn btn-ghost"
            >
              Edit
            </Link>
          </div>
        </div>
      </header>

      {/* Package Info */}
      <section className="card mb-6">
        <h2 className="text-lg font-semibold text-white mb-4">Package Details</h2>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <dt className="text-sm text-secondary-500">Base URL</dt>
            <dd className="text-white font-mono text-sm">{pkg.baseUrl}</dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Status</dt>
            <dd><StatusBadge status={pkg.status} /></dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Created</dt>
            <dd className="text-white">{formatDate(pkg.createdAt)}</dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Last Updated</dt>
            <dd className="text-white">{formatDate(pkg.updatedAt)}</dd>
          </div>
        </dl>
      </section>

      {/* Tabs */}
      <div className="border-b border-secondary-700 mb-6">
        <nav className="flex gap-4">
          <TabButton
            id="scenarios"
            label={`Scenarios (${String(scenarios?.totalElements ?? 0)})`}
            activeTab={activeTab}
            onClick={setActiveTab}
          />
          <TabButton
            id="runs"
            label={`Test Runs (${String(runs?.totalElements ?? 0)})`}
            activeTab={activeTab}
            onClick={setActiveTab}
          />
          <TabButton
            id="coverage"
            label="Coverage"
            activeTab={activeTab}
            onClick={setActiveTab}
          />
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'scenarios' && (
        <ScenariosTab scenarios={scenarios?.content ?? []} isLoading={scenariosLoading} />
      )}
      {activeTab === 'runs' && (
        <RunsTab runs={runs?.content ?? []} isLoading={runsLoading} packageId={packageId} />
      )}
      {activeTab === 'coverage' && (
        <CoverageTab scenarios={scenarios?.content ?? []} isLoading={scenariosLoading} />
      )}
    </div>
  )
}

function ScenariosTab({ scenarios, isLoading }: { scenarios: Scenario[]; isLoading: boolean }) {
  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-20" />
        ))}
      </div>
    )
  }

  if (scenarios.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="w-12 h-12 mx-auto mb-4 text-secondary-600">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
        </div>
        <h3 className="text-lg font-medium text-white mb-1">No scenarios yet</h3>
        <p className="text-secondary-400">
          Generate scenarios from the OpenAPI spec to get started
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {scenarios.map((scenario) => (
        <ScenarioCard key={scenario.id} scenario={scenario} />
      ))}
    </div>
  )
}

function ScenarioCard({ scenario }: { scenario: Scenario }) {
  return (
    <div className="card hover:border-primary-500 transition-colors">
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-medium text-white">{scenario.name}</h3>
            <StatusBadge status={scenario.status} />
          </div>
          {scenario.description && (
            <p className="text-secondary-400 text-sm">{scenario.description}</p>
          )}
          <p className="text-secondary-500 text-sm mt-2">
            {scenario.steps.length} step{scenario.steps.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>
    </div>
  )
}

function RunsTab({
  runs,
  isLoading,
  packageId,
}: {
  runs: TestRun[]
  isLoading: boolean
  packageId: string
}) {
  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-20" />
        ))}
      </div>
    )
  }

  if (runs.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="w-12 h-12 mx-auto mb-4 text-secondary-600">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
        </div>
        <h3 className="text-lg font-medium text-white mb-1">No test runs yet</h3>
        <p className="text-secondary-400">Start a run to execute your test scenarios</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {runs.map((run) => (
        <RunCard key={run.id} run={run} packageId={packageId} />
      ))}
    </div>
  )
}

function RunCard({ run, packageId }: { run: TestRun; packageId: string }) {
  const statusColors: Record<string, string> = {
    PENDING: 'bg-yellow-500/10 text-yellow-500',
    RUNNING: 'bg-blue-500/10 text-blue-500',
    COMPLETED: 'bg-green-500/10 text-green-500',
    CANCELLED: 'bg-secondary-500/10 text-secondary-500',
  }

  return (
    <Link
      to="/packages/$packageId"
      params={{ packageId }}
      className="card block hover:border-primary-500 transition-colors"
    >
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-medium text-white">Run #{run.id.slice(0, 8)}</span>
            <span
              className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[run.status] ?? 'bg-yellow-500/10 text-yellow-500'}`}
            >
              {run.status}
            </span>
          </div>
          <div className="flex gap-4 text-sm text-secondary-400">
            <span>{run.summary.passedScenarios}/{run.summary.totalScenarios} passed</span>
            {run.summary.failedScenarios > 0 && (
              <span className="text-red-500">{run.summary.failedScenarios} failed</span>
            )}
            {run.startedAt && <span>Started {formatDate(run.startedAt)}</span>}
          </div>
        </div>
        <div className="text-secondary-500">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </Link>
  )
}

function PackageDetailSkeleton() {
  return (
    <div className="package-detail-page">
      <div className="mb-8">
        <Skeleton className="h-4 w-40 mb-4" />
        <div className="flex justify-between items-center">
          <div>
            <Skeleton className="h-8 w-64 mb-2" />
            <Skeleton className="h-4 w-96" />
          </div>
          <div className="flex gap-2">
            <Skeleton className="h-10 w-32" />
            <Skeleton className="h-10 w-24" />
          </div>
        </div>
      </div>

      <Skeleton className="h-40 mb-6" />
      <Skeleton className="h-10 w-64 mb-6" />
      <div className="space-y-3">
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
      </div>
    </div>
  )
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function TabButton({
  id,
  label,
  activeTab,
  onClick,
}: {
  id: TabId
  label: string
  activeTab: TabId
  onClick: (tab: TabId) => void
}) {
  const isActive = activeTab === id
  return (
    <button
      onClick={() => { onClick(id) }}
      className={`pb-3 px-1 border-b-2 transition-colors ${
        isActive
          ? 'border-primary-500 text-white'
          : 'border-transparent text-secondary-400 hover:text-white'
      }`}
    >
      {label}
    </button>
  )
}

// HTTP Method colors for coverage display
const HTTP_METHOD_COLORS: Record<HttpMethod, string> = {
  GET: 'bg-green-500/10 text-green-400 border-green-500/30',
  POST: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
  PUT: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
  PATCH: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  DELETE: 'bg-red-500/10 text-red-400 border-red-500/30',
}

type CoverageStatus = 'covered' | 'failing' | 'missing'

interface EndpointCoverage {
  endpoint: string
  method: HttpMethod
  status: CoverageStatus
  scenarioCount: number
  scenarioIds: string[]
}

function CoverageTab({ scenarios, isLoading }: { scenarios: Scenario[]; isLoading: boolean }) {
  // Calculate coverage from scenarios
  const coverage = useMemo(() => {
    if (scenarios.length === 0) {
      return {
        endpoints: [] as EndpointCoverage[],
        covered: 0,
        failing: 0,
        missing: 0,
        total: 0,
        percentage: 0,
      }
    }

    // Extract unique endpoints from all scenarios
    const endpointMap = new Map<string, EndpointCoverage>()

    scenarios.forEach((scenario) => {
      scenario.steps.forEach((step) => {
        const key = `${step.method}:${step.endpoint}`
        const existing = endpointMap.get(key)

        // Determine status based on scenario status
        const isPassing = scenario.status === 'PASSED'
        const isFailing = scenario.status === 'FAILED'

        if (existing) {
          existing.scenarioCount++
          existing.scenarioIds.push(scenario.id)
          // If any scenario is passing, mark as covered; else if failing, mark as failing
          if (isPassing && existing.status !== 'covered') {
            existing.status = 'covered'
          } else if (isFailing && existing.status === 'missing') {
            existing.status = 'failing'
          }
        } else {
          endpointMap.set(key, {
            endpoint: step.endpoint,
            method: step.method,
            status: isPassing ? 'covered' : isFailing ? 'failing' : 'missing',
            scenarioCount: 1,
            scenarioIds: [scenario.id],
          })
        }
      })
    })

    const endpoints = Array.from(endpointMap.values())
    const covered = endpoints.filter((e) => e.status === 'covered').length
    const failing = endpoints.filter((e) => e.status === 'failing').length
    const missing = endpoints.filter((e) => e.status === 'missing').length
    const total = endpoints.length
    const percentage = total > 0 ? Math.round((covered / total) * 100) : 0

    // Sort: failing first, then covered, then missing
    endpoints.sort((a, b) => {
      const order: Record<CoverageStatus, number> = { failing: 0, covered: 1, missing: 2 }
      return order[a.status] - order[b.status]
    })

    return { endpoints, covered, failing, missing, total, percentage }
  }, [scenarios])

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-32" />
        <Skeleton className="h-64" />
      </div>
    )
  }

  if (coverage.total === 0) {
    return (
      <EmptyState
        title="No coverage data"
        description="Generate scenarios to see API coverage information"
        icon={
          <svg className="w-full h-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        }
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Coverage Summary */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-white">API Coverage</h3>
          <span className="text-2xl font-bold text-primary-400">{coverage.percentage}%</span>
        </div>

        {/* Progress Bar */}
        <div className="h-4 bg-secondary-700 rounded-full overflow-hidden mb-4">
          <div
            className="h-full bg-primary-500 rounded-full transition-all duration-500"
            style={{ width: `${String(coverage.percentage)}%` }}
          />
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 text-center">
          <div className="p-3 bg-green-500/10 rounded-lg">
            <div className="text-2xl font-bold text-green-400">{coverage.covered}</div>
            <div className="text-sm text-secondary-400">Covered</div>
          </div>
          <div className="p-3 bg-red-500/10 rounded-lg">
            <div className="text-2xl font-bold text-red-400">{coverage.failing}</div>
            <div className="text-sm text-secondary-400">Failing</div>
          </div>
          <div className="p-3 bg-secondary-700/50 rounded-lg">
            <div className="text-2xl font-bold text-secondary-400">{coverage.missing}</div>
            <div className="text-sm text-secondary-400">Pending</div>
          </div>
        </div>
      </div>

      {/* Endpoint Coverage Table */}
      <div className="card">
        <h3 className="text-lg font-semibold text-white mb-4">Endpoint Coverage</h3>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-secondary-400 text-sm border-b border-secondary-700">
                <th className="pb-3 pr-4">Endpoint</th>
                <th className="pb-3 px-4">Method</th>
                <th className="pb-3 px-4">Status</th>
                <th className="pb-3 pl-4 text-right">Scenarios</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-secondary-700">
              {coverage.endpoints.map((endpoint, index) => (
                <tr key={`${endpoint.method}-${endpoint.endpoint}-${String(index)}`} className="hover:bg-secondary-800/50">
                  <td className="py-3 pr-4">
                    <span className="font-mono text-white">{endpoint.endpoint}</span>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded text-xs font-mono border ${HTTP_METHOD_COLORS[endpoint.method]}`}>
                      {endpoint.method}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <CoverageStatusBadge status={endpoint.status} />
                  </td>
                  <td className="py-3 pl-4 text-right text-secondary-400">
                    {endpoint.scenarioCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Legend */}
        <div className="mt-4 pt-4 border-t border-secondary-700 flex gap-6 text-sm">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-green-500"></span>
            <span className="text-secondary-400">Covered (passing)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-red-500"></span>
            <span className="text-secondary-400">Failing (has tests)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-secondary-500"></span>
            <span className="text-secondary-400">Pending (no status)</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function CoverageStatusBadge({ status }: { status: CoverageStatus }) {
  const config: Record<CoverageStatus, { label: string; color: string }> = {
    covered: { label: 'Covered', color: 'bg-green-500/10 text-green-400' },
    failing: { label: 'Failing', color: 'bg-red-500/10 text-red-400' },
    missing: { label: 'Pending', color: 'bg-secondary-500/10 text-secondary-400' },
  }

  const { label, color } = config[status]

  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${color}`}>
      {label}
    </span>
  )
}
