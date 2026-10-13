import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { useToast } from '../components/Toast'
import Icon from '../components/Icon'
import { StatusBadge, STATUS_LABEL } from '../components/StatusTrack'
import { rupees, when } from '../utils'

const SETTABLE = ['PENDING', 'PRINTING', 'READY', 'COLLECTED', 'REJECTED']
const FILTERS = [
  { key: 'ACTIVE', label: 'To do', test: (r) => ['PENDING', 'PRINTING', 'READY'].includes(r.status) },
  { key: 'COLLECTED', label: 'Collected', test: (r) => r.status === 'COLLECTED' },
  { key: 'CLOSED', label: 'Rejected or cancelled', test: (r) => ['REJECTED', 'CANCELLED'].includes(r.status) },
  { key: 'ALL', label: 'All jobs', test: () => true },
]

export default function AdminDashboard() {
  const toast = useToast()
  const [tab, setTab] = useState('queue')
  const [requests, setRequests] = useState([])
  const [services, setServices] = useState([])
  const [stats, setStats] = useState(null)
  const [filter, setFilter] = useState('ACTIVE')
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)

  const load = useCallback(async () => {
    try {
      const [r, s, st] = await Promise.all([api.allRequests(), api.getServices(), api.stats()])
      setRequests(r); setServices(s); setStats(st)
    } catch (e) { toast(e.message, 'error') } finally { setLoading(false) }
  }, [toast])

  useEffect(() => { load() }, [load])

  const counts = useMemo(
    () => Object.fromEntries(FILTERS.map((f) => [f.key, requests.filter(f.test).length])),
    [requests]
  )
  const visible = useMemo(() => requests.filter(FILTERS.find((f) => f.key === filter).test), [requests, filter])

  async function changeStatus(r, status) {
    setSavingId(r.id)
    try {
      await api.setStatus(r.id, status)
      toast(`Job ${r.id} marked as ${STATUS_LABEL[status].toLowerCase()}.`)
      await load()
    } catch (e) { toast(e.message, 'error') } finally { setSavingId(null) }
  }

  async function download(r) {
    try {
      const blob = await api.downloadFile(r.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = r.fileName; a.click()
      URL.revokeObjectURL(url)
    } catch (e) { toast(e.message, 'error') }
  }

  return (
    <main id="main" className="wrap page">
      <header className="page-h">
        <h1>Print desk</h1>
        <p className="muted">Work the queue, update job status and keep the price list current.</p>
      </header>

      <section className="strip" aria-label="Today at a glance">
        <div><span className="n">{stats?.byStatus?.PENDING || 0}</span><span>Waiting to print</span></div>
        <div><span className="n">{stats?.byStatus?.PRINTING || 0}</span><span>On the printer</span></div>
        <div><span className="n">{stats?.byStatus?.READY || 0}</span><span>Ready for pickup</span></div>
        <div><span className="n">{rupees(stats?.revenue || 0)}</span><span>Collected so far</span></div>
      </section>

      <div className="tabs" role="tablist" aria-label="Print desk sections">
        <button role="tab" aria-selected={tab === 'queue'} className={tab === 'queue' ? 'on' : ''} onClick={() => setTab('queue')}>Print queue</button>
        <button role="tab" aria-selected={tab === 'prices'} className={tab === 'prices' ? 'on' : ''} onClick={() => setTab('prices')}>Price list</button>
      </div>

      {tab === 'queue' ? (
        <section aria-label="Print queue">
          <div className="filters" role="group" aria-label="Filter jobs">
            {FILTERS.map((f) => (
              <button key={f.key} className={`pill ${filter === f.key ? 'on' : ''}`} aria-pressed={filter === f.key} onClick={() => setFilter(f.key)}>
                {f.label} <span>{counts[f.key]}</span>
              </button>
            ))}
          </div>

          {loading ? <p className="muted">Loading the queue…</p> : visible.length === 0 ? (
            <div className="empty"><Icon name="inbox" size={28} /><p><strong>Nothing here.</strong> Jobs that match this filter will appear in this list.</p></div>
          ) : (
            <div className="table-wrap">
              <table className="table queue">
                <thead>
                  <tr><th>Job</th><th>Student</th><th>Details</th><th className="num">Total</th><th>Status</th><th><span className="sr">File</span></th></tr>
                </thead>
                <tbody>
                  {visible.map((r) => (
                    <tr key={r.id}>
                      <td><strong className="clip">{r.fileName}</strong><span className="muted small">Job {r.id} · {when(r.createdAt)}</span>
                        {r.notes && <span className="note">“{r.notes}”</span>}</td>
                      <td>{r.student.name}<span className="muted small">{r.student.rollNo ? `Roll ${r.student.rollNo}` : r.student.email}</span></td>
                      <td>{r.service.name}<span className="muted small">{r.pages} pages × {r.copies}</span></td>
                      <td className="num">{rupees(r.totalPrice)}</td>
                      <td>
                        {r.status === 'CANCELLED' ? <StatusBadge status="CANCELLED" /> : (
                          <select aria-label={`Status for job ${r.id}`} value={r.status} disabled={savingId === r.id}
                            onChange={(e) => changeStatus(r, e.target.value)} className={`status-select s-${r.status.toLowerCase()}`}>
                            {SETTABLE.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                          </select>
                        )}
                      </td>
                      <td><button className="btn btn-quiet btn-icon" onClick={() => download(r)} aria-label={`Download ${r.fileName}`}><Icon name="download" size={18} /></button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : (
        <PriceList services={services} reload={load} toast={toast} />
      )}
    </main>
  )
}

function PriceList({ services, reload, toast }) {
  const [draft, setDraft] = useState({ name: '', description: '', pricePerPage: '' })
  const [rates, setRates] = useState({})
  const [busy, setBusy] = useState(false)

  async function save(s) {
    const rate = Number(rates[s.id])
    if (!(rate > 0)) { toast('Enter a rate above zero.', 'error'); return }
    try {
      await api.updateService(s.id, { name: s.name, description: s.description, active: s.active, pricePerPage: rate })
      toast(`${s.name} is now ${rupees(rate)} per page.`)
      setRates((r) => { const n = { ...r }; delete n[s.id]; return n })
      await reload()
    } catch (e) { toast(e.message, 'error') }
  }

  async function toggle(s) {
    try {
      await api.updateService(s.id, { name: s.name, description: s.description, pricePerPage: s.pricePerPage, active: !s.active })
      toast(`${s.name} is ${s.active ? 'hidden from students' : 'available again'}.`)
      await reload()
    } catch (e) { toast(e.message, 'error') }
  }

  async function add(e) {
    e.preventDefault()
    setBusy(true)
    try {
      await api.createService({ name: draft.name.trim(), description: draft.description.trim(), pricePerPage: Number(draft.pricePerPage) })
      toast(`${draft.name} added to the price list.`)
      setDraft({ name: '', description: '', pricePerPage: '' })
      await reload()
    } catch (err) { toast(err.message, 'error') } finally { setBusy(false) }
  }

  return (
    <section aria-label="Price list">
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>Option</th><th>Per page (₹)</th><th>Visible to students</th></tr></thead>
          <tbody>
            {services.map((s) => (
              <tr key={s.id}>
                <td><strong>{s.name}</strong><span className="muted small">{s.description}</span></td>
                <td>
                  <div className="rate">
                    <input aria-label={`Rate for ${s.name}`} type="number" min="0.25" step="0.25"
                      value={rates[s.id] ?? s.pricePerPage} onChange={(e) => setRates({ ...rates, [s.id]: e.target.value })} />
                    {rates[s.id] !== undefined && Number(rates[s.id]) !== s.pricePerPage && (
                      <button className="btn btn-primary" onClick={() => save(s)}>Save rate</button>
                    )}
                  </div>
                </td>
                <td>
                  <button className="switch" role="switch" aria-checked={s.active} onClick={() => toggle(s)}>
                    <span className="knob" /><span className="sr">{s.active ? 'Visible' : 'Hidden'}</span>
                  </button>
                  <span className="small muted"> {s.active ? 'Visible' : 'Hidden'}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className="sheet add-service" onSubmit={add}>
        <h2>Add a print option</h2>
        <div className="three">
          <label className="field"><span>Name</span><input required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} /></label>
          <label className="field"><span>Paper and finish</span><input value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} /></label>
          <label className="field"><span>Per page (₹)</span><input required type="number" min="0.25" step="0.25" value={draft.pricePerPage} onChange={(e) => setDraft({ ...draft, pricePerPage: e.target.value })} /></label>
        </div>
        <button className="btn btn-primary" disabled={busy || !draft.name.trim() || !(Number(draft.pricePerPage) > 0)}>
          {busy ? 'Adding…' : 'Add option'}
        </button>
      </form>
    </section>
  )
}
