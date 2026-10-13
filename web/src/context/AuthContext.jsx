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

  const login = useCallback(async (credentials) => {
    const { token, user } = await api.login(credentials)
    tokenStore.set(token)
    setUser(user)
    return user
  }, [])

  const register = useCallback(async (data) => {
    const { token, user } = await api.register(data)
    tokenStore.set(token)
    setUser(user)
    return user
  }, [])

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
