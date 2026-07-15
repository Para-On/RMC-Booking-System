import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { AlertCircle, Loader2 } from 'lucide-react'
import GuestBookingSummary from '@/components/booking/GuestBookingSummary'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { BOOKING_ACTION_BUTTON_CLASS } from '@/lib/bookingFilters'
import { cn } from '@/lib/utils'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { cancelBooking, getBooking, payAdditionalCharge } from '@/api'

function BookingPageSkeleton() {
  return (
    <Card className="border-border/80 shadow-sm">
      <CardContent className="space-y-4 pt-6">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-36 w-full rounded-xl" />
        <div className="grid gap-3 sm:grid-cols-2">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      </CardContent>
    </Card>
  )
}

export default function BookingPage() {
  const { reference } = useParams()
  const { pricingPolicy } = usePricingPolicy()
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') || ''
  const [booking, setBooking] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(false)
  const [payingChargeId, setPayingChargeId] = useState(null)

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
    const mayaAwaitingApproval =
      booking?.status === 'PENDING_APPROVAL' && booking?.paymentMethod === 'ONLINE_MAYA'
    const confirmedPay =
      booking?.status === 'CONFIRMED' || booking?.status === 'CONFIRMED_PAY_LATER'
    const message =
      mayaAwaitingApproval || confirmedPay
        ? 'Cancel this booking? If a refund applies, the hotel will process it — you will not be charged again.'
        : 'Cancel this booking request?'
    if (!window.confirm(message)) return
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

  async function handlePayCharge(chargeId, paymentMethod) {
    setPayingChargeId(chargeId)
    setError('')
    try {
      const result = await payAdditionalCharge(reference, chargeId, email, paymentMethod)
      if (paymentMethod === 'ONLINE_MAYA' && result.checkoutRedirectUrl) {
        window.location.href = result.checkoutRedirectUrl
        return
      }
      const updated = await getBooking(reference, email)
      setBooking(updated)
    } catch (err) {
      setError(err.message)
    } finally {
      setPayingChargeId(null)
    }
  }

  if (loading) {
    return (
      <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
        <div className="w-full max-w-2xl space-y-4">
          <BookingPageSkeleton />
          <p className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
            Loading booking…
          </p>
        </div>
      </div>
    )
  }

  if (error && !booking) {
    return (
      <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
        <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-md">
          <Alert variant="destructive">
            <AlertCircle aria-hidden />
            <AlertTitle>Could not load booking</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
          <Button asChild className={cn('mt-4 w-full', BOOKING_ACTION_BUTTON_CLASS)}>
            <Link to="/booking/lookup">Try again</Link>
          </Button>
        </ScrollReveal>
      </div>
    )
  }

  const canCancel = booking.canCancel === true
  const isCancelled = booking.status === 'CANCELLED'
  const charges = booking.additionalCharges || []
  const cancelLabel =
    booking.status === 'CONFIRMED' ||
    booking.status === 'CONFIRMED_PAY_LATER' ||
    (booking.status === 'PENDING_APPROVAL' && booking.paymentMethod === 'ONLINE_MAYA')
      ? 'Cancel booking'
      : 'Cancel booking request'

  return (
    <div className="booking-detail-page flex w-full justify-center py-2 sm:py-6">
      <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-2xl">
        <div className="mb-4 space-y-3">
          <div>
            <h1 className="text-xl font-semibold tracking-tight sm:text-2xl">Your booking</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              View status, payment, and manage your reservation.
            </p>
          </div>

          {booking.status === 'PENDING_APPROVAL' && (
            <Alert>
              <AlertTitle>Awaiting hotel approval</AlertTitle>
              <AlertDescription>
                {booking.paymentMethod === 'ONLINE_MAYA'
                  ? 'Payment confirmed. The hotel still needs to approve your booking — you will get a confirmation email once approved.'
                  : 'Your pay-at-hotel request is pending staff approval. You will receive an email once it is confirmed.'}
              </AlertDescription>
            </Alert>
          )}

          {error ? (
            <Alert variant="destructive">
              <AlertCircle aria-hidden />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}

          {!isCancelled && booking.refundPolicyDescription ? (
            <p className="text-sm text-muted-foreground">{booking.refundPolicyDescription}</p>
          ) : null}

          {!isCancelled && canCancel && booking.refundPreview ? (
            <p className="text-sm text-muted-foreground">{booking.refundPreview}</p>
          ) : null}

          {isCancelled && booking.cancellationMessage ? (
            <Alert variant="destructive">
              <AlertDescription>{booking.cancellationMessage}</AlertDescription>
            </Alert>
          ) : null}
        </div>

        <GuestBookingSummary
          booking={booking}
          pricingPolicy={pricingPolicy}
          showPaymentSummary
          additionalCharges={charges}
          onPayCharge={handlePayCharge}
          payingChargeId={payingChargeId}
          footer={
            <>
              {canCancel ? (
                <Button
                  type="button"
                  variant="outline"
                  disabled={cancelling}
                  onClick={handleCancel}
                >
                  {cancelling ? 'Cancelling…' : cancelLabel}
                </Button>
              ) : null}
              <Button asChild variant="secondary">
                <Link to="/booking/lookup">Find another booking</Link>
              </Button>
              <Button asChild className={BOOKING_ACTION_BUTTON_CLASS}>
                <Link to="/">Book a stay</Link>
              </Button>
            </>
          }
        />
      </ScrollReveal>
    </div>
  )
}
