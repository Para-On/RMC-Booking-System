import { BedDouble, MapPin, Users } from 'lucide-react'

import { Badge } from '@/components/ui/badge'

import { Button } from '@/components/ui/button'

import { Card, CardContent, CardFooter } from '@/components/ui/card'

import {

  Carousel,

  CarouselContent,

  CarouselItem,

  CarouselNext,

  CarouselPrevious,

} from '@/components/ui/carousel'

import { cn } from '@/lib/utils'



function formatMoney(amount, currency = 'PHP') {

  if (amount == null || Number.isNaN(amount)) return null

  return new Intl.NumberFormat('en-PH', {

    style: 'currency',

    currency,

    maximumFractionDigits: 0,

  }).format(amount)

}



function RoomImageFrame({ images, name, roomCategoryLabel, vertical, compact }) {

  const frame = (

    <div

      className={cn(

        'relative h-full w-full overflow-hidden rounded-xl',

        'border border-white/80 bg-muted/30',

        'shadow-[0_14px_40px_-16px_rgba(15,23,42,0.35)]',

        'ring-1 ring-black/[0.05]',

        vertical ? 'min-h-[220px] sm:min-h-[260px]' : 'aspect-[16/10]'

      )}

    >

      {images.length > 0 ? (

        <Carousel className="h-full w-full">

          <CarouselContent className="h-full">

            {images.map((url) => (

              <CarouselItem key={url} className="h-full">

                <img

                  src={url}

                  alt={name}

                  className="h-full w-full object-cover object-center"

                />

              </CarouselItem>

            ))}

          </CarouselContent>

          {images.length > 1 && (

            <>

              <CarouselPrevious className="left-2" />

              <CarouselNext className="right-2" />

            </>

          )}

        </Carousel>

      ) : (

        <div className="flex h-full min-h-[inherit] items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 text-sm text-muted-foreground">

          <BedDouble className="mr-2 h-5 w-5 opacity-60" />

          Room photos coming soon

        </div>

      )}

      {roomCategoryLabel && (

        <Badge className="absolute top-3 left-3 z-10 shadow-sm" variant="secondary">

          {roomCategoryLabel}

        </Badge>

      )}

    </div>

  )



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

  taxInclusive = false,

  onBook,

  bookLabel = 'Book now',

  compact = false,

  layout = 'default',

  className,

}) {

  const vertical = layout === 'vertical'

  const meta = [roomViewLabel, bedTypeLabel, squareMeters != null ? `${squareMeters} m²` : null]

    .filter(Boolean)

    .join(' · ')



  const images = imageUrls?.length ? imageUrls : []

  const amenityLimit = compact ? 4 : vertical ? 10 : 8



  const body = (

    <>

      <CardContent

        className={cn(

          'space-y-3',

          compact ? 'p-4' : vertical ? 'p-5 sm:py-6 sm:pr-6 sm:pl-3' : 'p-5',

          vertical && 'flex-1'

        )}

      >

        <div>

          <h3

            className={cn(

              'font-semibold tracking-tight',

              compact ? 'text-lg' : vertical ? 'text-xl sm:text-2xl' : 'text-xl'

            )}

          >

            {name}

          </h3>

          {meta && (

            <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">

              <MapPin className="h-3.5 w-3.5 shrink-0 opacity-70" />

              {meta}

            </p>

          )}

        </div>



        {description && (

          <p

            className={cn(

              'text-sm text-muted-foreground',

              vertical ? 'line-clamp-4' : compact ? 'line-clamp-2' : 'line-clamp-3'

            )}

          >

            {description}

          </p>

        )}



        <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">

          <span className="inline-flex items-center gap-1">

            <Users className="h-3.5 w-3.5" />

            Up to {maxAdults} adults

            {maxChildren > 0 ? `, ${maxChildren} children` : ''}

          </span>

          {availableUnits != null && (

            <Badge variant="outline" className="font-normal">

              {availableUnits} room{availableUnits === 1 ? '' : 's'} left

            </Badge>

          )}

        </div>



        {(refundable || freeCancellation) && (

          <div className="flex flex-wrap gap-1.5">

            {refundable && <Badge variant="outline">Refundable</Badge>}

            {freeCancellation && <Badge variant="outline">Free cancellation</Badge>}

          </div>

        )}



        {amenities?.length > 0 && (

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

        )}

      </CardContent>



      {(totalPrice != null || onBook) && (

        <CardFooter

          className={cn(

            'flex flex-col items-stretch gap-3 border-t bg-muted/20 px-5 py-4 sm:flex-row sm:items-center sm:justify-between',

            vertical && 'mt-auto sm:px-6 sm:pr-6 sm:pl-3',

            vertical && 'sm:py-5'

          )}

        >

          <div>

            {totalPrice != null && (

              <p className="text-2xl font-semibold tracking-tight sm:text-3xl">

                {formatMoney(totalPrice, currency)}

              </p>

            )}

            {priceNote && <p className="text-xs text-muted-foreground">{priceNote}</p>}

            {!priceNote && totalPrice != null && taxInclusive && (

              <p className="text-xs text-muted-foreground">Total for selected stay · tax inclusive</p>

            )}

          </div>

          {onBook && (

            <Button

              type="button"

              size="lg"

              onClick={onBook}

              className="h-11 w-full shrink-0 justify-center text-base shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:brightness-110 active:translate-y-0 active:shadow-sm sm:min-w-[10rem] sm:w-auto"

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

        'overflow-hidden border-border/80 shadow-sm transition-shadow hover:shadow-md',

        vertical && 'flex min-h-[280px] flex-col sm:flex-row sm:min-h-[300px]',

        className

      )}

    >

      <RoomImageFrame

        images={images}

        name={name}

        roomCategoryLabel={roomCategoryLabel}

        vertical={vertical}

        compact={compact}

      />

      {vertical ? (

        <div className="flex min-w-0 flex-1 flex-col sm:w-2/3">{body}</div>

      ) : (

        body

      )}

    </Card>

  )

}

