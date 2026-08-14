import { MapPin, Users } from 'lucide-react'

import { BrandTag } from '@/components/branding/BrandTag'
import RoomAmenitiesList from '@/components/room/RoomAmenitiesList'
import { RoomPolicyBadges } from '@/components/room/RoomPolicyInfoBadge'
import RoomImageGallery from '@/components/room/RoomImageGallery'
import { cn } from '@/lib/utils'

export default function CheckoutRoomSummary({ catalog, className }) {
  if (!catalog) return null

  const meta = [
    catalog.roomViewLabel,
    catalog.bedTypeLabel,
    catalog.squareMeters != null ? `${catalog.squareMeters} m²` : null,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <div
      className={cn(
        'checkout-room-summary flex flex-col gap-4 sm:flex-row sm:items-start sm:gap-5',
        className
      )}
    >
      <div className="relative aspect-[4/3] w-full shrink-0 overflow-hidden rounded-xl bg-muted/30 sm:aspect-[4/3] sm:w-[40%] sm:max-w-[16rem] md:max-w-[18rem]">
        <RoomImageGallery
          images={catalog.imageUrls || []}
          name={catalog.name}
          className="absolute inset-0"
        />
      </div>

      <div className="min-w-0 flex-1 space-y-3">
        <div>
          <h3 className="text-base font-semibold tracking-tight sm:text-lg">{catalog.name}</h3>
          {meta && (
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground sm:text-sm">
              <MapPin className="size-3.5 shrink-0 opacity-70" />
              {meta}
            </p>
          )}
        </div>

        {catalog.description && (
          <p className="text-sm leading-relaxed text-muted-foreground">{catalog.description}</p>
        )}

        {catalog.ratePlanName && (
          <div className="rounded-lg bg-muted/40 px-3 py-2">
            <p className="text-sm font-semibold">{catalog.ratePlanName}</p>
          </div>
        )}

        {(catalog.refundable || catalog.freeCancellation) && (
          <div className="flex flex-wrap items-center gap-1.5">
            <RoomPolicyBadges
              refundable={catalog.refundable}
              freeCancellation={catalog.freeCancellation}
              policySummary={catalog.policySummary}
              dense
            />
          </div>
        )}

        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground sm:text-sm">
          <span className="inline-flex items-center gap-1">
            <Users className="size-3.5" />
            Up to {catalog.maxAdults} adult{catalog.maxAdults === 1 ? '' : 's'}
            {catalog.maxChildren > 0
              ? `, ${catalog.maxChildren} child${catalog.maxChildren === 1 ? '' : 'ren'}`
              : ''}
          </span>
          {catalog.availableUnits != null && (
            <BrandTag className="font-normal">
              {catalog.availableUnits} room{catalog.availableUnits === 1 ? '' : 's'} left
            </BrandTag>
          )}
        </div>

        {catalog.amenities?.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold">Amenities</h4>
            <RoomAmenitiesList amenities={catalog.amenities} className="mt-1.5" />
          </div>
        )}
      </div>
    </div>
  )
}
