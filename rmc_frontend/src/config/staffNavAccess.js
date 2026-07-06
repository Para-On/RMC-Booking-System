export const STAFF_NAV_PATHS = {
  DASHBOARD: '/staff/dashboard',
  ARRIVALS: '/staff/arrivals',
  ROOMS_CATALOG: '/staff/rooms/catalog',
  ROOMS_CONFIG: '/staff/rooms/config',
  ROOMS_OPERATIONS: '/staff/rooms/operations',
  SETTINGS: '/staff/settings',
  USERS: '/staff/users',
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
      paths.includes(STAFF_NAV_PATHS.ROOMS_OPERATIONS)
    )
  }
  return false
}
