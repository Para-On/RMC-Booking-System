import { MapPin } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter } from '@/components/ui/card'
import RoomAmenitiesList from '@/components/room/RoomAmenitiesList'
import RoomImageGallery from '@/components/room/RoomImageGallery'
import { BOOKING_ACTION_BUTTON_CLASS, BOOKING_ACTION_BUTTON_SM_CLASS } from '@/lib/bookingFilters'
import { cn } from '@/lib/utils'

function formatMoney(amount, currency = 'PHP') {
  if (amount == null || Number.isNaN(amount)) return null
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
}

function RoomImageFrame({ images, name, roomCategoryLabel, vertical, stack, horizontal, compact }) {
  const frame = (
    <div
      className={cn(
        'relative h-full w-full overflow-hidden bg-muted/30',
        stack
          ? 'aspect-[4/3] w-full shrink-0 rounded-none'
          : horizontal
            ? 'aspect-[4/3] h-full w-full rounded-none'
            : cn(
              'rounded-xl border border-white/80 shadow-[0_14px_40px_-16px_rgba(15,23,42,0.35)] ring-1 ring-black/[0.05]',
              vertical ? 'min-h-[220px] sm:min-h-[260px]' : 'aspect-[16/10]'
            )
      )}
    >
      <RoomImageGallery images={images} name={name} />
      {roomCategoryLabel && (
        <Badge className="absolute top-3 left-3 z-10 shadow-sm" variant="secondary">
          {roomCategoryLabel}
        </Badge>
      )}
    </div>
  )

  if (stack) {
    return frame
  }

  if (horizontal) {
    return (
      <div className="flex h-full w-[38%] max-w-[10.5rem] shrink-0 items-stretch sm:max-w-[12rem]">
        {frame}
      </div>
    )
  }

  if (vertical) {
    return (
      <div className="flex w-full shrink-0 items-stretch p-4 sm:w-1/3 sm:py-5 sm:pl-5 sm:pr-2">
        {frame}
      </div>
    )
  }

  return (
    <div className={cn('relative bg-muted/40', compact && 'shrink-0')}>
      {frame}
    </div>
  )
}

export default function RoomCatalogCard({
  name,
  description,
  imageUrls = [],
  amenities = [],
  roomCategoryLabel,
  roomViewLabel,
  bedTypeLabel,
  squareMeters,
  maxAdults,
  maxChildren,
  availableUnits,
  refundable,
  freeCancellation,
  totalPrice,
  currency = 'PHP',
  priceNote,
  excludedTax = false,
  taxInclusive = false,
  onBook,
  bookLabel = 'Book now',
  compact = false,
  layout = 'default',
  className,
  carouselItem = false,
}) {
  const vertical = layout === 'vertical'
  const stack = layout === 'stack'
  const horizontal = layout === 'horizontal'
  const dense = stack || horizontal
  const meta = [roomViewLabel, bedTypeLabel, squareMeters != null ? `${squareMeters} m²` : null]
    .filter(Boolean)
    .join(' · ')

  const images = imageUrls?.length ? imageUrls : []
  const amenityLimit = compact ? 4 : vertical ? 10 : dense ? 4 : 8
  const showDescription = Boolean(description) && !dense
  const showAmenities = amenities?.length > 0

  const body = (
    <>
      <CardContent
        className={cn(
          dense ? 'space-y-2 p-3.5 sm:p-4' : 'space-y-3',
          compact ? 'p-4' : !dense && (vertical ? 'p-5 sm:py-6 sm:pr-6 sm:pl-3' : 'p-5'),
          vertical && 'flex-1',
          horizontal && 'min-w-0 flex-1'
        )}
      >
        <div>
          <h3
            className={cn(
              'font-semibold tracking-tight',
              dense
                ? 'text-sm sm:text-base'
                : compact
                  ? 'text-lg'
                  : vertical
                    ? 'text-xl sm:text-2xl'
                    : 'text-xl'
            )}
          >
            {name}
          </h3>
          {meta && (
            <p
              className={cn(
                'mt-0.5 flex items-center gap-1 text-muted-foreground',
                dense ? 'text-[11px] sm:text-xs' : 'mt-1 text-sm'
              )}
            >
              <MapPin className="h-3 w-3 shrink-0 opacity-70 sm:h-3.5 sm:w-3.5" />
              {meta}
            </p>
          )}
        </div>

        {showDescription && (
          <p className="text-sm leading-relaxed text-muted-foreground">{description}</p>
        )}

        {(refundable || freeCancellation) && (
          <div className="flex flex-wrap gap-1.5">
            {refundable && (
              <Badge variant="outline" className={cn(dense && 'text-[10px] sm:text-[11px]')}>
                Refundable
              </Badge>
            )}
            {freeCancellation && (
              <Badge variant="outline" className={cn(dense && 'text-[10px] sm:text-[11px]')}>
                Free cancellation
              </Badge>
            )}
          </div>
        )}

        {!dense && (
          <div
            className={cn(
              'flex flex-wrap items-center gap-2 text-muted-foreground',
              compact ? 'text-[11px] sm:text-xs' : 'text-sm'
            )}
          >
            <span>
              Up to {maxAdults} adult{maxAdults === 1 ? '' : 's'}
              {maxChildren > 0 ? `, ${maxChildren} child${maxChildren === 1 ? '' : 'ren'}` : ''}
            </span>
            {availableUnits != null && (
              <Badge variant="outline" className="font-normal">
                {availableUnits} room{availableUnits === 1 ? '' : 's'} left
              </Badge>
            )}
          </div>
        )}

        {showAmenities &&
          (dense || compact ? (
            <RoomAmenitiesList amenities={amenities} maxVisible={amenityLimit} compact={dense} />
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {amenities.slice(0, amenityLimit).map((item) => (
                <Badge key={item} variant="secondary" className="font-normal">
                  {item}
                </Badge>
              ))}
              {amenities.length > amenityLimit && (
                <Badge variant="secondary" className="font-normal">
                  +{amenities.length - amenityLimit} more
                </Badge>
              )}
            </div>
          ))}
      </CardContent>

      {(totalPrice != null || onBook) && (
        <CardFooter
          className={cn(
            'flex flex-col items-stretch gap-2.5 border-t bg-muted/10',
            stack && 'px-3.5 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-4',
            !stack && 'gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5',
            vertical && 'mt-auto sm:px-6 sm:pr-6 sm:pl-3 sm:py-5'
          )}
        >
          <div>
            {totalPrice != null && (
              <p
                className={cn(
                  'font-semibold tracking-tight',
                  stack ? 'text-base sm:text-lg' : 'text-2xl sm:text-3xl'
                )}
              >
                {formatMoney(totalPrice, currency)}
              </p>
            )}
            {excludedTax && <p className="text-[11px] text-muted-foreground sm:text-xs">Excluded Tax</p>}
            {!excludedTax && priceNote && (
              <p className="text-xs text-muted-foreground">{priceNote}</p>
            )}
            {!excludedTax && !priceNote && totalPrice != null && taxInclusive && (
              <p className="text-xs text-muted-foreground">Total for your stay</p>
            )}
          </div>
          {onBook && (
            <Button
              type="button"
              size={stack ? 'default' : 'lg'}
              variant="default"
              onClick={onBook}
              className={cn(
                stack
                  ? cn(BOOKING_ACTION_BUTTON_SM_CLASS, 'w-full sm:w-auto')
                  : cn(
                      BOOKING_ACTION_BUTTON_CLASS,
                      'w-full sm:min-w-[10rem] sm:w-auto'
                    )
              )}
            >
              {bookLabel}
            </Button>
          )}
        </CardFooter>
      )}
    </>
  )

  return (
    <Card
      className={cn(
        'overflow-hidden border-border/80 shadow-sm',
        !carouselItem && 'transition-shadow hover:shadow-md',
        stack && 'flex flex-col gap-0 p-0',
        horizontal && 'flex min-h-0 flex-row items-stretch gap-0 p-0',
        vertical && 'flex min-h-[280px] flex-col sm:min-h-[300px] sm:flex-row',
        className
      )}
    >
      <RoomImageFrame
        images={images}
        name={name}
        roomCategoryLabel={roomCategoryLabel}
        vertical={vertical}
        stack={stack}
        horizontal={horizontal}
        compact={compact}
      />
      {vertical || horizontal ? (
        <div className={cn('flex min-w-0 flex-col', vertical && 'flex-1 sm:w-2/3', horizontal && 'flex-1')}>
          {body}
        </div>
      ) : (
        body
      )}
    </Card>
  )
}
