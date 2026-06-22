const STORAGE_KEY = 'rmc_staff_auth'

export function getStaffAuth() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function saveStaffAuth(auth) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}

export function clearStaffAuth() {
  localStorage.removeItem(STORAGE_KEY)
}

export function isStaffLoggedIn() {
  return Boolean(getStaffAuth()?.accessToken)
}

export function isManager() {
  return getStaffAuth()?.role === 'MANAGER'
}
