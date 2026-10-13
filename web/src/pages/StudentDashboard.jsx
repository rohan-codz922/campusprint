import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import Icon from '../components/Icon'
import Ticket from '../components/Ticket'
import StatusTrack from '../components/StatusTrack'
import { countPdfPages, estimate, formatSize, rupees, when } from '../utils'

const MAX_BYTES = 10 * 1024 * 1024
const ACCEPT = '.pdf,.doc,.docx,.ppt,.pptx,.jpg,.jpeg,.png,.txt'

export default function StudentDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const fileInput = useRef(null)

  const [services, setServices] = useState([])
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)

  const [file, setFile] = useState(null)
  const [serviceId, setServiceId] = useState('')
  const [pages, setPages] = useState(1)
  const [copies, setCopies] = useState(1)
  const [notes, setNotes] = useState('')
  const [pagesGuessed, setPagesGuessed] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const [s, r] = await Promise.all([api.getServices(), api.myRequests()])
      const active = s.filter((x) => x.active)
      setServices(active)
      setServiceId((cur) => cur || (active[0] ? String(active[0].id) : ''))
      setRequests(r)
    } catch (e) {
      toast(e.message, 'error')
    } finally { setLoading(false) }
  }, [toast])

  useEffect(() => { load() }, [load])

  const service = services.find((s) => String(s.id) === serviceId)
  const total = estimate(service, pages, copies)

  async function pick(f) {
    setError('')
    if (!f) return
    if (f.size > MAX_BYTES) { setError(`That file is ${formatSize(f.size)}. The limit is 10 MB.`); return }
    setFile(f)
    const guess = await countPdfPages(f)
    if (guess) { setPages(guess); setPagesGuessed(true) } else setPagesGuessed(false)
  }

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!file) { setError('Attach the file you want printed.'); return }
    if (!service) { setError('Choose a print option.'); return }
    setBusy(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('serviceId', service.id)
      fd.append('pages', pages)
      fd.append('copies', copies)
      fd.append('notes', notes)
      await api.createRequest(fd)
      toast('Sent to the print desk.')
      setFile(null); setNotes(''); setPages(1); setCopies(1); setPagesGuessed(false)
      if (fileInput.current) fileInput.current.value = ''
      await load()
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  async function cancel(id) {
    try {
      await api.cancelRequest(id)
      toast('Print job cancelled.')
      await load()
    } catch (err) { toast(err.message, 'error') }
  }

  return (
    <main id="main" className="wrap page">
      <header className="page-h">
        <h1>Hello, {user.name.split(' ')[0]}</h1>
        <p className="muted">Send a file to the print desk and follow it from here.</p>
      </header>

      <div className="split">
        <form className="sheet form" onSubmit={submit} noValidate aria-label="New print job">
          <h2>New print job</h2>

          <div
            className={`drop ${dragging ? 'is-over' : ''} ${file ? 'has-file' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => { e.preventDefault(); setDragging(false); pick(e.dataTransfer.files[0]) }}
          >
            <input ref={fileInput} id="file" type="file" accept={ACCEPT} onChange={(e) => pick(e.target.files[0])} />
            <label htmlFor="file">
              <Icon name={file ? 'file' : 'upload'} size={26} />
              {file ? (
                <span><strong>{file.name}</strong><small>{formatSize(file.size)} · click to replace</small></span>
              ) : (
                <span><strong>Choose a file or drop it here</strong><small>PDF, Word, PowerPoint, image or text · up to 10 MB</small></span>
              )}
            </label>
          </div>

          <label className="field"><span>Print option</span>
            <select value={serviceId} onChange={(e) => setServiceId(e.target.value)}>
              {services.map((s) => <option key={s.id} value={s.id}>{s.name} · {rupees(s.pricePerPage)} per page</option>)}
            </select>
          </label>

          <div className="two">
            <label className="field"><span>Pages in the file</span>
              <input type="number" min="1" max="500" value={pages}
                onChange={(e) => { setPages(Math.max(1, +e.target.value)); setPagesGuessed(false) }} />
              {pagesGuessed && <small>Counted from your PDF. Correct it if it looks wrong.</small>}
            </label>
            <label className="field"><span>Copies</span>
              <input type="number" min="1" max="100" value={copies} onChange={(e) => setCopies(Math.max(1, +e.target.value))} />
            </label>
          </div>

          <label className="field"><span>Note for the desk <em>(optional)</em></span>
            <textarea rows="2" maxLength="300" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="For example: staple the corner, print pages 3 to 10 only" />
          </label>

          {error && <p className="inline-error" role="alert">{error}</p>}
          <button className="btn btn-primary btn-lg" disabled={busy}>{busy ? 'Sending…' : 'Send to print desk'}</button>
        </form>

        <div className="side-sticky">
          <Ticket service={service} pages={pages} copies={copies} fileName={file ? file.name : ''} total={total}
            note="You pay at the counter when you collect your pages." />
        </div>
      </div>

      <section className="jobs" aria-labelledby="jobs-h">
        <h2 id="jobs-h">Your print jobs</h2>
        {loading ? <p className="muted">Loading your jobs…</p> : requests.length === 0 ? (
          <div className="empty">
            <Icon name="printer" size={28} />
            <p><strong>No jobs yet.</strong> Attach a file above and it will show up here with its progress.</p>
          </div>
        ) : (
          <ul className="job-list">
            {requests.map((r) => (
              <li key={r.id} className="job">
                <div className="job-main">
                  <span className="job-no">Job {r.id}</span>
                  <strong className="clip">{r.fileName}</strong>
                  <span className="muted">{r.service.name} · {r.pages} × {r.copies} · {rupees(r.totalPrice)}</span>
                  <span className="muted small">Sent {when(r.createdAt)}</span>
                </div>
                <div className="job-track"><StatusTrack status={r.status} /></div>
                <div className="job-act">
                  {r.status === 'PENDING' && <button className="btn btn-quiet btn-danger" onClick={() => cancel(r.id)}>Cancel job</button>}
                  {r.status === 'READY' && <span className="pickup">Ready at the desk. Pay {rupees(r.totalPrice)}.</span>}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
