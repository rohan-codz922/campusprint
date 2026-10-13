import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { isDemo } from '../api'

export function AuthShell({ title, intro, children, footer }) {
  return (
    <main id="main" className="wrap auth">
      <div className="auth-form">
        <h1>{title}</h1>
        <p className="muted">{intro}</p>
        {children}
        <p className="auth-foot">{footer}</p>
      </div>
      <aside className="auth-side" aria-label="What each role can do">
        <h2>Two roles, one queue</h2>
        <dl>
          <dt>Students</dt>
          <dd>Send files, see the price first, follow each job, and cancel it before printing starts.</dd>
          <dt>Print desk</dt>
          <dd>Work the queue, change job status, download files and manage the price list.</dd>
        </dl>
      </aside>
    </main>
  )
}

export function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/student'} replace />

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      const u = await login(form)
      navigate(u.role === 'ADMIN' ? '/admin' : '/student', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <AuthShell title="Sign in" intro="Use the email you registered with."
      footer={<>New here? <Link to="/register">Create a student account</Link></>}>
      <form onSubmit={submit} noValidate>
        <label className="field"><span>Email</span>
          <input type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        </label>
        <label className="field"><span>Password</span>
          <input type="password" autoComplete="current-password" required value={form.password} onChange={set('password')} />
        </label>
        <Link className="forgot" to="/forgot-password">Forgot password?</Link>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={busy || !form.email || !form.password}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      {isDemo && (
        <div className="demo-box">
          <p>Demo accounts</p>
          <div className="row">
            <button type="button" className="btn btn-quiet" onClick={() => setForm({ email: 'student@campus.edu', password: 'student123' })}>Fill student</button>
            <button type="button" className="btn btn-quiet" onClick={() => setForm({ email: 'admin@campus.edu', password: 'admin123' })}>Fill print desk</button>
          </div>
        </div>
      )}
    </AuthShell>
  )
}

export function Register() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', rollNo: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/student'} replace />

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const valid = form.name.trim() && /\S+@\S+\.\S+/.test(form.email) && form.password.length >= 6

  async function submit(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      await register(form)
      navigate('/student', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <AuthShell title="Create your account" intro="Student accounts can send print jobs right away."
      footer={<>Already registered? <Link to="/login">Sign in</Link></>}>
      <form onSubmit={submit} noValidate>
        <label className="field"><span>Full name</span>
          <input autoComplete="name" required value={form.name} onChange={set('name')} />
        </label>
        <label className="field"><span>Roll number <em>(optional)</em></span>
          <input value={form.rollNo} onChange={set('rollNo')} />
        </label>
        <label className="field"><span>College email</span>
          <input type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        </label>
        <label className="field"><span>Password</span>
          <input type="password" autoComplete="new-password" minLength={6} required value={form.password} onChange={set('password')} aria-describedby="pw-help" />
          <small id="pw-help">At least 6 characters.</small>
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={busy || !valid}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthShell>
  )
}
