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

  const hasMore = amenities.length > maxVisible
  const visible = expanded || !hasMore ? amenities : amenities.slice(0, maxVisible)

  function toggle(e) {
    e.preventDefault()
    e.stopPropagation()
    setExpanded((value) => !value)
  }

  return (
    <div className={cn('flex flex-wrap items-center gap-1.5', className)}>
      {visible.map((item) => (
        <Badge
          key={item}
          variant="secondary"
          className={cn('font-normal', compact && 'text-[10px] sm:text-[11px]')}
        >
          {item}
        </Badge>
      ))}
      {hasMore && (
        <span
          role="button"
          tabIndex={0}
          onClick={toggle}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              toggle(e)
            }
          }}
          className={cn(
            'cursor-pointer select-none font-normal text-muted-foreground',
            'underline-offset-2 hover:text-foreground hover:underline',
            compact ? 'text-[11px] sm:text-xs' : 'text-xs'
          )}
        >
          {expanded ? 'See less' : 'See more'}
        </span>
      )}
    </div>
  )
}
