import { createContext, useContext, useState, useEffect, useCallback, useMemo, type ReactNode } from 'react'
import Keycloak from 'keycloak-js'
import type { AuthContextType, User } from './types'
import { setAuthToken } from '@/api/client'

const AuthContext = createContext<AuthContextType | null>(null)

// Keycloak configuration from environment
const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL as string | undefined
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM as string | undefined ?? 'qawave'
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string | undefined ?? 'qawave-frontend'

// Token refresh interval (5 minutes before expiry)
const TOKEN_MIN_VALIDITY_SECONDS = 300

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)

  // Extract user info from Keycloak token
  const extractUser = useCallback((kc: Keycloak): User | null => {
    if (!kc.tokenParsed) return null

    const tokenParsed = kc.tokenParsed as {
      sub?: string
      email?: string
      name?: string
      preferred_username?: string
      realm_access?: { roles?: string[] }
    }

    return {
      id: tokenParsed.sub ?? '',
      email: tokenParsed.email ?? '',
      name: tokenParsed.name ?? '',
      username: tokenParsed.preferred_username ?? '',
      roles: tokenParsed.realm_access?.roles ?? [],
    }
  }, [])

  // Initialize Keycloak
  useEffect(() => {
    // Skip if Keycloak URL is not configured (development mode without auth)
    if (!KEYCLOAK_URL) {
      console.warn('Keycloak URL not configured. Running in development mode without auth.')
      setIsLoading(false)
      setIsAuthenticated(true) // Allow access in dev mode
      setUser({
        id: 'dev-user',
        email: 'dev@qawave.io',
        name: 'Development User',
        username: 'dev',
        roles: ['user'],
      })
      return
    }

    const kc = new Keycloak({
      url: KEYCLOAK_URL,
      realm: KEYCLOAK_REALM,
      clientId: KEYCLOAK_CLIENT_ID,
    })

    setKeycloak(kc)

    // Initialize Keycloak with PKCE
    kc.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
    })
      .then((authenticated) => {
        setIsAuthenticated(authenticated)

        if (authenticated && kc.token) {
          setToken(kc.token)
          setAuthToken(kc.token)
          setUser(extractUser(kc))
        }

        setIsLoading(false)
      })
      .catch((error) => {
        console.error('Keycloak initialization failed:', error)
        setIsLoading(false)
      })

    // Set up token refresh
    kc.onTokenExpired = () => {
      kc.updateToken(TOKEN_MIN_VALIDITY_SECONDS)
        .then((refreshed) => {
          if (refreshed && kc.token) {
            setToken(kc.token)
            setAuthToken(kc.token)
          }
        })
        .catch(() => {
          console.error('Token refresh failed, logging out')
          kc.logout()
        })
    }

    // Set up auth state change handlers
    kc.onAuthSuccess = () => {
      setIsAuthenticated(true)
      if (kc.token) {
        setToken(kc.token)
        setAuthToken(kc.token)
        setUser(extractUser(kc))
      }
    }

    kc.onAuthLogout = () => {
      setIsAuthenticated(false)
      setToken(null)
      setAuthToken(null)
      setUser(null)
    }

    // Cleanup
    return () => {
      // Keycloak doesn't need explicit cleanup
    }
  }, [extractUser])

  // Login handler
  const login = useCallback(() => {
    if (keycloak) {
      keycloak.login()
    }
  }, [keycloak])

  // Logout handler
  const logout = useCallback(() => {
    if (keycloak) {
      keycloak.logout({
        redirectUri: window.location.origin,
      })
    }
  }, [keycloak])

  // Manual token refresh
  const refreshToken = useCallback(async (): Promise<boolean> => {
    if (!keycloak) return false

    try {
      const refreshed = await keycloak.updateToken(TOKEN_MIN_VALIDITY_SECONDS)
      if (refreshed && keycloak.token) {
        setToken(keycloak.token)
        setAuthToken(keycloak.token)
      }
      return true
    } catch {
      return false
    }
  }, [keycloak])

  const value = useMemo<AuthContextType>(
    () => ({
      isAuthenticated,
      isLoading,
      user,
      token,
      keycloak,
      login,
      logout,
      refreshToken,
    }),
    [isAuthenticated, isLoading, user, token, keycloak, login, logout, refreshToken]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuthContext(): AuthContextType {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider')
  }
  return context
}
