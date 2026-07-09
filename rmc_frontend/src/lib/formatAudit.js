export function formatAuditTimestamp(iso, locale = 'en-US') {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(locale, {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  })
}

export function formatLoginEvent(event) {
  if (!event) return '—'
  return event === 'LOGOUT' ? 'Logout' : 'Login'
}

export function formatAuditStatus(status) {
  if (!status) return '—'
  return status === 'FAILED' ? 'Failed' : 'Success'
}

export function formatActivityAction(action) {
  if (!action) return '—'
  switch (action) {
    case 'VIEW':
      return 'View'
    case 'EDIT':
      return 'Edit'
    case 'ADD':
      return 'Add'
    case 'DELETE':
      return 'Delete'
    default:
      return action
  }
}

export function loginEventBadgeClass(event) {
  if (event === 'LOGOUT') {
    return 'border-violet-200 bg-violet-500/10 text-violet-700 dark:border-violet-800 dark:text-violet-300'
  }
  if (event === 'LOGIN') {
    return 'border-sky-200 bg-sky-500/10 text-sky-700 dark:border-sky-800 dark:text-sky-300'
  }
  return 'border-border bg-muted text-muted-foreground'
}

export function auditStatusBadgeClass(status) {
  if (status === 'FAILED') {
    return 'border-red-200 bg-red-500/10 text-red-700 dark:border-red-800 dark:text-red-300'
  }
  if (status === 'SUCCESS') {
    return 'border-emerald-200 bg-emerald-500/10 text-emerald-700 dark:border-emerald-800 dark:text-emerald-300'
  }
  return 'border-border bg-muted text-muted-foreground'
}

export function activityActionBadgeClass(action) {
  switch (action) {
    case 'VIEW':
      return 'border-slate-200 bg-slate-500/10 text-slate-700 dark:border-slate-700 dark:text-slate-300'
    case 'ADD':
      return 'border-emerald-200 bg-emerald-500/10 text-emerald-700 dark:border-emerald-800 dark:text-emerald-300'
    case 'EDIT':
      return 'border-amber-200 bg-amber-500/10 text-amber-700 dark:border-amber-800 dark:text-amber-300'
    case 'DELETE':
      return 'border-red-200 bg-red-500/10 text-red-700 dark:border-red-800 dark:text-red-300'
    default:
      return 'border-border bg-muted text-muted-foreground'
  }
}

export function formatStaffRole(role) {
  if (!role) return '—'
  return role
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function auditRangeStart(page, size, totalElements) {
  if (!totalElements) return 0
  return page * size + 1
}

export function auditRangeEnd(page, size, totalElements) {
  if (!totalElements) return 0
  return Math.min((page + 1) * size, totalElements)
}
