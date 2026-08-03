import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { MapPin, ShieldCheck, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { BrandTag } from '@/components/branding/BrandTag'
import RoomImageGallery from '@/components/room/RoomImageGallery'
import RoomAmenitiesList from '@/components/room/RoomAmenitiesList'
import { formatMoney, listItemAddons, listServiceAddons } from '@/api'
import { BOOKING_ACTION_BUTTON_CLASS, BOOKING_ACTION_BUTTON_SM_CLASS } from '@/lib/bookingFilters'
import { catalogFromAvailability, mergeRoomWithRatePlanOffer, resolveOfferPricing } from '@/lib/roomCatalog'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { cn } from '@/lib/utils'
import MotionReveal from '@/components/motion/MotionReveal'
import ScrollReveal from '@/components/motion/ScrollReveal'

export default function RoomDetailPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { pricingPolicy } = usePricingPolicy()

  const room = state?.room
  const checkIn = state?.checkIn
  const checkOut = state?.checkOut
  const guests = state?.guests
  const promoType = state?.promoType
  const offerCode = state?.offerCode
  const organizationCode = state?.organizationCode

  const catalog = useMemo(() => {
    if (!room) return null
    return catalogFromAvailability(room, {
      taxInclusive: false,
      checkIn,
      checkOut,
      pricingPolicy,
    })
  }, [room, checkIn, checkOut, pricingPolicy])

  const ratePlans = catalog?.ratePlans || []
  const lowestPlanId = ratePlans[0]?.ratePlanId

  const [extras, setExtras] = useState({ services: [], items: [] })
  const [extrasLoading, setExtrasLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setExtrasLoading(true)
    Promise.all([listServiceAddons().catch(() => []), listItemAddons().catch(() => [])])
      .then(([services, items]) => {
        if (cancelled) return
        setExtras({
          services: Array.isArray(services) ? services : [],
          items: Array.isArray(items) ? items : [],
        })
      })
      .finally(() => {
        if (!cancelled) setExtrasLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const display = catalog
  const availableExtras = [
    ...extras.services.map((s) => ({ ...s, _kind: 'service' })),
    ...extras.items.map((i) => ({ ...i, _kind: 'item' })),
  ]

  if (!room || !checkIn || !checkOut || !guests) {
    return <Navigate to="/" replace />
  }

  const meta = [display?.roomViewLabel, display?.bedTypeLabel, display?.squareMeters != null ? `${display.squareMeters} m²` : null]
    .filter(Boolean)
    .join(' · ')

  function handleBook(plan) {
    const bookingRoom = plan ? mergeRoomWithRatePlanOffer(room, plan) : room
    navigate('/checkout', {
      state: { room: bookingRoom, checkIn, checkOut, guests, promoType, offerCode, organizationCode },
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
            images={display?.imageUrls || []}
            name={catalog?.name || room.name}
            className="absolute inset-0"
          />
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

          {display?.description && (
            <MotionReveal variant="slide-up" delay={200} trigger="mount">
              <p className="text-sm leading-relaxed text-muted-foreground sm:text-base">
                {display.description}
              </p>
            </MotionReveal>
          )}

          {catalog?.policiesVary ? (
            <MotionReveal variant="slide-up" delay={280} trigger="mount">
              <p className="text-xs text-muted-foreground">
                Cancellation policy varies by rate plan — pick a plan below to book. Lowest rate is highlighted.
              </p>
            </MotionReveal>
          ) : (
            (display?.refundable || display?.freeCancellation) && (
              <MotionReveal variant="slide-up" delay={280} trigger="mount">
                <div className="flex flex-wrap gap-1.5">
                  {display.refundable && <BrandTag>Refundable</BrandTag>}
                  {display.freeCancellation && <BrandTag>Free cancellation</BrandTag>}
                </div>
              </MotionReveal>
            )
          )}

          <MotionReveal variant="slide-up" delay={360} trigger="mount">
            <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-1">
                <Users className="size-3.5" />
                Up to {display?.maxAdults} adult{display?.maxAdults === 1 ? '' : 's'}
                {display?.maxChildren > 0
                  ? `, ${display.maxChildren} child${display.maxChildren === 1 ? '' : 'ren'}`
                  : ''}
              </span>
              {catalog?.availableUnits != null && (
                <BrandTag className="font-normal">
                  {catalog.availableUnits} room{catalog.availableUnits === 1 ? '' : 's'} left
                </BrandTag>
              )}
            </div>
          </MotionReveal>

          {display?.amenities?.length > 0 && (
            <MotionReveal variant="slide-up" delay={440} trigger="mount">
              <div>
                <h2 className="text-sm font-semibold">Amenities</h2>
                <RoomAmenitiesList amenities={display.amenities} className="mt-2" />
              </div>
            </MotionReveal>
          )}

          {(extrasLoading || availableExtras.length > 0) && (
            <MotionReveal variant="slide-up" delay={460} trigger="mount">
              <div>
                <h2 className="text-sm font-semibold">Available extras</h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  Optional add-ons — you can select them during checkout.
                </p>
                {extrasLoading ? (
                  <p className="mt-2 text-sm text-muted-foreground">Loading extras…</p>
                ) : (
                  <ul className="mt-2 space-y-1.5">
                    {availableExtras.slice(0, 8).map((extra, index) => (
                      <li
                        key={`${extra._kind}-${extra.id ?? index}`}
                        className="flex items-baseline justify-between gap-3 text-sm"
                      >
                        <span className="min-w-0 truncate text-muted-foreground">
                          {extra.title || extra.name}
                        </span>
                        <span className="shrink-0 font-medium">
                          {extra.free ? 'Free' : formatMoney(extra.price, 'PHP')}
                        </span>
                      </li>
                    ))}
                    {availableExtras.length > 8 && (
                      <li className="text-xs text-muted-foreground">
                        +{availableExtras.length - 8} more at checkout
                      </li>
                    )}
                  </ul>
                )}
              </div>
            </MotionReveal>
          )}

          {ratePlans.length > 0 ? (
            <MotionReveal variant="slide-up" delay={480} trigger="mount">
              <div className="border-t border-border pt-5 sm:pt-6">
                <h2 className="text-sm font-semibold">
                  {ratePlans.length > 1 ? 'Choose your rate' : 'Rate plan'}
                </h2>
                <div className="mt-3 flex flex-col gap-2.5">
                  {ratePlans.map((plan) => {
                    const offerPricing = resolveOfferPricing(plan)
                    const isLowest = String(lowestPlanId) === String(plan.ratePlanId)
                    const hasPlanPromo =
                      offerPricing?.originalTotal != null &&
                      Number(offerPricing.originalTotal) > Number(offerPricing.total)
                    return (
                      <div
                        key={plan.ratePlanId}
                        className={cn(
                          'flex flex-col gap-3 rounded-xl border px-4 py-3.5 sm:flex-row sm:items-center sm:gap-4',
                          isLowest ? 'border-primary/40 bg-primary/5' : 'border-border'
                        )}
                      >
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="text-sm font-semibold sm:text-base">{plan.name}</p>
                            {isLowest && (
                              <BrandTag className="text-[10px] sm:text-[11px]">Lowest rate</BrandTag>
                            )}
                          </div>
                          {plan.policySummary && (
                            <p className="mt-1 flex items-start gap-1 text-xs text-muted-foreground sm:text-sm">
                              <ShieldCheck className="mt-0.5 size-3.5 shrink-0 opacity-70" />
                              <span>{plan.policySummary}</span>
                            </p>
                          )}
                          <div className="mt-1.5 flex flex-wrap gap-1.5">
                            {plan.refundable && (
                              <BrandTag className="text-[10px] sm:text-[11px]">Refundable</BrandTag>
                            )}
                            {plan.freeCancellation && (
                              <BrandTag className="text-[10px] sm:text-[11px]">Free cancellation</BrandTag>
                            )}
                          </div>
                        </div>
                        <div className="flex shrink-0 flex-col gap-2 sm:items-end">
                          <div className="flex flex-row items-baseline gap-2 sm:flex-col sm:items-end sm:gap-0.5">
                            <p
                              className={cn(
                                'text-base font-semibold tracking-tight sm:text-lg',
                                hasPlanPromo && 'text-emerald-600'
                              )}
                            >
                              {formatMoney(offerPricing?.total, catalog?.currency)}
                            </p>
                            {hasPlanPromo && (
                              <p className="text-xs text-muted-foreground line-through">
                                {formatMoney(offerPricing.originalTotal, catalog?.currency)}
                              </p>
                            )}
                          </div>
                          <Button
                            type="button"
                            size="sm"
                            className={cn(BOOKING_ACTION_BUTTON_SM_CLASS, 'w-full sm:w-auto')}
                            onClick={() => handleBook(plan)}
                          >
                            Book now
                          </Button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </MotionReveal>
          ) : (
            <MotionReveal variant="scale" delay={520} trigger="mount">
              <div className="flex flex-col gap-4 border-t border-border pt-6 sm:flex-row sm:items-end sm:justify-between sm:pt-8">
                {catalog?.totalPrice != null && (
                  <div>
                    <p className="text-2xl font-semibold tracking-tight sm:text-3xl">
                      {formatMoney(catalog.fromPrice ?? catalog.totalPrice, catalog.currency)}
                    </p>
                  </div>
                )}
                <Button
                  type="button"
                  size="lg"
                  className={cn(BOOKING_ACTION_BUTTON_CLASS, 'w-full sm:w-auto')}
                  onClick={() => handleBook(null)}
                >
                  Book now
                </Button>
              </div>
            </MotionReveal>
          )}
        </div>
      </div>
    </div>
  )
}
