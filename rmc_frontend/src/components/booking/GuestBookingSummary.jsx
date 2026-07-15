import {
  CalendarDays,
  CircleDollarSign,
  ConciergeBell,
  CreditCard,
  Package,
  Users,
} from 'lucide-react'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { catalogFromBooking } from '@/lib/roomCatalog'
import { formatStayRange } from '@/lib/formatDates'
import { cn } from '@/lib/utils'
import { formatMoney } from '@/api'

export const CHARGE_STATUS_LABELS = {
  AWAITING_PAYMENT: 'Unpaid',
  PENDING_MAYA: 'Maya payment pending',
  PENDING_APPROVAL: 'Unpaid',
  APPROVED_UNPAID: 'Unpaid',
  PAID: 'Paid',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

export function bookingStatusMeta(status) {
  switch (status) {
    case 'CONFIRMED':
      return { label: 'Confirmed', variant: 'default' }
    case 'CONFIRMED_PAY_LATER':
      return { label: 'Confirmed · pay at hotel', variant: 'default' }
    case 'PENDING_APPROVAL':
      return { label: 'Awaiting approval', variant: 'secondary' }
    case 'PENDING_PAYMENT':
      return { label: 'Pending payment', variant: 'secondary' }
    case 'CANCELLED':
      return { label: 'Cancelled', variant: 'destructive' }
    case 'NO_SHOW':
      return { label: 'No show', variant: 'outline' }
    default:
      return { label: status?.replaceAll('_', ' ') || 'Unknown', variant: 'outline' }
  }
}

export function paymentMethodLabel(method) {
  switch (method) {
    case 'ONLINE_MAYA':
      return 'Pay online (Maya)'
    case 'PAY_AT_HOTEL':
      return 'Pay at hotel on arrival'
    default:
      return method?.replaceAll('_', ' ') || '—'
  }
}

function DetailRow({ icon: Icon, label, children, className }) {
  return (
    <div className={cn('flex gap-3', className)}>
      <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
        <Icon className="h-4 w-4" aria-hidden />
      </div>
      <div className="min-w-0 flex-1 space-y-0.5">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
        <div className="text-sm leading-relaxed text-foreground">{children}</div>
      </div>
    </div>
  )
}

function ExtrasList({ title, icon: Icon, items }) {
  if (!items?.length) return null
  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-muted-foreground" aria-hidden />
          <CardTitle className="text-base">{title}</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2 text-sm">
          {items.map((item, index) => (
            <li
              key={`${item.key}-${index}`}
              className="flex items-start justify-between gap-3 border-b border-border/60 pb-2 last:border-0 last:pb-0"
            >
              <span>{item.label}</span>
              {item.amount != null ? (
                <span className="shrink-0 font-medium tabular-nums text-muted-foreground">
                  {item.amount}
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

export default function GuestBookingSummary({
  booking,
  pricingPolicy,
  showPaymentSummary = false,
  additionalCharges = [],
  onPayCharge,
  payingChargeId = null,
  footer = null,
}) {
  const catalog = catalogFromBooking(booking, { pricingPolicy })
  const status = bookingStatusMeta(booking.status)
  const guestCount = booking.guestCount || booking.occupants?.length || 1
  const otherGuests =
    booking.occupants?.filter((occupant) => !occupant.primary).map((o) => o.fullName) || []
  const balanceDue = booking.balanceDue != null ? Number(booking.balanceDue) : 0
  const amountPaid = booking.amountPaid != null ? Number(booking.amountPaid) : 0
  const currency = booking.currency || 'PHP'
  const stayRange = formatStayRange(booking.checkIn || booking.checkInDate, booking.checkOut || booking.checkOutDate)

  const serviceItems =
    booking.serviceAddons?.map((service, index) => ({
      key: `${service.title}-${index}`,
      label: service.title,
      amount:
        service.lineTotal > 0 ? formatMoney(service.lineTotal, currency) : 'Included',
    })) || []

  const itemAddonItems =
    booking.itemAddons?.map((item, index) => ({
      key: `${item.itemName}-${index}`,
      label: item.itemName,
      amount:
        item.unitPrice != null && Number(item.unitPrice) > 0
          ? formatMoney(item.unitPrice, currency)
          : 'Complimentary',
    })) || []

  return (
    <div className="space-y-4">
      <Card className="overflow-hidden border-border/80 shadow-sm">
        <CardHeader className="space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="space-y-1">
              <CardDescription>Booking reference</CardDescription>
              <CardTitle className="font-mono text-xl tracking-tight sm:text-2xl">
                {booking.reference}
              </CardTitle>
            </div>
            <Badge variant={status.variant} className="h-6 px-2.5 text-[11px]">
              {status.label}
            </Badge>
          </div>
          {catalog ? (
            <div className="booking-room-card -mx-1 overflow-hidden rounded-xl border border-border/60">
              <RoomCatalogCard {...catalog} compact />
            </div>
          ) : null}
        </CardHeader>
      </Card>

      <Card className="border-border/80 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Stay details</CardTitle>
          <CardDescription>Guest, room, and payment information.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <DetailRow icon={Users} label="Guests">
            <p className="font-medium">{booking.guestName}</p>
            {booking.guestEmail ? (
              <p className="text-muted-foreground">{booking.guestEmail}</p>
            ) : null}
            <p className="text-muted-foreground">{guestCount} guest{guestCount === 1 ? '' : 's'}</p>
            {otherGuests.length > 0 ? (
              <p className="text-muted-foreground">Also: {otherGuests.join(', ')}</p>
            ) : null}
          </DetailRow>

          <DetailRow icon={CalendarDays} label="Dates">
            <p className="font-medium">{stayRange}</p>
            <p className="text-muted-foreground">{booking.roomTypeName}</p>
          </DetailRow>

          <DetailRow icon={CreditCard} label="Payment method">
            <p className="font-medium">{paymentMethodLabel(booking.paymentMethod)}</p>
          </DetailRow>

          <DetailRow icon={CircleDollarSign} label="Booking total">
            <p className="font-medium tabular-nums">
              {formatMoney(booking.quotedTotal, currency)}
            </p>
          </DetailRow>
        </CardContent>
      </Card>

      {serviceItems.length > 0 ? (
        <ExtrasList title="Services" icon={ConciergeBell} items={serviceItems} />
      ) : null}

      {itemAddonItems.length > 0 ? (
        <ExtrasList title="Requested items" icon={Package} items={itemAddonItems} />
      ) : null}

      {booking.customExtrasRequest ? (
        <Card className="border-border/80 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Other requests</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm leading-relaxed text-muted-foreground">
              {booking.customExtrasRequest}
            </p>
          </CardContent>
        </Card>
      ) : null}

      {showPaymentSummary ? (
        <Card className="border-border/80 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Payment summary</CardTitle>
            <CardDescription>Amounts recorded on this booking.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid gap-3 sm:grid-cols-3">
              <div className="rounded-lg border border-border/70 bg-muted/30 px-3 py-2.5">
                <dt className="text-xs text-muted-foreground">Booking total</dt>
                <dd className="mt-1 text-lg font-semibold tabular-nums">
                  {formatMoney(booking.quotedTotal, currency)}
                </dd>
              </div>
              <div className="rounded-lg border border-border/70 bg-muted/30 px-3 py-2.5">
                <dt className="text-xs text-muted-foreground">Paid</dt>
                <dd className="mt-1 text-lg font-semibold tabular-nums text-emerald-700 dark:text-emerald-400">
                  {formatMoney(amountPaid, currency)}
                </dd>
              </div>
              <div className="rounded-lg border border-border/70 bg-muted/30 px-3 py-2.5">
                <dt className="text-xs text-muted-foreground">Balance due</dt>
                <dd
                  className={cn(
                    'mt-1 text-lg font-semibold tabular-nums',
                    balanceDue > 0 ? 'text-amber-700 dark:text-amber-400' : 'text-foreground',
                  )}
                >
                  {formatMoney(balanceDue, currency)}
                </dd>
              </div>
            </dl>
            <p className="text-sm text-muted-foreground">
              {balanceDue > 0
                ? 'Settle any open balance at the hotel desk. Unpaid additional charges can also be paid online with Maya below.'
                : 'No outstanding balance on this booking.'}
            </p>
          </CardContent>
        </Card>
      ) : null}

      {additionalCharges.length > 0 ? (
        <Card className="border-border/80 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Additional charges</CardTitle>
            <CardDescription>Extra fees added during your stay.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {additionalCharges.map((charge) => (
              <div
                key={charge.id}
                className="rounded-xl border border-border/70 bg-muted/20 p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <p className="font-medium">{charge.description}</p>
                    <p className="text-sm text-muted-foreground">
                      {CHARGE_STATUS_LABELS[charge.status] || charge.status}
                      {charge.paymentMethod
                        ? ` · ${charge.paymentMethod.replaceAll('_', ' ')}`
                        : ''}
                    </p>
                    {charge.rejectionReason ? (
                      <p className="text-sm text-destructive">Reason: {charge.rejectionReason}</p>
                    ) : null}
                  </div>
                  <p className="shrink-0 font-semibold tabular-nums">
                    {formatMoney(charge.amount, charge.currency || currency)}
                  </p>
                </div>
                {charge.canPayMaya && onPayCharge ? (
                  <div className="mt-3">
                    <Button
                      type="button"
                      size="sm"
                      disabled={payingChargeId === charge.id}
                      onClick={() => onPayCharge(charge.id, 'ONLINE_MAYA')}
                    >
                      {payingChargeId === charge.id ? 'Redirecting…' : 'Pay with Maya'}
                    </Button>
                  </div>
                ) : null}
              </div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      {footer ? (
        <>
          <Separator />
          <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
            {footer}
          </div>
        </>
      ) : null}
    </div>
  )
}
