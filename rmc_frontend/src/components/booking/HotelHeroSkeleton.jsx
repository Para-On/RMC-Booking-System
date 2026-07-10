import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

export default function HotelHeroSkeleton({ className }) {
  return (
    <div
      className={cn('hotel-hero relative h-full w-full overflow-hidden', className)}
      aria-busy="true"
      aria-label="Loading hotel hero"
    >
      <Skeleton className="absolute inset-0 rounded-none" />

      <div
        className="pointer-events-none absolute inset-0 bg-gradient-to-tr from-black/40 via-black/20 to-transparent"
        aria-hidden
      />

      <div className="absolute inset-x-0 bottom-0 left-0 flex items-end px-4 pb-24 sm:px-6 sm:pb-28 md:px-10 lg:inset-y-0 lg:items-center lg:px-12 lg:pb-0">
        <div className="w-full max-w-xl space-y-3 sm:max-w-2xl sm:space-y-4 md:max-w-3xl">
          <Skeleton className="h-4 w-36 bg-white/25" />
          <Skeleton className="h-9 w-full max-w-md bg-white/25 sm:h-11" />
          <Skeleton className="h-14 w-full max-w-md bg-white/20 sm:h-16" />
          <Skeleton className="h-12 w-full max-w-[14rem] rounded-lg bg-white/30 sm:w-36" />
        </div>
      </div>
    </div>
  )
}
