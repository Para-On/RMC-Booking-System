import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { cancelBooking, formatMoney, getBooking } from '../api'

export default function BookingPage() {
  const { reference } = useParams()
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') || ''
  const [booking, setBooking] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(false)

  useEffect(() => {
    if (!email) {
      setError('Email is required. Use Find booking and enter your reference + email.')
      setLoading(false)
      return
    }
    getBooking(reference, email)
      .then(setBooking)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [reference, email])

  async function handleCancel() {
    if (!window.confirm('Cancel this booking?')) return
    setCancelling(true)
    setError('')
    try {
      const updated = await cancelBooking(reference, email)
      setBooking(updated)
    } catch (err) {
      setError(err.message)
    } finally {
      setCancelling(false)
    }
  }

  if (loading) return <div className="card">Loading…</div>

  if (error && !booking) {
    return (
      <div className="card">
        <p className="error">{error}</p>
        <Link to="/booking/lookup" className="btn">
          Try again
        </Link>
      </div>
    )
  }

  const canCancel =
    booking.status === 'CONFIRMED_PAY_LATER' || booking.status === 'CONFIRMED'

  return (
    <div className="card">
      <h1>{booking.reference}</h1>
      <div className="meta-grid">
        <p>
          <strong>Status:</strong> {booking.status}
        </p>
        <p>
          <strong>Guest:</strong> {booking.guestName}
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
          <strong>Payment:</strong> {booking.paymentMethod.replace('_', ' ')}
        </p>
      </div>
      {error && <p className="error">{error}</p>}
      {canCancel && (
        <button type="button" className="secondary" onClick={handleCancel} disabled={cancelling}>
          {cancelling ? 'Cancelling…' : 'Cancel booking'}
        </button>
      )}
    </div>
  )
}
