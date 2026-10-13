import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { RegMark } from './Icon'
import Icon from './Icon'
import { isDemo } from '../api'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const home = user ? (user.role === 'ADMIN' ? '/admin' : '/student') : '/'

  return (
    <header className="nav">
      <a href="#main" className="skip">Skip to content</a>
      <div className="wrap nav-in">
        <Link to={home} className="brand" aria-label="CampusPrint home">
          <RegMark size={26} />
          <span>CampusPrint</span>
        </Link>
        <nav className="nav-r" aria-label="Account">
          {isDemo && <span className="demo-tag" title="Data is saved in this browser only">Demo data</span>}
          {user ? (
            <>
              <span className="who">
                <strong>{user.name}</strong>
                <small>{user.role === 'ADMIN' ? 'Print desk' : user.rollNo ? `Roll ${user.rollNo}` : 'Student'}</small>
              </span>
              <button className="btn btn-quiet" onClick={() => { logout(); navigate('/') }}>
                <Icon name="logout" size={18} /> Sign out
              </button>
            </>
          ) : (
            <>
              <Link className="btn btn-quiet" to="/login">Sign in</Link>
              <Link className="btn btn-primary" to="/register">Create account</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
