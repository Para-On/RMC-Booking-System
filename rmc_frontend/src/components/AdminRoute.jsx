import { Navigate } from 'react-router-dom'
import { getStaffAuth, isStaffLoggedIn } from '../staffAuth'

export default function AdminRoute({ children }) {
  if (!isStaffLoggedIn()) {
    return <Navigate to="/staff/login" replace />
  }
  const auth = getStaffAuth()
  if (auth?.role !== 'ADMIN') {
    return <Navigate to="/staff/arrivals" replace />
  }
  return children
}
