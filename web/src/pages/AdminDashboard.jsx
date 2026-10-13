import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { useToast } from '../components/Toast'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import UpiQr from '../components/UpiQr'
import { StatusBadge, PaymentChip, STATUS_LABEL } from '../components/StatusTrack'
import { plural, readUpiQr, rupees, when } from '../utils'

const SETTABLE = ['PENDING', 'PRINTING', 'READY', 'COLLECTED', 'REJECTED']
const NEEDS_PAYMENT_FIRST = ['PRINTING', 'READY', 'COLLECTED']
const FILTERS = [
  { key: 'ACTIVE', label: 'To do', test: (r) => ['PENDING', 'PRINTING', 'READY'].includes(r.status) },
  { key: 'PAY', label: 'Payments to check', test: (r) => r.paymentStatus === 'SUBMITTED' },
  { key: 'COLLECTED', label: 'Collected', test: (r) => r.status === 'COLLECTED' },
  { key: 'CLOSED', label: 'Rejected or cancelled', test: (r) => ['REJECTED', 'CANCELLED'].includes(r.status) },
  { key: 'ALL', label: 'All jobs', test: () => true },
]

export default function AdminDashboard() {
  const toast = useToast()
  const [tab, setTab] = useState('queue')
  const [requests, setRequests] = useState([])
  const [services, setServices] = useState([])
  const [settings, setSettings] = useState(null)
  const [stats, setStats] = useState(null)
  const [filter, setFilter] = useState('ACTIVE')
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [proofJob, setProofJob] = useState(null)

  const load = useCallback(async () => {
    try {
      const [r, s, st, ps] = await Promise.all([api.allRequests(), api.getServices(), api.stats(), api.getPaymentSettings()])
      setRequests(r); setServices(s); setStats(st); setSettings(ps)
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
    } catch (e) { toast(e.message, 'error') } finally {
      setSavingId(null)
      await load()
    }
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

  const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })

  return (
    <main id="main" className="wrap page">
      <header className="dash-h">
        <div>
          <h1>Print desk</h1>
          <p className="muted">{today}</p>
        </div>
        {!settings?.configured && !loading && (
          <button className="btn btn-quiet" onClick={() => setTab('payment')}><Icon name="rupee" size={18} /> Add your payment QR</button>
        )}
      </header>

      <section className="strip" aria-label="At a glance">
        <div><span className="n">{stats?.byStatus?.PENDING || 0}</span><span>Waiting to print</span></div>
        <div><span className="n">{stats?.byStatus?.PRINTING || 0}</span><span>On the printer</span></div>
        <div><span className="n">{stats?.byStatus?.READY || 0}</span><span>Ready for pickup</span></div>
        <div className={stats?.paymentsToVerify ? 'attn' : ''}><span className="n">{stats?.paymentsToVerify || 0}</span><span>Payments to check</span></div>
        <div><span className="n">{rupees(stats?.revenue || 0)}</span><span>Collected so far</span></div>
      </section>

      <div className="tabs" role="tablist" aria-label="Print desk sections">
        {[['queue', 'Print queue'], ['prices', 'Price list'], ['payment', 'Payment QR']].map(([k, label]) => (
          <button key={k} role="tab" aria-selected={tab === k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{label}</button>
        ))}
      </div>

      {tab === 'queue' && (
        <section aria-label="Print queue">
          <div className="filters" role="group" aria-label="Filter jobs">
            {FILTERS.map((f) => (
              <button key={f.key} className={`pill ${filter === f.key ? 'on' : ''}`} aria-pressed={filter === f.key} onClick={() => setFilter(f.key)}>
                {f.label} <span>{counts[f.key]}</span>
              </button>
            ))}
          </div>

          {loading ? (
            <ul className="jobs-admin" role="status">
              <li className="sr">Loading the queue…</li>
              <li className="skel-card" aria-hidden="true" /><li className="skel-card" aria-hidden="true" />
            </ul>
          ) : visible.length === 0 ? (
            <div className="empty"><Icon name="inbox" size={28} /><p><strong>Nothing here.</strong> Jobs that match this filter will appear in this list.</p></div>
          ) : (
            <ul className="jobs-admin">
              {visible.map((r) => {
                const blocked = r.paymentRequired && r.paymentStatus !== 'VERIFIED'
                return (
                  <li key={r.id} className={`jrow ${r.paymentStatus === 'SUBMITTED' ? 'needs-check' : ''}`}>
                    <div className="j-main">
                      <span className="j-meta"><span className="jobno">Job #{r.id}</span><span className="muted small">{when(r.createdAt)}</span></span>
                      <strong className="clip" title={r.fileName}>{r.fileName}</strong>
                      {r.notes && <span className="note">“{r.notes}”</span>}
                    </div>

                    <div className="j-info">
                      <span className="j-who"><strong>{r.student.name}</strong><span className="muted small">{r.student.rollNo ? `Roll ${r.student.rollNo}` : r.student.email}</span></span>
                      <span className="j-print">{r.service.name}<span className="muted small">{plural(r.pages, 'page')} × {r.copies}</span></span>
                    </div>

                    <div className="j-right">
                      <div className="j-money">
                        <strong className="j-total">{rupees(r.totalPrice)}</strong>
                        <PaymentChip status={r.paymentStatus} />
                      </div>
                      <div className="j-controls">
                        {r.status === 'CANCELLED' ? <StatusBadge status="CANCELLED" /> : (
                          <div className="j-status">
                            <select aria-label={`Status for job ${r.id}`} value={r.status} disabled={savingId === r.id}
                              onChange={(e) => changeStatus(r, e.target.value)} className={`status-select s-${r.status.toLowerCase()}`}>
                              {SETTABLE.map((s) => (
                                <option key={s} value={s} disabled={blocked && NEEDS_PAYMENT_FIRST.includes(s)}>{STATUS_LABEL[s]}</option>
                              ))}
                            </select>
                            {blocked && r.status !== 'REJECTED' && <span className="hint">Verify payment first</span>}
                          </div>
                        )}
                        <div className="acts">
                          <button className="btn btn-quiet btn-icon" onClick={() => download(r)} aria-label={`Download ${r.fileName}`} title="Download file"><Icon name="download" size={18} /><span className="lbl">Download</span></button>
                          {r.hasPaymentProof && (
                            <button className={`btn ${r.paymentStatus === 'SUBMITTED' ? 'btn-primary' : 'btn-quiet'}`} onClick={() => setProofJob(r)}>
                              <Icon name="eye" size={18} /> {r.paymentStatus === 'SUBMITTED' ? 'Check payment' : 'Payment'}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      )}

      {tab === 'prices' && <PriceList services={services} reload={load} toast={toast} />}
      {tab === 'payment' && <PaymentSettingsPanel settings={settings} reload={load} toast={toast} />}

      <ProofModal job={proofJob} onClose={() => setProofJob(null)} onDone={async () => { setProofJob(null); await load() }} toast={toast} />
    </main>
  )
}

// Shows the student's payment screenshot with Verify / Reject buttons.
function ProofModal({ job, onClose, onDone, toast }) {
  const [url, setUrl] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!job) return
    let alive = true
    let objectUrl = ''
    setUrl('')
    api.downloadProof(job.id)
      .then((blob) => { objectUrl = URL.createObjectURL(blob); if (alive) setUrl(objectUrl) })
      .catch((e) => toast(e.message, 'error'))
    return () => { alive = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [job, toast])

  async function decide(decision) {
    setBusy(true)
    try {
      await api.setPayment(job.id, decision)
      toast(decision === 'VERIFIED' ? 'Payment verified. You can start printing.' : 'Payment rejected and the job closed.')
      await onDone()
    } catch (e) { toast(e.message, 'error') } finally { setBusy(false) }
  }

  return (
    <Modal open={!!job} onClose={onClose} title={job ? `Payment for job #${job.id}` : ''}>
      {job && (
        <div className="proof">
          <dl className="proof-facts">
            <div><dt>Expected amount</dt><dd><strong>{rupees(job.totalPrice)}</strong></dd></div>
            <div><dt>Student</dt><dd>{job.student.name}</dd></div>
            <div><dt>Status</dt><dd><PaymentChip status={job.paymentStatus} /></dd></div>
          </dl>
          {url ? <img className="proof-img" src={url} alt="Payment screenshot uploaded by the student" /> : <p className="muted">Loading screenshot…</p>}
          {job.paymentStatus === 'SUBMITTED' ? (
            <div className="row">
              <button className="btn btn-quiet btn-danger" disabled={busy} onClick={() => decide('REJECTED')}>Reject payment</button>
              <button className="btn btn-primary btn-lg" disabled={busy} onClick={() => decide('VERIFIED')}>Payment received, verify</button>
            </div>
          ) : <p className="muted small">This payment has already been checked.</p>}
        </div>
      )}
    </Modal>
  )
}

// The shop uploads its UPI QR once. We read the UPI ID and name out of it, and from then on
// every student gets a fresh QR with the exact amount filled in.
function PaymentSettingsPanel({ settings, reload, toast }) {
  const [payeeName, setPayeeName] = useState(settings?.payeeName || '')
  const [upiId, setUpiId] = useState(settings?.upiId || '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const valid = /^[A-Za-z0-9.\-_]{2,256}@[A-Za-z0-9]{2,64}$/.test(upiId.trim()) && payeeName.trim().length > 0

  async function onQr(file) {
    setError('')
    if (!file) return
    try {
      const found = await readUpiQr(file)
      if (!found || !found.upiId) {
        setError('We could not find a UPI QR in that image. Try a clearer screenshot of the shop QR, or type the UPI ID below.')
        return
      }
      setUpiId(found.upiId)
      if (found.payeeName) setPayeeName(found.payeeName)
      toast('QR read. Check the details below, then save.')
    } catch {
      setError('That image could not be read. Use a PNG or JPG.')
    }
  }

  async function save(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await api.savePaymentSettings({ payeeName: payeeName.trim(), upiId: upiId.trim() })
      toast('Payment details saved. Students now see this QR.')
      await reload()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  return (
    <section aria-label="Payment QR" className="split paysettings">
      <form className="sheet form" onSubmit={save}>
        <h2>Payment QR</h2>
        <p className="muted">Upload the shop&apos;s UPI QR. We read the UPI ID and name from it. The image itself is not stored. Change it any time the shop gets a new QR.</p>
        <div className="drop">
          <input id="qrfile" type="file" accept="image/png,image/jpeg" onChange={(e) => onQr(e.target.files[0])} />
          <label htmlFor="qrfile"><Icon name="upload" size={26} /><span><strong>Upload the shop QR</strong><small>PNG or JPG screenshot of the QR</small></span></label>
        </div>
        <label className="field"><span>Shop name</span>
          <input value={payeeName} maxLength={100} onChange={(e) => setPayeeName(e.target.value)} required />
        </label>
        <label className="field"><span>UPI ID</span>
          <input value={upiId} maxLength={256} onChange={(e) => setUpiId(e.target.value)} placeholder="shop@bank" required />
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="btn btn-primary" disabled={busy || !valid}>{busy ? 'Saving…' : 'Save payment details'}</button>
      </form>
      <div className="sheet qr-preview">
        <h2>What students see</h2>
        {valid ? (
          <>
            <UpiQr settings={{ upiId: upiId.trim(), payeeName: payeeName.trim() }} amount={100} size={200} />
            <p className="muted small">Preview for ₹100. Each student gets their own amount.</p>
          </>
        ) : <p className="muted">Add a QR or type the UPI ID to see a preview.</p>}
      </div>
    </section>
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
        <table className="table stack">
          <thead><tr><th>Option</th><th>Per page (₹)</th><th>Visible to students</th></tr></thead>
          <tbody>
            {services.map((s) => (
              <tr key={s.id}>
                <td data-label="Option"><div className="cell-stack"><strong>{s.name}</strong><span className="muted small">{s.description}</span></div></td>
                <td data-label="Per page (₹)">
                  <div className="rate">
                    <input aria-label={`Rate for ${s.name}`} type="number" min="0.25" step="0.25"
                      value={rates[s.id] ?? s.pricePerPage} onChange={(e) => setRates({ ...rates, [s.id]: e.target.value })} />
                    {rates[s.id] !== undefined && Number(rates[s.id]) !== s.pricePerPage && (
                      <button className="btn btn-primary" onClick={() => save(s)}>Save rate</button>
                    )}
                  </div>
                </td>
                <td data-label="Visible to students">
                  <div className="sw-wrap">
                    <button className="switch" role="switch" aria-checked={s.active} aria-label={`Show ${s.name} to students`} onClick={() => toggle(s)}>
                      <span className="knob" />
                    </button>
                    <span className="small muted">{s.active ? 'Visible' : 'Hidden'}</span>
                  </div>
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
