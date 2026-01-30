import type Keycloak from 'keycloak-js'

export interface User {
  id: string
  email: string
  name: string
  username: string
  roles: string[]
}

export interface AuthContextType {
  isAuthenticated: boolean
  isLoading: boolean
  user: User | null
  token: string | null
  keycloak: Keycloak | null
  login: () => void
  logout: () => void
  refreshToken: () => Promise<boolean>
}
