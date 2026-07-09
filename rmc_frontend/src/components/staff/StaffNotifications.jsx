import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { formatStaffDateTime } from '@/lib/formatDates'
import { cn } from '@/lib/utils'
import {
  getStaffNotificationUnreadCount,
  getStaffNotifications,
  markStaffNotificationsSeen,
} from '@/staffApi'

function formatBadgeCount(count) {
  if (count <= 0) return null
  return count > 99 ? '99+' : String(count)
}

export function StaffNotifications() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)

  async function refreshUnreadCount() {
    try {
      const count = await getStaffNotificationUnreadCount()
      setUnreadCount(count)
    } catch {
      // Ignore polling errors (e.g. session expiry handled elsewhere).
    }
  }

  useEffect(() => {
    refreshUnreadCount()
    const interval = setInterval(refreshUnreadCount, 30000)
    return () => clearInterval(interval)
  }, [])

  async function handleOpenChange(nextOpen) {
    setOpen(nextOpen)
    if (!nextOpen) return

    setLoading(true)
    try {
      const data = await markStaffNotificationsSeen()
      setNotifications(data.notifications || [])
      setUnreadCount(data.unreadCount ?? 0)
    } catch {
      try {
        const data = await getStaffNotifications()
        setNotifications(data.notifications || [])
        setUnreadCount(data.unreadCount ?? 0)
      } catch {
        setNotifications([])
      }
    } finally {
      setLoading(false)
    }
  }

  function handleSelect(notification) {
    setOpen(false)
    if (notification.linkPath) {
      navigate(notification.linkPath)
    }
  }

  const badgeLabel = formatBadgeCount(unreadCount)

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="relative h-9 w-9 shrink-0"
          aria-label={
            unreadCount > 0
              ? `Notifications, ${unreadCount} unread`
              : 'Notifications'
          }
        >
          <Bell className="h-4 w-4" />
          {badgeLabel ? (
            <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold leading-none text-destructive-foreground">
              {badgeLabel}
            </span>
          ) : null}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0 sm:w-96">
        <div className="border-b px-4 py-3">
          <p className="text-sm font-semibold">Notifications</p>
          <p className="text-xs text-muted-foreground">Bookings and system alerts</p>
        </div>
        <div className="max-h-80 overflow-y-auto">
          {loading && (
            <p className="px-4 py-6 text-sm text-muted-foreground">Loading notifications…</p>
          )}
          {!loading && notifications.length === 0 && (
            <p className="px-4 py-6 text-sm text-muted-foreground">No notifications yet.</p>
          )}
          {!loading && notifications.length > 0 && (
            <ul className="divide-y">
              {notifications.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    className={cn(
                      'flex w-full flex-col gap-1 px-4 py-3 text-left text-sm transition-colors hover:bg-accent hover:text-accent-foreground',
                      item.unread && 'bg-primary/5'
                    )}
                    onClick={() => handleSelect(item)}
                  >
                    <span className="font-medium leading-snug">{item.title}</span>
                    <span className="text-xs text-muted-foreground leading-relaxed">
                      {item.message}
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      {formatStaffDateTime(item.createdAt)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </PopoverContent>
    </Popover>
  )
}
