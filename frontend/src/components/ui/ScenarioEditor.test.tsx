import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ScenarioEditor, type ValidationError } from './ScenarioEditor'
import type { TestStep } from '@/api/types'

// Mock Monaco Editor
vi.mock('@monaco-editor/react', () => ({
  default: ({
    value,
    onChange,
    loading,
  }: {
    value: string
    onChange: (value: string | undefined) => void
    loading: React.ReactNode
  }) => (
    <div data-testid="monaco-editor">
      <textarea
        data-testid="editor-textarea"
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
        }}
        aria-label="JSON Editor"
      />
      {loading && <div data-testid="editor-loading">{loading}</div>}
    </div>
  ),
}))

const mockSteps: TestStep[] = [
  {
    id: 'step-1',
    order: 1,
    method: 'GET',
    endpoint: '/api/users',
    headers: {},
    body: null,
    expectedStatus: 200,
    assertions: [],
    extractors: [],
    timeoutMs: 5000,
  },
  {
    id: 'step-2',
    order: 2,
    method: 'POST',
    endpoint: '/api/users/create',
    headers: { 'Content-Type': 'application/json' },
    body: '{"name": "Test"}',
    expectedStatus: 201,
    assertions: [
      {
        type: 'JSON_PATH',
        path: '$.id',
        expected: null,
        operator: 'NOT_EQUALS',
      },
    ],
    extractors: [
      {
        name: 'userId',
        type: 'JSON_PATH',
        path: '$.id',
      },
    ],
    timeoutMs: 5000,
  },
]

describe('ScenarioEditor', () => {
  const mockOnSave = vi.fn()
  const mockOnValidate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockOnSave.mockResolvedValue(undefined)
    mockOnValidate.mockResolvedValue([])
  })

  it('renders the editor with initial value', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument()
    expect(screen.getByTestId('editor-textarea')).toHaveValue(JSON.stringify(mockSteps, null, 2))
  })

  it('renders toolbar buttons', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByLabelText('Undo')).toBeInTheDocument()
    expect(screen.getByLabelText('Redo')).toBeInTheDocument()
    expect(screen.getByLabelText('Format JSON')).toBeInTheDocument()
    expect(screen.getByLabelText('Toggle Preview')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reset' })).toBeInTheDocument()
  })

  it('shows validate button when onValidate is provided', () => {
    render(
      <ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} onValidate={mockOnValidate} />
    )

    expect(screen.getByRole('button', { name: 'Validate' })).toBeInTheDocument()
  })

  it('does not show validate button when onValidate is not provided', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.queryByRole('button', { name: 'Validate' })).not.toBeInTheDocument()
  })

  it('shows preview panel by default', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByText('Preview')).toBeInTheDocument()
    expect(screen.getByText('2 steps')).toBeInTheDocument()
  })

  it('shows step details in preview', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByText('GET')).toBeInTheDocument()
    expect(screen.getByText('POST')).toBeInTheDocument()
    expect(screen.getByText('/api/users')).toBeInTheDocument()
    expect(screen.getByText('/api/users/create')).toBeInTheDocument()
    expect(screen.getByText('Status: 200')).toBeInTheDocument()
    expect(screen.getByText('Status: 201')).toBeInTheDocument()
  })

  it('toggles preview panel', async () => {
    const user = userEvent.setup()
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Preview should be visible initially
    expect(screen.getByText('Preview')).toBeInTheDocument()

    // Click toggle button
    await user.click(screen.getByLabelText('Toggle Preview'))

    // Preview should be hidden
    expect(screen.queryByText('Preview')).not.toBeInTheDocument()

    // Click again to show
    await user.click(screen.getByLabelText('Toggle Preview'))

    // Preview should be visible again
    expect(screen.getByText('Preview')).toBeInTheDocument()
  })

  it('shows unsaved changes indicator when content is modified', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Initially no unsaved indicator
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()

    // Modify the content using fireEvent (to handle special characters)
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '[]' } })

    // Should show unsaved indicator
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
  })

  it('shows parse error for invalid JSON', async () => {
    const user = userEvent.setup()
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Type invalid JSON
    const editor = screen.getByTestId('editor-textarea')
    await user.clear(editor)
    await user.type(editor, 'invalid json')

    // Should show error
    await waitFor(() => {
      expect(screen.getByText(/JSON Parse Error/)).toBeInTheDocument()
    })
  })

  it('shows error when root is not an array', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Type object instead of array using fireEvent
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '{"name": "test"}' } })

    // Should show error (text is split by elements, so use regex)
    expect(screen.getByText(/Root must be an array of test steps/)).toBeInTheDocument()
  })

  it('disables save button when there are parse errors', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Type invalid JSON
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '{invalid' } })

    // Save should be disabled
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('disables save button when there are no changes', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('enables save button when there are valid changes', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Modify content to valid JSON array
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '[]' } })

    // Save should be enabled
    expect(screen.getByRole('button', { name: 'Save' })).not.toBeDisabled()
  })

  it('calls onSave when save button is clicked', async () => {
    const user = userEvent.setup()
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Modify content
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '[]' } })

    // Click save
    await user.click(screen.getByRole('button', { name: 'Save' }))

    // Should call onSave with parsed value
    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledWith([])
    })
  })

  it('calls onValidate when validate button is clicked', async () => {
    const user = userEvent.setup()
    render(
      <ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} onValidate={mockOnValidate} />
    )

    // Click validate
    await user.click(screen.getByRole('button', { name: 'Validate' }))

    // Should call onValidate
    await waitFor(() => {
      expect(mockOnValidate).toHaveBeenCalledWith(JSON.stringify(mockSteps, null, 2))
    })
  })

  it('shows validation errors from onValidate', async () => {
    const user = userEvent.setup()
    const validationErrors: ValidationError[] = [
      { line: 5, column: 1, message: 'Missing required field: method', severity: 'error' },
      { line: 10, column: 1, message: 'Unknown field: foo', severity: 'warning' },
    ]
    mockOnValidate.mockResolvedValue(validationErrors)

    render(
      <ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} onValidate={mockOnValidate} />
    )

    // Click validate
    await user.click(screen.getByRole('button', { name: 'Validate' }))

    // Should show validation errors
    await waitFor(() => {
      expect(screen.getByText('Line 5: Missing required field: method')).toBeInTheDocument()
      expect(screen.getByText('Line 10: Unknown field: foo')).toBeInTheDocument()
    })
  })

  it('resets content when reset button is clicked', async () => {
    const user = userEvent.setup()
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Modify content
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '[]' } })

    // Verify modified
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()

    // Click reset
    await user.click(screen.getByRole('button', { name: 'Reset' }))

    // Should reset to initial value
    await waitFor(() => {
      expect(editor).toHaveValue(JSON.stringify(mockSteps, null, 2))
      expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
    })
  })

  it('disables reset button when there are no changes', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    expect(screen.getByRole('button', { name: 'Reset' })).toBeDisabled()
  })

  it('applies custom className', () => {
    const { container } = render(
      <ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} className="custom-class" />
    )

    expect(container.firstChild).toHaveClass('custom-class')
  })

  it('shows loading state in editor', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} isLoading />)

    // Editor should render loading indicator
    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument()
  })

  it('disables buttons when isLoading is true', () => {
    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} isLoading />)

    expect(screen.getByRole('button', { name: 'Reset' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('renders empty array correctly', () => {
    render(<ScenarioEditor initialValue={[]} onSave={mockOnSave} />)

    expect(screen.getByTestId('editor-textarea')).toHaveValue('[]')
    expect(screen.getByText('0 steps')).toBeInTheDocument()
  })

  it('shows method color coding in preview', () => {
    const createStep = (id: string, order: number, method: TestStep['method']): TestStep => ({
      id,
      order,
      method,
      endpoint: '/api/test',
      headers: {},
      body: null,
      expectedStatus: 200,
      assertions: [],
      extractors: [],
      timeoutMs: 5000,
    })

    const stepsWithMethods: TestStep[] = [
      createStep('step-get', 1, 'GET'),
      createStep('step-post', 2, 'POST'),
      createStep('step-put', 3, 'PUT'),
      createStep('step-patch', 4, 'PATCH'),
      createStep('step-delete', 5, 'DELETE'),
    ]

    render(<ScenarioEditor initialValue={stepsWithMethods} onSave={mockOnSave} />)

    expect(screen.getByText('GET')).toBeInTheDocument()
    expect(screen.getByText('POST')).toBeInTheDocument()
    expect(screen.getByText('PUT')).toBeInTheDocument()
    expect(screen.getByText('PATCH')).toBeInTheDocument()
    expect(screen.getByText('DELETE')).toBeInTheDocument()
  })

  it('handles save error gracefully', async () => {
    const user = userEvent.setup()
    mockOnSave.mockRejectedValue(new Error('Save failed'))

    render(<ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} />)

    // Modify content
    const editor = screen.getByTestId('editor-textarea')
    fireEvent.change(editor, { target: { value: '[]' } })

    // Click save
    await user.click(screen.getByRole('button', { name: 'Save' }))

    // Should still have unsaved changes after error
    await waitFor(() => {
      expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    })
  })

  it('handles validate error gracefully', async () => {
    const user = userEvent.setup()
    mockOnValidate.mockRejectedValue(new Error('Validation failed'))

    render(
      <ScenarioEditor initialValue={mockSteps} onSave={mockOnSave} onValidate={mockOnValidate} />
    )

    // Click validate
    await user.click(screen.getByRole('button', { name: 'Validate' }))

    // Should show error message
    await waitFor(() => {
      expect(screen.getByText('Line 1: Validation request failed')).toBeInTheDocument()
    })
  })
})
