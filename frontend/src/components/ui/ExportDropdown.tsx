import { useState, useRef, useEffect, useCallback } from 'react'

export type ExportFormat = 'json' | 'csv'

export interface ExportDropdownProps {
  onExport: (format: ExportFormat) => Promise<void>
  isLoading?: boolean
  disabled?: boolean
  className?: string
}

export function ExportDropdown({
  onExport,
  isLoading = false,
  disabled = false,
  className = '',
}: ExportDropdownProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [loadingFormat, setLoadingFormat] = useState<ExportFormat | null>(null)
  const dropdownRef = useRef<HTMLDivElement>(null)

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  // Close dropdown on escape
  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleEscape)
    }

    return () => {
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen])

  const handleExport = useCallback(
    (format: ExportFormat) => {
      setLoadingFormat(format)
      setIsOpen(false)

      onExport(format)
        .catch(() => {
          // Error handled by parent
        })
        .finally(() => {
          setLoadingFormat(null)
        })
    },
    [onExport]
  )

  const handleJsonExport = useCallback(() => {
    handleExport('json')
  }, [handleExport])

  const handleCsvExport = useCallback(() => {
    handleExport('csv')
  }, [handleExport])

  const isExporting = isLoading || loadingFormat !== null
  const isDisabled = disabled || isExporting

  const toggleDropdown = useCallback(() => {
    setIsOpen((prev) => !prev)
  }, [])

  return (
    <div ref={dropdownRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={toggleDropdown}
        disabled={isDisabled}
        className="btn btn-secondary flex items-center gap-2 disabled:opacity-50"
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label="Export options"
      >
        {isExporting ? (
          <>
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
            <span>Exporting...</span>
          </>
        ) : (
          <>
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
            <span>Export</span>
            <svg
              className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </>
        )}
      </button>

      {isOpen && (
        <div
          className="absolute right-0 mt-2 w-48 bg-secondary-800 border border-secondary-700 rounded-lg shadow-lg z-50"
          role="menu"
          aria-orientation="vertical"
        >
          <button
            type="button"
            onClick={handleJsonExport}
            disabled={loadingFormat !== null}
            className="w-full px-4 py-3 text-left text-white hover:bg-secondary-700 transition-colors rounded-t-lg flex items-center gap-3 disabled:opacity-50"
            role="menuitem"
          >
            <svg className="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
              />
            </svg>
            <div>
              <div className="font-medium">Export as JSON</div>
              <div className="text-xs text-secondary-400">Full structured data</div>
            </div>
          </button>
          <button
            type="button"
            onClick={handleCsvExport}
            disabled={loadingFormat !== null}
            className="w-full px-4 py-3 text-left text-white hover:bg-secondary-700 transition-colors rounded-b-lg flex items-center gap-3 disabled:opacity-50 border-t border-secondary-700"
            role="menuitem"
          >
            <svg className="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <div>
              <div className="font-medium">Export as CSV</div>
              <div className="text-xs text-secondary-400">Spreadsheet format</div>
            </div>
          </button>
        </div>
      )}
    </div>
  )
}
