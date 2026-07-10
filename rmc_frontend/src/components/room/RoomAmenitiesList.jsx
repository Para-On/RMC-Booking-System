import { useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

export default function RoomAmenitiesList({
  amenities = [],
  maxVisible = 4,
  compact = false,
  className,
}) {
  const [expanded, setExpanded] = useState(false)

  if (!amenities?.length) return null

  const hiddenCount = amenities.length - maxVisible
  const hasMore = hiddenCount > 0
  const visible = expanded || !hasMore ? amenities : amenities.slice(0, maxVisible)

  return (
    <div className={cn('space-y-1.5', className)}>
      <div className="flex flex-wrap gap-1.5">
        {visible.map((item) => (
          <Badge
            key={item}
            variant="secondary"
            className={cn('font-normal', compact && 'text-[10px] sm:text-[11px]')}
          >
            {item}
          </Badge>
        ))}
      </div>
      {hasMore && (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          className={cn(
            'text-left font-medium text-primary underline-offset-2 hover:underline',
            compact ? 'text-[11px] sm:text-xs' : 'text-xs'
          )}
        >
          {expanded ? 'View less' : `View more (${hiddenCount})`}
        </button>
      )}
    </div>
  )
}
