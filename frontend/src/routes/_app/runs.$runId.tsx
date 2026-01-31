import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import { runsApi } from '@/api'
import { StatusBadge, Skeleton, Collapsible, JsonViewer, CopyButton, ExportDropdown } from '@/components/ui'
import type { ExportFormat } from '@/components/ui'
import type { ScenarioResult, StepResult, TestRun } from '@/api/types'

export const Route = createFileRoute('/_app/runs/$runId')({
  component: RunDetailPage,
})

function RunDetailPage() {
  const { runId } = Route.useParams()

  // Fetch run data with polling for active runs
  const { data: run, isLoading, isError, error } = useQuery({
    queryKey: ['runs', 'detail', runId],
    queryFn: ({ signal }) => runsApi.get(runId, signal),
    enabled: Boolean(runId),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PENDING' || status === 'RUNNING' ? 3000 : false
    },
  })

  const queryClient = useQueryClient()

  const cancelRun = useMutation({
    mutationFn: (id: string) => runsApi.cancel(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['runs', 'detail', runId] })
    },
  })

  const retryRun = useMutation({
    mutationFn: (id: string) => runsApi.retry(id),
    onSuccess: (newRun: TestRun) => {
      void queryClient.invalidateQueries({ queryKey: ['runs'] })
      void queryClient.setQueryData(['runs', 'detail', newRun.id], newRun)
    },
  })

  const [exportError, setExportError] = useState<string | null>(null)

  const handleExport = useCallback(
    async (format: ExportFormat) => {
      setExportError(null)
      try {
        const blob = await runsApi.exportResults(runId, format)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `run-${runId.slice(0, 8)}-results.${format}`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (err) {
        setExportError(err instanceof Error ? err.message : 'Export failed')
        throw err
      }
    },
    [runId]
  )

  if (isLoading) {
    return <RunDetailSkeleton />
  }

  if (isError || !run) {
    return (
      <div className="error-state">
        <h2>Error loading run</h2>
        <p>{error?.message ?? 'Run not found'}</p>
        <Link to="/packages" className="btn btn-primary mt-4">
          Back to Packages
        </Link>
      </div>
    )
  }

  const isRunning = run.status === 'PENDING' || run.status === 'RUNNING'
  const canRetry = run.status === 'COMPLETED' || run.status === 'CANCELLED'
  const duration = run.completedAt && run.startedAt
    ? Math.round((new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime()) / 1000)
    : null

  return (
    <div className="run-detail-page">
      {/* Header */}
      <header className="mb-8">
        <div className="flex items-center gap-2 text-secondary-400 text-sm mb-2">
          <Link to="/packages" className="hover:text-white transition-colors">
            Packages
          </Link>
          <span>/</span>
          <Link
            to="/packages/$packageId"
            params={{ packageId: run.packageId }}
            className="hover:text-white transition-colors"
          >
            Package
          </Link>
          <span>/</span>
          <span className="text-white">Run #{runId.slice(0, 8)}</span>
        </div>

        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-white">Test Run #{runId.slice(0, 8)}</h1>
              <StatusBadge status={run.status} />
            </div>
          </div>

          <div className="flex gap-2">
            <ExportDropdown
              onExport={handleExport}
              disabled={isRunning}
            />
            {isRunning && (
              <button
                onClick={() => {
                  cancelRun.mutate(runId)
                }}
                disabled={cancelRun.isPending}
                className="btn btn-danger disabled:opacity-50"
              >
                {cancelRun.isPending ? 'Cancelling...' : 'Cancel Run'}
              </button>
            )}
            {canRetry && (
              <button
                onClick={() => {
                  retryRun.mutate(runId)
                }}
                disabled={retryRun.isPending}
                className="btn btn-primary disabled:opacity-50"
              >
                {retryRun.isPending ? 'Retrying...' : 'Retry Run'}
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Export Error */}
      {exportError && (
        <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg flex items-center justify-between">
          <p className="text-red-500 text-sm">{exportError}</p>
          <button
            onClick={() => {
              setExportError(null)
            }}
            className="text-red-500 hover:text-red-400 transition-colors"
            aria-label="Dismiss error"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      {/* Summary */}
      <section className="card mb-6">
        <h2 className="text-lg font-semibold text-white mb-4">Run Summary</h2>
        <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div>
            <dt className="text-sm text-secondary-500">Total Scenarios</dt>
            <dd className="text-2xl font-bold text-white">{run.summary.totalScenarios}</dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Passed</dt>
            <dd className="text-2xl font-bold text-green-500">{run.summary.passedScenarios}</dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Failed</dt>
            <dd className="text-2xl font-bold text-red-500">{run.summary.failedScenarios}</dd>
          </div>
          <div>
            <dt className="text-sm text-secondary-500">Duration</dt>
            <dd className="text-2xl font-bold text-white">
              {duration !== null ? `${String(duration)}s` : '-'}
            </dd>
          </div>
        </dl>

        <div className="mt-4 pt-4 border-t border-secondary-700 grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-secondary-500">Started:</span>{' '}
            <span className="text-white">
              {run.startedAt ? formatDate(run.startedAt) : 'Not started'}
            </span>
          </div>
          <div>
            <span className="text-secondary-500">Completed:</span>{' '}
            <span className="text-white">
              {run.completedAt ? formatDate(run.completedAt) : 'In progress...'}
            </span>
          </div>
        </div>
      </section>

      {/* Scenario Results */}
      <section>
        <h2 className="text-lg font-semibold text-white mb-4">Scenario Results</h2>
        {run.scenarioResults.length === 0 ? (
          <div className="text-center py-8 text-secondary-400">
            {isRunning ? 'Waiting for results...' : 'No scenario results'}
          </div>
        ) : (
          <div className="space-y-4">
            {run.scenarioResults.map((result) => (
              <ScenarioResultCard key={result.scenarioId} result={result} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

function ScenarioResultCard({ result }: { result: ScenarioResult }) {
  return (
    <div className="card">
      <Collapsible
        defaultOpen={result.status === 'FAILED'}
        title={
          <div className="flex items-center gap-3 flex-1">
            <StatusBadge status={result.status} />
            <span className="font-medium text-white">{result.scenarioName}</span>
            <span className="text-secondary-500 text-sm ml-auto mr-2">
              {result.duration}ms
            </span>
          </div>
        }
      >
        {result.error && (
          <div className="mt-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
            <p className="text-red-500 text-sm">{result.error}</p>
          </div>
        )}

        <div className="mt-4 space-y-3">
          {result.stepResults.map((step, index) => (
            <StepResultCard key={step.stepId} step={step} index={index} />
          ))}
        </div>
      </Collapsible>
    </div>
  )
}

function StepResultCard({ step, index }: { step: StepResult; index: number }) {
  const curlCommand = generateCurlCommand(step)

  return (
    <div className="border border-secondary-700 rounded-lg">
      <Collapsible
        defaultOpen={step.status === 'FAILED' || step.status === 'ERROR'}
        title={
          <div className="flex items-center gap-3 flex-1 p-3">
            <span className="text-secondary-500 text-sm">Step {index + 1}</span>
            <StatusBadge status={step.status} />
            <span className="text-sm text-secondary-400">
              HTTP {step.actualStatus}
            </span>
            <span className="text-secondary-500 text-sm ml-auto mr-2">
              {step.duration}ms
            </span>
          </div>
        }
        className="bg-secondary-800/50"
      >
        <div className="p-3 pt-0 space-y-4">
          {step.error && (
            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
              <p className="text-red-500 text-sm">{step.error}</p>
            </div>
          )}

          {/* Response */}
          {step.responseBody && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium text-secondary-400">Response Body</h4>
              </div>
              <JsonViewer data={tryParseJson(step.responseBody)} />
            </div>
          )}

          {/* Headers */}
          {Object.keys(step.responseHeaders).length > 0 && (
            <Collapsible
              title={<h4 className="text-sm font-medium text-secondary-400">Response Headers</h4>}
            >
              <div className="mt-2 space-y-1 text-sm">
                {Object.entries(step.responseHeaders).map(([key, value]) => (
                  <div key={key} className="flex gap-2">
                    <span className="text-secondary-500">{key}:</span>
                    <span className="text-white font-mono">{value}</span>
                  </div>
                ))}
              </div>
            </Collapsible>
          )}

          {/* Assertions */}
          {step.assertionResults.length > 0 && (
            <div>
              <h4 className="text-sm font-medium text-secondary-400 mb-2">Assertions</h4>
              <div className="space-y-2">
                {step.assertionResults.map((assertion, i) => (
                  <div
                    key={i}
                    className={`p-2 rounded text-sm ${
                      assertion.passed
                        ? 'bg-green-500/10 text-green-400'
                        : 'bg-red-500/10 text-red-400'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      {assertion.passed ? (
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      ) : (
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      )}
                      <span>{assertion.assertion.type}: {assertion.assertion.path}</span>
                    </div>
                    {assertion.message && (
                      <p className="mt-1 text-xs opacity-80">{assertion.message}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* cURL Command */}
          {(step.status === 'FAILED' || step.status === 'ERROR') && curlCommand && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium text-secondary-400">cURL Command</h4>
                <CopyButton text={curlCommand} />
              </div>
              <pre className="bg-secondary-900 p-3 rounded-lg overflow-x-auto text-sm font-mono text-secondary-300">
                {curlCommand}
              </pre>
            </div>
          )}
        </div>
      </Collapsible>
    </div>
  )
}

function RunDetailSkeleton() {
  return (
    <div className="run-detail-page">
      <div className="mb-8">
        <Skeleton className="h-4 w-48 mb-4" />
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-3">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-5 w-20 rounded-full" />
          </div>
          <Skeleton className="h-10 w-24" />
        </div>
      </div>

      <Skeleton className="h-40 mb-6" />

      <Skeleton className="h-6 w-40 mb-4" />
      <div className="space-y-4">
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
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
    second: '2-digit',
  })
}

function tryParseJson(str: string): unknown {
  try {
    return JSON.parse(str)
  } catch {
    return str
  }
}

function generateCurlCommand(_step: StepResult): string | null {
  // This would typically use the original request data
  // For now, return a placeholder
  return null
}
