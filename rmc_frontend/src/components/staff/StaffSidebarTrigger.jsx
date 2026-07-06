import { Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useSidebar } from '@/components/ui/sidebar'
import { cn } from '@/lib/utils'

export function StaffSidebarTrigger({ className, ...props }) {
  const { toggleSidebar } = useSidebar()

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      className={cn('shrink-0', className)}
      onClick={toggleSidebar}
      aria-label="Toggle navigation menu"
      {...props}
    >
      <Menu className="size-5" />
    </Button>
  )
}
