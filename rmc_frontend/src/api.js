const API_BASE = '/api/guest'

export async function searchAvailability(checkIn, checkOut) {
  const params = new URLSearchParams({ checkIn, checkOut })
  const res = await fetch(`${API_BASE}/availability?${params}`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to search availability')
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

export async function confirmPayment(reference) {
  const res = await fetch(`${API_BASE}/bookings/${reference}/confirm-payment`, {
    method: 'POST',
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || 'Unable to confirm payment')
  return data
}

export function formatMoney(amount, currency = 'PHP') {
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency,
  }).format(Number(amount))
}

export function tomorrowIso() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}

export function dayAfter(isoDate) {
  const d = new Date(isoDate + 'T00:00:00')
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}
