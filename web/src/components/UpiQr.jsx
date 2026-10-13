import { useEffect, useState } from 'react'
import { upiLink } from '../utils'

// Draws a UPI QR code for exactly this amount. Scanning it opens the payment app
// with the shop and the amount already filled in.
export default function UpiQr({ settings, amount, size = 220 }) {
  const [src, setSrc] = useState('')
  useEffect(() => {
    let alive = true
    import('qrcode')
      .then(({ default: QRCode }) =>
        QRCode.toDataURL(upiLink(settings, amount), { width: size * 2, margin: 1, errorCorrectionLevel: 'M' }))
      .then((url) => alive && setSrc(url))
      .catch(() => alive && setSrc(''))
    return () => { alive = false }
  }, [settings.upiId, settings.payeeName, amount, size])

  if (!src) return <div className="qr qr-wait" style={{ width: size, height: size }} aria-hidden="true" />
  return <img className="qr" src={src} width={size} height={size} alt={`QR code to pay ${amount} rupees to ${settings.payeeName}`} />
}
