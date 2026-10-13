import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import Icon from '../components/Icon'
import StatusTrack, { PaymentChip } from '../components/StatusTrack'
import { plural, rupees, when } from '../utils'

export default function StudentDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setRequests(await api.myRequests())
    } catch (e) {
      toast(e.message, 'error')
    } finally { setLoading(false) }
  }, [toast])

  useEffect(() => { load() }, [load])

  async function cancel(id) {
    try {
      await api.cancelRequest(id)
      toast('Print cancelled.')
      await load()
    } catch (err) { toast(err.message, 'error') }
  }

  const ready = requests.filter((r) => r.status === 'READY').length
  const active = requests.filter((r) => ['PENDING', 'PRINTING'].includes(r.status)).length

  return (
    <main id="main" className="wrap page">
      <header className="dash-h">
        <div>
          <h1>Your prints</h1>
          <p className="muted">
            {loading ? 'Loading…' : requests.length === 0
              ? `Hello, ${user.name.split(' ')[0]}. Nothing printed yet.`
              : `Hello, ${user.name.split(' ')[0]}. ${ready ? `${plural(ready, 'print')} ready to collect. ` : ''}${active ? `${active} in progress.` : ''}`}
          </p>
        </div>
        <Link className={`btn btn-primary btn-lg ${!loading && requests.length === 0 ? 'hide-sm' : ''}`} to="/student/new"><Icon name="plus" size={20} /> New print</Link>
      </header>

      {loading ? (
        <ul className="prints" role="status">
          <li className="sr">Loading your prints…</li>
          <li className="skel-card" aria-hidden="true" /><li className="skel-card" aria-hidden="true" />
        </ul>
      ) : requests.length === 0 ? (
        <section className="blank">
          <Link to="/student/new" className="blank-plus" aria-label="Add your first print"><Icon name="plus" size={34} /></Link>
          <h2>Add your first print</h2>
          <p className="muted">Upload a PDF or photo, see the exact price, and the print desk gets it straight away. Your past prints will be listed here.</p>
          <Link className="btn btn-primary" to="/student/new">Add a print</Link>
        </section>
      ) : (
        <ul className="prints">
          {requests.map((r, i) => (
            <li key={r.id} className={`print ${r.status === 'READY' ? 'is-ready' : ''}`}>
              <div className="print-main">
                {/* Student's own count (oldest = 1). The shop's internal job numbers are never shown here. */}
                <span className="print-no">Print {requests.length - i}</span>
                <strong className="clip">{r.fileName}</strong>
                <span className="muted">{plural(r.pages, 'page')} × {r.copies} · {r.service.name}</span>
                <span className="muted small">Sent {when(r.createdAt)}</span>
              </div>
              <div className="print-track"><StatusTrack status={r.status} /></div>
              <div className="print-side">
                <strong className="print-price">{rupees(r.totalPrice)}</strong>
                <PaymentChip status={r.paymentStatus} />
                {r.status === 'READY' && r.paymentStatus !== 'VERIFIED' && <span className="pickup">Pay {rupees(r.totalPrice)} at the desk</span>}
                {r.status === 'READY' && r.paymentStatus === 'VERIFIED' && <span className="pickup">Ready. Just collect it.</span>}
                {r.status === 'PENDING' && r.paymentStatus === 'NONE' && (
                  <button className="btn btn-quiet btn-danger btn-sm" onClick={() => cancel(r.id)}>Cancel</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {requests.length > 0 && (
        <Link to="/student/new" className="fab" aria-label="New print"><Icon name="plus" size={26} /></Link>
      )}
    </main>
  )
}
