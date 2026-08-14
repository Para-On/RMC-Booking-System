import { Link, useLocation } from 'react-router-dom'
import { CalendarDays, CheckCircle2, CreditCard, Mail, Users } from 'lucide-react'
import {
  bookingStatusMeta,
  paymentMethodLabel,
} from '@/components/booking/GuestBookingSummary'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { BOOKING_ACTION_BUTTON_CLASS } from '@/lib/bookingFilters'
import { countStayNights, formatStayRange } from '@/lib/formatDates'
import { catalogFromBooking } from '@/lib/roomCatalog'
import { cn } from '@/lib/utils'
import { formatMoney } from '@/api'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'

function confirmationHeadline(booking) {
  const pendingApproval = booking.status === 'PENDING_APPROVAL'
  const mayaPaidPending = pendingApproval && booking.paymentMethod === 'ONLINE_MAYA'

  if (mayaPaidPending) {
    return {
      title: 'Payment confirmed',
      description:
        'Your Maya payment is confirmed. The hotel still needs to approve the booking — you will get a confirmation email when that happens.',
    }
  }
  if (pendingApproval) {
    return {
      title: 'Request received',
      description:
        'Your pay-at-hotel request is pending staff approval. You will receive an email once it is confirmed.',
    }
  }
  if (booking.paymentMethod === 'ONLINE_MAYA') {
    return {
      title: 'Booking confirmed',
      description: 'Payment received and your reservation is confirmed.',
    }
  }
  return {
    title: 'Booking confirmed',
    description: 'Your reservation is confirmed — pay at the hotel on arrival.',
  }
}

function ReceiptLine({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-4 text-sm">
      <dt className="shrink-0 text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium leading-snug text-foreground">{children}</dd>
    </div>
  )
}

export default function ConfirmationPage() {
  const { state } = useLocation()
  const { pricingPolicy } = usePricingPolicy()
  const booking = state?.booking

  if (!booking) {
    return (
      <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
        <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-md">
          <Card className="border-border/80 shadow-sm">
            <CardHeader>
              <CardTitle>No booking details</CardTitle>
              <CardDescription>
                This page opens right after checkout. If you landed here directly, look up your
                booking with your reference and email.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3 sm:flex-row">
              <Button asChild className={cn('w-full sm:flex-1', BOOKING_ACTION_BUTTON_CLASS)}>
                <Link to="/">Search rooms</Link>
              </Button>
              <Button asChild variant="secondary" className="w-full sm:flex-1">
                <Link to="/booking/lookup">Find booking</Link>
              </Button>
            </CardContent>
          </Card>
        </ScrollReveal>
      </div>
    )
  }

  const headline = confirmationHeadline(booking)
  const status = bookingStatusMeta(booking.status)
  const catalog = catalogFromBooking(booking, { pricingPolicy })
  const currency = booking.currency || 'PHP'
  const checkIn = booking.checkIn || booking.checkInDate
  const checkOut = booking.checkOut || booking.checkOutDate
  const stayRange = formatStayRange(checkIn, checkOut)
  const nights = countStayNights(checkIn, checkOut)
  const guestCount = booking.guestCount || booking.occupants?.length || 1
  const otherGuests =
    booking.occupants?.filter((occupant) => !occupant.primary).map((o) => o.fullName) || []
  const roomName = catalog?.name || booking.roomTypeName || 'Room'
  const roomMeta = catalog
    ? [catalog.roomViewLabel, catalog.bedTypeLabel, catalog.squareMeters != null ? `${catalog.squareMeters} m²` : null]
        .filter(Boolean)
        .join(' · ')
    : ''
  const imageUrl = catalog?.imageUrls?.[0] || null

  const serviceLines =
    booking.serviceAddons?.map((service, index) => ({
      key: `svc-${index}`,
      label: service.title,
      amount:
        service.lineTotal > 0 ? formatMoney(service.lineTotal, currency) : 'Included',
    })) || []
  const itemLines =
    booking.itemAddons?.map((item, index) => ({
      key: `item-${index}`,
      label: item.itemName,
      amount:
        item.unitPrice != null && Number(item.unitPrice) > 0
          ? formatMoney(item.unitPrice, currency)
          : 'Complimentary',
    })) || []

  return (
    <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
      <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-4xl space-y-6">
        <header className="flex gap-3 rounded-xl border border-emerald-500/25 bg-emerald-50/70 px-4 py-4 dark:bg-emerald-950/25 sm:px-5">
          <CheckCircle2
            className="mt-0.5 h-6 w-6 shrink-0 text-emerald-600 dark:text-emerald-400"
            aria-hidden
          />
          <div className="min-w-0 space-y-1">
            <h1 className="text-lg font-semibold tracking-tight text-emerald-950 dark:text-emerald-50 sm:text-xl">
              {headline.title}
            </h1>
            <p className="text-sm leading-relaxed text-emerald-900/80 dark:text-emerald-100/80">
              {headline.description}
            </p>
          </div>
        </header>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)] lg:items-start">
          {/* Receipt */}
          <section
            aria-label="Booking receipt"
            className="overflow-hidden rounded-xl border border-border/80 bg-card shadow-sm"
          >
            <div className="flex items-start justify-between gap-3 border-b border-border/60 px-5 py-4">
              <div className="min-w-0">
                <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">
                  Booking reference
                </p>
                <p className="mt-1 font-mono text-xl font-semibold tracking-tight sm:text-2xl">
                  {booking.reference}
                </p>
              </div>
              <Badge variant={status.variant} className="mt-1 shrink-0">
                {status.label}
              </Badge>
            </div>

            <div className="flex gap-3 px-5 py-4">
              <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-muted">
                {imageUrl ? (
                  <img src={imageUrl} alt="" className="h-full w-full object-cover" />
                ) : null}
              </div>
              <div className="min-w-0 self-center">
                <p className="font-semibold leading-snug">{roomName}</p>
                {roomMeta ? (
                  <p className="mt-0.5 text-xs text-muted-foreground">{roomMeta}</p>
                ) : null}
                {booking.ratePlanName ? (
                  <p className="mt-0.5 text-xs text-muted-foreground">{booking.ratePlanName}</p>
                ) : null}
              </div>
            </div>

            <dl className="space-y-2.5 border-t border-dashed border-border/70 px-5 py-4">
              <ReceiptLine label="Stay">
                {stayRange}
                {nights > 0 ? (
                  <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
                    {nights} night{nights === 1 ? '' : 's'}
                  </span>
                ) : null}
              </ReceiptLine>
              <ReceiptLine label="Guests">
                {guestCount} guest{guestCount === 1 ? '' : 's'}
              </ReceiptLine>
              <ReceiptLine label="Payment">{paymentMethodLabel(booking.paymentMethod)}</ReceiptLine>

              {serviceLines.map((line) => (
                <ReceiptLine key={line.key} label={line.label}>
                  {line.amount}
                </ReceiptLine>
              ))}
              {itemLines.map((line) => (
                <ReceiptLine key={line.key} label={line.label}>
                  {line.amount}
                </ReceiptLine>
              ))}
            </dl>

            <div className="flex items-baseline justify-between gap-4 border-t border-border/80 bg-muted/30 px-5 py-4">
              <span className="text-sm font-medium">Total</span>
              <span className="text-xl font-semibold tabular-nums tracking-tight">
                {formatMoney(booking.quotedTotal, currency)}
              </span>
            </div>
          </section>

          {/* Details / next steps */}
          <section aria-label="Booking details" className="flex min-w-0 flex-col gap-4">
            <Card className="border-border/80 shadow-sm">
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Keep these handy</CardTitle>
                <CardDescription>
                  You need both to look up or cancel this booking later.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-lg border border-border/70 bg-muted/20 px-3 py-2.5">
                  <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                    Reference
                  </p>
                  <p className="mt-0.5 font-mono text-base font-semibold">{booking.reference}</p>
                </div>
                <div className="rounded-lg border border-border/70 bg-muted/20 px-3 py-2.5">
                  <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                    Email
                  </p>
                  <p className="mt-0.5 break-all text-sm font-medium">
                    {booking.guestEmail || '—'}
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card className="border-border/80 shadow-sm">
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Guest</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex gap-3">
                  <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                    <Users className="h-4 w-4" aria-hidden />
                  </div>
                  <div className="min-w-0 text-sm">
                    <p className="font-medium">{booking.guestName}</p>
                    <p className="text-muted-foreground">
                      {guestCount} guest{guestCount === 1 ? '' : 's'}
                    </p>
                    {otherGuests.length > 0 ? (
                      <p className="text-muted-foreground">Also: {otherGuests.join(', ')}</p>
                    ) : null}
                  </div>
                </div>
                {booking.guestEmail ? (
                  <div className="flex gap-3">
                    <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                      <Mail className="h-4 w-4" aria-hidden />
                    </div>
                    <div className="min-w-0 text-sm">
                      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Email
                      </p>
                      <p className="break-all font-medium">{booking.guestEmail}</p>
                    </div>
                  </div>
                ) : null}
                <div className="flex gap-3">
                  <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                    <CalendarDays className="h-4 w-4" aria-hidden />
                  </div>
                  <div className="min-w-0 text-sm">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      Dates
                    </p>
                    <p className="font-medium">{stayRange}</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                    <CreditCard className="h-4 w-4" aria-hidden />
                  </div>
                  <div className="min-w-0 text-sm">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      Payment
                    </p>
                    <p className="font-medium">{paymentMethodLabel(booking.paymentMethod)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {booking.customExtrasRequest ? (
              <Card className="border-border/80 shadow-sm">
                <CardHeader className="pb-3">
                  <CardTitle className="text-base">Special request</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    {booking.customExtrasRequest}
                  </p>
                </CardContent>
              </Card>
            ) : null}

            <Separator className="my-1" />

            <div className="flex flex-col gap-3 sm:flex-row">
              <Button asChild className={cn('sm:flex-1', BOOKING_ACTION_BUTTON_CLASS)}>
                <Link to="/booking/lookup">Look up booking</Link>
              </Button>
              <Button asChild variant="secondary" className="sm:flex-1">
                <Link to="/">Book another stay</Link>
              </Button>
            </div>
          </section>
        </div>
      </ScrollReveal>
    </div>
  )
}
