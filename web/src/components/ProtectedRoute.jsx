import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Client-side role gate. This only controls what the UI shows;
// the REAL protection is on the server (Spring Security checks the JWT role on every API call).
export default function ProtectedRoute({ role, children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="wrap page"><p className="muted">Loading…</p></div>
  if (!user) return <Navigate to="/login" replace />
  if (role && user.role !== role) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/student'} replace />
  return children
}
