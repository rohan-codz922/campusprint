import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../api'

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
          <dd>Send files, see the price first, pay by UPI if you like, and follow each print.</dd>
          <dt>Print desk</dt>
          <dd>Work the queue, check payments, download files and manage prices.</dd>
        </dl>
      </aside>
    </main>
  )
}

const homeFor = (u) => (u.role === 'ADMIN' ? '/admin' : '/student')

export function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homeFor(user)} replace />

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      const u = await login({ email: form.email.trim(), password: form.password })
      navigate(homeFor(u), { replace: true })
    } catch (err) {
      if (err.code === 'EMAIL_NOT_VERIFIED') {
        navigate('/verify-email', { state: { email: form.email.trim(), resend: true } })
      } else {
        setError(err.message)
      }
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
    </AuthShell>
  )
}

const PASSWORD_OK = (p) => p.length >= 8 && p.length <= 72 && /[A-Za-z]/.test(p) && /\d/.test(p)

export function Register() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', rollNo: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homeFor(user)} replace />

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const valid = form.name.trim() && /\S+@\S+\.\S+/.test(form.email) && PASSWORD_OK(form.password)

  async function submit(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      await api.register({ ...form, name: form.name.trim(), email: form.email.trim(), rollNo: form.rollNo.trim() })
      navigate('/verify-email', { state: { email: form.email.trim() } })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <AuthShell title="Create your account" intro="We will email you a code to confirm it is really your address."
      footer={<>Already registered? <Link to="/login">Sign in</Link></>}>
      <form onSubmit={submit} noValidate>
        <label className="field"><span>Full name</span>
          <input autoComplete="name" required value={form.name} onChange={set('name')} />
        </label>
        <label className="field"><span>Roll number <em>(optional)</em></span>
          <input value={form.rollNo} onChange={set('rollNo')} />
        </label>
        <label className="field"><span>Email</span>
          <input type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        </label>
        <label className="field"><span>Password</span>
          <input type="password" autoComplete="new-password" required value={form.password} onChange={set('password')} aria-describedby="pw-help" />
          <small id="pw-help">At least 8 characters, with a letter and a number.</small>
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={busy || !valid}>
          {busy ? 'Sending code…' : 'Create account'}
        </button>
      </form>
    </AuthShell>
  )
}
