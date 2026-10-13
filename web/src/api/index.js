// API client. The UI only ever imports `api` from this file.
//  - VITE_API_URL set   -> talks to the real Kotlin + Spring Boot backend over HTTP
//  - VITE_API_URL empty -> DEMO MODE: same methods, data kept in the browser (see mock.js)
import { mockApi } from './mock.js'

const BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
export const isDemo = BASE === ''

const TOKEN_KEY = 'cp_token'
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

async function http(path, { method = 'GET', json, form, auth = true, blob = false } = {}) {
  const headers = {}
  if (auth && tokenStore.get()) headers.Authorization = `Bearer ${tokenStore.get()}`
  let body
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
  } else if (form) {
    body = form // browser sets the multipart boundary itself
  }

  let res
  try {
    res = await fetch(`${BASE}${path}`, { method, headers, body })
  } catch {
    throw new Error('Cannot reach the server. Check your connection and try again.')
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const data = await res.json()
      if (data.message) message = data.message
    } catch { /* body was not JSON */ }
    if (res.status === 401 && auth) tokenStore.clear()
    throw new Error(message)
  }
  if (blob) return res.blob()
  if (res.status === 204) return null
  return res.json()
}

const realApi = {
  register: (data) => http('/api/auth/register', { method: 'POST', json: data, auth: false }),
  login: (data) => http('/api/auth/login', { method: 'POST', json: data, auth: false }),
  me: () => http('/api/auth/me'),
  forgotPassword: (email) => http('/api/auth/forgot-password', { method: 'POST', json: { email }, auth: false }),
  resetPassword: (data) => http('/api/auth/reset-password', { method: 'POST', json: data, auth: false }),

  getServices: () => http('/api/services', { auth: false }),
  createService: (data) => http('/api/services', { method: 'POST', json: data }),
  updateService: (id, data) => http(`/api/services/${id}`, { method: 'PUT', json: data }),

  createRequest: (formData) => http('/api/requests', { method: 'POST', form: formData }),
  myRequests: () => http('/api/requests/mine'),
  allRequests: () => http('/api/requests'),
  setStatus: (id, status) => http(`/api/requests/${id}/status`, { method: 'PATCH', json: { status } }),
  cancelRequest: (id) => http(`/api/requests/${id}/cancel`, { method: 'PATCH' }),
  downloadFile: (id) => http(`/api/requests/${id}/file`, { blob: true }),

  stats: () => http('/api/admin/stats'),
}

export const api = isDemo ? mockApi : realApi
