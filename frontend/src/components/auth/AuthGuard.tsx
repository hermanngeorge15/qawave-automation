import type { ReactNode } from 'react'
import { useLocation, useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import { useAuth } from '@/lib/auth'

interface AuthGuardProps {
  children: ReactNode
  roles?: string[] | undefined
}

export function AuthGuard({ children, roles }: AuthGuardProps) {
  const { isAuthenticated, isLoading, hasAnyRole, login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      // Store the return URL in sessionStorage for after login
      // TanStack Router's location.search is an object, use window.location.search for the string
      const searchString = window.location.search
      sessionStorage.setItem('returnUrl', location.pathname + searchString)
      // Trigger Keycloak login
      login()
    }
  }, [isLoading, isAuthenticated, location.pathname, login])

  useEffect(() => {
    if (!isLoading && isAuthenticated && roles && roles.length > 0 && !hasAnyRole(roles)) {
      void navigate({ to: '/unauthorized' })
    }
  }, [isLoading, isAuthenticated, roles, hasAnyRole, navigate])

  if (isLoading) {
    return (
      <div className="auth-loading">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-secondary-400">Checking authentication...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    // While redirecting to login
    return (
      <div className="auth-loading">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-secondary-400">Redirecting to login...</p>
        </div>
      </div>
    )
  }

  // Check role-based access
  if (roles && roles.length > 0 && !hasAnyRole(roles)) {
    return null // Will redirect via useEffect
  }

  return <>{children}</>
}

// Higher-order component for protecting routes
// eslint-disable-next-line react-refresh/only-export-components
export function withAuthGuard<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  roles?: string[]
) {
  return function ProtectedComponent(props: P) {
    return (
      <AuthGuard roles={roles}>
        <WrappedComponent {...props} />
      </AuthGuard>
    )
  }
}
