import { useAuthContext } from './AuthProvider'
import type { User } from './types'

interface UseAuthReturn {
  // Auth state
  isAuthenticated: boolean
  isLoading: boolean
  user: User | null
  token: string | null

  // Actions
  login: () => void
  logout: () => void
  refreshToken: () => Promise<boolean>

  // Role checks
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
}

export function useAuth(): UseAuthReturn {
  const auth = useAuthContext()

  const hasRole = (role: string): boolean => {
    return auth.user?.roles.includes(role) ?? false
  }

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some((role) => hasRole(role))
  }

  return {
    isAuthenticated: auth.isAuthenticated,
    isLoading: auth.isLoading,
    user: auth.user,
    token: auth.token,
    login: auth.login,
    logout: auth.logout,
    refreshToken: auth.refreshToken,
    hasRole,
    hasAnyRole,
  }
}
