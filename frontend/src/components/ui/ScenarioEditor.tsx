import { useState, useCallback, useRef, useEffect } from 'react'
import Editor, { type OnMount } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'
import type { TestStep } from '@/api/types'

export interface ValidationError {
  line: number
  column: number
  message: string
  severity: 'error' | 'warning'
}

export interface ScenarioEditorProps {
  initialValue: TestStep[]
  onSave: (steps: TestStep[]) => Promise<void>
  onValidate?: (json: string) => Promise<ValidationError[]>
  isLoading?: boolean
  className?: string
}

export function ScenarioEditor({
  initialValue,
  onSave,
  onValidate,
  isLoading = false,
  className = '',
}: ScenarioEditorProps) {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  // Monaco instance stored as unknown to avoid type issues with the library
  const monacoRef = useRef<unknown>(null)
  const [value, setValue] = useState(() => JSON.stringify(initialValue, null, 2))
  const [parseError, setParseError] = useState<string | null>(null)
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([])
  const [isDirty, setIsDirty] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isValidating, setIsValidating] = useState(false)
  const [showPreview, setShowPreview] = useState(true)
  const [parsedValue, setParsedValue] = useState<TestStep[] | null>(initialValue)

  // Parse JSON and update preview
  const parseJson = useCallback((jsonString: string): TestStep[] | null => {
    try {
      const parsed = JSON.parse(jsonString) as TestStep[]
      if (!Array.isArray(parsed)) {
        setParseError('Root must be an array of test steps')
        return null
      }
      setParseError(null)
      return parsed
    } catch (e) {
      if (e instanceof SyntaxError) {
        setParseError(e.message)
      } else {
        setParseError('Invalid JSON')
      }
      return null
    }
  }, [])

  // Handle editor changes
  const handleChange = useCallback(
    (newValue: string | undefined) => {
      if (newValue === undefined) return
      setValue(newValue)
      setIsDirty(newValue !== JSON.stringify(initialValue, null, 2))
      const parsed = parseJson(newValue)
      setParsedValue(parsed)
    },
    [initialValue, parseJson]
  )

  // Handle editor mount
  const handleEditorDidMount: OnMount = useCallback((editorInstance, monaco) => {
    editorRef.current = editorInstance
    monacoRef.current = monaco

    // Configure JSON diagnostics (monaco types are unresolved at compile time)
    /* eslint-disable @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-member-access */
    monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
      validate: true,
      allowComments: false,
      schemaValidation: 'error',
    })
    /* eslint-enable @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-member-access */
  }, [])

  // Update markers for validation errors
  useEffect(() => {
    const currentEditor = editorRef.current
    const currentMonaco = monacoRef.current as {
      MarkerSeverity: { Error: number; Warning: number }
      editor: { setModelMarkers: (model: editor.ITextModel, owner: string, markers: editor.IMarkerData[]) => void }
    } | null
    if (!currentEditor || !currentMonaco) return

    const model = currentEditor.getModel()
    if (!model) return

    const markers: editor.IMarkerData[] = validationErrors.map((error) => ({
      severity:
        error.severity === 'error'
          ? currentMonaco.MarkerSeverity.Error
          : currentMonaco.MarkerSeverity.Warning,
      startLineNumber: error.line,
      startColumn: error.column,
      endLineNumber: error.line,
      endColumn: error.column + 1,
      message: error.message,
    }))

    currentMonaco.editor.setModelMarkers(model, 'scenario-editor', markers)
  }, [validationErrors])

  // Handle format
  const handleFormat = useCallback(() => {
    const currentEditor = editorRef.current
    if (currentEditor) {
      void currentEditor.getAction('editor.action.formatDocument')?.run()
    }
  }, [])

  // Handle undo
  const handleUndo = useCallback(() => {
    if (editorRef.current) {
      editorRef.current.trigger('keyboard', 'undo', null)
    }
  }, [])

  // Handle redo
  const handleRedo = useCallback(() => {
    if (editorRef.current) {
      editorRef.current.trigger('keyboard', 'redo', null)
    }
  }, [])

  // Handle validate
  const handleValidate = useCallback(() => {
    if (!onValidate) return

    setIsValidating(true)
    onValidate(value)
      .then((errors) => {
        setValidationErrors(errors)
      })
      .catch(() => {
        setValidationErrors([
          {
            line: 1,
            column: 1,
            message: 'Validation request failed',
            severity: 'error',
          },
        ])
      })
      .finally(() => {
        setIsValidating(false)
      })
  }, [onValidate, value])

  // Handle save
  const handleSave = useCallback(() => {
    if (parseError || !parsedValue) return

    setIsSaving(true)
    onSave(parsedValue)
      .then(() => {
        setIsDirty(false)
        setValidationErrors([])
      })
      .catch(() => {
        // Error handled by parent
      })
      .finally(() => {
        setIsSaving(false)
      })
  }, [parseError, parsedValue, onSave])

  // Reset to initial value
  const handleReset = useCallback(() => {
    const initialJson = JSON.stringify(initialValue, null, 2)
    setValue(initialJson)
    setParsedValue(initialValue)
    setIsDirty(false)
    setParseError(null)
    setValidationErrors([])
  }, [initialValue])

  // Toggle preview handler
  const handleTogglePreview = useCallback(() => {
    setShowPreview((prev) => !prev)
  }, [])

  const hasErrors = parseError !== null || validationErrors.some((e) => e.severity === 'error')

  // Helper function to get method class
  const getMethodClass = (method: string): string => {
    switch (method) {
      case 'GET':
        return 'bg-green-500/20 text-green-400'
      case 'POST':
        return 'bg-blue-500/20 text-blue-400'
      case 'PUT':
        return 'bg-yellow-500/20 text-yellow-400'
      case 'PATCH':
        return 'bg-orange-500/20 text-orange-400'
      case 'DELETE':
        return 'bg-red-500/20 text-red-400'
      default:
        return 'bg-secondary-500/20 text-secondary-400'
    }
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Toolbar */}
      <div className="flex items-center justify-between gap-2 px-4 py-3 border-b border-secondary-700 bg-secondary-800/50">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleUndo}
            className="p-2 text-secondary-400 hover:text-white hover:bg-secondary-700 rounded-lg transition-colors"
            title="Undo (Ctrl+Z)"
            aria-label="Undo"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"
              />
            </svg>
          </button>
          <button
            type="button"
            onClick={handleRedo}
            className="p-2 text-secondary-400 hover:text-white hover:bg-secondary-700 rounded-lg transition-colors"
            title="Redo (Ctrl+Y)"
            aria-label="Redo"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 10h-10a8 8 0 00-8 8v2M21 10l-6 6m6-6l-6-6"
              />
            </svg>
          </button>
          <div className="w-px h-5 bg-secondary-600" />
          <button
            type="button"
            onClick={handleFormat}
            className="p-2 text-secondary-400 hover:text-white hover:bg-secondary-700 rounded-lg transition-colors"
            title="Format JSON"
            aria-label="Format JSON"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16m-7 6h7"
              />
            </svg>
          </button>
          <button
            type="button"
            onClick={handleTogglePreview}
            className={`p-2 rounded-lg transition-colors ${
              showPreview
                ? 'text-primary-400 bg-primary-500/20'
                : 'text-secondary-400 hover:text-white hover:bg-secondary-700'
            }`}
            title="Toggle Preview"
            aria-label="Toggle Preview"
            aria-pressed={showPreview}
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
              />
            </svg>
          </button>
        </div>

        <div className="flex items-center gap-2">
          {isDirty && (
            <span className="text-xs text-yellow-400 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-yellow-400" />
              Unsaved changes
            </span>
          )}
          <button
            type="button"
            onClick={handleReset}
            disabled={!isDirty || isLoading}
            className="px-3 py-1.5 text-sm text-secondary-300 hover:text-white hover:bg-secondary-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Reset
          </button>
          {onValidate && (
            <button
              type="button"
              onClick={handleValidate}
              disabled={isValidating || !!parseError || isLoading}
              className="px-3 py-1.5 text-sm bg-secondary-700 hover:bg-secondary-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {isValidating && (
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
              Validate
            </button>
          )}
          <button
            type="button"
            onClick={handleSave}
            disabled={hasErrors || !isDirty || isSaving || isLoading}
            className="px-4 py-1.5 text-sm bg-primary-600 hover:bg-primary-500 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {isSaving && (
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
            Save
          </button>
        </div>
      </div>

      {/* Error banner */}
      {(parseError || validationErrors.length > 0) && (
        <div className="px-4 py-2 bg-red-500/10 border-b border-red-500/20">
          {parseError && (
            <p className="text-sm text-red-400 flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              JSON Parse Error: {parseError}
            </p>
          )}
          {validationErrors.map((error, index) => (
            <p
              key={index}
              className={`text-sm flex items-center gap-2 ${
                error.severity === 'error' ? 'text-red-400' : 'text-yellow-400'
              }`}
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d={
                    error.severity === 'error'
                      ? 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
                      : 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z'
                  }
                />
              </svg>
              Line {error.line}: {error.message}
            </p>
          ))}
        </div>
      )}

      {/* Editor and Preview */}
      <div className="flex-1 flex min-h-0">
        {/* Monaco Editor */}
        <div className={`flex-1 min-w-0 ${showPreview ? 'w-1/2' : 'w-full'}`}>
          <Editor
            height="100%"
            language="json"
            theme="vs-dark"
            value={value}
            onChange={handleChange}
            onMount={handleEditorDidMount}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              lineNumbers: 'on',
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: 2,
              wordWrap: 'on',
              folding: true,
              renderWhitespace: 'selection',
              bracketPairColorization: { enabled: true },
            }}
            loading={
              <div className="flex items-center justify-center h-full bg-secondary-900">
                <div className="flex items-center gap-2 text-secondary-400">
                  <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
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
                  Loading editor...
                </div>
              </div>
            }
          />
        </div>

        {/* Preview Panel */}
        {showPreview && (
          <>
            <div className="w-px bg-secondary-700" />
            <div className="w-1/2 overflow-auto bg-secondary-900 p-4">
              <h3 className="text-sm font-medium text-secondary-400 mb-3">Preview</h3>
              {parsedValue ? (
                <div className="space-y-3">
                  <p className="text-sm text-secondary-300">
                    {parsedValue.length} step{parsedValue.length !== 1 ? 's' : ''}
                  </p>
                  {parsedValue.map((step, index) => (
                    <div
                      key={step.id || index}
                      className="p-3 bg-secondary-800 rounded-lg border border-secondary-700"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-xs font-medium text-secondary-500">
                          #{step.order || index + 1}
                        </span>
                        <span className={`px-2 py-0.5 text-xs font-medium rounded ${getMethodClass(step.method)}`}>
                          {step.method}
                        </span>
                        <code className="text-sm text-secondary-300 truncate flex-1">
                          {step.endpoint}
                        </code>
                      </div>
                      <div className="flex items-center gap-4 text-xs text-secondary-500">
                        <span>Status: {step.expectedStatus}</span>
                        <span>{step.assertions.length} assertions</span>
                        <span>{step.extractors.length} extractors</span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex items-center justify-center h-32 text-secondary-500">
                  <p>Fix JSON errors to see preview</p>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
