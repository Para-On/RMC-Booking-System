import { useEffect, useRef, useState } from 'react'
import { Building2, MapPin } from 'lucide-react'

import HotelHeroSkeleton from '@/components/booking/HotelHeroSkeleton'
import { Button } from '@/components/ui/button'
import { BOOKING_ACTION_BUTTON_CLASS, HOTELS } from '@/lib/bookingFilters'
import { useBranding } from '@/context/BrandingProvider'
import { cn } from '@/lib/utils'

const DEFAULT_HERO_IMAGE = '/images/hotel-hero.png'
const ROOMS_SECTION_ID = 'search-rooms'

const HERO_REVEAL_DELAYS = {
  location: 0,
  tagline: 140,
  description: 280,
  cta: 420,
}

function HeroAnimBlock({ delay, className, children }) {
  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  return (
    <div
      className={className}
      style={
        prefersReducedMotion
          ? undefined
          : {
              animation: 'hero-slide-in 750ms cubic-bezier(0.22, 1, 0.36, 1) both',
              animationDelay: `${delay}ms`,
            }
      }
    >
      {children}
    </div>
  )
}

export default function HotelHeroSection({ hotelId, className }) {
  const { branding } = useBranding()
  const [imageLoaded, setImageLoaded] = useState(false)
  const [showCopy, setShowCopy] = useState(false)
  const imageRef = useRef(null)
  const hotel = HOTELS.find((h) => h.id === hotelId) ?? HOTELS[0]
  const location = hotel.location?.trim() || hotel.name
  const tagline = hotel.tagline?.trim() || branding.footerText?.trim() || 'Book your stay with confidence.'
  const description =
    hotel.description?.trim() ||
    'Browse available rooms and book your stay in just a few clicks.'
  const imageUrl = hotel.imageUrl || DEFAULT_HERO_IMAGE

  useEffect(() => {
    if (!imageUrl) {
      setImageLoaded(true)
      return
    }

    setImageLoaded(false)
    setShowCopy(false)
    const img = imageRef.current
    if (img?.complete && img.naturalWidth > 0) {
      setImageLoaded(true)
    }
  }, [imageUrl])

  useEffect(() => {
    if (!imageLoaded) {
      setShowCopy(false)
      return undefined
    }

    let frame2 = 0
    const frame1 = window.requestAnimationFrame(() => {
      frame2 = window.requestAnimationFrame(() => setShowCopy(true))
    })

    return () => {
      window.cancelAnimationFrame(frame1)
      if (frame2) window.cancelAnimationFrame(frame2)
    }
  }, [imageLoaded])

  function handleImageReady() {
    setImageLoaded(true)
  }

  function scrollToRooms() {
    document.getElementById(ROOMS_SECTION_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <div className={cn('hotel-hero relative h-full w-full overflow-hidden', className)}>
      {!imageLoaded && <HotelHeroSkeleton className="absolute inset-0 z-10" />}

      {imageUrl ? (
        <img
          ref={imageRef}
          src={imageUrl}
          alt=""
          className={cn(
            'hotel-hero-image transition-opacity duration-500',
            imageLoaded ? 'opacity-100' : 'opacity-0'
          )}
          decoding="async"
          loading="eager"
          fetchPriority="high"
          onLoad={handleImageReady}
        />
      ) : (
        <div
          className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-primary/90 via-primary to-primary/75"
          aria-hidden
        >
          <Building2 className="h-16 w-16 text-primary-foreground/25 sm:h-20 sm:w-20" />
        </div>
      )}

      <div
        className="pointer-events-none absolute inset-0 z-[1] bg-gradient-to-tr from-black/85 via-black/45 to-transparent"
        aria-hidden
      />

      <div className="absolute inset-x-0 bottom-0 left-0 z-[2] flex items-end px-4 pb-24 sm:px-6 sm:pb-28 md:px-10 lg:inset-y-0 lg:items-center lg:px-12 lg:pb-0">
        {showCopy ? (
          <div className="hotel-hero-copy pointer-events-auto w-full max-w-xl sm:max-w-2xl md:max-w-3xl">
            <HeroAnimBlock delay={HERO_REVEAL_DELAYS.location} className="hotel-hero-copy__location">
              <p className="flex items-center gap-1.5 text-[11px] font-medium uppercase tracking-[0.14em] text-white/75 sm:text-xs md:text-sm">
                <MapPin className="size-3.5 shrink-0 sm:size-4" aria-hidden />
                {location}
              </p>
            </HeroAnimBlock>

            <HeroAnimBlock
              delay={HERO_REVEAL_DELAYS.tagline}
              className="hotel-hero-copy__tagline mt-3 sm:mt-4"
            >
              <h1 className="text-2xl tracking-tight text-white sm:text-3xl md:text-4xl lg:text-[2.75rem] lg:leading-tight">
                {tagline}
              </h1>
            </HeroAnimBlock>

            <HeroAnimBlock
              delay={HERO_REVEAL_DELAYS.description}
              className="hotel-hero-copy__description mt-3 sm:mt-4"
            >
              <p className="max-w-md text-sm leading-relaxed text-white/85 sm:text-base">
                {description}
              </p>
            </HeroAnimBlock>

            <HeroAnimBlock delay={HERO_REVEAL_DELAYS.cta} className="hotel-hero-copy__cta mt-5 sm:mt-6">
              <Button
                type="button"
                size="lg"
                className={cn(BOOKING_ACTION_BUTTON_CLASS, 'w-full sm:w-auto')}
                onClick={scrollToRooms}
              >
                Find rooms
              </Button>
            </HeroAnimBlock>
          </div>
        ) : null}
      </div>
    </div>
  )
}
