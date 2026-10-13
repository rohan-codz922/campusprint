import { useId, useState } from 'react'
import Icon from './Icon'

// A password box with a show/hide button. The label sits outside the button so the HTML stays valid.
export default function PasswordField({ label, value, onChange, autoComplete, help }) {
  const id = useId()
  const [show, setShow] = useState(false)
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="pw">
        <input id={id} type={show ? 'text' : 'password'} autoComplete={autoComplete} autoCapitalize="none" spellCheck={false}
          required value={value} onChange={onChange} aria-describedby={help ? `${id}-help` : undefined} />
        <button type="button" className="pw-toggle" onClick={() => setShow((s) => !s)}
          aria-label={show ? 'Hide password' : 'Show password'} aria-pressed={show}>
          <Icon name={show ? 'eyeoff' : 'eye'} size={20} />
        </button>
      </div>
      {help && <small id={`${id}-help`}>{help}</small>}
    </div>
  )
}
