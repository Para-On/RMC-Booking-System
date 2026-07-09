import { useEffect, useState } from 'react'
import {
  BedDouble,
  CalendarDays,
  ChevronRight,
  Hotel,
  LayoutDashboard,
  Menu,
  Palette,
  Settings,
  Users,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import { BrandMark, BRAND_LOGO_SLOT_CLASS } from '@/components/branding/BrandMark'
import { StaffSidebarFooter } from '@/components/staff/StaffSidebarFooter'
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
import { useBranding } from '@/context/BrandingProvider'
import { cn } from '@/lib/utils'

const ICONS = {
  calendar: CalendarDays,
  bed: BedDouble,
  settings: Settings,
  users: Users,
  layout: LayoutDashboard,
  palette: Palette,
}

function isPathActive(pathname, path) {
  return pathname === path || pathname.startsWith(`${path}/`)
}

function hasActiveChild(module, pathname) {
  return module.children?.some((child) => isPathActive(pathname, child.path)) ?? false
}

function NavModuleItem({ module, location }) {
  const Icon = ICONS[module.icon] || Menu
  const hasChildren = module.children?.length > 0
  const isGroupHeader = module.path === '#'
  const childActive = hasActiveChild(module, location.pathname)
  const isActive =
    !isGroupHeader && isPathActive(location.pathname, module.path)
  const [open, setOpen] = useState(childActive)

  useEffect(() => {
    if (childActive) {
      setOpen(true)
    }
  }, [childActive])

  if (hasChildren) {
    return (
      <SidebarMenuItem>
        <SidebarMenuButton
          type="button"
          tooltip={module.label}
          isActive={childActive}
          data-state={open ? 'open' : 'closed'}
          onClick={() => setOpen((value) => !value)}
          className="cursor-pointer"
        >
          <Icon />
          <span>{module.label}</span>
          <ChevronRight
            className={cn(
              'ml-auto size-4 shrink-0 text-sidebar-foreground/60 transition-transform duration-200',
              open && 'rotate-90'
            )}
          />
        </SidebarMenuButton>
        {open ? (
          <SidebarMenuSub>
            {module.children.map((child) => (
              <SidebarMenuSubItem key={child.id}>
                <SidebarMenuSubButton
                  asChild
                  isActive={isPathActive(location.pathname, child.path)}
                >
                  <NavLink to={child.path}>
                    <span>{child.label}</span>
                  </NavLink>
                </SidebarMenuSubButton>
              </SidebarMenuSubItem>
            ))}
          </SidebarMenuSub>
        ) : null}
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

export function StaffSidebar({ modules, onLogout }) {
  const location = useLocation()
  const { branding } = useBranding()

  return (
    <Sidebar collapsible="offcanvas" variant="sidebar">
      <SidebarHeader className="border-b border-sidebar-border px-5 py-0">
        <div className="flex items-center justify-center">
          {branding.logoUrl ? (
            <BrandMark />
          ) : (
            <div
              className={`${BRAND_LOGO_SLOT_CLASS} rounded-lg bg-sidebar-primary text-sidebar-primary-foreground`}
            >
              <Hotel className="size-5 shrink-0" />
            </div>
          )}
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
        <StaffSidebarFooter onLogout={onLogout} />
      </SidebarFooter>
    </Sidebar>
  )
}
