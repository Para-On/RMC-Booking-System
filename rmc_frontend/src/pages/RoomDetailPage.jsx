import { useMemo } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { MapPin, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import RoomImageGallery from '@/components/room/RoomImageGallery'
import RoomAmenitiesList from '@/components/room/RoomAmenitiesList'
import { BOOKING_ACTION_BUTTON_CLASS } from '@/lib/bookingFilters'
import { catalogFromAvailability } from '@/lib/roomCatalog'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { hasGuestFees } from '@/lib/pricingPolicy'
import { cn } from '@/lib/utils'
import MotionReveal from '@/components/motion/MotionReveal'
import ScrollReveal from '@/components/motion/ScrollReveal'

function formatMoney(amount, currency = 'PHP') {
  if (amount == null || Number.isNaN(amount)) return null
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
}

export default function RoomDetailPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { pricingPolicy } = usePricingPolicy()

  const room = state?.room
  const checkIn = state?.checkIn
  const checkOut = state?.checkOut
  const guests = state?.guests

  const catalog = useMemo(() => {
    if (!room) return null
    return catalogFromAvailability(room, {
      taxInclusive: false,
      checkIn,
      checkOut,
      pricingPolicy,
    })
  }, [room, checkIn, checkOut, pricingPolicy])

  if (!room || !checkIn || !checkOut || !guests) {
    return <Navigate to="/" replace />
  }

  const meta = [catalog?.roomViewLabel, catalog?.bedTypeLabel, catalog?.squareMeters != null ? `${catalog.squareMeters} m²` : null]
    .filter(Boolean)
    .join(' · ')
  const showExcludedTax = hasGuestFees(pricingPolicy)

  function handleBook() {
    navigate('/checkout', {
      state: { room, checkIn, checkOut, guests },
    })
  }

  return (
    <div className="room-detail-page mx-auto w-full max-w-6xl bg-white">
      <ScrollReveal variant="fade" trigger="mount">
        <p className="mb-4 sm:mb-5">
          <Link to="/" className="room-detail-page__back-link text-sm font-semibold text-primary transition-colors hover:underline">
            ← Back to rooms
          </Link>
        </p>
      </ScrollReveal>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
        <ScrollReveal
          className="room-detail-page__media relative aspect-[4/3] w-full shrink-0 overflow-hidden rounded-xl bg-muted/30 sm:aspect-[16/10] lg:aspect-[4/3] lg:w-[46%] xl:w-[48%]"
          variant="scale"
          delay={60}
          trigger="mount"
        >
          <RoomImageGallery
            images={catalog?.imageUrls || []}
            name={catalog?.name || room.name}
            className="absolute inset-0"
          />
          {catalog?.roomCategoryLabel && (
            <Badge className="absolute top-4 left-4 z-10 shadow-sm" variant="secondary">
              {catalog.roomCategoryLabel}
            </Badge>
          )}
        </ScrollReveal>

        <div className="room-detail-page__content flex min-w-0 flex-1 flex-col space-y-5 sm:space-y-6 lg:pt-1">
          <MotionReveal variant="slide-up" delay={120} trigger="mount">
            <div>
              <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl lg:text-4xl">{catalog?.name}</h1>
              {meta && (
                <p className="mt-2 flex items-center gap-1.5 text-sm text-muted-foreground sm:text-base">
                  <MapPin className="size-3.5 shrink-0 opacity-70" />
                  {meta}
                </p>
              )}
            </div>
          </MotionReveal>

          {catalog?.description && (
            <MotionReveal variant="slide-up" delay={200} trigger="mount">
              <p className="text-sm leading-relaxed text-muted-foreground sm:text-base">
                {catalog.description}
              </p>
            </MotionReveal>
          )}

          {(catalog?.refundable || catalog?.freeCancellation) && (
            <MotionReveal variant="slide-up" delay={280} trigger="mount">
              <div className="flex flex-wrap gap-1.5">
                {catalog.refundable && <Badge variant="outline">Refundable</Badge>}
                {catalog.freeCancellation && <Badge variant="outline">Free cancellation</Badge>}
              </div>
            </MotionReveal>
          )}

          <MotionReveal variant="slide-up" delay={360} trigger="mount">
            <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-1">
                <Users className="size-3.5" />
                Up to {catalog?.maxAdults} adult{catalog?.maxAdults === 1 ? '' : 's'}
                {catalog?.maxChildren > 0
                  ? `, ${catalog.maxChildren} child${catalog.maxChildren === 1 ? '' : 'ren'}`
                  : ''}
              </span>
              {catalog?.availableUnits != null && (
                <Badge variant="outline" className="font-normal">
                  {catalog.availableUnits} room{catalog.availableUnits === 1 ? '' : 's'} left
                </Badge>
              )}
            </div>
          </MotionReveal>

          {catalog?.amenities?.length > 0 && (
            <MotionReveal variant="slide-up" delay={440} trigger="mount">
              <div>
                <h2 className="text-sm font-semibold">Amenities</h2>
                <RoomAmenitiesList amenities={catalog.amenities} className="mt-2" />
              </div>
            </MotionReveal>
          )}

          <MotionReveal variant="scale" delay={520} trigger="mount">
            <div className="flex flex-col gap-4 border-t border-border pt-6 sm:flex-row sm:items-end sm:justify-between sm:pt-8">
              <div>
                {catalog?.totalPrice != null && (
                  <p className="text-2xl font-semibold tracking-tight sm:text-3xl">
                    {formatMoney(catalog.totalPrice, catalog.currency)}
                  </p>
                )}
                {showExcludedTax && (
                  <p className="mt-1 text-xs text-muted-foreground">Excluded Tax</p>
                )}
              </div>
              <Button
                type="button"
                size="lg"
                className={cn(BOOKING_ACTION_BUTTON_CLASS, 'w-full sm:w-auto')}
                onClick={handleBook}
              >
                Book now
              </Button>
            </div>
          </MotionReveal>
        </div>
      </div>
    </div>
  )
}