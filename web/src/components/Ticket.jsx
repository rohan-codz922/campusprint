import { rupees } from '../utils'

// The "job ticket": a price slip with a perforated tear-off, the signature element of the UI.
export default function Ticket({ service, pages, copies, fileName, total, note }) {
  const sheets = Math.max(0, pages) * Math.max(0, copies)
  return (
    <article className="ticket" aria-label="Price estimate">
      <header className="ticket-h">
        <h3>Job ticket</h3>
        <span>Estimate</span>
      </header>

      <dl className="ticket-rows">
        {fileName !== undefined && (
          <div><dt>File</dt><dd className="clip" title={fileName || undefined}>{fileName || 'Not attached yet'}</dd></div>
        )}
        <div><dt>Print option</dt><dd>{service ? service.name : 'Choose one'}</dd></div>
        <div><dt>Rate</dt><dd>{service ? `${rupees(service.pricePerPage)} per page` : '–'}</dd></div>
        <div><dt>Pages × copies</dt><dd>{pages || 0} × {copies || 0} = {sheets} {sheets === 1 ? 'page' : 'pages'}</dd></div>
      </dl>

      <div className="tear" aria-hidden="true" />

      <footer className="ticket-f">
        <span>Total to pay at the desk</span>
        <strong className="amount">{rupees(total)}</strong>
      </footer>
      {note && <p className="ticket-note">{note}</p>}
    </article>
  )
}
