import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { api, tokenStore } from '../api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // On first load, if a token is saved, ask the server who we are.
  useEffect(() => {
    let alive = true
    async function restore() {
      if (!tokenStore.get()) { setLoading(false); return }
      try {
        const me = await api.me()
        if (alive) setUser(me)
      } catch {
        tokenStore.clear()
      } finally {
        if (alive) setLoading(false)
      }
    }
    restore()
    return () => { alive = false }
  }, [])

  const startSession = useCallback(({ token, user }) => {
    tokenStore.set(token)
    setUser(user)
    return user
  }, [])

  const login = useCallback(async (credentials) => startSession(await api.login(credentials)), [startSession])

  // Registering does NOT sign anyone in: the email must be verified first.
  const verifyEmail = useCallback(async (email, code) => startSession(await api.verifyEmail({ email, code })), [startSession])

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, verifyEmail, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
