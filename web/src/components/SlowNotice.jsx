import { useEffect, useState } from 'react'

// The free server sleeps when nobody uses it. If a request is slow, say why instead of leaving people guessing.
export default function SlowNotice() {
  const [slow, setSlow] = useState(0)
  useEffect(() => {
    const on = (e) => setSlow(e.detail)
    window.addEventListener('cp:slow', on)
    return () => window.removeEventListener('cp:slow', on)
  }, [])
  if (!slow) return null
  return (
    <div className="slow" role="status">
      <span className="spin" aria-hidden="true" />
      <span>Still working. The free server may be waking up, which can take up to a minute.</span>
    </div>
  )
}
