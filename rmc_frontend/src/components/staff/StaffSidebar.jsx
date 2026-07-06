import {
  BedDouble,
  CalendarDays,
  Hotel,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  Users,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from '@/components/ui/sidebar'

const ICONS = {
  calendar: CalendarDays,
  bed: BedDouble,
  settings: Settings,
  users: Users,
  layout: LayoutDashboard,
}

function NavModuleItem({ module, location }) {
  const Icon = ICONS[module.icon] || Menu
  const hasChildren = module.children?.length > 0
  const isGroupHeader = module.path === '#'
  const isActive =
    !isGroupHeader &&
    (location.pathname === module.path || location.pathname.startsWith(`${module.path}/`))

  if (hasChildren) {
    return (
      <SidebarMenuItem>
        {isGroupHeader ? (
          <SidebarMenuButton tooltip={module.label}>
            <Icon />
            <span>{module.label}</span>
          </SidebarMenuButton>
        ) : (
          <SidebarMenuButton asChild isActive={isActive} tooltip={module.label}>
            <NavLink to={module.path}>
              <Icon />
              <span>{module.label}</span>
            </NavLink>
          </SidebarMenuButton>
        )}
        <SidebarMenuSub>
          {module.children.map((child) => (
            <SidebarMenuSubItem key={child.id}>
              <SidebarMenuSubButton
                asChild
                isActive={
                  location.pathname === child.path ||
                  location.pathname.startsWith(`${child.path}/`)
                }
              >
                <NavLink to={child.path}>
                  <span>{child.label}</span>
                </NavLink>
              </SidebarMenuSubButton>
            </SidebarMenuSubItem>
          ))}
        </SidebarMenuSub>
      </SidebarMenuItem>
    )
  }

  if (isGroupHeader) {
    return null
  }

  return (
    <SidebarMenuItem>
      <SidebarMenuButton asChild isActive={isActive} tooltip={module.label}>
        <NavLink to={module.path}>
          <Icon />
          <span>{module.label}</span>
        </NavLink>
      </SidebarMenuButton>
    </SidebarMenuItem>
  )
}

export function StaffSidebar({ modules, auth, onLogout }) {
  const location = useLocation()

  return (
    <Sidebar collapsible="offcanvas" variant="sidebar">
      <SidebarHeader className="border-b border-sidebar-border px-4 py-3">
        <div className="flex items-center gap-2">
          <div className="flex size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
            <Hotel className="size-4" />
          </div>
          <div className="min-w-0 group-data-[collapsible=offcanvas]:hidden">
            <p className="truncate text-sm font-semibold">RMC Staff</p>
            <p className="truncate text-xs text-sidebar-foreground/70">Booking portal</p>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {modules.map((module) => (
                <NavModuleItem key={module.id} module={module} location={location} />
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border p-2">
        <div className="mb-2 rounded-lg bg-sidebar-accent/50 px-3 py-2 group-data-[collapsible=offcanvas]:hidden">
          <p className="truncate text-sm font-medium">{auth?.fullName}</p>
          <p className="truncate text-xs text-sidebar-foreground/70">{auth?.role}</p>
        </div>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton onClick={onLogout} className="text-sidebar-foreground/90">
              <LogOut />
              <span>Log out</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  )
}
