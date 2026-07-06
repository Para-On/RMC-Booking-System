import { Navigate } from 'react-router-dom'

/** Dates are chosen on the home catalog page; keep route for old links. */
export default function BookDatesPage() {
  return <Navigate to="/" replace />
}
