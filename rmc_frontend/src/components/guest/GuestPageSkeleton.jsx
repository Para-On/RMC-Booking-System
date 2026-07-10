import HotelHeroSkeleton from '@/components/booking/HotelHeroSkeleton'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

function RoomCardSkeleton({ className }) {
  return (
    <div className={cn('overflow-hidden rounded-xl border border-border bg-card shadow-sm', className)}>
      <Skeleton className="aspect-[4/3] w-full rounded-none" />
      <div className="space-y-3 p-4">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-4/5" />
        <div className="flex items-center justify-between pt-2">
          <Skeleton className="h-7 w-24" />
          <Skeleton className="h-9 w-28 rounded-lg" />
        </div>
      </div>
    </div>
  )
}

function SearchPageSkeleton() {
  return (
    <div className="search-page search-page-skeleton" aria-busy="true" aria-label="Loading search page">
      <div className="search-page-hero-wrap">
        <section className="search-page-section search-page-section--hero" aria-label="Loading hotel">
          <HotelHeroSkeleton />
        </section>
        <div className="search-page-filters">
          <div className="search-page-filters-inner">
            <Skeleton className="h-[4.5rem] w-full rounded-xl" />
          </div>
        </div>
      </div>
      <section className="search-page-section search-page-section--rooms">
        <div className="search-page-rooms-inner">
          <div className="search-page-rooms-intro">
            <Skeleton className="mx-auto h-8 w-52" />
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <RoomCardSkeleton />
            <RoomCardSkeleton className="hidden sm:block" />
            <RoomCardSkeleton className="hidden lg:block" />
          </div>
        </div>
      </section>
    </div>
  )
}

function DefaultPageSkeleton() {
  return (
    <div className="mx-auto w-full max-w-2xl space-y-4 px-1 py-2" aria-busy="true" aria-label="Loading page">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-4 w-full max-w-lg" />
      <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
        <div className="space-y-4">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-36 rounded-lg" />
        </div>
      </div>
    </div>
  )
}

export function GuestPageSkeleton({ pathname = '/' }) {
  if (pathname === '/') {
    return <SearchPageSkeleton />
  }
  return <DefaultPageSkeleton />
}

export function RoomSearchCarouselSkeleton() {
  return (
    <div
      className="room-search-carousel grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
      aria-busy="true"
      aria-label="Loading rooms"
    >
      <RoomCardSkeleton />
      <RoomCardSkeleton className="hidden sm:block" />
      <RoomCardSkeleton className="hidden lg:block" />
    </div>
  )
}
