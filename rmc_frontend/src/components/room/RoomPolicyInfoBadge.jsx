import { CircleHelp } from 'lucide-react'

import { BrandTag } from '@/components/branding/BrandTag'
import { Badge } from '@/components/ui/badge'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

function PolicyInfoButton({ title, description, dense }) {
  if (!description) return null

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          data-slot="button"
          aria-label={title}
          className={cn(
            'inline-flex aspect-square shrink-0 items-center justify-center rounded-full border-0 bg-transparent p-0',
            'text-primary shadow-none hover:bg-transparent hover:opacity-80',
            'focus-visible:ring-2 focus-visible:ring-ring/50 focus-visible:outline-none',
            dense ? 'size-3.5' : 'size-4'
          )}
          onClick={(event) => event.stopPropagation()}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <CircleHelp className="size-full" strokeWidth={2} />
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="max-w-72 p-3 sm:max-w-80">
        <p className="text-sm font-semibold">{title}</p>
        <p className="mt-1.5 whitespace-pre-wrap text-xs leading-relaxed text-muted-foreground">{description}</p>
      </PopoverContent>
    </Popover>
  )
}

export function RoomPolicyInfoBadge({
  label,
  title,
  description,
  dense = false,
  variant = 'brand',
}) {
  const Tag = variant === 'outline' ? Badge : BrandTag
  const tagProps =
    variant === 'outline'
      ? { variant: 'outline', className: cn('font-normal text-muted-foreground', dense && 'text-[10px] sm:text-[11px]') }
      : { className: cn(dense && 'text-[10px] sm:text-[11px]') }

  return (
    <span className="inline-flex items-center gap-0.5">
      <Tag {...tagProps}>{label}</Tag>
      <PolicyInfoButton title={title} description={description} dense={dense} />
    </span>
  )
}

export function RoomPolicyBadges({
  refundable,
  freeCancellation,
  policySummary,
  policiesVary = false,
  dense = false,
}) {
  if (policiesVary) {
    return (
      <RoomPolicyInfoBadge
        variant="outline"
        label="Policies vary by rate plan"
        title="Cancellation and refunds"
        description={null}
        dense={dense}
      />
    )
  }

  if (!refundable && !freeCancellation) return null

  return (
    <>
      {refundable && (
        <RoomPolicyInfoBadge
          label="Refundable"
          title="How refunds work"
          description={policySummary}
          dense={dense}
        />
      )}
      {freeCancellation && (
        <RoomPolicyInfoBadge
          label="Free cancellation"
          title="Cancellation policy"
          description={policySummary}
          dense={dense}
        />
      )}
    </>
  )
}
