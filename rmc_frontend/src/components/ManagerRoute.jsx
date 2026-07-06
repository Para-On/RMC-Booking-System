import { Navigate } from 'react-router-dom'
import { isAdmin, isStaffLoggedIn } from '../staffAuth'

/** @deprecated Use AdminRoute instead. */
export default function ManagerRoute({ children }) {
  if (!isStaffLoggedIn()) {
    return <Navigate to="/staff/login" replace />
  }
  if (!isAdmin()) {
    return <Navigate to="/staff/arrivals" replace />
  }
  return children
}
