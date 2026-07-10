import { useCallback, useEffect, useState } from 'react'
import useEmblaCarousel from 'embla-carousel-react'
import { BedDouble } from 'lucide-react'

import { cn } from '@/lib/utils'

export default function RoomImageGallery({
  images = [],
  name,
  className,
  imageClassName,
  placeholderClassName,
  dotsClassName,
}) {
  const slides = images?.length ? images : []
  const [emblaRef, emblaApi] = useEmblaCarousel({
    align: 'start',
    loop: slides.length > 1,
    dragFree: false,
  })
  const [selectedIndex, setSelectedIndex] = useState(0)

  const onSelect = useCallback(() => {
    if (!emblaApi) return
    setSelectedIndex(emblaApi.selectedScrollSnap())
  }, [emblaApi])

  useEffect(() => {
    if (!emblaApi) return undefined
    onSelect()
    emblaApi.on('select', onSelect)
    emblaApi.on('reInit', onSelect)
    return () => {
      emblaApi.off('select', onSelect)
      emblaApi.off('reInit', onSelect)
    }
  }, [emblaApi, onSelect])

  if (!slides.length) {
    return (
      <div
        className={cn(
          'flex h-full min-h-[inherit] items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 text-sm text-muted-foreground',
          placeholderClassName
        )}
      >
        <BedDouble className="mr-2 h-5 w-5 opacity-60" />
        Room photos coming soon
      </div>
    )
  }

  return (
    <div
      className={cn('relative h-full w-full', className)}
      data-room-image-gallery
      onPointerDownCapture={(event) => event.stopPropagation()}
      onTouchStartCapture={(event) => event.stopPropagation()}
    >
      <div ref={emblaRef} className="h-full touch-pan-y overflow-hidden">
        <div className="flex h-full">
          {slides.map((url) => (
            <div key={url} className="h-full min-w-0 flex-[0_0_100%]">
              <img
                src={url}
                alt={name}
                className={cn('h-full w-full object-cover object-center', imageClassName)}
                draggable={false}
              />
            </div>
          ))}
        </div>
      </div>

      {slides.length > 1 && (
        <div
          className={cn(
            'pointer-events-none absolute inset-x-0 bottom-3 flex items-center justify-center gap-1.5',
            dotsClassName
          )}
          aria-hidden
        >
          {slides.map((url, index) => (
            <span
              key={url}
              className={cn(
                'rounded-full transition-all duration-200',
                index === selectedIndex ? 'size-2 bg-white' : 'size-1.5 bg-white/55'
              )}
            />
          ))}
        </div>
      )}
    </div>
  )
}
