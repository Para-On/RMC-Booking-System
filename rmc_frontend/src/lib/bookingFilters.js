import { format, parseISO } from 'date-fns'
import { todayIso, tomorrowIso } from '@/api'

export const HOTELS = [
  {
    id: 'rmc',
    name: 'RMC Hotel',
    location: 'Manila, Philippines',
    tagline: 'Comfortable stays in the heart of the city.',
    description:
      'Well-appointed rooms, thoughtful amenities, and easy access to the city—ideal for business trips and weekend getaways.',
    imageUrl: '/images/hotel-hero.png',
  },
]

export const DEFAULT_GUESTS = {
  rooms: 1,
  adults: 2,
  children: 1,
}

export const BOOKING_ACTION_BUTTON_CLASS =
  'h-12 min-w-[9rem] justify-center px-5 text-center text-base shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:brightness-110 active:translate-y-0 active:shadow-sm disabled:hover:translate-y-0 disabled:hover:shadow-sm disabled:hover:brightness-100'

export const BOOKING_ACTION_BUTTON_SM_CLASS =
  'h-9 min-w-[6.5rem] justify-center px-3.5 text-center text-xs shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:brightness-110 active:translate-y-0 active:shadow-sm disabled:hover:translate-y-0 disabled:hover:shadow-sm disabled:hover:brightness-100 sm:min-w-[7rem] sm:px-4 sm:text-sm'

export function getDefaultSearchParams() {
  return {
    hotelId: HOTELS[0].id,
    checkIn: todayIso(),
    checkOut: tomorrowIso(),
    guests: { ...DEFAULT_GUESTS },
  }
}

export function formatGuestsLabel({ rooms, adults, children }) {
  const adultLabel = `${adults} adult${adults === 1 ? '' : 's'}`
  const childLabel =
    children > 0 ? ` and ${children} kid${children === 1 ? '' : 's'}` : ''
  if ((rooms ?? 1) > 1) {
    const roomLabel = `${rooms} room${rooms === 1 ? '' : 's'}, `
    return `${roomLabel}${adultLabel}${childLabel}`
  }
  return `${adultLabel}${childLabel}`
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
  const roomCount = Math.max(1, guests?.rooms ?? 1)
  const adults = Math.max(1, guests?.adults ?? 1)
  const children = Math.max(0, guests?.children ?? 0)
  const adultsNeeded = Math.ceil(adults / roomCount)
  const childrenNeeded = Math.ceil(children / roomCount)

  return (rooms || []).filter((room) => {
    if ((room.availableUnits ?? 0) < roomCount) return false
    if ((room.maxAdults ?? 0) < adultsNeeded) return false
    if ((room.maxChildren ?? 0) < childrenNeeded) return false
    return true
  })
}
