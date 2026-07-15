import { Link, useLocation } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import GuestBookingSummary from '@/components/booking/GuestBookingSummary'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { BOOKING_ACTION_BUTTON_CLASS } from '@/lib/bookingFilters'
import { cn } from '@/lib/utils'
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

  return (
    <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
      <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-2xl">
        <div className="mb-4 space-y-3">
          <Alert className="border-emerald-500/30 bg-emerald-50/80 text-emerald-950 dark:bg-emerald-950/30 dark:text-emerald-50">
            <CheckCircle2 className="text-emerald-600 dark:text-emerald-400" aria-hidden />
            <AlertTitle>{headline.title}</AlertTitle>
            <AlertDescription>{headline.description}</AlertDescription>
          </Alert>

          <Card className="border-border/80 bg-muted/20 shadow-sm">
            <CardContent className="pt-6">
              <p className="text-sm leading-relaxed text-muted-foreground">
                Save your reference and email — you&apos;ll need both to look up or cancel this
                booking later.
              </p>
            </CardContent>
          </Card>
        </div>

        <GuestBookingSummary
          booking={booking}
          pricingPolicy={pricingPolicy}
          footer={
            <>
              <Button asChild className={BOOKING_ACTION_BUTTON_CLASS}>
                <Link to="/booking/lookup">Look up booking</Link>
              </Button>
              <Button asChild variant="secondary">
                <Link to="/">Book another stay</Link>
              </Button>
            </>
          }
        />
      </ScrollReveal>
    </div>
  )
}
