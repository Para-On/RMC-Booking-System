import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

/** Solid brand primary tag — branding container color with light text. */
export function BrandTag({ children, className }) {
  return (
    <Badge
      variant="default"
      className={cn(
        'border-transparent bg-primary font-medium text-primary-foreground shadow-none hover:bg-primary/90',
        className
      )}
    >
      {children}
    </Badge>
  )
}
