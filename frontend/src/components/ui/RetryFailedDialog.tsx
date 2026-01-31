import { useState, useCallback } from 'react'
import { Modal } from './Modal'
import type { ScenarioResult } from '@/api/types'

export interface RetryFailedDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: (scenarioIds: string[]) => Promise<void>
  failedScenarios: ScenarioResult[]
  isLoading?: boolean
}

export function RetryFailedDialog({
  isOpen,
  onClose,
  onConfirm,
  failedScenarios,
  isLoading = false,
}: RetryFailedDialogProps) {
  const [selectedScenarios, setSelectedScenarios] = useState<Set<string>>(
    () => new Set(failedScenarios.map((s) => s.scenarioId))
  )
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleToggleScenario = useCallback((scenarioId: string) => {
    setSelectedScenarios((prev) => {
      const next = new Set(prev)
      if (next.has(scenarioId)) {
        next.delete(scenarioId)
      } else {
        next.add(scenarioId)
      }
      return next
    })
  }, [])

  const handleSelectAll = useCallback(() => {
    setSelectedScenarios(new Set(failedScenarios.map((s) => s.scenarioId)))
  }, [failedScenarios])

  const handleDeselectAll = useCallback(() => {
    setSelectedScenarios(new Set())
  }, [])

  const handleConfirm = useCallback(() => {
    if (selectedScenarios.size === 0) return

    setIsSubmitting(true)
    onConfirm(Array.from(selectedScenarios))
      .catch(() => {
        // Error handled by parent
      })
      .finally(() => {
        setIsSubmitting(false)
      })
  }, [selectedScenarios, onConfirm])

  const isDisabled = isLoading || isSubmitting || selectedScenarios.size === 0

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Retry Failed Scenarios" size="lg">
      <div className="space-y-4">
        <p className="text-secondary-400 text-sm">
          Select which failed scenarios you want to retry. A new run will be created with only the selected scenarios.
        </p>

        {/* Selection controls */}
        <div className="flex items-center justify-between">
          <span className="text-sm text-secondary-400">
            {selectedScenarios.size} of {failedScenarios.length} selected
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleSelectAll}
              className="text-sm text-primary-400 hover:text-primary-300 transition-colors"
            >
              Select All
            </button>
            <span className="text-secondary-600">|</span>
            <button
              type="button"
              onClick={handleDeselectAll}
              className="text-sm text-primary-400 hover:text-primary-300 transition-colors"
            >
              Deselect All
            </button>
          </div>
        </div>

        {/* Scenario list */}
        <div className="max-h-64 overflow-y-auto border border-secondary-700 rounded-lg divide-y divide-secondary-700">
          {failedScenarios.map((scenario) => (
            <label
              key={scenario.scenarioId}
              className="flex items-center gap-3 p-3 hover:bg-secondary-800/50 cursor-pointer transition-colors"
            >
              <input
                type="checkbox"
                checked={selectedScenarios.has(scenario.scenarioId)}
                onChange={() => {
                  handleToggleScenario(scenario.scenarioId)
                }}
                className="w-4 h-4 rounded border-secondary-600 bg-secondary-800 text-primary-500 focus:ring-primary-500 focus:ring-offset-0"
              />
              <div className="flex-1 min-w-0">
                <p className="text-white font-medium truncate">{scenario.scenarioName}</p>
                {scenario.error && (
                  <p className="text-red-400 text-xs truncate mt-0.5">{scenario.error}</p>
                )}
              </div>
              <span className="text-secondary-500 text-sm">{scenario.duration}ms</span>
            </label>
          ))}
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="btn btn-secondary disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isDisabled}
            className="btn btn-primary disabled:opacity-50 flex items-center gap-2"
          >
            {isSubmitting && (
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
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
            )}
            Retry {selectedScenarios.size} Scenario{selectedScenarios.size !== 1 ? 's' : ''}
          </button>
        </div>
      </div>
    </Modal>
  )
}
