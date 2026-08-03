const API_BASE = '/api/guest'

export async function getBranding() {
  const res = await fetch(`${API_BASE}/branding`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to load branding')
  }
  return res.json()
}

export async function getPricingPolicy() {
  const res = await fetch(`${API_BASE}/pricing-policy`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to load pricing policy')
  }
  return res.json()
}

export async function listRoomCatalog() {
  const res = await fetch(`${API_BASE}/room-catalog`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to load room catalog')
  }
  return res.json()
}

export async function searchAvailability(checkIn, checkOut, roomTypeId, promoCodes = {}) {
  const params = new URLSearchParams({ checkIn, checkOut })
  if (roomTypeId != null) params.set('roomTypeId', String(roomTypeId))
  if (promoCodes.promoType) params.set('promoType', promoCodes.promoType)
  if (promoCodes.offerCode) params.set('offerCode', promoCodes.offerCode)
  if (promoCodes.organizationCode) params.set('organizationCode', promoCodes.organizationCode)
  const res = await fetch(`${API_BASE}/availability?${params}`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to search availability')
  }
  return res.json()
}

export async function checkStayAvailability(roomTypeId, checkIn, checkOut, ratePlanId, promoCodes = {}) {
  const params = new URLSearchParams({
    roomTypeId: String(roomTypeId),
    checkIn,
    checkOut,
  })
  if (ratePlanId != null) params.set('ratePlanId', String(ratePlanId))
  if (promoCodes.promoType) params.set('promoType', promoCodes.promoType)
  if (promoCodes.offerCode) params.set('offerCode', promoCodes.offerCode)
  if (promoCodes.organizationCode) params.set('organizationCode', promoCodes.organizationCode)
  const res = await fetch(`${API_BASE}/availability/check?${params}`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to check availability')
  }
  return res.json()
}

export async function createBooking(payload) {
  const res = await fetch(`${API_BASE}/bookings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Booking failed')
  return data
}

export async function getBooking(reference, email) {
  const params = new URLSearchParams({ email })
  const res = await fetch(`${API_BASE}/bookings/${reference}?${params}`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Booking not found')
  return data
}

export async function cancelBooking(reference, email) {
  const params = new URLSearchParams({ email })
  const res = await fetch(`${API_BASE}/bookings/${reference}/cancel?${params}`, {
    method: 'POST',
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Cancellation failed')
  return data
}

export async function getBookingStatus(reference) {
  const res = await fetch(`${API_BASE}/bookings/${reference}/status`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Unable to load booking status')
  return data
}

export async function listServiceAddons() {
  const res = await fetch(`${API_BASE}/extras/services`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to load service add-ons')
  }
  return res.json()
}

export async function listItemAddons() {
  const res = await fetch(`${API_BASE}/extras/items`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to load item add-ons')
  }
  return res.json()
}

export async function confirmPayment(reference) {
  const res = await fetch(`${API_BASE}/bookings/${reference}/confirm-payment`, {
    method: 'POST',
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Unable to confirm payment')
  return data
}

export async function payAdditionalCharge(reference, chargeId, email, paymentMethod) {
  const res = await fetch(`${API_BASE}/bookings/${reference}/charges/${chargeId}/pay`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, paymentMethod }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Unable to start charge payment')
  return data
}

export async function confirmAdditionalChargePayment(reference, chargeId) {
  const res = await fetch(
    `${API_BASE}/bookings/${reference}/charges/${chargeId}/confirm-payment`,
    { method: 'POST' }
  )
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Unable to confirm charge payment')
  return data
}

export function formatMoney(amount, currency = 'PHP') {
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency,
  }).format(Number(amount))
}

function localDateIso(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function todayIso() {
  return localDateIso(new Date())
}

export function tomorrowIso() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return localDateIso(d)
}

export function dayAfter(isoDate) {
  const d = new Date(isoDate + 'T12:00:00')
  d.setDate(d.getDate() + 1)
  return localDateIso(d)
}
