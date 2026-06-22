import { Navigate } from 'react-router-dom'
import { isStaffLoggedIn } from '../staffAuth'

export default function StaffRoute({ children }) {
  if (!isStaffLoggedIn()) {
    return <Navigate to="/staff/login" replace />
  }
  return children
}
