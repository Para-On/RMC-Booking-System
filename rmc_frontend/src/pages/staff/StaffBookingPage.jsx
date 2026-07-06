import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
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
import {
  checkInBooking,
  checkOutBooking,
  getStaffBooking,
  overrideBookingStatus,
  refundBooking,
} from '@/staffApi'
import { isAdmin } from '@/staffAuth'

export default function StaffBookingPage() {
  const { id } = useParams()
  const [booking, setBooking] = useState(null)
  const [roomUnitId, setRoomUnitId] = useState('')
  const [overrideStatus, setOverrideStatus] = useState('NO_SHOW')
  const [overrideReason, setOverrideReason] = useState('')
  const [refundReason, setRefundReason] = useState('')
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
      if (data.allowedStatusOverrides?.length) {
        setOverrideStatus(data.allowedStatusOverrides[0])
      }
      if (data.assignedRoomUnitId) {
        setRoomUnitId(String(data.assignedRoomUnitId))
      } else if (data.roomNumber && data.availableRooms?.length) {
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

  async function handleRefund(e) {
    e.preventDefault()
    setActionMsg('')
    setError('')
    try {
      const result = await refundBooking(id, null, refundReason)
      setRefundReason('')
      setActionMsg(
        `Refunded ${formatMoney(result.refundedAmount, result.currency)}. Status: ${result.status}.`
      )
      await loadBooking()
    } catch (err) {
      setError(err.message)
    }
  }

  if (loading) {
    return (
      <StaffPageShell title="Booking">
        <p className="text-sm text-muted-foreground">Loading booking…</p>
      </StaffPageShell>
    )
  }

  if (error && !booking) {
    return (
      <StaffPageShell title="Booking">
        <StaffAlert>{error}</StaffAlert>
      </StaffPageShell>
    )
  }

  if (!booking) return null

  const canCheckIn =
    !booking.checkedInAt &&
    (booking.status === 'CONFIRMED' || booking.status === 'CONFIRMED_PAY_LATER')
  const canCheckOut = booking.checkedInAt && !booking.checkedOutAt

  return (
    <StaffPageShell
      title={booking.reference}
      description={`${booking.guestName} · ${booking.checkInDate} → ${booking.checkOutDate}`}
      actions={
        <Button variant="outline" size="sm" asChild>
          <Link to="/staff/arrivals">← Arrivals</Link>
        </Button>
      }
    >
      <StaffAlert variant="success">{actionMsg}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      <Card>
        <CardHeader>
          <div className="flex flex-wrap items-center gap-2">
            <CardTitle className="text-lg">Booking details</CardTitle>
            <Badge variant="secondary">{booking.status}</Badge>
          </div>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <DetailItem label="Guest" value={`${booking.guestName} (${booking.guestEmail})`} />
            <DetailItem label="Phone" value={booking.guestPhone} />
            <DetailItem label="Room type" value={booking.roomTypeName} />
            <DetailItem label="Payment" value={booking.paymentMethod} />
            <DetailItem label="Total" value={formatMoney(booking.quotedTotal, booking.currency)} />
            <DetailItem
              label="Balance due"
              value={formatMoney(booking.ledgerBalance, booking.currency)}
            />
            {booking.refundableAmount != null && (
              <DetailItem
                label="Refundable"
                value={formatMoney(booking.refundableAmount, booking.currency)}
              />
            )}
            <DetailItem label="Assigned room" value={booking.roomNumber || 'Not assigned'} />
            <DetailItem label="Checked in" value={booking.checkedInAt ? 'Yes' : 'No'} />
            <DetailItem label="Checked out" value={booking.checkedOutAt ? 'Yes' : 'No'} />
          </dl>
        </CardContent>
      </Card>

      {canCheckIn && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Check in</CardTitle>
            <CardDescription>
              Rooms shown are free for this guest&apos;s full stay ({booking.checkInDate} →{' '}
              {booking.checkOutDate}), excluding rooms with overlapping assigned bookings.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="grid w-full max-w-sm gap-2">
              <Label htmlFor="room-unit">Assign room (optional)</Label>
              <Select value={roomUnitId || 'none'} onValueChange={(v) => setRoomUnitId(v === 'none' ? '' : v)}>
                <SelectTrigger id="room-unit">
                  <SelectValue placeholder="Select room" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">— Select room —</SelectItem>
                  {booking.availableRooms?.map((room) => (
                    <SelectItem key={room.id} value={String(room.id)}>
                      {room.roomNumber} (floor {room.floorLabel || '—'})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {booking.availableRooms?.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  No rooms are free for the full stay on these dates.
                </p>
              )}
            </div>
            <Button type="button" onClick={handleCheckIn}>
              Check in guest
            </Button>
          </CardContent>
        </Card>
      )}

      {canCheckOut && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Check out</CardTitle>
          </CardHeader>
          <CardContent>
            <Button type="button" onClick={handleCheckOut}>
              Check out guest
            </Button>
          </CardContent>
        </Card>
      )}

      {folio && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Folio summary</CardTitle>
          </CardHeader>
          <CardContent className="text-sm">
            Total: {formatMoney(folio.quotedTotal, folio.currency)} · Paid:{' '}
            {formatMoney(folio.amountPaid, folio.currency)} · Balance:{' '}
            {formatMoney(folio.balanceDue, folio.currency)}
          </CardContent>
        </Card>
      )}

      {booking.refundEligible && isAdmin() && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Maya refund</CardTitle>
            <CardDescription>
              Full refund via Maya. Booking will be cancelled. Maya sandbox may only allow refunds
              from the day after payment.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid max-w-md gap-4" onSubmit={handleRefund}>
              <div className="grid gap-2">
                <Label htmlFor="refund-reason">Reason (required)</Label>
                <Input
                  id="refund-reason"
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  required
                  placeholder="Guest requested cancellation"
                />
              </div>
              <Button type="submit">
                Refund {formatMoney(booking.refundableAmount, booking.currency)}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Status override</CardTitle>
        </CardHeader>
        <CardContent>
          {booking.allowedStatusOverrides?.length ? (
            <form className="grid max-w-md gap-4" onSubmit={handleOverride}>
              <div className="grid gap-2">
                <Label htmlFor="override-status">Target status</Label>
                <Select value={overrideStatus} onValueChange={setOverrideStatus}>
                  <SelectTrigger id="override-status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(booking.allowedStatusOverrides || []).map((status) => (
                      <SelectItem key={status} value={status}>
                        {status}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="override-reason">Reason (required)</Label>
                <Input
                  id="override-reason"
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  required
                  placeholder="Explain why this override is needed"
                />
              </div>
              <Button type="submit" variant="secondary">
                Apply override
              </Button>
            </form>
          ) : (
            <p className="text-sm text-muted-foreground">No status overrides available for this booking.</p>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Ledger</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2 text-sm">
              {booking.ledger?.map((entry, i) => (
                <li key={i} className="flex justify-between gap-4 border-b pb-2 last:border-0">
                  <span>{entry.entryType}</span>
                  <span className="font-medium">{formatMoney(entry.amount, booking.currency)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Audit log</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2 text-sm">
              {booking.auditLog?.map((entry, i) => (
                <li key={i} className="border-b pb-2 last:border-0">
                  {entry.fromStatus || '—'} → {entry.toStatus} ({entry.triggerSource})
                  {entry.reason ? ` — ${entry.reason}` : ''}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </StaffPageShell>
  )
}

function DetailItem({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm">{value}</dd>
    </div>
  )
}
