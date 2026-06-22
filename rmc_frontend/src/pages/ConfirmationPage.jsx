import { Link, useLocation } from 'react-router-dom'
import { formatMoney } from '../api'

export default function ConfirmationPage() {
  const { state } = useLocation()
  const booking = state?.booking

  if (!booking) {
    return (
      <div className="card">
        <p>No booking details.</p>
        <Link to="/" className="btn">
          Search rooms
        </Link>
      </div>
    )
  }

  return (
    <div className="card">
      <div className="success">Booking confirmed — pay at hotel on arrival.</div>
      <h1>Reference: {booking.reference}</h1>
      <div className="meta-grid">
        <p>
          <strong>Guest:</strong> {booking.guestName} ({booking.guestEmail})
        </p>
        <p>
          <strong>Room:</strong> {booking.roomTypeName}
        </p>
        <p>
          <strong>Stay:</strong> {booking.checkIn} → {booking.checkOut}
        </p>
        <p>
          <strong>Total:</strong> {formatMoney(booking.quotedTotal, booking.currency)}
        </p>
        <p>
          <strong>Status:</strong> {booking.status}
        </p>
      </div>
      <p>Save your reference and email — you'll need both to look up or cancel this booking.</p>
      <Link to="/booking/lookup" className="btn">
        Look up booking
      </Link>
    </div>
  )
}
