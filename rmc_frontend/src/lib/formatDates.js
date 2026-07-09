export function formatStayDate(isoDate, locale = 'en-US') {
  const date = new Date(`${isoDate}T12:00:00`)
  return date.toLocaleDateString(locale, {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

export function formatStayRange(checkIn, checkOut, locale = 'en-US') {
  return `${formatStayDate(checkIn, locale)} to ${formatStayDate(checkOut, locale)}`
}

export function formatStaffDateTime(value, locale = 'en-US', timeZone = 'Asia/Manila') {
  if (!value) return '—'
  return new Date(value).toLocaleString(locale, {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    timeZone,
  })
}

export function countStayNights(checkIn, checkOut) {
  if (!checkIn || !checkOut || checkOut <= checkIn) return 0
  const start = new Date(`${checkIn}T12:00:00`)
  const end = new Date(`${checkOut}T12:00:00`)
  const msPerDay = 24 * 60 * 60 * 1000
  return Math.round((end.getTime() - start.getTime()) / msPerDay)
}
