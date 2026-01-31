import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { runsApi, packagesApi } from '@/api'
import { StatusBadge, Skeleton, EmptyState } from '@/components/ui'
import type { TestRun, ScenarioResult, ScenarioStatus } from '@/api/types'

// Search params validation
interface CompareSearchParams {
  baseline?: string | undefined
  compare?: string | undefined
}

export const Route = createFileRoute('/_app/runs/compare')({
  component: RunComparisonPage,
  validateSearch: (search: Record<string, unknown>): CompareSearchParams => ({
    baseline: typeof search.baseline === 'string' ? search.baseline : undefined,
    compare: typeof search.compare === 'string' ? search.compare : undefined,
  }),
})

type ChangeType = 'new_failure' | 'fix' | 'unchanged' | 'both_failed' | 'both_passed'

interface ScenarioComparison {
  scenarioId: string
  scenarioName: string
  baselineStatus: ScenarioStatus | null
  compareStatus: ScenarioStatus | null
  changeType: ChangeType
  baselineDuration: number | null
  compareDuration: number | null
  durationChange: number | null
}

function RunComparisonPage() {
  const { baseline: baselineId, compare: compareId } = Route.useSearch()
  const [selectedBaseline, setSelectedBaseline] = useState(baselineId ?? '')
  const [selectedCompare, setSelectedCompare] = useState(compareId ?? '')

  // Fetch recent runs for selection
  const { data: recentRuns } = useQuery({
    queryKey: ['runs', 'list', { size: 20 }],
    queryFn: ({ signal }) => runsApi.list({ size: 20 }, signal),
  })

  // Fetch baseline run
  const { data: baselineRun, isLoading: baselineLoading } = useQuery({
    queryKey: ['runs', 'detail', selectedBaseline],
    queryFn: ({ signal }) => runsApi.get(selectedBaseline, signal),
    enabled: Boolean(selectedBaseline),
  })

  // Fetch comparison run
  const { data: compareRun, isLoading: compareLoading } = useQuery({
    queryKey: ['runs', 'detail', selectedCompare],
    queryFn: ({ signal }) => runsApi.get(selectedCompare, signal),
    enabled: Boolean(selectedCompare),
  })

  // Fetch packages for names
  const { data: packagesData } = useQuery({
    queryKey: ['packages', 'list', { size: 100 }],
    queryFn: ({ signal }) => packagesApi.list(0, 100, signal),
  })

  const packageNameMap = useMemo(() => {
    const map = new Map<string, string>()
    packagesData?.content.forEach((pkg) => {
      map.set(pkg.id, pkg.name)
    })
    return map
  }, [packagesData])

  // Compare scenarios
  const comparison = useMemo(() => {
    if (!baselineRun || !compareRun) return null

    const baselineMap = new Map<string, ScenarioResult>()
    baselineRun.scenarioResults.forEach((result) => {
      baselineMap.set(result.scenarioId, result)
    })

    const compareMap = new Map<string, ScenarioResult>()
    compareRun.scenarioResults.forEach((result) => {
      compareMap.set(result.scenarioId, result)
    })

    // Get all unique scenario IDs
    const allScenarioIds = new Set([...baselineMap.keys(), ...compareMap.keys()])

    const scenarios: ScenarioComparison[] = []
    let newFailures = 0
    let fixes = 0
    let unchanged = 0

    allScenarioIds.forEach((scenarioId) => {
      const baseline = baselineMap.get(scenarioId)
      const compare = compareMap.get(scenarioId)

      const baselinePassed = baseline?.status === 'PASSED'
      const comparePassed = compare?.status === 'PASSED'

      let changeType: ChangeType
      if (baselinePassed && !comparePassed && compare) {
        changeType = 'new_failure'
        newFailures++
      } else if (!baselinePassed && comparePassed && baseline) {
        changeType = 'fix'
        fixes++
      } else if (baselinePassed && comparePassed) {
        changeType = 'both_passed'
        unchanged++
      } else if (!baselinePassed && !comparePassed) {
        changeType = 'both_failed'
        unchanged++
      } else {
        changeType = 'unchanged'
        unchanged++
      }

      const durationChange = baseline && compare
        ? compare.duration - baseline.duration
        : null

      scenarios.push({
        scenarioId,
        scenarioName: compare?.scenarioName ?? baseline?.scenarioName ?? 'Unknown',
        baselineStatus: baseline?.status ?? null,
        compareStatus: compare?.status ?? null,
        changeType,
        baselineDuration: baseline?.duration ?? null,
        compareDuration: compare?.duration ?? null,
        durationChange,
      })
    })

    // Sort: new failures first, then fixes, then rest
    scenarios.sort((a, b) => {
      const order: Record<ChangeType, number> = {
        new_failure: 0,
        fix: 1,
        both_failed: 2,
        both_passed: 3,
        unchanged: 4,
      }
      return order[a.changeType] - order[b.changeType]
    })

    return {
      scenarios,
      summary: {
        newFailures,
        fixes,
        unchanged,
        total: scenarios.length,
      },
    }
  }, [baselineRun, compareRun])

  const isLoading = baselineLoading || compareLoading
  const hasSelection = selectedBaseline && selectedCompare
  const canCompare = hasSelection && selectedBaseline !== selectedCompare

  return (
    <div className="run-comparison-page">
      <header className="mb-8">
        <div className="flex items-center gap-2 text-secondary-400 text-sm mb-2">
          <Link to="/runs" className="hover:text-white transition-colors">
            Test Runs
          </Link>
          <span>/</span>
          <span className="text-white">Compare</span>
        </div>
        <h1 className="page-title">Compare Test Runs</h1>
        <p className="text-secondary-400">Select two runs to compare their results</p>
      </header>

      {/* Run Selection */}
      <div className="grid md:grid-cols-2 gap-6 mb-8">
        {/* Baseline Selection */}
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">
            <span className="text-primary-400">A</span> Baseline Run
          </h2>
          <select
            value={selectedBaseline}
            onChange={(e) => { setSelectedBaseline(e.target.value) }}
            className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
          >
            <option value="">Select a run...</option>
            {recentRuns?.content.map((run) => (
              <option key={run.id} value={run.id} disabled={run.id === selectedCompare}>
                Run #{run.id.slice(0, 8)} - {packageNameMap.get(run.packageId) ?? 'Unknown'} ({run.status})
              </option>
            ))}
          </select>
          {baselineRun && (
            <RunSummaryCard run={baselineRun} packageName={packageNameMap.get(baselineRun.packageId)} />
          )}
        </div>

        {/* Compare Selection */}
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">
            <span className="text-blue-400">B</span> Comparison Run
          </h2>
          <select
            value={selectedCompare}
            onChange={(e) => { setSelectedCompare(e.target.value) }}
            className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
          >
            <option value="">Select a run...</option>
            {recentRuns?.content.map((run) => (
              <option key={run.id} value={run.id} disabled={run.id === selectedBaseline}>
                Run #{run.id.slice(0, 8)} - {packageNameMap.get(run.packageId) ?? 'Unknown'} ({run.status})
              </option>
            ))}
          </select>
          {compareRun && (
            <RunSummaryCard run={compareRun} packageName={packageNameMap.get(compareRun.packageId)} />
          )}
        </div>
      </div>

      {/* Comparison Results */}
      {!canCompare ? (
        <EmptyState
          title="Select runs to compare"
          description="Choose a baseline run (A) and a comparison run (B) to see the differences"
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
      ) : isLoading ? (
        <ComparisonSkeleton />
      ) : comparison ? (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <SummaryCard
              title="New Failures"
              value={comparison.summary.newFailures}
              color="red"
              icon="↓"
            />
            <SummaryCard
              title="Fixes"
              value={comparison.summary.fixes}
              color="green"
              icon="↑"
            />
            <SummaryCard
              title="Unchanged"
              value={comparison.summary.unchanged}
              color="gray"
              icon="="
            />
            <SummaryCard
              title="Total Scenarios"
              value={comparison.summary.total}
              color="blue"
              icon="#"
            />
          </div>

          {/* Scenario Comparison Table */}
          <div className="card">
            <h2 className="text-lg font-semibold text-white mb-4">Scenario Comparison</h2>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-secondary-400 text-sm border-b border-secondary-700">
                    <th className="pb-3 pr-4">Scenario</th>
                    <th className="pb-3 px-4 text-center">Baseline (A)</th>
                    <th className="pb-3 px-4 text-center">Compare (B)</th>
                    <th className="pb-3 px-4 text-center">Change</th>
                    <th className="pb-3 pl-4 text-right">Duration Δ</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-secondary-700">
                  {comparison.scenarios.map((scenario) => (
                    <tr key={scenario.scenarioId} className="hover:bg-secondary-800/50">
                      <td className="py-3 pr-4">
                        <span className="font-medium text-white">{scenario.scenarioName}</span>
                      </td>
                      <td className="py-3 px-4 text-center">
                        {scenario.baselineStatus ? (
                          <StatusBadge status={scenario.baselineStatus} />
                        ) : (
                          <span className="text-secondary-500">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {scenario.compareStatus ? (
                          <StatusBadge status={scenario.compareStatus} />
                        ) : (
                          <span className="text-secondary-500">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <ChangeIndicator type={scenario.changeType} />
                      </td>
                      <td className="py-3 pl-4 text-right">
                        <DurationDelta delta={scenario.durationChange} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}
    </div>
  )
}

function RunSummaryCard({ run, packageName }: { run: TestRun; packageName: string | undefined }) {
  return (
    <div className="mt-4 p-3 bg-secondary-800/50 rounded-lg">
      <div className="flex items-center gap-2 mb-2">
        <StatusBadge status={run.status} />
        <span className="text-sm text-secondary-400">{packageName ?? 'Unknown Package'}</span>
      </div>
      <div className="grid grid-cols-3 gap-2 text-sm">
        <div>
          <span className="text-secondary-500">Total:</span>{' '}
          <span className="text-white">{run.summary.totalScenarios}</span>
        </div>
        <div>
          <span className="text-secondary-500">Passed:</span>{' '}
          <span className="text-green-400">{run.summary.passedScenarios}</span>
        </div>
        <div>
          <span className="text-secondary-500">Failed:</span>{' '}
          <span className="text-red-400">{run.summary.failedScenarios}</span>
        </div>
      </div>
    </div>
  )
}

function SummaryCard({ title, value, color, icon }: {
  title: string
  value: number
  color: 'red' | 'green' | 'gray' | 'blue'
  icon: string
}) {
  const colors = {
    red: 'border-red-500/30 bg-red-500/5 text-red-400',
    green: 'border-green-500/30 bg-green-500/5 text-green-400',
    gray: 'border-secondary-500/30 bg-secondary-500/5 text-secondary-400',
    blue: 'border-blue-500/30 bg-blue-500/5 text-blue-400',
  }

  return (
    <div className={`card border ${colors[color]}`}>
      <div className="flex items-center gap-2 mb-1">
        <span className="text-lg">{icon}</span>
        <span className="text-sm text-secondary-400">{title}</span>
      </div>
      <p className="text-3xl font-bold">{value}</p>
    </div>
  )
}

function ChangeIndicator({ type }: { type: ChangeType }) {
  const indicators: Record<ChangeType, { label: string; color: string }> = {
    new_failure: { label: '↓ Regression', color: 'text-red-400 bg-red-500/10' },
    fix: { label: '↑ Fixed', color: 'text-green-400 bg-green-500/10' },
    both_passed: { label: '= Passing', color: 'text-secondary-400 bg-secondary-500/10' },
    both_failed: { label: '= Failing', color: 'text-yellow-400 bg-yellow-500/10' },
    unchanged: { label: '-', color: 'text-secondary-500' },
  }

  const { label, color } = indicators[type]

  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${color}`}>
      {label}
    </span>
  )
}

function DurationDelta({ delta }: { delta: number | null }) {
  if (delta === null) return <span className="text-secondary-500">-</span>

  const isSlower = delta > 0
  const isFaster = delta < 0
  const absDelta = Math.abs(delta)

  if (absDelta < 10) {
    return <span className="text-secondary-500">~{absDelta}ms</span>
  }

  return (
    <span className={isSlower ? 'text-red-400' : isFaster ? 'text-green-400' : 'text-secondary-400'}>
      {isSlower ? '+' : ''}{delta}ms
    </span>
  )
}

function ComparisonSkeleton() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-24" />
        ))}
      </div>
      <Skeleton className="h-64" />
    </div>
  )
}
