import { useState, useMemo, useCallback } from 'react'
import type { Scenario, HttpMethod } from '@/api/types'

export type CoverageStatus = 'covered' | 'failing' | 'untested'

export interface OperationCoverage {
  endpoint: string
  method: HttpMethod
  status: CoverageStatus
  scenarioCount: number
  scenarios: Array<{ id: string; name: string; status: string }>
}

export interface CoverageStats {
  total: number
  covered: number
  failing: number
  untested: number
  percentage: number
}

export interface OperationCoverageTableProps {
  scenarios: Scenario[]
  isLoading?: boolean
  onExport?: () => void
  isExporting?: boolean
}

// HTTP Method colors
const HTTP_METHOD_COLORS: Record<HttpMethod, string> = {
  GET: 'bg-green-500/10 text-green-400 border-green-500/30',
  POST: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
  PUT: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
  PATCH: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  DELETE: 'bg-red-500/10 text-red-400 border-red-500/30',
}

export function OperationCoverageTable({
  scenarios,
  isLoading = false,
  onExport,
  isExporting = false,
}: OperationCoverageTableProps) {
  const [statusFilter, setStatusFilter] = useState<CoverageStatus | 'all'>('all')
  const [expandedEndpoint, setExpandedEndpoint] = useState<string | null>(null)

  // Calculate coverage from scenarios
  const { operations, stats } = useMemo(() => {
    if (scenarios.length === 0) {
      return {
        operations: [] as OperationCoverage[],
        stats: { total: 0, covered: 0, failing: 0, untested: 0, percentage: 0 } as CoverageStats,
      }
    }

    const operationMap = new Map<string, OperationCoverage>()

    scenarios.forEach((scenario) => {
      scenario.steps.forEach((step) => {
        const key = `${step.method}:${step.endpoint}`
        const existing = operationMap.get(key)

        const isPassing = scenario.status === 'PASSED'
        const isFailing = scenario.status === 'FAILED'

        if (existing) {
          existing.scenarioCount++
          existing.scenarios.push({
            id: scenario.id,
            name: scenario.name,
            status: scenario.status,
          })
          // Priority: covered > failing > untested
          if (isPassing) {
            existing.status = 'covered'
          } else if (isFailing && existing.status === 'untested') {
            existing.status = 'failing'
          }
        } else {
          operationMap.set(key, {
            endpoint: step.endpoint,
            method: step.method,
            status: isPassing ? 'covered' : isFailing ? 'failing' : 'untested',
            scenarioCount: 1,
            scenarios: [{
              id: scenario.id,
              name: scenario.name,
              status: scenario.status,
            }],
          })
        }
      })
    })

    const operationsList = Array.from(operationMap.values())
    const covered = operationsList.filter((e) => e.status === 'covered').length
    const failing = operationsList.filter((e) => e.status === 'failing').length
    const untested = operationsList.filter((e) => e.status === 'untested').length
    const total = operationsList.length
    const percentage = total > 0 ? Math.round((covered / total) * 100) : 0

    // Sort: failing first, then covered, then untested
    operationsList.sort((a, b) => {
      const order: Record<CoverageStatus, number> = { failing: 0, covered: 1, untested: 2 }
      return order[a.status] - order[b.status]
    })

    return {
      operations: operationsList,
      stats: { total, covered, failing, untested, percentage },
    }
  }, [scenarios])

  // Filter operations
  const filteredOperations = useMemo(() => {
    if (statusFilter === 'all') return operations
    return operations.filter((op) => op.status === statusFilter)
  }, [operations, statusFilter])

  const handleToggleExpand = useCallback((key: string) => {
    setExpandedEndpoint((prev) => (prev === key ? null : key))
  }, [])

  const handleFilterChange = useCallback((filter: CoverageStatus | 'all') => {
    setStatusFilter(filter)
  }, [])

  if (isLoading) {
    return (
      <div className="space-y-4" data-testid="coverage-loading">
        <div className="h-32 bg-secondary-800 rounded-lg animate-pulse" />
        <div className="h-64 bg-secondary-800 rounded-lg animate-pulse" />
      </div>
    )
  }

  if (stats.total === 0) {
    return (
      <div className="text-center py-12" data-testid="coverage-empty">
        <div className="w-12 h-12 mx-auto mb-4 text-secondary-600">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        </div>
        <h3 className="text-lg font-medium text-white mb-1">No coverage data</h3>
        <p className="text-secondary-400">
          Generate scenarios to see API coverage information
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6" data-testid="coverage-table">
      {/* Coverage Summary */}
      <div className="card">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-4">
          <h3 className="text-lg font-semibold text-white">API Coverage</h3>
          <div className="flex items-center gap-3">
            <span className="text-2xl font-bold text-primary-400" data-testid="coverage-percentage">
              {stats.percentage}%
            </span>
            {onExport && (
              <button
                onClick={onExport}
                disabled={isExporting}
                className="btn btn-secondary text-sm disabled:opacity-50"
                aria-label="Export coverage report"
              >
                {isExporting ? (
                  <>
                    <svg className="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    Exporting...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                      />
                    </svg>
                    Export Report
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div
          className="h-4 bg-secondary-700 rounded-full overflow-hidden mb-4"
          role="meter"
          aria-valuenow={stats.percentage}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`Coverage: ${String(stats.percentage)}%`}
        >
          <div
            className="h-full bg-primary-500 rounded-full transition-all duration-500"
            style={{ width: `${String(stats.percentage)}%` }}
          />
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 text-center">
          <div className="p-3 bg-green-500/10 rounded-lg">
            <div className="text-2xl font-bold text-green-400" data-testid="covered-count">
              {stats.covered}
            </div>
            <div className="text-sm text-secondary-400">Covered</div>
          </div>
          <div className="p-3 bg-red-500/10 rounded-lg">
            <div className="text-2xl font-bold text-red-400" data-testid="failing-count">
              {stats.failing}
            </div>
            <div className="text-sm text-secondary-400">Failing</div>
          </div>
          <div className="p-3 bg-secondary-700/50 rounded-lg">
            <div className="text-2xl font-bold text-secondary-400" data-testid="untested-count">
              {stats.untested}
            </div>
            <div className="text-sm text-secondary-400">Untested</div>
          </div>
        </div>
      </div>

      {/* Filter Controls */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-secondary-400">Filter:</span>
        <FilterButton
          label="All"
          count={stats.total}
          isActive={statusFilter === 'all'}
          onClick={() => {
            handleFilterChange('all')
          }}
        />
        <FilterButton
          label="Covered"
          count={stats.covered}
          isActive={statusFilter === 'covered'}
          onClick={() => {
            handleFilterChange('covered')
          }}
          color="green"
        />
        <FilterButton
          label="Failing"
          count={stats.failing}
          isActive={statusFilter === 'failing'}
          onClick={() => {
            handleFilterChange('failing')
          }}
          color="red"
        />
        <FilterButton
          label="Untested"
          count={stats.untested}
          isActive={statusFilter === 'untested'}
          onClick={() => {
            handleFilterChange('untested')
          }}
          color="gray"
        />
      </div>

      {/* Operations Table */}
      <div className="card">
        <h3 className="text-lg font-semibold text-white mb-4">
          Operation Coverage
          <span className="text-sm font-normal text-secondary-400 ml-2">
            ({filteredOperations.length} of {stats.total})
          </span>
        </h3>

        {filteredOperations.length === 0 ? (
          <div className="text-center py-8 text-secondary-400" data-testid="no-results">
            No operations match the current filter
          </div>
        ) : (
          <div className="space-y-2">
            {filteredOperations.map((operation) => {
              const key = `${operation.method}:${operation.endpoint}`
              const isExpanded = expandedEndpoint === key

              return (
                <div
                  key={key}
                  className="border border-secondary-700 rounded-lg overflow-hidden"
                >
                  <button
                    onClick={() => {
                      handleToggleExpand(key)
                    }}
                    className="w-full flex items-center gap-3 p-3 hover:bg-secondary-800/50 transition-colors text-left"
                    aria-expanded={isExpanded}
                    aria-label={`${operation.method} ${operation.endpoint}, ${String(operation.scenarioCount)} scenarios, ${operation.status}`}
                  >
                    <span
                      className={`px-2 py-1 rounded text-xs font-mono border ${HTTP_METHOD_COLORS[operation.method]}`}
                    >
                      {operation.method}
                    </span>
                    <span className="font-mono text-white flex-1 truncate">
                      {operation.endpoint}
                    </span>
                    <CoverageStatusBadge status={operation.status} />
                    <span className="text-secondary-400 text-sm">
                      {operation.scenarioCount} scenario{operation.scenarioCount !== 1 ? 's' : ''}
                    </span>
                    <svg
                      className={`w-5 h-5 text-secondary-400 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </button>

                  {isExpanded && (
                    <div className="px-3 pb-3 pt-1 bg-secondary-800/30" data-testid="scenario-list">
                      <div className="text-sm text-secondary-400 mb-2">Linked Scenarios:</div>
                      <ul className="space-y-1">
                        {operation.scenarios.map((scenario) => (
                          <li
                            key={scenario.id}
                            className="flex items-center gap-2 py-1 px-2 rounded bg-secondary-800/50"
                          >
                            <ScenarioStatusDot status={scenario.status} />
                            <span className="text-white">{scenario.name}</span>
                            <span className="text-secondary-500 text-xs">
                              ({scenario.status})
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}

        {/* Legend */}
        <div className="mt-4 pt-4 border-t border-secondary-700 flex flex-wrap gap-4 sm:gap-6 text-sm">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-green-500" />
            <span className="text-secondary-400">Covered (passing tests)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-red-500" />
            <span className="text-secondary-400">Failing (tests exist)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-secondary-500" />
            <span className="text-secondary-400">Untested (no status)</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function FilterButton({
  label,
  count,
  isActive,
  onClick,
  color,
}: {
  label: string
  count: number
  isActive: boolean
  onClick: () => void
  color?: 'green' | 'red' | 'gray'
}) {
  const colorClasses = {
    green: 'bg-green-500/10 text-green-400 border-green-500/30',
    red: 'bg-red-500/10 text-red-400 border-red-500/30',
    gray: 'bg-secondary-700/50 text-secondary-400 border-secondary-600',
  }

  const baseClasses = color ? colorClasses[color] : 'bg-secondary-700/50 text-white border-secondary-600'
  const activeClasses = isActive ? 'ring-2 ring-primary-500 ring-offset-1 ring-offset-secondary-900' : ''

  return (
    <button
      onClick={onClick}
      className={`px-3 py-1.5 rounded-lg border text-sm font-medium transition-all ${baseClasses} ${activeClasses}`}
      aria-pressed={isActive}
    >
      {label} ({count})
    </button>
  )
}

function CoverageStatusBadge({ status }: { status: CoverageStatus }) {
  const config: Record<CoverageStatus, { label: string; color: string }> = {
    covered: { label: 'Covered', color: 'bg-green-500/10 text-green-400' },
    failing: { label: 'Failing', color: 'bg-red-500/10 text-red-400' },
    untested: { label: 'Untested', color: 'bg-secondary-500/10 text-secondary-400' },
  }

  const { label, color } = config[status]

  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${color}`}>
      {label}
    </span>
  )
}

function ScenarioStatusDot({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    PASSED: 'bg-green-500',
    FAILED: 'bg-red-500',
    PENDING: 'bg-yellow-500',
    RUNNING: 'bg-blue-500',
    SKIPPED: 'bg-secondary-500',
  }

  return (
    <span
      className={`w-2 h-2 rounded-full ${colorMap[status] ?? 'bg-secondary-500'}`}
      aria-label={status}
    />
  )
}
