import { QueryErrorResetBoundary } from '@tanstack/react-query'
import { Component, type ReactNode, type ErrorInfo } from 'react'

type FallbackRender = (props: { error: Error; reset: () => void }) => ReactNode

interface QueryErrorBoundaryProps {
  children: ReactNode
  fallback?: FallbackRender | undefined
}

interface ErrorBoundaryInnerProps {
  children: ReactNode
  fallback: FallbackRender | undefined
  onReset: () => void
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

class ErrorBoundaryInner extends Component<ErrorBoundaryInnerProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Query Error Boundary caught an error:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    this.props.onReset()
  }

  render() {
    if (this.state.hasError && this.state.error) {
      if (this.props.fallback) {
        return this.props.fallback({
          error: this.state.error,
          reset: this.handleReset,
        })
      }

      return (
        <div className="query-error-boundary">
          <h2>Something went wrong</h2>
          <p>{this.state.error.message}</p>
          <button onClick={this.handleReset} className="error-retry-button">
            Try again
          </button>
        </div>
      )
    }

    return this.props.children
  }
}

export function QueryErrorBoundary({ children, fallback }: QueryErrorBoundaryProps) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ErrorBoundaryInner fallback={fallback} onReset={reset}>
          {children}
        </ErrorBoundaryInner>
      )}
    </QueryErrorResetBoundary>
  )
}
