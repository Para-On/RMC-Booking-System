import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'

const TAB_LIST_CLASS =
  'h-8 w-fit max-w-full self-start rounded-none border-b bg-transparent p-0'
const TAB_TRIGGER_CLASS = 'h-8 rounded-none px-3 text-xs sm:text-sm'

export function StaffPageTabs({ className, ...props }) {
  return <Tabs className={cn('gap-4', className)} {...props} />
}

export function StaffPageTabList({ className, ...props }) {
  return <TabsList variant="line" className={cn(TAB_LIST_CLASS, className)} {...props} />
}

export function StaffPageTabTrigger({ className, ...props }) {
  return <TabsTrigger className={cn(TAB_TRIGGER_CLASS, className)} {...props} />
}

export { TabsContent as StaffPageTabContent }
