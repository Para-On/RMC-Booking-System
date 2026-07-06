import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { formatMoney } from '@/api'
import { checkOutBooking, getStaffBooking } from '@/staffApi'

export default function RoomOpsBookingDialog({ row, open, onOpenChange, onUpdated }) {
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(false)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!open || !row?.bookingId) {
      setBooking(null)
      setError('')
      setMessage('')
      return
    }
    loadBooking()
  }, [open, row?.bookingId])

  async function loadBooking() {
    setLoading(true)
    setError('')
    try {
      const data = await getStaffBooking(row.bookingId)
      setBooking(data)
    } catch (err) {
      setError(err.message)
      setBooking(null)
    } finally {
      setLoading(false)
    }
  }

  async function handleCheckOut() {
    setActing(true)
    setError('')
    setMessage('')
    try {
      await checkOutBooking(row.bookingId)
      setMessage('Guest checked out.')
      onUpdated?.()
      await loadBooking()
    } catch (err) {
      setError(err.message)
    } finally {
      setActing(false)
    }
  }

  const canCheckOut = booking?.checkedInAt && !booking?.checkedOutAt

  return (
    <StaffModal
      open={open}
      onOpenChange={onOpenChange}
      title={`Room ${row?.roomNumber ?? ''}`}
      description={
        [row?.roomTypeName, row?.floorLabel && `Floor ${row.floorLabel}`]
          .filter(Boolean)
          .join(' · ') || 'Booking details'
      }
      size="md"
      footer={
        <>
          {booking && (
            <Button variant="link" className="mr-auto h-auto px-0 sm:mr-0" asChild>
              <Link to={`/staff/bookings/${booking.bookingId}`}>Full booking page</Link>
            </Button>
          )}
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          {canCheckOut && (
            <Button type="button" onClick={handleCheckOut} disabled={acting}>
              {acting ? 'Checking out…' : 'Check out'}
            </Button>
          )}
        </>
      }
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      {loading && <p className="text-sm text-muted-foreground">Loading…</p>}

      {!loading && booking && (
        <dl className="grid gap-4 text-sm sm:grid-cols-2">
          <Detail label="Guest" value={booking.guestName} />
          <Detail label="Email" value={booking.guestEmail} />
          <Detail label="Reference" value={booking.reference} />
          <Detail label="Stay" value={`${booking.checkInDate} → ${booking.checkOutDate}`} />
          <Detail label="Status" value={booking.status} />
          <Detail label="Payment" value={booking.paymentMethod} />
          <Detail label="Balance" value={formatMoney(booking.ledgerBalance, booking.currency)} />
          {row?.checkoutAlert && (
            <div className="sm:col-span-2">
              <dt className="text-muted-foreground">Checkout reminder</dt>
              <dd className="mt-1">
                {row.checkoutAlert === 'TODAY' ? (
                  <Badge variant="destructive">Checking out today</Badge>
                ) : (
                  <Badge variant="outline">Checking out tomorrow</Badge>
                )}
              </dd>
            </div>
          )}
        </dl>
      )}
    </StaffModal>
  )
}

function Detail({ label, value }) {
  return (
    <div>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium">{value}</dd>
    </div>
  )
}
