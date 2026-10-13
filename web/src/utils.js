export const rupees = (n) =>
  '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: Number.isInteger(+n) ? 0 : 2, maximumFractionDigits: 2 })

export const when = (iso) =>
  new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })

export const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`

// Estimated price. The server recalculates this itself; the browser value is only a preview.
export const estimate = (service, pages, copies) =>
  service && pages ? +(service.pricePerPage * pages * Math.max(0, copies)).toFixed(2) : 0

// Reads the real page count of a PDF in the browser using Mozilla's PDF.js (loaded lazily).
async function countPdfPages(file) {
  try {
    const pdfjs = await import('pdfjs-dist')
    const worker = (await import('pdfjs-dist/build/pdf.worker.min.mjs?url')).default
    pdfjs.GlobalWorkerOptions.workerSrc = worker
    const doc = await pdfjs.getDocument({ data: await file.arrayBuffer() }).promise
    const n = doc.numPages
    doc.destroy()
    return n
  } catch {
    return null // unreadable or password-protected
  }
}

// Page count for the price preview. The server counts again and its number is the one that counts.
export async function countPages(file) {
  if (/\.pdf$/i.test(file.name)) return countPdfPages(file)
  if (/\.(jpe?g|png)$/i.test(file.name)) return 1
  return null
}

export const formatSize = (bytes) =>
  bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`

// Link that opens a UPI app with the payee and the exact amount already filled in.
export const upiLink = ({ upiId, payeeName }, amount, note = 'CampusPrint') =>
  `upi://pay?pa=${upiId}&pn=${encodeURIComponent(payeeName)}&am=${Number(amount).toFixed(2)}&cu=INR&tn=${encodeURIComponent(note)}`

// Reads the UPI ID and shop name out of a photo or screenshot of a UPI QR code.
export async function readUpiQr(file) {
  const [{ default: jsQR }, bitmap] = await Promise.all([import('jsqr'), createImageBitmap(file)])
  const scale = Math.min(1, 1200 / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  const ctx = canvas.getContext('2d')
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height)
  const found = jsQR(pixels.data, pixels.width, pixels.height)
  if (!found) return null
  try {
    const url = new URL(found.data)
    if (url.protocol !== 'upi:') return null
    return { upiId: url.searchParams.get('pa') || '', payeeName: url.searchParams.get('pn') || '' }
  } catch {
    return null
  }
}
