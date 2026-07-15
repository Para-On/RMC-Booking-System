import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import { StaffGuestIdentity } from '@/components/staff/StaffTable'
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
  approvePayLaterBooking,
  checkInBooking,
  checkOutBooking,
  createAdditionalCharge,
  getStaffBooking,
  manualRefundBooking,
  overrideBookingStatus,
  recordAdditionalChargePayment,
  recordFolioPayment,
  refundBooking,
  rejectPayLaterBooking,
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

const AUDIT_TRIGGER_LABELS = {
  STAFF_CHECK_IN: 'Check-in',
  STAFF_CHECK_OUT: 'Check-out',
  STAFF_APPROVE_PAY_LATER: 'Approve pay-later',
  STAFF_APPROVE_MAYA: 'Approve Maya booking',
  STAFF_REJECT_PAY_LATER: 'Reject pay-later',
  STAFF_REJECT_MAYA: 'Reject Maya booking',
  STAFF_REJECT_MAYA_REFUND_PENDING: 'Reject Maya booking (refund queued)',
  STAFF_OVERRIDE: 'Status override',
  STAFF_OVERRIDE_REFUND_PENDING: 'Cancelled — refund queued',
  STAFF_ROOM_TRANSFER: 'Room transfer',
  STAFF_REFUND: 'Refund',
  STAFF_PARTIAL_REFUND: 'Partial refund',
  STAFF_VOID: 'Void',
  STAFF_MANUAL_REFUND: 'Manual refund',
  STAFF_CREATE_ADDITIONAL_CHARGE: 'Create additional charge',
  STAFF_APPROVE_ADDITIONAL_CHARGE: 'Approve additional charge',
  STAFF_REJECT_ADDITIONAL_CHARGE: 'Reject additional charge',
  STAFF_RECORD_CHARGE_PAYMENT: 'Record charge payment',
  STAFF_RECORD_FOLIO_PAYMENT: 'Record folio payment',
  GUEST_CHARGE_MAYA: 'Guest pay charge (Maya)',
  GUEST_CHARGE_PAY_AT_HOTEL: 'Guest request charge pay-at-hotel',
  GUEST_BOOKING: 'Guest booking',
  GUEST_BOOKING_PAY_LATER: 'Guest pay-later request',
  GUEST_BOOKING_MAYA: 'Guest booking (Maya)',
  GUEST_CANCEL: 'Guest cancellation',
  GUEST_CANCEL_REFUND_PENDING: 'Guest cancellation (refund pending)',
  MAYA_WEBHOOK: 'Maya payment',
  MAYA_CONFIRM_POLL: 'Maya confirmation',
  MAYA_CHARGE_CONFIRM_POLL: 'Maya charge confirmation',
}

function auditTriggerLabel(triggerSource) {
  return AUDIT_TRIGGER_LABELS[triggerSource] || triggerSource || 'Update'
}

function latestAuditByTrigger(auditLog, triggerSource) {
  if (!auditLog?.length) return null
  for (let i = auditLog.length - 1; i >= 0; i -= 1) {
    if (auditLog[i].triggerSource === triggerSource) return auditLog[i]
  }
  return null
}

function formatActorSummary(entry, fallbackTime) {
  if (!entry && !fallbackTime) return '—'
  const when = formatInstant(entry?.createdAt || fallbackTime)
  if (entry?.staffName) {
    return `${when} · by ${entry.staffName}`
  }
  if (when) return when
  return '—'
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
  const [chargeDescription, setChargeDescription] = useState('')
  const [chargeAmount, setChargeAmount] = useState('')

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

  async function handleApprovePayLater() {
    setActionMsg('')
    setError('')
    try {
      const data = await approvePayLaterBooking(id)
      setBooking(data)
      setActionMsg(
        booking?.paymentMethod === 'ONLINE_MAYA'
          ? 'Paid booking approved. Guest confirmation email will be sent.'
          : 'Booking approved. Guest confirmation email will be sent.'
      )
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleRejectPayLater() {
    const paidMaya = booking?.paymentMethod === 'ONLINE_MAYA'
    const reason = window.prompt(
      paidMaya
        ? 'Optional reason for rejecting this paid booking (a refund will be queued):'
        : 'Optional reason for rejecting this booking:',
      ''
    )
    if (reason === null) return
    setActionMsg('')
    setError('')
    try {
      const data = await rejectPayLaterBooking(id, reason.trim() || null)
      setBooking(data)
      setActionMsg(
        paidMaya
          ? 'Booking rejected. Refund queued — process it under Refund below.'
          : 'Booking rejected and cancelled.'
      )
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCheckOut() {
    setActionMsg('')
    setError('')
    try {
      const data = await checkOutBooking(id)
      setFolio(data)
      await loadBooking()
      setActionMsg('Guest checked out.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleRecordFolioPayment() {
    setActionMsg('')
    setError('')
    try {
      const data = await recordFolioPayment(id)
      setBooking(data)
      setActionMsg('Outstanding balance recorded as paid. Revenue updated.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreateCharge(e) {
    e.preventDefault()
    setActionMsg('')
    setError('')
    try {
      await createAdditionalCharge(id, chargeDescription.trim(), Number(chargeAmount))
      setChargeDescription('')
      setChargeAmount('')
      await loadBooking()
      setActionMsg('Additional charge added to booking balance.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleRecordChargePayment(chargeId) {
    setActionMsg('')
    setError('')
    try {
      await recordAdditionalChargePayment(id, chargeId)
      await loadBooking()
      setActionMsg('Charge marked paid at hotel. Revenue updated.')
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

  const canApprovePayLater = booking.status === 'PENDING_APPROVAL'
  const canCheckIn =
    !booking.checkedInAt &&
    (booking.status === 'CONFIRMED' || booking.status === 'CONFIRMED_PAY_LATER')
  const canCheckOut = booking.checkedInAt && !booking.checkedOutAt
  const canTransferRoom = canCheckOut
  const checkInAudit = latestAuditByTrigger(booking.auditLog, 'STAFF_CHECK_IN')
  const checkOutAudit = latestAuditByTrigger(booking.auditLog, 'STAFF_CHECK_OUT')
  const approveAudit = latestAuditByTrigger(booking.auditLog, 'STAFF_APPROVE_PAY_LATER')
  const overrideAudit =
    latestAuditByTrigger(booking.auditLog, 'STAFF_OVERRIDE_REFUND_PENDING') ||
    latestAuditByTrigger(booking.auditLog, 'STAFF_OVERRIDE')
  const transferAudit = latestAuditByTrigger(booking.auditLog, 'STAFF_ROOM_TRANSFER')
  const hasGuestRefundRequest =
    booking.refundStatus === 'PENDING' || booking.refundStatus === 'FAILED'
  const stayRange = formatStayRange(booking.checkInDate, booking.checkOutDate)
  const refundAmount =
    hasGuestRefundRequest && Number(booking.pendingRefundAmount) > 0
      ? booking.pendingRefundAmount
      : booking.refundableAmount
  const isMayaPayment = booking.paymentMethod === 'ONLINE_MAYA'
  const isPayLaterPayment = booking.paymentMethod === 'PAY_AT_HOTEL'
  const mayaRefundDisabled = Boolean(booking.mayaRefundBlocked) || !booking.refundEligible
  const showRefundSection =
    canProcessRefunds() &&
    !booking.checkedOutAt &&
    ((isMayaPayment &&
      (booking.refundEligible ||
        hasGuestRefundRequest ||
        (booking.status === 'CONFIRMED' && Number(booking.refundableAmount) > 0))) ||
      (isPayLaterPayment && hasGuestRefundRequest))
  const showManualRefund = booking.manualRefundAllowed && canProcessRefunds()
  const refundStepLabel =
    booking.status === 'CANCELLED' && hasGuestRefundRequest
      ? 'Step 2 — Complete refund'
      : booking.status === 'CONFIRMED'
        ? 'Refund & cancel'
        : 'Refund'
  const unpaidChargeStatuses = new Set([
    'APPROVED_UNPAID',
    'AWAITING_PAYMENT',
    'PENDING_APPROVAL',
    'PENDING_MAYA',
  ])

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
            <Badge
              variant={booking.status === 'PENDING_APPROVAL' ? 'destructive' : 'secondary'}
            >
              {booking.status}
            </Badge>
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
            <DetailItem
              label="Occupancy"
              value={`${booking.guestCount || booking.occupants?.length || 1} guest${
                (booking.guestCount || booking.occupants?.length || 1) === 1 ? '' : 's'
              }`}
            />
            {booking.occupants?.length > 0 ? (
              <div className="sm:col-span-2 lg:col-span-3">
                <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Guests in room
                </dt>
                <dd className="mt-2 space-y-2.5 text-sm text-foreground">
                  {booking.occupants.map((occupant, index) => (
                    <StaffGuestIdentity
                      key={`${occupant.fullName}-${index}`}
                      guestId={occupant.guestId}
                      name={`${occupant.fullName}${occupant.primary ? ' (primary)' : ''}`}
                      email={occupant.email}
                      phone={occupant.phone}
                    />
                  ))}
                </dd>
              </div>
            ) : null}
            <DetailItem label="Room type" value={booking.roomTypeName} />
            <DetailItem label="Payment" value={booking.paymentMethod} />
            <DetailItem label="Total" value={formatMoney(booking.quotedTotal, booking.currency)} />
            <DetailItem
              label="Amount paid"
              value={formatMoney(booking.amountPaid ?? 0, booking.currency)}
            />
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
            {hasGuestRefundRequest &&
              booking.pendingRefundAmount != null &&
              booking.pendingRefundAmount > 0 && (
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
              <DetailItem
                label={
                  booking.status === 'CANCELLED'
                    ? 'Refund applied at cancel'
                    : 'Refund if cancelled now'
                }
                value={booking.refundPreview}
              />
            )}
            <DetailItem label="Assigned room" value={booking.roomNumber || 'Not assigned'} />
            {transferAudit ? (
              <DetailItem
                label="Last room transfer"
                value={`${transferAudit.reason || 'Room transferred'} · ${formatActorSummary(transferAudit)}`}
              />
            ) : null}
            <DetailItem
              label="Checked in"
              value={
                booking.checkedInAt || checkInAudit
                  ? formatActorSummary(checkInAudit, booking.checkedInAt)
                  : 'No'
              }
            />
            <DetailItem
              label="Checked out"
              value={
                booking.checkedOutAt || checkOutAudit
                  ? formatActorSummary(checkOutAudit, booking.checkedOutAt)
                  : 'No'
              }
            />
            {approveAudit ? (
              <DetailItem
                label="Approved"
                value={formatActorSummary(approveAudit, approveAudit.createdAt)}
              />
            ) : null}
            {overrideAudit ? (
              <DetailItem
                label="Last status override"
                value={`${overrideAudit.fromStatus || '—'} → ${overrideAudit.toStatus} · ${formatActorSummary(overrideAudit)}`}
              />
            ) : null}
          </dl>
        </CardContent>
      </Card>

      {canApprovePayLater && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">
              {booking.paymentMethod === 'ONLINE_MAYA'
                ? 'Approve paid Maya booking'
                : 'Approve pay-at-hotel booking'}
            </CardTitle>
            <CardDescription>
              {booking.paymentMethod === 'ONLINE_MAYA'
                ? 'Payment is already captured. Approving confirms the stay and emails the guest. Rejecting cancels the booking and queues a refund.'
                : 'This reservation is holding inventory but is not confirmed yet. Approving notifies the guest by email. Rejecting cancels the request and frees the rooms.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-3">
            <Button type="button" onClick={handleApprovePayLater}>
              Approve booking
            </Button>
            <Button type="button" variant="outline" onClick={handleRejectPayLater}>
              Reject
            </Button>
          </CardContent>
        </Card>
      )}

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
            <CardDescription>
              Guest must have a zero balance and no unpaid additional charges before check-out.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {Number(booking.ledgerBalance) > 0 && (
              <StaffAlert>
                Balance due {formatMoney(booking.ledgerBalance, booking.currency)}. Record payment
                first.
              </StaffAlert>
            )}
            {Number(booking.ledgerBalance) > 0 && (
              <Button type="button" variant="secondary" onClick={handleRecordFolioPayment}>
                Record outstanding payment
              </Button>
            )}
            <Button
              type="button"
              onClick={handleCheckOut}
              disabled={Number(booking.ledgerBalance) > 0}
            >
              Check out guest
            </Button>
          </CardContent>
        </Card>
      )}

      {showRefundSection && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{refundStepLabel}</CardTitle>
            <CardDescription>
              {isPayLaterPayment && hasGuestRefundRequest
                ? 'Pay-later booking is cancelled. Record a manual GCash, bank, or cash payout for the guest.'
                : booking.status === 'CANCELLED' && hasGuestRefundRequest
                  ? 'The booking is already cancelled. Return the guest’s money via Maya, or record a manual payout if Maya is unavailable.'
                  : booking.status === 'CONFIRMED'
                    ? 'Option A: Refund via Maya below (cancels the booking). Option B: Status override → Cancelled to queue a refund, then complete it here.'
                    : 'Process the guest refund.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {hasGuestRefundRequest && Number(booking.pendingRefundAmount) > 0 && (
              <StaffAlert variant="success">
                Refund queued: {formatMoney(booking.pendingRefundAmount, booking.currency)} (
                {booking.refundStatus})
              </StaffAlert>
            )}
            {!booking.refundEligible && booking.status === 'CONFIRMED' && booking.refundPreview && (
              <StaffAlert>{booking.refundPreview}</StaffAlert>
            )}
            {isMayaPayment && booking.mayaRefundBlocked && booking.mayaRefundBlockedReason && (
              <StaffAlert>{booking.mayaRefundBlockedReason}</StaffAlert>
            )}
            {isMayaPayment && booking.mayaRefundBlocked && booking.mayaRefundAvailableAt && (
              <p className="text-sm text-muted-foreground">
                Maya partial refund available after{' '}
                <span className="font-medium">{formatInstant(booking.mayaRefundAvailableAt)}</span>{' '}
                (Asia/Manila). Use manual refund below if you need to pay the guest today.
              </p>
            )}

            {isMayaPayment && (
              <form className="grid max-w-md gap-4" onSubmit={handleRefund}>
                <div className="grid gap-2">
                  <Label htmlFor="refund-reason">Maya refund reason</Label>
                  <Input
                    id="refund-reason"
                    value={refundReason}
                    onChange={(e) => setRefundReason(e.target.value)}
                    required
                    placeholder="Guest requested cancellation"
                  />
                </div>
                <Button type="submit" disabled={mayaRefundDisabled}>
                  {booking.status === 'CONFIRMED'
                    ? `Refund ${formatMoney(refundAmount, booking.currency)} via Maya & cancel`
                    : `Refund ${formatMoney(refundAmount, booking.currency)} via Maya`}
                </Button>
              </form>
            )}

            {showManualRefund ? (
              <form
                className={`grid max-w-md gap-4 ${isMayaPayment ? 'border-t pt-6' : ''}`}
                onSubmit={handleManualRefund}
              >
                <p className="text-sm text-muted-foreground">
                  {isPayLaterPayment
                    ? 'Manual refund — record a GCash, bank, or cash payout returned to the guest for pay-later payments.'
                    : 'Manual refund — record a GCash, bank, or cash payout already made outside Maya.'}
                </p>
                <div className="grid gap-2">
                  <Label htmlFor="manual-method">Payout method</Label>
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
                  <Label htmlFor="manual-reference">External reference</Label>
                  <Input
                    id="manual-reference"
                    value={manualReference}
                    onChange={(e) => setManualReference(e.target.value)}
                    required
                    placeholder="GCash ref, bank txn ID, receipt no."
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="manual-reason">Reason</Label>
                  <Input
                    id="manual-reason"
                    value={manualReason}
                    onChange={(e) => setManualReason(e.target.value)}
                    required
                    placeholder="Paid via GCash today"
                  />
                </div>
                <Button type="submit" variant="secondary">
                  Record manual refund {formatMoney(refundAmount, booking.currency)}
                </Button>
              </form>
            ) : (
              booking.manualRefundEnabled === false &&
              hasGuestRefundRequest && (
                <p className="text-sm text-muted-foreground">
                  {isPayLaterPayment
                    ? 'Manual refunds are turned off in Settings. Enable them to record GCash/bank/cash payouts for cancelled pay-later bookings.'
                    : 'Manual refunds are turned off in Settings. Enable them to record GCash/bank/cash payouts when Maya cannot pay out yet.'}
                </p>
              )
            )}
          </CardContent>
        </Card>
      )}

      {(booking.status === 'CONFIRMED' || booking.status === 'CONFIRMED_PAY_LATER') &&
        !booking.checkedOutAt && (
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Additional charges</CardTitle>
              <CardDescription>
                Create a charge to add it to the booking balance immediately. Mark it Paid when the
                guest settles at the desk (adds revenue), or the guest can pay online with Maya from
                Find booking.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <form className="grid max-w-lg gap-3" onSubmit={handleCreateCharge}>
                <div className="grid gap-2">
                  <Label htmlFor="charge-description">Description</Label>
                  <Input
                    id="charge-description"
                    value={chargeDescription}
                    onChange={(e) => setChargeDescription(e.target.value)}
                    required
                    placeholder="e.g. Mini bar, laundry"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="charge-amount">Amount ({booking.currency})</Label>
                  <Input
                    id="charge-amount"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={chargeAmount}
                    onChange={(e) => setChargeAmount(e.target.value)}
                    required
                  />
                </div>
                <Button type="submit">Create charge</Button>
              </form>

              {(booking.additionalCharges || []).length === 0 ? (
                <p className="text-sm text-muted-foreground">No additional charges yet.</p>
              ) : (
                <ul className="space-y-3">
                  {booking.additionalCharges.map((charge) => (
                    <li
                      key={charge.id}
                      className="rounded-lg border border-border p-3 text-sm space-y-2"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <p className="font-medium">{charge.description}</p>
                          <p className="text-muted-foreground">
                            {charge.status === 'APPROVED_UNPAID' || charge.status === 'AWAITING_PAYMENT'
                              ? 'Unpaid'
                              : charge.status}
                            {charge.paymentMethod ? ` · ${charge.paymentMethod}` : ''}
                          </p>
                        </div>
                        <p className="font-medium">
                          {formatMoney(charge.amount, charge.currency || booking.currency)}
                        </p>
                      </div>
                      {unpaidChargeStatuses.has(charge.status) && (
                        <Button
                          type="button"
                          size="sm"
                          onClick={() => handleRecordChargePayment(charge.id)}
                        >
                          Mark paid
                        </Button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
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
            <CardDescription>
              Status changes, check-in/out, overrides, and refunds with the staff member responsible.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {booking.auditLog?.length ? (
              <ul className="space-y-3 text-sm">
                {booking.auditLog.map((entry, i) => (
                  <li key={i} className="border-b pb-3 last:border-0">
                    <div className="font-medium">
                      {auditTriggerLabel(entry.triggerSource)}
                      {entry.fromStatus || entry.toStatus
                        ? `: ${entry.fromStatus || '—'} → ${entry.toStatus}`
                        : ''}
                    </div>
                    <div className="mt-1 text-muted-foreground">
                      {formatInstant(entry.createdAt)}
                      {entry.staffName
                        ? ` · by ${entry.staffName}`
                        : entry.staffUserId
                          ? ` · staff #${entry.staffUserId}`
                          : ' · System'}
                    </div>
                    {entry.reason ? (
                      <p className="mt-1 text-xs text-muted-foreground">Reason: {entry.reason}</p>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">No audit entries yet.</p>
            )}
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
