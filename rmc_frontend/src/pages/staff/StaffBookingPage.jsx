import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { formatMoney } from '../../api'
import {
  checkInBooking,
  checkOutBooking,
  getStaffBooking,
  overrideBookingStatus,
} from '../../staffApi'

export default function StaffBookingPage() {
  const { id } = useParams()
  const [booking, setBooking] = useState(null)
  const [roomUnitId, setRoomUnitId] = useState('')
  const [overrideStatus, setOverrideStatus] = useState('NO_SHOW')
  const [overrideReason, setOverrideReason] = useState('')
  const [folio, setFolio] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionMsg, setActionMsg] = useState('')

  useEffect(() => {
    loadBooking()
  }, [id])

  async function loadBooking() {
    setLoading(true)
    setError('')
    try {
      const data = await getStaffBooking(id)
      setBooking(data)
      if (data.roomNumber && data.availableRooms?.length) {
        const match = data.availableRooms.find((r) => r.roomNumber === data.roomNumber)
        if (match) setRoomUnitId(String(match.id))
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleCheckIn() {
    setActionMsg('')
    try {
      const data = await checkInBooking(id, roomUnitId ? Number(roomUnitId) : null)
      setBooking(data)
      setActionMsg('Guest checked in.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCheckOut() {
    setActionMsg('')
    try {
      const data = await checkOutBooking(id)
      setFolio(data)
      await loadBooking()
      setActionMsg('Guest checked out.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleOverride(e) {
    e.preventDefault()
    setActionMsg('')
    try {
      const data = await overrideBookingStatus(id, overrideStatus, overrideReason)
      setBooking(data)
      setOverrideReason('')
      setActionMsg(`Status updated to ${overrideStatus}.`)
    } catch (err) {
      setError(err.message)
    }
  }

  if (loading) return <p>Loading booking…</p>
  if (error && !booking) return <p className="error">{error}</p>
  if (!booking) return null

  const canCheckIn =
    !booking.checkedInAt &&
    (booking.status === 'CONFIRMED' || booking.status === 'CONFIRMED_PAY_LATER')
  const canCheckOut = booking.checkedInAt && !booking.checkedOutAt

  return (
    <div>
      <p>
        <Link to="/staff/arrivals">← Back to arrivals</Link>
      </p>

      <div className="card">
        <h1>{booking.reference}</h1>
        <div className="meta-grid">
          <div>
            <strong>Guest:</strong> {booking.guestName} ({booking.guestEmail})
          </div>
          <div>
            <strong>Phone:</strong> {booking.guestPhone}
          </div>
          <div>
            <strong>Room type:</strong> {booking.roomTypeName}
          </div>
          <div>
            <strong>Stay:</strong> {booking.checkInDate} → {booking.checkOutDate}
          </div>
          <div>
            <strong>Status:</strong> {booking.status}
          </div>
          <div>
            <strong>Payment:</strong> {booking.paymentMethod}
          </div>
          <div>
            <strong>Total:</strong> {formatMoney(booking.quotedTotal, booking.currency)}
          </div>
          <div>
            <strong>Balance due:</strong>{' '}
            {formatMoney(booking.ledgerBalance, booking.currency)}
          </div>
          <div>
            <strong>Assigned room:</strong> {booking.roomNumber || 'Not assigned'}
          </div>
          <div>
            <strong>Checked in:</strong> {booking.checkedInAt ? 'Yes' : 'No'}
          </div>
          <div>
            <strong>Checked out:</strong> {booking.checkedOutAt ? 'Yes' : 'No'}
          </div>
        </div>
      </div>

      {actionMsg && <p className="success">{actionMsg}</p>}
      {error && <p className="error">{error}</p>}

      {canCheckIn && (
        <div className="card">
          <h2>Check in</h2>
          <div className="search-form">
            <label>
              Assign room (optional)
              <select value={roomUnitId} onChange={(e) => setRoomUnitId(e.target.value)}>
                <option value="">— Select room —</option>
                {booking.availableRooms?.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.roomNumber} (floor {room.floorLabel || '—'})
                  </option>
                ))}
              </select>
            </label>
            <button type="button" onClick={handleCheckIn}>
              Check in guest
            </button>
          </div>
        </div>
      )}

      {canCheckOut && (
        <div className="card">
          <h2>Check out</h2>
          <button type="button" onClick={handleCheckOut}>
            Check out guest
          </button>
        </div>
      )}

      {folio && (
        <div className="card">
          <h2>Folio summary</h2>
          <p>
            Total: {formatMoney(folio.quotedTotal, folio.currency)} · Paid:{' '}
            {formatMoney(folio.amountPaid, folio.currency)} · Balance:{' '}
            {formatMoney(folio.balanceDue, folio.currency)}
          </p>
        </div>
      )}

      <div className="card">
        <h2>Status override</h2>
        <form className="search-form" onSubmit={handleOverride}>
          <label>
            Target status
            <select value={overrideStatus} onChange={(e) => setOverrideStatus(e.target.value)}>
              <option value="NO_SHOW">NO_SHOW</option>
              <option value="CONFIRMED_PAY_LATER">CONFIRMED_PAY_LATER</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </label>
          <label>
            Reason (required)
            <input
              value={overrideReason}
              onChange={(e) => setOverrideReason(e.target.value)}
              required
              placeholder="Explain why this override is needed"
            />
          </label>
          <button type="submit" className="secondary">
            Apply override
          </button>
        </form>
      </div>

      <div className="card">
        <h2>Ledger</h2>
        <ul className="breakdown">
          {booking.ledger?.map((entry, i) => (
            <li key={i}>
              {entry.entryType}: {formatMoney(entry.amount, booking.currency)}
            </li>
          ))}
        </ul>
      </div>

      <div className="card">
        <h2>Audit log</h2>
        <ul className="breakdown">
          {booking.auditLog?.map((entry, i) => (
            <li key={i}>
              {entry.fromStatus || '—'} → {entry.toStatus} ({entry.triggerSource})
              {entry.reason ? ` — ${entry.reason}` : ''}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
