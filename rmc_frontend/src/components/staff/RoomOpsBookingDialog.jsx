import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { formatMoney } from '@/api'
import { formatStayRange } from '@/lib/formatDates'
import { checkOutBooking, getStaffBooking, transferRoomBooking } from '@/staffApi'

export default function RoomOpsBookingDialog({ row, open, onOpenChange, onUpdated }) {
  const [booking, setBooking] = useState(null)
  const [transferRoomId, setTransferRoomId] = useState('')
  const [transferReason, setTransferReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!open || !row?.bookingId) {
      setBooking(null)
      setTransferRoomId('')
      setTransferReason('')
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
      if (data.assignedRoomUnitId) {
        setTransferRoomId(String(data.assignedRoomUnitId))
      }
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
      const data = await checkOutBooking(row.bookingId)
      const paid = data?.amountPaid != null ? Number(data.amountPaid) : 0
      const balance = data?.balanceDue != null ? Number(data.balanceDue) : 0
      setMessage(
        paid > 0 && balance <= 0
          ? `Guest checked out. Payment of ${formatMoney(data.amountPaid, data.currency)} recorded.`
          : 'Guest checked out.'
      )
      onUpdated?.()
      await loadBooking()
    } catch (err) {
      setError(err.message)
    } finally {
      setActing(false)
    }
  }

  async function handleTransferRoom() {
    setActing(true)
    setError('')
    setMessage('')
    try {
      const data = await transferRoomBooking(
        row.bookingId,
        Number(transferRoomId),
        transferReason.trim() || null,
      )
      setBooking(data)
      setTransferReason('')
      if (data.assignedRoomUnitId) {
        setTransferRoomId(String(data.assignedRoomUnitId))
      }
      setMessage('Guest transferred to new room.')
      onUpdated?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setActing(false)
    }
  }

  const canCheckOut = booking?.checkedInAt && !booking?.checkedOutAt
  const canTransferRoom = canCheckOut
  const stayRange = booking
    ? formatStayRange(booking.checkInDate, booking.checkOutDate)
    : ''

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
        <>
          <dl className="grid gap-4 text-sm sm:grid-cols-2">
            <Detail label="Guest" value={booking.guestName} />
            <Detail label="Email" value={booking.guestEmail} />
            <Detail label="Reference" value={booking.reference} />
            <Detail label="Stay" value={stayRange} />
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

          {canTransferRoom && (
            <div className="mt-4 space-y-3 border-t pt-4">
              <p className="text-sm font-medium">Transfer room</p>
              <div className="grid gap-3">
                <div className="grid gap-2">
                  <Label htmlFor="ops-transfer-room">New room</Label>
                  <Select
                    value={transferRoomId || 'none'}
                    onValueChange={(v) => setTransferRoomId(v === 'none' ? '' : v)}
                  >
                    <SelectTrigger id="ops-transfer-room">
                      <SelectValue placeholder="Select room" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">— Select room —</SelectItem>
                      {booking.availableRooms?.map((room) => (
                        <SelectItem key={room.id} value={String(room.id)}>
                          {room.roomNumber} (floor {room.floorLabel || '—'})
                          {room.id === booking.assignedRoomUnitId ? ' · current' : ''}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="ops-transfer-reason">Reason (optional)</Label>
                  <Input
                    id="ops-transfer-reason"
                    value={transferReason}
                    onChange={(e) => setTransferReason(e.target.value)}
                    placeholder="e.g. Guest request"
                  />
                </div>
                <Button
                  type="button"
                  size="sm"
                  className="w-fit"
                  onClick={handleTransferRoom}
                  disabled={
                    acting ||
                    !transferRoomId ||
                    Number(transferRoomId) === booking.assignedRoomUnitId
                  }
                >
                  {acting ? 'Transferring…' : 'Transfer guest'}
                </Button>
              </div>
            </div>
          )}
        </>
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
