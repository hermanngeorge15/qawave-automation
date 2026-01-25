import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { usePackage, usePackageScenarios, usePackageRuns, useStartRun, useGenerateScenarios } from '@/hooks'
import { StatusBadge, Skeleton } from '@/components/ui'
import type { Scenario, TestRun } from '@/api/types'

export const Route = createFileRoute('/_app/packages/$packageId')({
  component: PackageDetailPage,
})

function PackageDetailPage() {
  const { packageId } = Route.useParams()
  const [activeTab, setActiveTab] = useState<'scenarios' | 'runs'>('scenarios')

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
          <button
            onClick={() => setActiveTab('scenarios')}
            className={`pb-3 px-1 border-b-2 transition-colors ${
              activeTab === 'scenarios'
                ? 'border-primary-500 text-white'
                : 'border-transparent text-secondary-400 hover:text-white'
            }`}
          >
            Scenarios ({scenarios?.totalElements ?? 0})
          </button>
          <button
            onClick={() => setActiveTab('runs')}
            className={`pb-3 px-1 border-b-2 transition-colors ${
              activeTab === 'runs'
                ? 'border-primary-500 text-white'
                : 'border-transparent text-secondary-400 hover:text-white'
            }`}
          >
            Test Runs ({runs?.totalElements ?? 0})
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'scenarios' ? (
        <ScenariosTab scenarios={scenarios?.content ?? []} isLoading={scenariosLoading} />
      ) : (
        <RunsTab runs={runs?.content ?? []} isLoading={runsLoading} packageId={packageId} />
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
              className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[run.status] ?? statusColors.PENDING}`}
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
