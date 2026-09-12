import { createContext, useContext, useMemo, useState } from 'react'
import { login as loginRequest } from '../api/auth'

const AuthContext = createContext(null)

function readStoredAuth() {
  const raw = localStorage.getItem('auth')
  return raw ? JSON.parse(raw) : null
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth)

  const value = useMemo(
    () => ({
      user: auth,
      isAuthenticated: !!auth,
      async login(username, password) {
        const data = await loginRequest(username, password)
        localStorage.setItem('auth', JSON.stringify(data))
        setAuth(data)
        return data
      },
      logout() {
        localStorage.removeItem('auth')
        setAuth(null)
      },
    }),
    [auth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
