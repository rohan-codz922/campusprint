export const rupees = (n) =>
  '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: Number.isInteger(+n) ? 0 : 2, maximumFractionDigits: 2 })

export const when = (iso) =>
  new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })

// Estimated price. The server recalculates this itself; the browser value is only a preview.
export const estimate = (service, pages, copies) =>
  service ? +(service.pricePerPage * Math.max(0, pages) * Math.max(0, copies)).toFixed(2) : 0

// Reads the real page count of a PDF in the browser using Mozilla's PDF.js.
// The library is loaded lazily (dynamic import), so it only downloads when a PDF is chosen.
export async function countPdfPages(file) {
  if (!file || !/\.pdf$/i.test(file.name)) return null
  try {
    const pdfjs = await import('pdfjs-dist')
    const worker = (await import('pdfjs-dist/build/pdf.worker.min.mjs?url')).default
    pdfjs.GlobalWorkerOptions.workerSrc = worker
    const doc = await pdfjs.getDocument({ data: await file.arrayBuffer() }).promise
    const n = doc.numPages
    doc.destroy()
    return n
  } catch {
    return null // unreadable or encrypted PDF: the student types the number instead
  }
}

export const formatSize = (bytes) =>
  bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
