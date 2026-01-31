import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useScenario, useRunScenario } from '@/hooks'
import { useQuery } from '@tanstack/react-query'
import { packagesApi, runsApi } from '@/api'
import { StatusBadge, Skeleton, Collapsible, JsonViewer, CopyButton, EmptyState } from '@/components/ui'
import type { Scenario, TestStep, HttpMethod } from '@/api/types'

export const Route = createFileRoute('/_app/scenarios/$scenarioId')({
  component: ScenarioDetailPage,
})

type TabId = 'steps' | 'history' | 'json'

const HTTP_METHOD_COLORS: Record<HttpMethod, string> = {
  GET: 'bg-green-500/10 text-green-400 border-green-500/30',
  POST: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
  PUT: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
  PATCH: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  DELETE: 'bg-red-500/10 text-red-400 border-red-500/30',
}

function ScenarioDetailPage() {
  const { scenarioId } = Route.useParams()
  const [activeTab, setActiveTab] = useState<TabId>('steps')

  const { data: scenario, isLoading, isError, error } = useScenario(scenarioId)

  const { data: packageData } = useQuery({
    queryKey: ['packages', 'detail', scenario?.packageId],
    queryFn: ({ signal }) => packagesApi.get(scenario?.packageId ?? '', signal),
    enabled: Boolean(scenario?.packageId),
  })

  const runScenario = useRunScenario()

  const handleRunScenario = () => {
    runScenario.mutate(scenarioId)
  }

  if (isLoading) {
    return <ScenarioDetailSkeleton />
  }

  if (isError || !scenario) {
    return (
      <div className="error-state">
        <h2>Error loading scenario</h2>
        <p>{error?.message ?? 'Scenario not found'}</p>
        <Link to="/scenarios" className="btn btn-primary mt-4">
          Back to Scenarios
        </Link>
      </div>
    )
  }

  return (
    <div className="scenario-detail-page">
      {/* Header */}
      <header className="mb-8">
        <div className="flex items-center gap-2 text-secondary-400 text-sm mb-2">
          <Link to="/scenarios" className="hover:text-white transition-colors">
            Scenarios
          </Link>
          <span>/</span>
          <span className="text-white">{scenario.name}</span>
        </div>

        <div className="card">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-bold text-white">{scenario.name}</h1>
                <StatusBadge status={scenario.status} />
              </div>
              {scenario.description && (
                <p className="text-secondary-400 mt-2">{scenario.description}</p>
              )}
              <div className="flex items-center gap-4 mt-3 text-sm text-secondary-500">
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                    />
                  </svg>
                  {packageData?.name ?? 'Unknown Package'}
                </span>
                <span>{scenario.steps.length} steps</span>
                <span>Created {formatRelativeTime(scenario.createdAt)}</span>
              </div>
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleRunScenario}
                disabled={runScenario.isPending}
                className="btn btn-primary disabled:opacity-50"
              >
                {runScenario.isPending ? 'Running...' : 'Run Test'}
              </button>
              <button className="btn btn-ghost" disabled>
                Edit
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Tabs */}
      <div className="border-b border-secondary-700 mb-6">
        <nav className="flex gap-6">
          <TabButton id="steps" label="Steps" activeTab={activeTab} onClick={setActiveTab} />
          <TabButton id="history" label="Run History" activeTab={activeTab} onClick={setActiveTab} />
          <TabButton id="json" label="JSON" activeTab={activeTab} onClick={setActiveTab} />
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'steps' && <StepsTab scenario={scenario} />}
      {activeTab === 'history' && <RunHistoryTab scenarioId={scenarioId} />}
      {activeTab === 'json' && <JsonTab scenario={scenario} />}
    </div>
  )
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
      className={`pb-3 text-sm font-medium transition-colors ${
        isActive
          ? 'text-primary-400 border-b-2 border-primary-400'
          : 'text-secondary-400 hover:text-white'
      }`}
    >
      {label}
    </button>
  )
}

function StepsTab({ scenario }: { scenario: Scenario }) {
  if (scenario.steps.length === 0) {
    return (
      <EmptyState
        title="No steps defined"
        description="This scenario doesn't have any test steps yet"
        icon={
          <svg className="w-full h-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
        }
      />
    )
  }

  return (
    <div className="space-y-4">
      {scenario.steps.map((step, index) => (
        <StepCard key={step.id} step={step} index={index} isLast={index === scenario.steps.length - 1} />
      ))}
    </div>
  )
}

function StepCard({ step, index, isLast }: { step: TestStep; index: number; isLast: boolean }) {
  return (
    <div className="relative">
      {/* Connection line */}
      {!isLast && (
        <div className="absolute left-6 top-full h-4 w-0.5 bg-secondary-700" />
      )}

      <div className="card">
        <Collapsible
          defaultOpen={index === 0}
          title={
            <div className="flex items-center gap-3 flex-1">
              <span className="w-8 h-8 rounded-full bg-secondary-700 flex items-center justify-center text-sm font-medium text-white">
                {index + 1}
              </span>
              <span className={`px-2 py-1 rounded text-xs font-mono border ${HTTP_METHOD_COLORS[step.method]}`}>
                {step.method}
              </span>
              <span className="font-mono text-white">{step.endpoint}</span>
              <span className="text-secondary-500 text-sm ml-auto mr-2">
                Expected: {step.expectedStatus}
              </span>
            </div>
          }
        >
          <div className="mt-4 space-y-4 pl-11">
            {/* Headers */}
            {Object.keys(step.headers).length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-secondary-400 mb-2">Headers</h4>
                <div className="bg-secondary-800/50 rounded-lg p-3 space-y-1 text-sm">
                  {Object.entries(step.headers).map(([key, value]) => (
                    <div key={key} className="flex gap-2">
                      <span className="text-secondary-500">{key}:</span>
                      <span className="text-white font-mono">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Request Body */}
            {step.body && (
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-medium text-secondary-400">Request Body</h4>
                  <CopyButton text={step.body} />
                </div>
                <JsonViewer data={tryParseJson(step.body)} />
              </div>
            )}

            {/* Assertions */}
            {step.assertions.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-secondary-400 mb-2">
                  Assertions ({step.assertions.length})
                </h4>
                <div className="space-y-2">
                  {step.assertions.map((assertion, i) => (
                    <div
                      key={i}
                      className="bg-secondary-800/50 rounded-lg p-2 text-sm flex items-center gap-2"
                    >
                      <span className="text-primary-400">{assertion.type}</span>
                      <span className="text-secondary-400">{assertion.path}</span>
                      <span className="text-secondary-500">{assertion.operator}</span>
                      <span className="text-white font-mono">{JSON.stringify(assertion.expected)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Extractors */}
            {step.extractors.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-secondary-400 mb-2">
                  Extractions ({step.extractors.length})
                </h4>
                <div className="space-y-2">
                  {step.extractors.map((extractor, i) => (
                    <div
                      key={i}
                      className="bg-secondary-800/50 rounded-lg p-2 text-sm flex items-center gap-2"
                    >
                      <span className="text-yellow-400">{extractor.name}</span>
                      <span className="text-secondary-500">=</span>
                      <span className="text-secondary-400">{extractor.type}</span>
                      <span className="text-white font-mono">{extractor.path}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Timeout */}
            <div className="text-sm text-secondary-500">
              Timeout: {step.timeoutMs}ms
            </div>
          </div>
        </Collapsible>
      </div>
    </div>
  )
}

function RunHistoryTab({ scenarioId }: { scenarioId: string }) {
  // Fetch runs that include this scenario
  const { data: runsData, isLoading } = useQuery({
    queryKey: ['runs', 'list', { size: 20 }],
    queryFn: ({ signal }) => runsApi.list({ size: 20 }, signal),
  })

  // Filter runs that have results for this scenario
  const relevantRuns = runsData?.content.filter((run) =>
    run.scenarioResults.some((result) => result.scenarioId === scenarioId)
  ) ?? []

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
      </div>
    )
  }

  if (relevantRuns.length === 0) {
    return (
      <EmptyState
        title="No run history"
        description="This scenario hasn't been run yet"
        icon={
          <svg className="w-full h-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        }
      />
    )
  }

  return (
    <div className="space-y-4">
      {relevantRuns.map((run) => {
        const scenarioResult = run.scenarioResults.find((r) => r.scenarioId === scenarioId)
        if (!scenarioResult) return null

        return (
          <Link
            key={run.id}
            to="/runs/$runId"
            params={{ runId: run.id }}
            className="card hover:border-primary-500 transition-colors block"
          >
            <div className="flex items-center gap-4">
              <StatusBadge status={scenarioResult.status} />
              <div className="flex-1">
                <span className="font-medium text-white">Run #{run.id.slice(0, 8)}</span>
                <div className="text-sm text-secondary-400">
                  {run.startedAt ? formatDate(run.startedAt) : 'Not started'}
                </div>
              </div>
              <div className="text-sm text-secondary-400">
                {scenarioResult.duration}ms
              </div>
              <span className="text-secondary-400">→</span>
            </div>
            {scenarioResult.error && (
              <div className="mt-3 p-2 bg-red-500/10 border border-red-500/30 rounded text-sm text-red-400">
                {scenarioResult.error}
              </div>
            )}
          </Link>
        )
      })}
    </div>
  )
}

function JsonTab({ scenario }: { scenario: Scenario }) {
  const jsonString = JSON.stringify(scenario, null, 2)

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-white">Scenario JSON</h3>
        <CopyButton text={jsonString} />
      </div>
      <JsonViewer data={scenario} />
    </div>
  )
}

function ScenarioDetailSkeleton() {
  return (
    <div className="scenario-detail-page">
      <div className="mb-8">
        <Skeleton className="h-4 w-48 mb-4" />
        <div className="card">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-3">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-5 w-20 rounded-full" />
            </div>
            <Skeleton className="h-10 w-24" />
          </div>
          <Skeleton className="h-4 w-64 mt-4" />
        </div>
      </div>

      <Skeleton className="h-10 w-64 mb-6" />

      <div className="space-y-4">
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
      </div>
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

function tryParseJson(str: string): unknown {
  try {
    return JSON.parse(str)
  } catch {
    return str
  }
}
