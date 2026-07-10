import { useCallback, useEffect, useState } from 'react'

import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { BrandMark } from '@/components/branding/BrandMark'
import {
  Carousel,
  CarouselContent,
  CarouselItem,
} from '@/components/ui/carousel'
import { catalogFromAvailability } from '@/lib/roomCatalog'
import { cn } from '@/lib/utils'

function isImageGalleryDragTarget(target) {
  return target instanceof Element && Boolean(target.closest('[data-room-image-gallery]'))
}

function RoomCarouselInactiveOverlay() {
  return (
    <div className="room-search-carousel-card__inactive-overlay" aria-hidden>
      <BrandMark
        slotClassName="!h-auto !w-auto max-w-[9rem]"
        imageClassName="room-search-carousel-card__inactive-logo"
      />
    </div>
  )
}

export default function RoomSearchCarousel({ rooms, appliedSearch, pricingPolicy, onViewDetails }) {
  const [api, setApi] = useState(null)
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [snapCount, setSnapCount] = useState(0)

  const onSelect = useCallback(() => {
    if (!api) return
    setSelectedIndex(api.selectedScrollSnap())
    setSnapCount(api.scrollSnapList().length)
  }, [api])

  useEffect(() => {
    if (!api) return undefined
    onSelect()
    api.on('select', onSelect)
    api.on('reInit', onSelect)
    return () => {
      api.off('select', onSelect)
      api.off('reInit', onSelect)
    }
  }, [api, onSelect])

  if (!rooms.length) return null

  const showDots = snapCount > 1

  return (
    <div className="room-search-carousel relative w-full py-2">
      <Carousel
        setApi={setApi}
        opts={{
          align: 'start',
          containScroll: 'trimSnaps',
          watchDrag: (_emblaApi, event) => !isImageGalleryDragTarget(event.target),
        }}
        className="w-full"
      >
        <CarouselContent className="room-search-carousel__track -ml-4 items-start overflow-visible">
          {rooms.map((room, index) => (
            <CarouselItem key={room.roomTypeId} className="basis-full pl-4 lg:basis-1/3">
              <div className="room-search-carousel-card h-full">
                <div className="room-search-carousel-card__frame relative h-full rounded-xl">
                  <ScrollReveal delay={index * 80} variant="scale" className="h-full">
                    <RoomCatalogCard
                      layout="stack"
                      carouselItem
                      className="room-search-carousel-card__surface h-full"
                      {...catalogFromAvailability(room, {
                        taxInclusive: false,
                        checkIn: appliedSearch?.checkIn,
                        checkOut: appliedSearch?.checkOut,
                        pricingPolicy,
                      })}
                      bookLabel="View details"
                      onBook={() => onViewDetails(room)}
                    />
                  </ScrollReveal>
                  <RoomCarouselInactiveOverlay />
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>
      </Carousel>

      {showDots && (
        <div
          className="room-search-carousel-dots mt-4 flex items-center justify-center gap-1.5"
          aria-hidden
        >
          {Array.from({ length: snapCount }).map((_, index) => (
            <span
              key={index}
              className={cn(
                'rounded-full transition-all duration-200',
                index === selectedIndex
                  ? 'size-2 bg-primary'
                  : 'size-1.5 bg-muted-foreground/35'
              )}
            />
          ))}
        </div>
      )}
    </div>
  )
}
