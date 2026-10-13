import Icon from './Icon'

export const STATUS_LABEL = {
  PENDING: 'Submitted',
  PRINTING: 'Printing',
  READY: 'Ready to collect',
  COLLECTED: 'Collected',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

const STEPS = ['PENDING', 'PRINTING', 'READY', 'COLLECTED']

// Chip: colour + text + icon, so state never depends on colour alone.
export function StatusBadge({ status }) {
  const icon = { PENDING: 'clock', PRINTING: 'printer', READY: 'inbox', COLLECTED: 'check', REJECTED: 'x', CANCELLED: 'x' }[status]
  return (
    <span className={`chip chip-${status.toLowerCase()}`}>
      <Icon name={icon} size={14} />
      {STATUS_LABEL[status]}
    </span>
  )
}

// Four-stage progress line shown on each student job.
export default function StatusTrack({ status }) {
  if (status === 'REJECTED' || status === 'CANCELLED') return <StatusBadge status={status} />
  // A collected job is finished, so every step (including the last) shows as complete.
  const current = status === 'COLLECTED' ? STEPS.length : STEPS.indexOf(status)
  return (
    <ol className="track" aria-label={`Progress: ${STATUS_LABEL[status]}`}>
      {STEPS.map((s, i) => (
        <li key={s} className={i < current ? 'done' : i === current ? 'now' : ''} aria-current={i === current ? 'step' : undefined}>
          <span className="dot">{i < current && <Icon name="check" size={11} />}</span>
          <span className="lbl">{STATUS_LABEL[s]}</span>
        </li>
      ))}
    </ol>
  )
}

// How the student is paying, and where that payment stands.
export function PaymentChip({ status }) {
  const map = {
    NONE: ['none', 'Pay at the counter', 'rupee'],
    SUBMITTED: ['review', 'Payment under review', 'clock'],
    VERIFIED: ['ok', 'Payment verified', 'check'],
    REJECTED: ['bad', 'Payment rejected', 'x'],
  }
  const [kind, label, icon] = map[status] || map.NONE
  return (
    <span className={`pay pay-${kind}`}>
      <Icon name={icon} size={14} />
      {label}
    </span>
  )
}
