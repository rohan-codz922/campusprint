import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useToast } from '../components/Toast'
import Icon from '../components/Icon'
import Ticket from '../components/Ticket'
import UpiQr from '../components/UpiQr'
import { countPages, estimate, formatSize, plural, rupees, upiLink } from '../utils'

const MAX_DOC = 10 * 1024 * 1024
const MAX_PROOF = 3 * 1024 * 1024

// Three screens: (1) details, (2) pay, (3) upload the payment screenshot.
// Small jobs can skip payment and pay at the counter. Big jobs must pay first.
export default function NewPrint() {
  const navigate = useNavigate()
  const toast = useToast()
  const docInput = useRef(null)

  const [step, setStep] = useState('details')
  const [services, setServices] = useState([])
  const [settings, setSettings] = useState({ configured: false, payeeName: '', upiId: '', requiredPages: 20 })
  const [file, setFile] = useState(null)
  const [pages, setPages] = useState(null)
  const [counting, setCounting] = useState(false)
  const [serviceId, setServiceId] = useState('')
  const [copies, setCopies] = useState(1)
  const [notes, setNotes] = useState('')
  const [payChoice, setPayChoice] = useState('counter')
  const [proof, setProof] = useState(null)
  const [proofUrl, setProofUrl] = useState('')
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let alive = true
    Promise.all([api.getServices(), api.getPaymentSettings()])
      .then(([s, p]) => {
        if (!alive) return
        const active = s.filter((x) => x.active)
        setServices(active)
        setServiceId(active[0] ? String(active[0].id) : '')
        setSettings(p)
      })
      .catch((e) => toast(e.message, 'error'))
    return () => { alive = false }
  }, [toast])

  useEffect(() => () => { if (proofUrl) URL.revokeObjectURL(proofUrl) }, [proofUrl])

  const service = services.find((s) => String(s.id) === serviceId)
  const sheets = pages ? pages * copies : 0
  const total = estimate(service, pages, copies)
  const required = sheets >= settings.requiredPages          // must pay before printing
  const payOnline = required || payChoice === 'online'

  async function pickDoc(f) {
    setError('')
    if (!f) return
    if (!/\.(pdf|jpe?g|png)$/i.test(f.name)) {
      setError('Only PDF, JPG or PNG files can be printed. Save Word or PowerPoint files as PDF first.')
      return
    }
    if (f.size > MAX_DOC) { setError(`That file is ${formatSize(f.size)}. The limit is 10 MB.`); return }
    setFile(f); setPages(null); setCounting(true)
    const n = await countPages(f)
    setCounting(false)
    if (n) setPages(n)
    else setError('We could not read this file. If it is a PDF, it may be password-protected or damaged.')
  }

  function pickProof(f) {
    setError('')
    if (!f) return
    if (!/\.(jpe?g|png)$/i.test(f.name)) { setError('Upload the payment screenshot as a PNG or JPG image.'); return }
    if (f.size > MAX_PROOF) { setError(`That screenshot is ${formatSize(f.size)}. The limit is 3 MB.`); return }
    setProof(f)
    setProofUrl(URL.createObjectURL(f))
  }

  function goNext(e) {
    e.preventDefault()
    setError('')
    if (!file || !pages) { setError('Attach the file you want printed.'); return }
    if (!service) { setError('Choose a print option.'); return }
    if (payOnline) setStep('pay')
    else submit()
  }

  async function submit() {
    setError(''); setBusy(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('serviceId', service.id)
      fd.append('copies', copies)
      fd.append('notes', notes)
      if (payOnline && proof) fd.append('paymentProof', proof)
      await api.createRequest(fd)
      toast(payOnline ? 'Sent. The print desk will check your payment.' : 'Sent to the print desk.')
      navigate('/student', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  const stepNo = { details: 1, pay: 2, proof: 3 }[step]

  return (
    <main id="main" className="wrap page narrow-page">
      <Link to="/student" className="backlink"><Icon name="back" size={18} /> Your prints</Link>
      <header className="page-h">
        <h1>{{ details: 'Add a print', pay: 'Pay for your print', proof: 'Upload your payment screenshot' }[step]}</h1>
        {payOnline && (
          <ol className="steps-bar" aria-label="Progress">
            {['Details', 'Pay', 'Screenshot'].map((l, i) => (
              <li key={l} className={i + 1 < stepNo ? 'done' : i + 1 === stepNo ? 'now' : ''}>{l}</li>
            ))}
          </ol>
        )}
      </header>

      {step === 'details' && (
        <div className="split">
          <form className="sheet form" onSubmit={goNext} noValidate aria-label="New print">
            <div
              className={`drop ${dragging ? 'is-over' : ''} ${file ? 'has-file' : ''}`}
              onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => { e.preventDefault(); setDragging(false); pickDoc(e.dataTransfer.files[0]) }}
            >
              <input ref={docInput} id="file" type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={(e) => pickDoc(e.target.files[0])} />
              <label htmlFor="file">
                <Icon name={file ? 'file' : 'upload'} size={26} />
                {file ? (
                  <span>
                    <strong>{file.name}</strong>
                    <small>{counting ? 'Counting pages…' : pages ? `${plural(pages, 'page')} · ${formatSize(file.size)} · click to replace` : formatSize(file.size)}</small>
                  </span>
                ) : (
                  <span><strong>Choose a file or drop it here</strong><small>PDF, JPG or PNG · up to 10 MB · Word files: save as PDF first</small></span>
                )}
              </label>
            </div>

            <label className="field"><span>Print option</span>
              <select value={serviceId} onChange={(e) => setServiceId(e.target.value)}>
                {services.map((s) => <option key={s.id} value={s.id}>{s.name} · {rupees(s.pricePerPage)} per page</option>)}
              </select>
            </label>

            <label className="field"><span>Copies</span>
              <input type="number" min="1" max="100" value={copies} onChange={(e) => setCopies(Math.min(100, Math.max(1, +e.target.value || 1)))} />
              <small>We count the pages from your file, so the price is exact.</small>
            </label>

            <label className="field"><span>Note for the desk <em>(optional)</em></span>
              <textarea rows="2" maxLength="300" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="For example: staple the corner" />
            </label>

            <fieldset className="paychoice">
              <legend>How will you pay?</legend>
              <label className={!payOnline ? 'on' : ''}>
                <input type="radio" name="pay" checked={!payOnline} disabled={required} onChange={() => setPayChoice('counter')} />
                <span><strong>At the counter</strong><small>When you collect your pages</small></span>
              </label>
              <label className={payOnline ? 'on' : ''}>
                <input type="radio" name="pay" checked={payOnline} onChange={() => setPayChoice('online')} />
                <span><strong>Online now (UPI)</strong><small>Scan a QR, then upload the screenshot</small></span>
              </label>
            </fieldset>
            {required && (
              <p className="notice">Prints of {settings.requiredPages} or more pages ({plural(sheets, 'page')} here) are paid first. The print desk starts once your payment is checked.</p>
            )}

            {error && <p className="inline-error" role="alert">{error}</p>}
            <button className="btn btn-primary btn-lg" disabled={busy || counting || !pages}>
              {busy ? 'Sending…' : payOnline ? 'Continue to payment' : 'Send to print desk'}
            </button>
          </form>

          <div className="side-sticky">
            <Ticket service={service} pages={pages} copies={copies} fileName={file ? file.name : ''} total={total}
              note={payOnline ? 'Pay online, then upload the screenshot.' : 'You pay at the counter when you collect your pages.'} />
          </div>
        </div>
      )}

      {step === 'pay' && (
        <section className="sheet paybox">
          <div className="pay-amount">
            <span className="muted">Amount to pay</span>
            <strong>{rupees(total)}</strong>
            <span className="muted small">{plural(pages, 'page')} × {copies} · {service.name}</span>
          </div>

          {settings.configured ? (
            <div className="pay-qr">
              <UpiQr settings={settings} amount={total} />
              <p className="muted small">Scan with any UPI app. The amount is filled in for you.</p>
              <a className="btn btn-quiet" href={upiLink(settings, total)}>Open in UPI app</a>
              <dl className="payee">
                <div><dt>Paying to</dt><dd>{settings.payeeName}</dd></div>
                <div><dt>UPI ID</dt><dd>{settings.upiId}</dd></div>
              </dl>
            </div>
          ) : (
            <div className="pay-qr pay-empty">
              <p><strong>The print desk has not added a payment QR yet.</strong></p>
              <p className="muted small">Payment is not set up for this shop at the moment. When it is, a QR code will appear here.</p>
            </div>
          )}

          <div className="row">
            <button className="btn btn-quiet" onClick={() => setStep('details')}>Back</button>
            <button className="btn btn-primary btn-lg" onClick={() => { setError(''); setStep('proof') }}>I have paid</button>
          </div>
        </section>
      )}

      {step === 'proof' && (
        <section className="sheet paybox">
          <p className="muted">Upload a screenshot that shows the payment went through. The print desk checks it before printing.</p>
          <div className={`drop ${proof ? 'has-file' : ''}`}>
            <input id="proof" type="file" accept=".jpg,.jpeg,.png" onChange={(e) => pickProof(e.target.files[0])} />
            <label htmlFor="proof">
              <Icon name={proof ? 'image' : 'upload'} size={26} />
              {proof
                ? <span><strong>{proof.name}</strong><small>{formatSize(proof.size)} · click to replace</small></span>
                : <span><strong>Choose your payment screenshot</strong><small>PNG or JPG · up to 3 MB</small></span>}
            </label>
          </div>
          {proofUrl && <img className="proof-preview" src={proofUrl} alt="Preview of your payment screenshot" />}
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="row">
            <button className="btn btn-quiet" onClick={() => setStep('pay')} disabled={busy}>Back</button>
            <button className="btn btn-primary btn-lg" onClick={submit} disabled={busy || !proof}>
              {busy ? 'Sending…' : 'Submit print request'}
            </button>
          </div>
        </section>
      )}
    </main>
  )
}
