export const STAFF_NAV_PATHS = {
  DASHBOARD: '/staff/dashboard',
  ARRIVALS: '/staff/arrivals',
  BOOKINGS: '/staff/bookings',
  /** @deprecated Use BOOKINGS — kept for old bookmarks */
  ARRIVALS_BOOKINGS: '/staff/bookings',
  GUESTS: '/staff/guests',
  ROOMS_CATALOG: '/staff/rooms/catalog',
  ROOMS_CONFIG: '/staff/rooms/config',
  ROOMS_OPERATIONS: '/staff/rooms/operations',
  ROOMS_EXTRAS: '/staff/rooms/extras',
  SETTINGS: '/staff/settings',
  SETTINGS_AUDIT: '/staff/settings/audit',
  SETTINGS_REFUND_POLICY: '/staff/settings/refund-policy',
  SETTINGS_PROMOS: '/staff/settings/promos',
  BRANDING: '/staff/branding',
  USERS: '/staff/users',
  PROFILE: '/staff/profile',
  MODULES: '/staff/modules',
}

export function flattenNavPaths(modules) {
  const paths = []
  for (const module of modules || []) {
    if (module.path && module.path !== '#') {
      paths.push(module.path)
    }
    if (module.children?.length) {
      paths.push(...flattenNavPaths(module.children))
    }
  }
  return paths
}

export function canAccessNavPath(modules, pathname) {
  const paths = flattenNavPaths(modules)
  if (paths.includes(pathname)) {
    return true
  }
  if (pathname.startsWith('/staff/bookings/')) {
    return (
      paths.includes(STAFF_NAV_PATHS.DASHBOARD) ||
      paths.includes(STAFF_NAV_PATHS.ARRIVALS) ||
      paths.includes(STAFF_NAV_PATHS.BOOKINGS) ||
      paths.includes(STAFF_NAV_PATHS.GUESTS) ||
      paths.includes(STAFF_NAV_PATHS.ROOMS_OPERATIONS)
    )
  }
  if (pathname.startsWith('/staff/guests/')) {
    return paths.includes(STAFF_NAV_PATHS.GUESTS) || paths.includes(STAFF_NAV_PATHS.ARRIVALS)
  }
  if (pathname === STAFF_NAV_PATHS.PROFILE) {
    return true
  }
  return false
}
