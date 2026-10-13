import { useEffect, useRef } from 'react'
import Icon from './Icon'

// Native <dialog>: gives us focus trapping, Escape-to-close and a backdrop for free.
export default function Modal({ open, onClose, title, children }) {
  const ref = useRef(null)
  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (open && !d.open) d.showModal()
    if (!open && d.open) d.close()
  }, [open])

  return (
    <dialog ref={ref} className="modal" aria-label={title} onClose={onClose}
      onClick={(e) => { if (e.target === ref.current) onClose() }}>
      {open && (
        <div className="modal-in">
          <header>
            <h2>{title}</h2>
            <button className="btn btn-quiet btn-icon" onClick={onClose} aria-label="Close"><Icon name="x" size={18} /></button>
          </header>
          {children}
        </div>
      )}
    </dialog>
  )
}
