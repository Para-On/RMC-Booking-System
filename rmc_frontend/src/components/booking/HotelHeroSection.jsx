import { Building2 } from 'lucide-react'

import { HOTELS } from '@/lib/bookingFilters'
import { useBranding } from '@/context/BrandingProvider'
import { cn } from '@/lib/utils'

export default function HotelHeroSection({ hotelId, className }) {
  const { branding } = useBranding()
  const hotel = HOTELS.find((h) => h.id === hotelId) ?? HOTELS[0]
  const tagline = hotel.tagline?.trim() || branding.footerText?.trim() || 'Book your stay with confidence.'

  return (
    <section
      className={cn('hotel-hero search-page-panel search-page-panel--hero', className)}
      aria-label="Hotel"
    >
      <div className="search-page-panel-body relative">
        {hotel.imageUrl ? (
          <img src={hotel.imageUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <div
            className="flex h-full w-full items-center justify-center bg-gradient-to-br from-primary/90 via-primary to-primary/75"
            aria-hidden
          >
            <Building2 className="h-12 w-12 text-primary-foreground/25 sm:h-14 sm:w-14" />
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-black/10" />
        <div className="absolute inset-x-0 bottom-0 px-4 pb-4 pt-8 sm:px-5 sm:pb-5">
          <h1 className="text-lg font-semibold tracking-tight text-white sm:text-xl md:text-2xl">
            {hotel.name}
          </h1>
          <p className="mt-0.5 line-clamp-2 max-w-2xl text-xs text-white/85 sm:text-sm">{tagline}</p>
        </div>
      </div>
    </section>
  )
}
