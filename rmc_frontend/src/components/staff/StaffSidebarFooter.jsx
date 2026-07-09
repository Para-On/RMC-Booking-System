import { ChevronUp, LogOut, Moon, Sun, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem } from '@/components/ui/sidebar'
import { useStaffProfile } from '@/context/StaffProfileProvider'
import { staffInitials } from '@/lib/staffProfile'
import { STAFF_ROLE_LABELS } from '@/staffAuth'
import { cn } from '@/lib/utils'

export function StaffSidebarFooter({ onLogout }) {
  const { profile, toggleTheme, themeLoading, isDarkMode } = useStaffProfile()

  const roleLabel = STAFF_ROLE_LABELS[profile?.role] || profile?.role || 'Staff'

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="h-auto items-center gap-2 px-2 py-2 data-[state=open]:bg-sidebar-accent"
            >
              <Avatar size="default" className="size-9 shrink-0">
                {profile?.profileImageUrl ? (
                  <AvatarImage src={profile.profileImageUrl} alt={profile.fullName} />
                ) : null}
                <AvatarFallback className="bg-sidebar-primary text-sidebar-primary-foreground text-xs font-medium">
                  {staffInitials(profile?.fullName)}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1 text-left group-data-[collapsible=offcanvas]:hidden">
                <p className="truncate text-sm font-medium leading-tight">{profile?.fullName}</p>
                <p className="truncate text-xs text-sidebar-foreground/70">{roleLabel}</p>
              </div>
              <ChevronUp className="size-4 shrink-0 text-sidebar-foreground/60 group-data-[collapsible=offcanvas]:hidden" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="start" className="w-56">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col gap-0.5">
                <span className="truncate font-medium">{profile?.fullName}</span>
                <span className="truncate text-xs text-muted-foreground">{profile?.email}</span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link to="/staff/profile" className="cursor-pointer">
                <UserRound />
                Profile
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem
              disabled={themeLoading}
              onSelect={(event) => {
                event.preventDefault()
                toggleTheme()
              }}
            >
              {isDarkMode ? <Sun /> : <Moon />}
              {isDarkMode ? 'Light mode' : 'Dark mode'}
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              variant="destructive"
              onSelect={(event) => {
                event.preventDefault()
                onLogout()
              }}
            >
              <LogOut />
              Log out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}

export function StaffHeaderAvatar({ className }) {
  const { profile } = useStaffProfile()

  return (
    <Avatar className={cn('size-8', className)}>
      {profile?.profileImageUrl ? (
        <AvatarImage src={profile.profileImageUrl} alt={profile.fullName} />
      ) : null}
      <AvatarFallback className="text-xs font-medium">
        {staffInitials(profile?.fullName)}
      </AvatarFallback>
    </Avatar>
  )
}
