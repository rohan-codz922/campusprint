import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../context/AuthContext'
import Ticket from '../components/Ticket'
import { estimate, rupees } from '../utils'

export default function Landing() {
  const { user } = useAuth()
  const [services, setServices] = useState([])
  const [state, setState] = useState('loading') // loading | ready | error
  const [serviceId, setServiceId] = useState('')
  const [pages, setPages] = useState(12)
  const [copies, setCopies] = useState(2)

  const alive = useRef(true)
  const load = useCallback(() => {
    setState('loading')
    api.getServices()
      .then((all) => {
        if (!alive.current) return
        const active = all.filter((s) => s.active)
        setServices(active)
        setServiceId(active[0] ? String(active[0].id) : '')
        setState('ready')
      })
      .catch(() => alive.current && setState('error'))
  }, [])

  useEffect(() => {
    alive.current = true
    load()
    return () => { alive.current = false }
  }, [load])

  const service = services.find((s) => String(s.id) === serviceId)
  const dash = user ? (user.role === 'ADMIN' ? '/admin' : '/student') : '/register'

  return (
    <main id="main">
      <section className="wrap hero">
        <div className="hero-copy">
          <h1>Send it to print from your seat. Collect it on your way.</h1>
          <p className="lede">
            Upload your assignment, report or poster, choose the paper, and see the exact price before you
            send it. The print desk gets your job in a queue and you can follow it until it is ready.
          </p>
          <div className="row cta-row">
            <Link className="btn btn-primary btn-lg" to={dash}>{user ? 'Open your dashboard' : 'Create a free account'}</Link>
            {!user && <Link className="btn btn-quiet btn-lg" to="/login">I already have an account</Link>}
          </div>
        </div>

        <div className="hero-ticket">
          <form className="estimator" onSubmit={(e) => e.preventDefault()} aria-label="Price estimator">
            <h2>What does it cost?</h2>
            <label className="field">
              <span>Print option</span>
              <select value={serviceId} onChange={(e) => setServiceId(e.target.value)} disabled={state !== 'ready'}>
                {state === 'loading' && <option>Loading options…</option>}
                {state === 'error' && <option>Options unavailable</option>}
                {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </label>
            <div className="two">
              <label className="field">
                <span>Pages</span>
                <input type="number" min="1" max="500" value={pages} onChange={(e) => setPages(Math.max(0, +e.target.value))} />
              </label>
              <label className="field">
                <span>Copies</span>
                <input type="number" min="1" max="100" value={copies} onChange={(e) => setCopies(Math.max(0, +e.target.value))} />
              </label>
            </div>
          </form>
          {state === 'error' && (
            <div className="inline-error with-action" role="alert">
              <span>We could not load the prices. The server may be waking up.</span>
              <button type="button" className="btn btn-quiet" onClick={load}>Try again</button>
            </div>
          )}
          <Ticket service={service} pages={pages} copies={copies} total={estimate(service, pages, copies)} />
        </div>
      </section>

      <section className="wrap band">
        <h2>How a job moves through the desk</h2>
        <ol className="steps">
          <li><strong>You send the file</strong><p>Upload a PDF or a photo, up to 10 MB. We count the pages for you, so the price is exact.</p></li>
          <li><strong>The desk prints it</strong><p>Staff see every job in one queue. Big jobs are paid first by UPI, small ones at the counter.</p></li>
          <li><strong>You collect and pay</strong><p>Show your name at the counter, pay if you have not already, and take your pages.</p></li>
        </ol>
      </section>

      <section className="wrap band">
        <h2>Print options and prices</h2>
        {state === 'ready' ? (
          <div className="table-wrap">
            <table className="table priceable">
              <thead><tr><th>Option</th><th>Paper and finish</th><th className="num">Per page</th></tr></thead>
              <tbody>
                {services.map((s) => (
                  <tr key={s.id}><td><strong>{s.name}</strong></td><td>{s.description}</td><td className="num">{rupees(s.pricePerPage)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : state === 'loading' ? (
          <div className="skel-list" role="status">
            <span className="sr">Loading prices…</span>
            <div className="skel" aria-hidden="true" /><div className="skel" aria-hidden="true" />
          </div>
        ) : <p className="muted">Prices are unavailable right now.</p>}
      </section>

      <footer className="foot">
        <div className="wrap">
          <span>CampusPrint · Web and Mobile Application Development project</span>
          <span>Built with React, Kotlin and Spring Boot</span>
        </div>
      </footer>
    </main>
  )
}
