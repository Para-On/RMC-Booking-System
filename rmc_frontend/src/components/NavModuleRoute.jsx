import { Navigate, useLocation } from 'react-router-dom'
import { canAccessNavPath } from '@/config/staffNavAccess'
import { useStaffNav } from '@/components/staff/StaffNavContext'

export default function NavModuleRoute({ path, children, fallback = '/staff/arrivals' }) {
  const { modules, loading } = useStaffNav()
  const location = useLocation()
  const checkPath = path ?? location.pathname

  if (loading) {
    return <p className="text-sm text-muted-foreground">Loading access…</p>
  }

  if (!canAccessNavPath(modules, checkPath)) {
    return <Navigate to={fallback} replace />
  }

  return children
}
