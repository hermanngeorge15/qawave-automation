import { useState, useCallback } from 'react'

interface User {
  id: string
  email: string
  name: string
}

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
}

export function useAuth(): AuthState & {
  login: (email: string, password: string) => Promise<void>
  logout: () => void
} {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: false,
  })

  const login = useCallback(async (_email: string, _password: string) => {
    setState((prev) => ({ ...prev, isLoading: true }))
    // TODO: Implement actual login logic
    setState({
      user: { id: '1', email: _email, name: 'User' },
      isAuthenticated: true,
      isLoading: false,
    })
  }, [])

  const logout = useCallback(() => {
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    })
  }, [])

  return {
    ...state,
    login,
    logout,
  }
}
