import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../context/AuthContext'
import { AuthShell } from './Auth'

// Shown after sign-up (and after a login attempt on an unverified account).
export default function VerifyEmail() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { verifyEmail } = useAuth()
  const [email, setEmail] = useState(state?.email || '')
  const [code, setCode] = useState('')
  const [info, setInfo] = useState(state?.email && !state?.resend ? 'We sent a 6-digit code to your email. It expires in 10 minutes.' : '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [cooldown, setCooldown] = useState(state?.email && !state?.resend ? 60 : 0)
  const asked = useRef(false)

  useEffect(() => {
    if (cooldown <= 0) return
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000)
    return () => clearTimeout(t)
  }, [cooldown])

  // Arriving from a login attempt: send a fresh code once.
  useEffect(() => {
    if (state?.resend && state?.email && !asked.current) {
      asked.current = true
      api.resendCode(state.email)
        .then(() => { setInfo('Your email is not verified yet. We sent you a new 6-digit code.'); setCooldown(60) })
        .catch((e) => setError(e.message))
    }
  }, [state])

  async function resend() {
    setError('')
    try {
      await api.resendCode(email.trim())
      setInfo('If that email can receive a code, a new one is on its way.')
      setCooldown(60)
    } catch (e) { setError(e.message) }
  }

  async function submit(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      const u = await verifyEmail(email.trim(), code.trim())
      navigate(u.role === 'ADMIN' ? '/admin' : '/student', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <AuthShell title="Check your email" intro="Enter the 6-digit code we sent you. Look in spam if you cannot find it."
      footer={<>Wrong address? <Link to="/register">Start again</Link></>}>
      <form onSubmit={submit} noValidate>
        {info && <p className="notice">{info}</p>}
        {!state?.email && (
          <label className="field"><span>Email</span>
            <input type="email" autoComplete="email" autoCapitalize="none" spellCheck={false} inputMode="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
        )}
        <label className="field"><span>6-digit code</span>
          <input className="code-input" inputMode="numeric" autoComplete="one-time-code" maxLength={6} required value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} />
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6 || !email}>
          {busy ? 'Checking…' : 'Verify and continue'}
        </button>
      </form>
      <p className="auth-foot">
        <button type="button" className="linklike resend" onClick={resend} disabled={cooldown > 0 || !email}>
          {cooldown > 0 ? `Send a new code in ${cooldown}s` : 'Send a new code'}
        </button>
      </p>
    </AuthShell>
  )
}
