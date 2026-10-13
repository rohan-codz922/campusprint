// API client. The UI only ever talks to the server through the `api` object in this file.
// The server address comes from VITE_API_URL (set at build time). It is not a secret:
// it is the public address of the API. There are no passwords or keys anywhere in the website.
const BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

const TOKEN_KEY = 'cp_token'
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.status = status
    this.code = code // e.g. EMAIL_NOT_VERIFIED
  }
}

async function http(path, { method = 'GET', json, form, auth = true, blob = false } = {}) {
  if (!BASE) throw new ApiError('The website is not connected to a server yet.', 0)
  const headers = {}
  if (auth && tokenStore.get()) headers.Authorization = `Bearer ${tokenStore.get()}`
  let body
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
  } else if (form) {
    body = form // the browser sets the multipart boundary itself
  }

  let res
  try {
    res = await fetch(`${BASE}${path}`, { method, headers, body })
  } catch {
    throw new ApiError('Cannot reach the server. Check your connection and try again.', 0)
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    let code
    try {
      const data = await res.json()
      if (data.message) message = data.message
      code = data.code
    } catch { /* body was not JSON */ }
    if (res.status === 401 && auth) tokenStore.clear()
    throw new ApiError(message, res.status, code)
  }
  if (blob) return res.blob()
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  register: (data) => http('/api/auth/register', { method: 'POST', json: data, auth: false }),
  verifyEmail: (data) => http('/api/auth/verify-email', { method: 'POST', json: data, auth: false }),
  resendCode: (email) => http('/api/auth/resend-code', { method: 'POST', json: { email }, auth: false }),
  login: (data) => http('/api/auth/login', { method: 'POST', json: data, auth: false }),
  me: () => http('/api/auth/me'),
  forgotPassword: (email) => http('/api/auth/forgot-password', { method: 'POST', json: { email }, auth: false }),
  resetPassword: (data) => http('/api/auth/reset-password', { method: 'POST', json: data, auth: false }),

  getServices: () => http('/api/services', { auth: false }),
  createService: (data) => http('/api/services', { method: 'POST', json: data }),
  updateService: (id, data) => http(`/api/services/${id}`, { method: 'PUT', json: data }),

  getPaymentSettings: () => http('/api/settings/payment'),
  savePaymentSettings: (data) => http('/api/settings/payment', { method: 'PUT', json: data }),

  createRequest: (formData) => http('/api/requests', { method: 'POST', form: formData }),
  myRequests: () => http('/api/requests/mine'),
  allRequests: () => http('/api/requests'),
  setStatus: (id, status) => http(`/api/requests/${id}/status`, { method: 'PATCH', json: { status } }),
  setPayment: (id, decision) => http(`/api/requests/${id}/payment`, { method: 'PATCH', json: { decision } }),
  cancelRequest: (id) => http(`/api/requests/${id}/cancel`, { method: 'PATCH' }),
  downloadFile: (id) => http(`/api/requests/${id}/file`, { blob: true }),
  downloadProof: (id) => http(`/api/requests/${id}/payment-proof`, { blob: true }),

  stats: () => http('/api/admin/stats'),
}
