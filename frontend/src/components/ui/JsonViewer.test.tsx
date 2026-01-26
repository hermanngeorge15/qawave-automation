import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { JsonViewer } from './JsonViewer'

describe('JsonViewer', () => {
  it('renders formatted JSON', () => {
    const data = { name: 'test', value: 123 }
    render(<JsonViewer data={data} />)

    // Check if the formatted JSON is present
    const code = screen.getByRole('code')
    expect(code.textContent).toContain('"name": "test"')
    expect(code.textContent).toContain('"value": 123')
  })

  it('formats JSON with proper indentation', () => {
    const data = { nested: { key: 'value' } }
    render(<JsonViewer data={data} />)

    const code = screen.getByRole('code')
    // Check for 2-space indentation
    expect(code.textContent).toMatch(/"nested": \{/)
    expect(code.textContent).toMatch(/"key": "value"/)
  })

  it('handles arrays', () => {
    const data = [1, 2, 3]
    render(<JsonViewer data={data} />)

    const code = screen.getByRole('code')
    expect(code.textContent).toContain('[')
    expect(code.textContent).toContain('1')
    expect(code.textContent).toContain('2')
    expect(code.textContent).toContain('3')
  })

  it('handles null values', () => {
    const data = { value: null }
    render(<JsonViewer data={data} />)

    const code = screen.getByRole('code')
    expect(code.textContent).toContain('null')
  })

  it('handles strings', () => {
    render(<JsonViewer data="plain string" />)

    const code = screen.getByRole('code')
    expect(code.textContent).toBe('"plain string"')
  })

  it('handles numbers', () => {
    render(<JsonViewer data={42} />)

    const code = screen.getByRole('code')
    expect(code.textContent).toBe('42')
  })

  it('handles booleans', () => {
    render(<JsonViewer data={true} />)

    const code = screen.getByRole('code')
    expect(code.textContent).toBe('true')
  })

  it('applies custom className', () => {
    const { container } = render(<JsonViewer data={{}} className="custom-class" />)
    expect(container.querySelector('pre')).toHaveClass('custom-class')
  })

  it('has proper styling for code block', () => {
    const { container } = render(<JsonViewer data={{}} />)
    const pre = container.querySelector('pre')

    expect(pre).toHaveClass('bg-secondary-900')
    expect(pre).toHaveClass('font-mono')
    expect(pre).toHaveClass('overflow-x-auto')
  })

  it('renders complex nested structures', () => {
    const data = {
      users: [
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' },
      ],
      metadata: {
        total: 2,
        page: 1,
      },
    }
    render(<JsonViewer data={data} />)

    const code = screen.getByRole('code')
    expect(code.textContent).toContain('"users"')
    expect(code.textContent).toContain('"Alice"')
    expect(code.textContent).toContain('"metadata"')
  })
})
