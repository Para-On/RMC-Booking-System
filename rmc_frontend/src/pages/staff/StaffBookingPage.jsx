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
import { formatStaffDateTime, formatStayRange } from '@/lib/formatDates'
import {
  checkInBooking,
  checkOutBooking,
  getStaffBooking,
  manualRefundBooking,
  overrideBookingStatus,
  refundBooking,
  transferRoomBooking,
} from '@/staffApi'
import { canProcessRefunds } from '@/staffAuth'

const MANUAL_REFUND_METHODS = [
  { value: 'GCASH', label: 'GCash' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CASH', label: 'Cash' },
  { value: 'OTHER', label: 'Other' },
]

function formatInstant(value) {
  return formatStaffDateTime(value)
}

export default function StaffBookingPage() {
  const { id } = useParams()
  const [booking, setBooking] = useState(null)
  const [roomUnitId, setRoomUnitId] = useState('')
  const [transferRoomId, setTransferRoomId] = useState('')
  const [transferReason, setTransferReason] = useState('')
  const [overrideStatus, setOverrideStatus] = useState('NO_SHOW')
  const [overrideReason, setOverrideReason] = useState('')
  const [refundReason, setRefundReason] = useState('')
  const [manualReason, setManualReason] = useState('')
  const [manualReference, setManualReference] = useState('')
  const [manualMethod, setManualMethod] = useState('GCASH')
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
        setTransferRoomId(String(data.assignedRoomUnitId))
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
      if (data.assignedRoomUnitId) {
        setTransferRoomId(String(data.assignedRoomUnitId))
      }
      setActionMsg('Guest checked in.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleTransferRoom() {
    setActionMsg('')
    try {
      const data = await transferRoomBooking(
        id,
        Number(transferRoomId),
        transferReason.trim() || null,
      )
      setBooking(data)
      setTransferReason('')
      if (data.assignedRoomUnitId) {
        setTransferRoomId(String(data.assignedRoomUnitId))
      }
      setActionMsg('Guest transferred to new room.')
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
        `Maya refund ${formatMoney(result.refundedAmount, result.currency)} processed. Status: ${result.status}.`
      )
      await loadBooking()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleManualRefund(e) {
    e.preventDefault()
    setActionMsg('')
    setError('')
    try {
      const result = await manualRefundBooking(id, {
        amount: null,
        reason: manualReason,
        externalReference: manualReference,
        method: manualMethod,
      })
      setManualReason('')
      setManualReference('')
      setActionMsg(
        `Manual refund ${formatMoney(result.refundedAmount, result.currency)} recorded. Status: ${result.status}.`
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
  const canTransferRoom = canCheckOut
  const stayRange = formatStayRange(booking.checkInDate, booking.checkOutDate)
  const refundAmount =
    booking.pendingRefundAmount > 0 ? booking.pendingRefundAmount : booking.refundableAmount
  const mayaRefundDisabled = booking.mayaRefundBlocked

  return (
    <StaffPageShell
      title={booking.reference}
      description={`${booking.guestName} · ${stayRange}`}
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
            {booking.refundStatus === 'PENDING' && (
              <Badge variant="destructive">Refund pending</Badge>
            )}
            {booking.refundStatus === 'FAILED' && (
              <Badge variant="destructive">Refund failed</Badge>
            )}
            {booking.refundStatus === 'COMPLETED' && (
              <Badge variant="outline">Refund completed</Badge>
            )}
            {booking.cancellationTier && (
              <Badge variant="outline">{booking.cancellationTier} cancellation</Badge>
            )}
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
                label="Maya refundable"
                value={formatMoney(booking.refundableAmount, booking.currency)}
              />
            )}
            {booking.pendingRefundAmount != null && booking.pendingRefundAmount > 0 && (
              <DetailItem
                label="Pending refund"
                value={formatMoney(booking.pendingRefundAmount, booking.currency)}
              />
            )}
            {booking.totalRefundedAmount != null && booking.totalRefundedAmount > 0 && (
              <DetailItem
                label="Total refunded"
                value={formatMoney(booking.totalRefundedAmount, booking.currency)}
              />
            )}
            {booking.mayaRefundedAmount != null && booking.mayaRefundedAmount > 0 && (
              <DetailItem
                label="Maya refunded"
                value={formatMoney(booking.mayaRefundedAmount, booking.currency)}
              />
            )}
            {booking.manualRefundedAmount != null && booking.manualRefundedAmount > 0 && (
              <DetailItem
                label="Manual refunded"
                value={formatMoney(booking.manualRefundedAmount, booking.currency)}
              />
            )}
            {booking.refundEligibleAmount != null && (
              <DetailItem
                label="Policy refund cap"
                value={formatMoney(booking.refundEligibleAmount, booking.currency)}
              />
            )}
            {booking.refundPercentApplied != null && (
              <DetailItem label="Refund %" value={`${booking.refundPercentApplied}%`} />
            )}
            {booking.deductionAmount != null && (
              <DetailItem
                label="Deduction"
                value={formatMoney(booking.deductionAmount, booking.currency)}
              />
            )}
            {booking.refundPolicyDescription && (
              <DetailItem label="Policy" value={booking.refundPolicyDescription} />
            )}
            {booking.refundPreview && (
              <DetailItem label="Refund if cancelled now" value={booking.refundPreview} />
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
              Rooms shown are free for this guest&apos;s full stay ({stayRange}), excluding rooms
              with overlapping assigned bookings.
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

      {canTransferRoom && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Transfer room</CardTitle>
            <CardDescription>
              Move this in-house guest to another available room for the remainder of their stay (
              {stayRange}). The previous room becomes available on the room board.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid w-full max-w-sm gap-2">
              <Label htmlFor="transfer-room-unit">New room</Label>
              <Select
                value={transferRoomId || 'none'}
                onValueChange={(v) => setTransferRoomId(v === 'none' ? '' : v)}
              >
                <SelectTrigger id="transfer-room-unit">
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
            <div className="grid w-full max-w-md gap-2">
              <Label htmlFor="transfer-reason">Reason (optional)</Label>
              <Input
                id="transfer-reason"
                value={transferReason}
                onChange={(e) => setTransferReason(e.target.value)}
                placeholder="e.g. Maintenance issue in current room"
              />
            </div>
            <Button
              type="button"
              onClick={handleTransferRoom}
              disabled={
                !transferRoomId || Number(transferRoomId) === booking.assignedRoomUnitId
              }
            >
              Transfer guest
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

      {booking.refundEligible && canProcessRefunds() && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Maya refund</CardTitle>
            <CardDescription>
              {booking.status === 'CANCELLED'
                ? 'Process the guest refund through Maya. Same-day full refunds use void; partial refunds require the next calendar day (Asia/Manila). Manual refunds count toward the policy cap and block duplicate payouts.'
                : 'Full refund via Maya. Booking will be cancelled. Same-day payments may void instantly.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {mayaRefundDisabled && booking.mayaRefundBlockedReason && (
              <StaffAlert>{booking.mayaRefundBlockedReason}</StaffAlert>
            )}
            {mayaRefundDisabled && booking.mayaRefundAvailableAt && (
              <p className="text-sm text-muted-foreground">
                Maya partial refund available after:{' '}
                <span className="font-medium">{formatInstant(booking.mayaRefundAvailableAt)}</span>{' '}
                (Asia/Manila)
              </p>
            )}
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
              <Button type="submit" disabled={mayaRefundDisabled}>
                Refund {formatMoney(refundAmount, booking.currency)} via Maya
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {booking.manualRefundAllowed && canProcessRefunds() && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Manual refund</CardTitle>
            <CardDescription>
              Record a refund paid outside Maya (GCash, bank transfer, or cash). This counts toward
              the policy refund cap and prevents a duplicate Maya refund for the same amount.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid max-w-md gap-4" onSubmit={handleManualRefund}>
              <div className="grid gap-2">
                <Label htmlFor="manual-method">Payment method</Label>
                <Select value={manualMethod} onValueChange={setManualMethod}>
                  <SelectTrigger id="manual-method">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {MANUAL_REFUND_METHODS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="manual-reference">External reference (required)</Label>
                <Input
                  id="manual-reference"
                  value={manualReference}
                  onChange={(e) => setManualReference(e.target.value)}
                  required
                  placeholder="GCash ref, bank txn ID, receipt no."
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="manual-reason">Reason (required)</Label>
                <Input
                  id="manual-reason"
                  value={manualReason}
                  onChange={(e) => setManualReason(e.target.value)}
                  required
                  placeholder="Same-day partial refund paid via GCash"
                />
              </div>
              <Button type="submit" variant="secondary">
                Record manual refund {formatMoney(refundAmount, booking.currency)}
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
                <li key={i} className="border-b pb-2 last:border-0">
                  <div className="flex justify-between gap-4">
                    <span>{entry.entryType}</span>
                    <span className="font-medium">{formatMoney(entry.amount, booking.currency)}</span>
                  </div>
                  {entry.paymentReference && (
                    <p className="mt-1 text-xs text-muted-foreground">Ref: {entry.paymentReference}</p>
                  )}
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
