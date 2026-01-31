import { useState, useCallback } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import {
  usePackage,
  usePackageScenarios,
  usePackageRuns,
  useStartRun,
  useGenerateScenarios,
  useUpdatePackage,
  useDeletePackage,
} from '@/hooks'
import { StatusBadge, Skeleton, OperationCoverageTable } from '@/components/ui'
import type { Scenario, TestRun, QaPackage, UpdateQaPackageRequest } from '@/api/types'

export const Route = createFileRoute('/_app/packages/$packageId')({
  component: PackageDetailPage,
})

type TabId = 'scenarios' | 'runs' | 'coverage' | 'settings'

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
          <TabButton
            id="settings"
            label="Settings"
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
        <RunsTab runs={runs?.content ?? []} isLoading={runsLoading} />
      )}
      {activeTab === 'coverage' && (
        <CoverageTab scenarios={scenarios?.content ?? []} isLoading={scenariosLoading} />
      )}
      {activeTab === 'settings' && (
        <SettingsTab pkg={pkg} packageId={packageId} />
      )}
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
}: {
  runs: TestRun[]
  isLoading: boolean
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
        <RunCard key={run.id} run={run} />
      ))}
    </div>
  )
}

function RunCard({ run }: { run: TestRun }) {
  const statusColors: Record<string, string> = {
    PENDING: 'bg-yellow-500/10 text-yellow-500',
    RUNNING: 'bg-blue-500/10 text-blue-500',
    COMPLETED: 'bg-green-500/10 text-green-500',
    CANCELLED: 'bg-secondary-500/10 text-secondary-500',
  }

  return (
    <Link
      to="/runs/$runId"
      params={{ runId: run.id }}
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

function CoverageTab({ scenarios, isLoading }: { scenarios: Scenario[]; isLoading: boolean }) {
  const [isExporting, setIsExporting] = useState(false)

  const handleExport = useCallback(() => {
    setIsExporting(true)
    try {
      // Generate coverage report as JSON
      const coverage = generateCoverageReport(scenarios)
      const blob = new Blob([JSON.stringify(coverage, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      const dateStr = new Date().toISOString().split('T')[0] ?? 'export'
      link.download = `coverage-report-${dateStr}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } finally {
      setIsExporting(false)
    }
  }, [scenarios])

  return (
    <OperationCoverageTable
      scenarios={scenarios}
      isLoading={isLoading}
      onExport={handleExport}
      isExporting={isExporting}
    />
  )
}

function generateCoverageReport(scenarios: Scenario[]) {
  const operationMap = new Map<string, {
    endpoint: string
    method: string
    status: string
    scenarioCount: number
    scenarios: Array<{ id: string; name: string; status: string }>
  }>()

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

  const operations = Array.from(operationMap.values())
  const covered = operations.filter((e) => e.status === 'covered').length
  const failing = operations.filter((e) => e.status === 'failing').length
  const untested = operations.filter((e) => e.status === 'untested').length
  const total = operations.length
  const percentage = total > 0 ? Math.round((covered / total) * 100) : 0

  return {
    generatedAt: new Date().toISOString(),
    summary: {
      total,
      covered,
      failing,
      untested,
      percentage,
    },
    operations,
  }
}

function SettingsTab({ pkg, packageId }: { pkg: QaPackage; packageId: string }) {
  const navigate = useNavigate()
  const updatePackage = useUpdatePackage()
  const deletePackage = useDeletePackage()

  const [formData, setFormData] = useState<UpdateQaPackageRequest>({
    name: pkg.name,
    description: pkg.description ?? '',
    openApiSpec: pkg.openApiSpec,
    baseUrl: pkg.baseUrl,
  })
  const [hasChanges, setHasChanges] = useState(false)
  const [deleteConfirmation, setDeleteConfirmation] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const handleInputChange = (field: keyof UpdateQaPackageRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    setHasChanges(true)
  }

  const handleSave = () => {
    updatePackage.mutate(
      { id: packageId, data: formData },
      {
        onSuccess: () => {
          setHasChanges(false)
        },
      }
    )
  }

  const handleCancel = () => {
    setFormData({
      name: pkg.name,
      description: pkg.description ?? '',
      openApiSpec: pkg.openApiSpec,
      baseUrl: pkg.baseUrl,
    })
    setHasChanges(false)
  }

  const handleDelete = () => {
    if (deleteConfirmation !== pkg.name) return

    deletePackage.mutate(packageId, {
      onSuccess: () => {
        void navigate({ to: '/packages' })
      },
    })
  }

  return (
    <div className="space-y-8">
      {/* General Information */}
      <section className="card">
        <h3 className="text-lg font-semibold text-white mb-4">General Information</h3>
        <div className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-secondary-400 mb-1">
              Package Name *
            </label>
            <input
              id="name"
              type="text"
              value={formData.name ?? ''}
              onChange={(e) => { handleInputChange('name', e.target.value) }}
              className="w-full px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="Enter package name"
            />
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-secondary-400 mb-1">
              Description
            </label>
            <textarea
              id="description"
              value={formData.description ?? ''}
              onChange={(e) => { handleInputChange('description', e.target.value) }}
              rows={3}
              className="w-full px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder="Describe what this package tests"
            />
          </div>
        </div>
      </section>

      {/* API Configuration */}
      <section className="card">
        <h3 className="text-lg font-semibold text-white mb-4">API Configuration</h3>
        <div className="space-y-4">
          <div>
            <label htmlFor="openApiSpec" className="block text-sm font-medium text-secondary-400 mb-1">
              OpenAPI Specification URL *
            </label>
            <input
              id="openApiSpec"
              type="url"
              value={formData.openApiSpec ?? ''}
              onChange={(e) => { handleInputChange('openApiSpec', e.target.value) }}
              className="w-full px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white font-mono text-sm placeholder-secondary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="https://api.example.com/openapi.yaml"
            />
            <p className="mt-1 text-xs text-secondary-500">
              URL to your OpenAPI/Swagger specification file
            </p>
          </div>

          <div>
            <label htmlFor="baseUrl" className="block text-sm font-medium text-secondary-400 mb-1">
              Base URL (System Under Test) *
            </label>
            <input
              id="baseUrl"
              type="url"
              value={formData.baseUrl ?? ''}
              onChange={(e) => { handleInputChange('baseUrl', e.target.value) }}
              className="w-full px-3 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white font-mono text-sm placeholder-secondary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="https://api.staging.example.com"
            />
            <p className="mt-1 text-xs text-secondary-500">
              The base URL where tests will be executed against
            </p>
          </div>
        </div>
      </section>

      {/* Save/Cancel Buttons */}
      {hasChanges && (
        <div className="flex justify-end gap-3">
          <button
            onClick={handleCancel}
            className="btn btn-ghost"
            disabled={updatePackage.isPending}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="btn btn-primary disabled:opacity-50"
            disabled={updatePackage.isPending || !formData.name?.trim()}
          >
            {updatePackage.isPending ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      )}

      {/* Success/Error Messages */}
      {updatePackage.isSuccess && !hasChanges && (
        <div className="p-3 bg-green-500/10 border border-green-500/30 rounded-lg">
          <p className="text-green-500 text-sm">Settings saved successfully.</p>
        </div>
      )}

      {updatePackage.isError && (
        <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
          <p className="text-red-500 text-sm">
            Failed to save settings. Please try again.
          </p>
        </div>
      )}

      {/* Danger Zone */}
      <section className="card border-red-500/30">
        <h3 className="text-lg font-semibold text-red-500 mb-4">Danger Zone</h3>
        <div className="p-4 bg-red-500/5 border border-red-500/20 rounded-lg">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h4 className="font-medium text-white">Delete Package</h4>
              <p className="text-sm text-secondary-400 mt-1">
                Permanently delete this package and all its scenarios and run history.
              </p>
            </div>
            <button
              onClick={() => { setShowDeleteConfirm(true) }}
              className="btn bg-red-500/10 text-red-500 border border-red-500/30 hover:bg-red-500/20 whitespace-nowrap"
            >
              Delete Package
            </button>
          </div>
        </div>
      </section>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-secondary-800 rounded-xl max-w-md w-full p-6 border border-secondary-700">
            <h3 className="text-lg font-semibold text-white mb-2">Delete Package</h3>
            <p className="text-secondary-400 text-sm mb-4">
              Are you sure you want to delete <span className="text-white font-medium">{pkg.name}</span>?
              This will permanently delete:
            </p>
            <ul className="text-secondary-400 text-sm mb-4 list-disc list-inside space-y-1">
              <li>All test scenarios</li>
              <li>All run history records</li>
              <li>All associated coverage data</li>
            </ul>
            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-lg mb-4">
              <p className="text-red-400 text-sm">This action cannot be undone.</p>
            </div>
            <div className="mb-4">
              <label htmlFor="deleteConfirm" className="block text-sm text-secondary-400 mb-1">
                Type <span className="text-white font-mono">{pkg.name}</span> to confirm:
              </label>
              <input
                id="deleteConfirm"
                type="text"
                value={deleteConfirmation}
                onChange={(e) => { setDeleteConfirmation(e.target.value) }}
                className="w-full px-3 py-2 bg-secondary-900 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                placeholder="Enter package name"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowDeleteConfirm(false)
                  setDeleteConfirmation('')
                }}
                className="btn btn-ghost"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={deleteConfirmation !== pkg.name || deletePackage.isPending}
                className="btn bg-red-500 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {deletePackage.isPending ? 'Deleting...' : 'Delete Package'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
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
