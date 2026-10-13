// DEMO MODE backend. Mirrors the REST API contract of the Spring Boot server,
// but stores everything in localStorage so the site works with no server.
// Business rules (price calculation, role checks, status rules) are repeated here
// on purpose so the demo behaves exactly like the real API.

const K = { users: 'cp_demo_users', services: 'cp_demo_services', requests: 'cp_demo_requests', token: 'cp_token' }
const delay = (ms = 260) => new Promise((r) => setTimeout(r, ms))
const read = (k, fallback) => {
  try { return JSON.parse(localStorage.getItem(k)) ?? fallback } catch { return fallback }
}
const write = (k, v) => localStorage.setItem(k, JSON.stringify(v))

function seed() {
  if (!read(K.users)) {
    write(K.users, [
      { id: 1, name: 'Print Desk Admin', email: 'admin@campus.edu', password: 'admin123', role: 'ADMIN', rollNo: null },
      { id: 2, name: 'Rohan Mishra', email: 'student@campus.edu', password: 'student123', role: 'STUDENT', rollNo: '24106120' },
    ])
  }
  if (!read(K.services)) {
    write(K.services, [
      { id: 1, name: 'Black & white, A4', description: 'Single-sided, 80 gsm paper', pricePerPage: 1.0, active: true },
      { id: 2, name: 'Black & white, double-sided', description: 'Both sides, 80 gsm paper', pricePerPage: 0.75, active: true },
      { id: 3, name: 'Colour, A4', description: 'Single-sided, 100 gsm paper', pricePerPage: 6.0, active: true },
      { id: 4, name: 'Colour, A3 poster', description: 'Posters and charts, 120 gsm paper', pricePerPage: 25.0, active: true },
    ])
  }
  if (!read(K.requests)) {
    const now = Date.now()
    const h = (n) => new Date(now - n * 3600e3).toISOString()
    const svc = read(K.services)
    const stu = { id: 2, name: 'Rohan Mishra', email: 'student@campus.edu', rollNo: '24106120' }
    const mk = (id, svcIdx, fileName, pages, copies, status, hoursAgo) => ({
      id, fileName, pages, copies, notes: '',
      totalPrice: +(svc[svcIdx].pricePerPage * pages * copies).toFixed(2),
      status, createdAt: h(hoursAgo), updatedAt: h(hoursAgo - 1),
      service: { id: svc[svcIdx].id, name: svc[svcIdx].name, pricePerPage: svc[svcIdx].pricePerPage },
      student: stu,
    })
    write(K.requests, [
      mk(1, 0, 'DBMS-assignment-3.pdf', 12, 1, 'COLLECTED', 70),
      mk(2, 2, 'mini-project-report.pdf', 40, 2, 'READY', 6),
      mk(3, 3, 'techfest-poster.pdf', 1, 3, 'PRINTING', 2),
    ])
  }
}
seed()

const publicUser = (u) => ({ id: u.id, name: u.name, email: u.email, role: u.role, rollNo: u.rollNo })
const fail = (message) => { throw new Error(message) }

function current() {
  const t = localStorage.getItem(K.token) || ''
  const id = Number(t.replace('demo.', ''))
  const u = read(K.users, []).find((x) => x.id === id)
  if (!u) fail('Your session has expired. Please sign in again.')
  return u
}
const requireAdmin = () => { const u = current(); if (u.role !== 'ADMIN') fail('Only the print desk can do this.'); return u }

export const mockApi = {
  async register({ name, email, password, rollNo }) {
    await delay()
    const users = read(K.users, [])
    if (users.some((u) => u.email.toLowerCase() === email.toLowerCase())) fail('An account with this email already exists.')
    const u = { id: Math.max(0, ...users.map((x) => x.id)) + 1, name, email, password, role: 'STUDENT', rollNo: rollNo || null }
    write(K.users, [...users, u])
    return { token: `demo.${u.id}`, user: publicUser(u) }
  },

  async login({ email, password }) {
    await delay()
    const u = read(K.users, []).find((x) => x.email.toLowerCase() === email.toLowerCase())
    if (!u || u.password !== password) fail('Email or password is incorrect.')
    return { token: `demo.${u.id}`, user: publicUser(u) }
  },

  async me() { await delay(120); return publicUser(current()) },

  async getServices() { await delay(120); return read(K.services, []) },

  async createService(data) {
    await delay(); requireAdmin()
    const list = read(K.services, [])
    const s = { id: Math.max(0, ...list.map((x) => x.id)) + 1, active: true, description: '', ...data }
    write(K.services, [...list, s]); return s
  },

  async updateService(id, data) {
    await delay(); requireAdmin()
    const list = read(K.services, [])
    const i = list.findIndex((x) => x.id === id)
    if (i < 0) fail('Service not found.')
    list[i] = { ...list[i], ...data }
    write(K.services, list); return list[i]
  },

  async createRequest(formData) {
    await delay(500)
    const u = current()
    if (u.role !== 'STUDENT') fail('Only students can submit print jobs.')
    const file = formData.get('file')
    const pages = Number(formData.get('pages'))
    const copies = Number(formData.get('copies'))
    const service = read(K.services, []).find((s) => s.id === Number(formData.get('serviceId')) && s.active)
    if (!file || !file.name) fail('Please attach a file to print.')
    if (!service) fail('Choose a print option.')
    if (!(pages >= 1)) fail('Pages must be at least 1.')
    if (!(copies >= 1)) fail('Copies must be at least 1.')
    const list = read(K.requests, [])
    const now = new Date().toISOString()
    const r = {
      id: Math.max(0, ...list.map((x) => x.id)) + 1,
      fileName: file.name, pages, copies, notes: formData.get('notes') || '',
      totalPrice: +(service.pricePerPage * pages * copies).toFixed(2),
      status: 'PENDING', createdAt: now, updatedAt: now,
      service: { id: service.id, name: service.name, pricePerPage: service.pricePerPage },
      student: publicUser(u),
    }
    write(K.requests, [r, ...list]); return r
  },

  async myRequests() {
    await delay(); const u = current()
    return read(K.requests, []).filter((r) => r.student.id === u.id).sort((a, b) => b.id - a.id)
  },

  async allRequests() { await delay(); requireAdmin(); return read(K.requests, []).sort((a, b) => b.id - a.id) },

  async setStatus(id, status) {
    await delay(); requireAdmin()
    const list = read(K.requests, [])
    const i = list.findIndex((r) => r.id === id)
    if (i < 0) fail('Print job not found.')
    list[i] = { ...list[i], status, updatedAt: new Date().toISOString() }
    write(K.requests, list); return list[i]
  },

  async cancelRequest(id) {
    await delay(); const u = current()
    const list = read(K.requests, [])
    const i = list.findIndex((r) => r.id === id && r.student.id === u.id)
    if (i < 0) fail('Print job not found.')
    if (list[i].status !== 'PENDING') fail('Only jobs that have not started printing can be cancelled.')
    list[i] = { ...list[i], status: 'CANCELLED', updatedAt: new Date().toISOString() }
    write(K.requests, list); return list[i]
  },

  async downloadFile() { fail('File download is only available when connected to the live server.') },

  async stats() {
    await delay(); requireAdmin()
    const rs = read(K.requests, [])
    const byStatus = {}
    rs.forEach((r) => { byStatus[r.status] = (byStatus[r.status] || 0) + 1 })
    return {
      totalRequests: rs.length,
      byStatus,
      pagesPrinted: rs.filter((r) => ['READY', 'COLLECTED'].includes(r.status)).reduce((a, r) => a + r.pages * r.copies, 0),
      revenue: +rs.filter((r) => r.status === 'COLLECTED').reduce((a, r) => a + r.totalPrice, 0).toFixed(2),
    }
  },
}
