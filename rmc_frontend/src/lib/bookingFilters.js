import { format, parseISO } from 'date-fns'
import { todayIso, tomorrowIso } from '@/api'

export const HOTELS = [
  {
    id: 'rmc',
    name: 'RMC Hotel',
    tagline: 'Comfortable stays in the heart of the city.',
    imageUrl: null,
  },
]

export const DEFAULT_GUESTS = {
  rooms: 1,
  adults: 2,
  children: 0,
}

export function getDefaultSearchParams() {
  return {
    hotelId: HOTELS[0].id,
    checkIn: todayIso(),
    checkOut: tomorrowIso(),
    guests: { ...DEFAULT_GUESTS },
  }
}

export function formatGuestsLabel({ rooms, adults, children }) {
  const roomLabel = `${rooms} room${rooms === 1 ? '' : 's'}`
  const adultLabel = `${adults} adult${adults === 1 ? '' : 's'}`
  const childLabel = `${children} child${children === 1 ? '' : 'ren'}`
  return `${roomLabel} · ${adultLabel}${children > 0 ? ` · ${childLabel}` : ''}`
}

export function formatCompactDate(iso) {
  if (!iso) return ''
  return format(parseISO(iso), 'MMM d, yyyy')
}

export function formatAppliedSummary(search) {
  if (!search) return null
  const hotel = HOTELS.find((h) => h.id === search.hotelId)?.name ?? 'Hotel'
  return {
    hotel,
    dates: `${formatCompactDate(search.checkIn)} – ${formatCompactDate(search.checkOut)}`,
    guests: formatGuestsLabel(search.guests),
  }
}

export function filterRoomsByGuests(rooms, guests) {
  const { rooms: roomCount, adults, children } = guests
  const adultsPerRoom = Math.ceil(adults / roomCount)
  const childrenPerRoom = Math.ceil(children / roomCount)

  return (rooms || []).filter((room) => {
    if ((room.availableUnits ?? 0) < roomCount) return false
    if ((room.maxAdults ?? 0) < adultsPerRoom) return false
    if ((room.maxChildren ?? 0) < childrenPerRoom) return false
    return true
  })
}
