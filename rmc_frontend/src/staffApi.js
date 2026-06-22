import { clearStaffAuth, getStaffAuth, saveStaffAuth } from './staffAuth'

const STAFF_BASE = '/api/staff'

async function staffFetch(path, options = {}) {
  const auth = getStaffAuth()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (auth?.accessToken) {
    headers.Authorization = `Bearer ${auth.accessToken}`
  }

  let res = await fetch(`${STAFF_BASE}${path}`, { ...options, headers })

  if (res.status === 401 && auth?.refreshToken && !options._retried) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return staffFetch(path, { ...options, _retried: true })
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Request failed')
  return data
}

async function refreshTokens(refreshToken) {
  const res = await fetch(`${STAFF_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) return false
  saveStaffAuth(data)
  return true
}

export async function staffLogin(email, password) {
  const res = await fetch(`${STAFF_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Login failed')
  saveStaffAuth(data)
  return data
}

export function staffLogout() {
  clearStaffAuth()
}

export async function getArrivals(date) {
  const params = date ? `?date=${date}` : ''
  return staffFetch(`/arrivals${params}`)
}

export async function getStaffBooking(id) {
  return staffFetch(`/bookings/${id}`)
}

export async function checkInBooking(id, roomUnitId) {
  return staffFetch(`/bookings/${id}/check-in`, {
    method: 'POST',
    body: JSON.stringify({ roomUnitId: roomUnitId || null }),
  })
}

export async function checkOutBooking(id) {
  return staffFetch(`/bookings/${id}/check-out`, { method: 'POST' })
}

export async function overrideBookingStatus(id, targetStatus, reason) {
  return staffFetch(`/bookings/${id}/override`, {
    method: 'POST',
    body: JSON.stringify({ targetStatus, reason }),
  })
}

export async function getManagerConfig() {
  return staffFetch('/config')
}

export async function updateSystemConfig(key, value) {
  return staffFetch(`/config/system/${encodeURIComponent(key)}`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  })
}

export async function updateRatePlanConfig(id, payload) {
  return staffFetch(`/config/rate-plans/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function updateRoomTypeConfig(id, payload) {
  return staffFetch(`/config/room-types/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
