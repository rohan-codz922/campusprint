import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, isDemo } from '../api'
import { useToast } from '../components/Toast'
import { AuthShell } from './Auth'

// Two steps: (1) enter email -> we send a 6-digit code, (2) enter code + new password.
export default function ForgotPassword() {
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [info, setInfo] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function sendCode(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      const res = await api.forgotPassword(email.trim())
      setInfo(res.message)
      setStep('code')
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  async function reset(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      await api.resetPassword({ email: email.trim(), code: code.trim(), newPassword: password })
      toast('Password changed. Sign in with your new password.')
      navigate('/login', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  if (step === 'email') {
    return (
      <AuthShell title="Reset your password" intro="Enter the email you registered with. We will send you a 6-digit code."
        footer={<>Remembered it? <Link to="/login">Sign in</Link></>}>
        <form onSubmit={sendCode} noValidate>
          <label className="field"><span>Email</span>
            <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <button className="btn btn-primary btn-block" disabled={busy || !/\S+@\S+\.\S+/.test(email)}>
            {busy ? 'Sending code…' : 'Send code'}
          </button>
        </form>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Enter your code" intro="Check your inbox, and your spam folder if it is not there."
      footer={<>No code? <button type="button" className="linklike" onClick={() => { setStep('email'); setError('') }}>Send a new one</button> (one per minute).</>}>
      <form onSubmit={reset} noValidate>
        <p className="notice">{info}</p>
        {isDemo && <p className="muted small" style={{ marginBottom: '.9rem' }}>Demo mode: the code is 123456.</p>}
        <label className="field"><span>6-digit code</span>
          <input className="code-input" inputMode="numeric" autoComplete="one-time-code" maxLength={6} required value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} />
        </label>
        <label className="field"><span>New password</span>
          <input type="password" autoComplete="new-password" minLength={6} required value={password}
            onChange={(e) => setPassword(e.target.value)} aria-describedby="np-help" />
          <small id="np-help">At least 6 characters.</small>
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6 || password.length < 6}>
          {busy ? 'Saving…' : 'Change password'}
        </button>
      </form>
    </AuthShell>
  )
}
