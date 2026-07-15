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

  if (res.status === 204) return null
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
  if (!data.mfaRequired) {
    saveStaffAuth(data)
  }
  return data
}

export async function verifyStaffMfa(mfaToken, code) {
  const res = await fetch(`${STAFF_BASE}/auth/mfa/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mfaToken, code }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'MFA verification failed')
  saveStaffAuth(data)
  return data
}

export async function staffLogout() {
  const auth = getStaffAuth()
  try {
    await fetch(`${STAFF_BASE}/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
      },
      body: JSON.stringify({ refreshToken: auth?.refreshToken || null }),
    })
  } finally {
    clearStaffAuth()
  }
}

export async function startMfaSetup() {
  return staffFetch('/auth/mfa/setup', { method: 'POST' })
}

export async function confirmMfaSetup(code) {
  return staffFetch('/auth/mfa/confirm', {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
}

export async function disableMfa() {
  return staffFetch('/auth/mfa/disable', { method: 'POST' })
}

export async function getArrivals(date) {
  const params = date ? `?date=${date}` : ''
  return staffFetch(`/arrivals${params}`)
}

export async function getStaffBookings({ status, q, page = 0, size = 25 } = {}) {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  const query = params.toString()
  return staffFetch(`/bookings?${query}`)
}

export async function getStaffGuests({ q, page = 0, size = 25 } = {}) {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  return staffFetch(`/guests?${params.toString()}`)
}

export async function getStaffGuest(id) {
  return staffFetch(`/guests/${id}`)
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

export async function approvePayLaterBooking(id) {
  return staffFetch(`/bookings/${id}/approve`, { method: 'POST' })
}

export async function rejectPayLaterBooking(id, reason) {
  return staffFetch(`/bookings/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason || null }),
  })
}

export async function createAdditionalCharge(id, description, amount) {
  return staffFetch(`/bookings/${id}/charges`, {
    method: 'POST',
    body: JSON.stringify({ description, amount }),
  })
}

export async function approveAdditionalCharge(bookingId, chargeId) {
  return staffFetch(`/bookings/${bookingId}/charges/${chargeId}/approve`, {
    method: 'POST',
  })
}

export async function rejectAdditionalCharge(bookingId, chargeId, reason) {
  return staffFetch(`/bookings/${bookingId}/charges/${chargeId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason || null }),
  })
}

export async function recordAdditionalChargePayment(bookingId, chargeId) {
  return staffFetch(`/bookings/${bookingId}/charges/${chargeId}/record-payment`, {
    method: 'POST',
  })
}

export async function recordFolioPayment(id) {
  return staffFetch(`/bookings/${id}/record-payment`, { method: 'POST' })
}

export async function transferRoomBooking(id, roomUnitId, reason) {
  return staffFetch(`/bookings/${id}/transfer-room`, {
    method: 'POST',
    body: JSON.stringify({ roomUnitId, reason: reason || null }),
  })
}

export async function overrideBookingStatus(id, targetStatus, reason) {
  return staffFetch(`/bookings/${id}/override`, {
    method: 'POST',
    body: JSON.stringify({ targetStatus, reason }),
  })
}

export async function refundBooking(id, amount, reason) {
  return staffFetch(`/bookings/${id}/refund`, {
    method: 'POST',
    body: JSON.stringify({ amount: amount || null, reason }),
  })
}

export async function manualRefundBooking(id, payload) {
  return staffFetch(`/bookings/${id}/manual-refund`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function getRefundPolicy() {
  return staffFetch('/refund-policy')
}

export async function updateRefundPolicy(payload) {
  return staffFetch('/refund-policy', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function listStaffPromos() {
  return staffFetch('/promos')
}

export async function createStaffPromo(payload) {
  return staffFetch('/promos', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffPromo(id, payload) {
  return staffFetch(`/promos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteStaffPromo(id) {
  return staffFetch(`/promos/${id}`, {
    method: 'DELETE',
  })
}

export async function getManagerConfig() {
  return staffFetch('/config')
}

export async function getConfigAuditLog() {
  return staffFetch('/config/audit')
}

export async function getStaffLoginAudit({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return staffFetch(`/audit/login?${params}`)
}

export async function getStaffActivityAudit({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return staffFetch(`/audit/activity?${params}`)
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

export async function createRoomUnit(roomTypeId, roomNumber, floorLabel) {
  return staffFetch(`/config/room-types/${roomTypeId}/units`, {
    method: 'POST',
    body: JSON.stringify({ roomNumber, floorLabel: floorLabel || null }),
  })
}

export async function updateRoomUnitStatus(unitId, statusOptionId) {
  return staffFetch(`/config/room-units/${unitId}`, {
    method: 'PUT',
    body: JSON.stringify({ statusOptionId }),
  })
}

export async function updateDailyRates(ratePlanId, fromDate, toDate, amount) {
  return staffFetch(`/config/rate-plans/${ratePlanId}/daily-rates`, {
    method: 'PUT',
    body: JSON.stringify({ fromDate, toDate, amount: Number(amount) }),
  })
}

export async function listStaffUsers() {
  return staffFetch('/users')
}

export async function createStaffUser(payload) {
  return staffFetch('/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffUser(id, payload) {
  return staffFetch(`/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function resetStaffPassword(id, password) {
  return staffFetch(`/users/${id}/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

export async function getStaffNav() {
  return staffFetch('/nav')
}

export async function getStaffNavRoles() {
  return staffFetch('/nav/roles')
}

export async function getAllStaffNavModules() {
  return staffFetch('/nav/all')
}

export async function createStaffNavModule(payload) {
  return staffFetch('/nav/modules', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffNavModule(id, payload) {
  return staffFetch(`/nav/modules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteStaffNavModule(id) {
  return staffFetch(`/nav/modules/${id}`, {
    method: 'DELETE',
  })
}

export async function updateStaffNavModules(modules) {
  return staffFetch('/nav', {
    method: 'PUT',
    body: JSON.stringify({ modules }),
  })
}

export async function listRoomTypes() {
  return staffFetch('/config/room-types')
}

export async function createRoomType(payload) {
  return staffFetch('/config/room-types', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function listRoomNumbers(unassignedOnly = false) {
  const query = unassignedOnly ? '?unassignedOnly=true' : ''
  return staffFetch(`/config/room-numbers${query}`)
}

export async function createRoomNumber(roomNumber, floorLabel) {
  return staffFetch('/config/room-numbers', {
    method: 'POST',
    body: JSON.stringify({ roomNumber, floorLabel: floorLabel || null }),
  })
}

export async function updateRoomNumber(id, payload) {
  return staffFetch(`/config/room-numbers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteRoomNumber(id) {
  return staffFetch(`/config/room-numbers/${id}`, {
    method: 'DELETE',
  })
}

export async function getRoomConfigOptions() {
  return staffFetch('/config/room-options')
}

export async function createRoomConfigOption(optionType, label) {
  return staffFetch('/config/room-options', {
    method: 'POST',
    body: JSON.stringify({ optionType, label }),
  })
}

export async function updateRoomConfigOption(id, label) {
  return staffFetch(`/config/room-options/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ label }),
  })
}

export async function deleteRoomConfigOption(id) {
  return staffFetch(`/config/room-options/${id}`, {
    method: 'DELETE',
  })
}

export async function getRoomDailyStatus({
  date,
  page = 0,
  size = 20,
  status,
  roomTypeId,
  search,
} = {}) {
  const params = new URLSearchParams()
  if (date) params.set('date', date)
  params.set('page', String(page))
  params.set('size', String(size))
  if (status) params.set('status', status)
  if (roomTypeId != null) params.set('roomTypeId', String(roomTypeId))
  if (search) params.set('search', search)
  return staffFetch(`/rooms/daily-status?${params}`)
}

export async function getRoomCalendar(roomUnitId, from, to) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return staffFetch(`/rooms/${roomUnitId}/calendar?${params}`)
}

export async function updateRoomTypeCatalog(id, payload) {
  return staffFetch(`/config/room-types/${id}/catalog`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function getStaffDashboard({ fromDate, toDate } = {}) {
  const params = new URLSearchParams()
  if (fromDate) params.set('fromDate', fromDate)
  if (toDate) params.set('toDate', toDate)
  const query = params.toString()
  return staffFetch(`/dashboard${query ? `?${query}` : ''}`)
}

export async function staffGlobalSearch(query) {
  const params = new URLSearchParams({ q: query })
  return staffFetch(`/search?${params}`)
}

export async function getStaffNotifications() {
  return staffFetch('/notifications')
}

export async function getStaffNotificationUnreadCount() {
  return staffFetch('/notifications/unread-count')
}

export async function markStaffNotificationsSeen() {
  return staffFetch('/notifications/mark-seen', { method: 'POST' })
}

export async function getStaffBranding() {
  return staffFetch('/branding')
}

export async function updateStaffBranding(payload) {
  return staffFetch('/branding', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

async function uploadBrandingAsset(path, file) {
  const auth = getStaffAuth()
  const formData = new FormData()
  formData.append('file', file)

  let res = await fetch(`${STAFF_BASE}/branding/${path}`, {
    method: 'POST',
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    body: formData,
  })

  if (res.status === 401 && auth?.refreshToken) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return uploadBrandingAsset(path, file)
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Upload failed')
  return data
}

export function uploadBrandingLogo(file) {
  return uploadBrandingAsset('logo', file)
}

export async function listStaffServiceAddons() {
  return staffFetch('/rooms/extras/services')
}

export async function createStaffServiceAddon(payload) {
  return staffFetch('/rooms/extras/services', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffServiceAddon(id, payload) {
  return staffFetch(`/rooms/extras/services/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteStaffServiceAddon(id) {
  return staffFetch(`/rooms/extras/services/${id}`, { method: 'DELETE' })
}

export async function uploadServiceAddonImage(file) {
  const auth = getStaffAuth()
  const formData = new FormData()
  formData.append('file', file)

  let res = await fetch(`${STAFF_BASE}/rooms/extras/services/image`, {
    method: 'POST',
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    body: formData,
  })

  if (res.status === 401 && auth?.refreshToken) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return uploadServiceAddonImage(file)
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Image upload failed')
  return data
}

export async function uploadItemAddonImage(file) {
  const auth = getStaffAuth()
  const formData = new FormData()
  formData.append('file', file)

  let res = await fetch(`${STAFF_BASE}/rooms/extras/items/image`, {
    method: 'POST',
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    body: formData,
  })

  if (res.status === 401 && auth?.refreshToken) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return uploadItemAddonImage(file)
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Image upload failed')
  return data
}

export async function listStaffItemAddons() {
  return staffFetch('/rooms/extras/items')
}

export async function createStaffItemAddon(payload) {
  return staffFetch('/rooms/extras/items', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffItemAddon(id, payload) {
  return staffFetch(`/rooms/extras/items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteStaffItemAddon(id) {
  return staffFetch(`/rooms/extras/items/${id}`, { method: 'DELETE' })
}

export async function uploadRoomImage(file) {
  const auth = getStaffAuth()
  const formData = new FormData()
  formData.append('file', file)

  let res = await fetch(`${STAFF_BASE}/config/room-images`, {
    method: 'POST',
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    body: formData,
  })

  if (res.status === 401 && auth?.refreshToken) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return uploadRoomImage(file)
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Image upload failed')
  return data
}

export async function getStaffProfile() {
  return staffFetch('/profile')
}

export async function updateStaffProfile(payload) {
  return staffFetch('/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function updateStaffTheme(themePreference) {
  return staffFetch('/profile/theme', {
    method: 'PUT',
    body: JSON.stringify({ themePreference }),
  })
}

export async function changeStaffPassword(currentPassword, newPassword) {
  return staffFetch('/profile/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}

async function uploadProfileAsset(file) {
  const auth = getStaffAuth()
  const formData = new FormData()
  formData.append('file', file)

  let res = await fetch(`${STAFF_BASE}/profile/avatar`, {
    method: 'POST',
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    body: formData,
  })

  if (res.status === 401 && auth?.refreshToken) {
    const refreshed = await refreshTokens(auth.refreshToken)
    if (refreshed) {
      return uploadProfileAsset(file)
    }
    clearStaffAuth()
    throw new Error('Session expired. Please log in again.')
  }

  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Profile photo upload failed')
  return data
}

export function uploadStaffProfilePhoto(file) {
  return uploadProfileAsset(file)
}
