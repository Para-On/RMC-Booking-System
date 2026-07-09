import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from '@/components/ui/carousel'
import { catalogFromAvailability } from '@/lib/roomCatalog'

export default function RoomSearchCarousel({ rooms, appliedSearch, pricingPolicy, onBook }) {
  if (!rooms.length) return null

  return (
    <div className="room-search-carousel relative h-full">
      <Carousel
        opts={{
          align: 'start',
          containScroll: 'trimSnaps',
        }}
        className="h-full w-full"
      >
        <CarouselContent className="-ml-3 h-full items-stretch sm:-ml-4">
          {rooms.map((room) => (
            <CarouselItem
              key={room.roomTypeId}
              className="basis-full pl-3 sm:pl-4 lg:basis-1/3"
            >
              <RoomCatalogCard
                layout="stack"
                className="h-full"
                {...catalogFromAvailability(room, {
                  taxInclusive: false,
                  checkIn: appliedSearch?.checkIn,
                  checkOut: appliedSearch?.checkOut,
                  pricingPolicy,
                })}
                bookLabel="Book now"
                onBook={() => onBook(room)}
              />
            </CarouselItem>
          ))}
        </CarouselContent>
        {rooms.length > 1 && (
          <>
            <CarouselPrevious className="left-1 size-9 border-border/80 bg-background/95 shadow-md backdrop-blur-sm disabled:pointer-events-none disabled:opacity-40 sm:left-2 lg:-left-5" />
            <CarouselNext className="right-1 size-9 border-border/80 bg-background/95 shadow-md backdrop-blur-sm disabled:pointer-events-none disabled:opacity-40 sm:right-2 lg:-right-5" />
          </>
        )}
      </Carousel>
    </div>
  )
}
