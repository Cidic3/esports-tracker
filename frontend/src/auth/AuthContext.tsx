import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { clearToken, getToken, setToken } from '../api/client'
import { useMe } from '../api/queries'
import type { UserResponse } from '../api/types'

interface AuthContextValue {
  isAuthenticated: boolean
  user: UserResponse | undefined
  login: (token: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  // Token lives in localStorage (survives reloads); React state just mirrors
  // it so components re-render when it changes.
  const [authenticated, setAuthenticated] = useState(() => getToken() !== null)
  const queryClient = useQueryClient()

  const { data: user } = useMe(authenticated)

  const login = useCallback((token: string) => {
    setToken(token)
    setAuthenticated(true)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setAuthenticated(false)
    // Drop every cached response — personalized data must not survive logout.
    queryClient.clear()
  }, [queryClient])

  return (
    <AuthContext.Provider value={{ isAuthenticated: authenticated, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
