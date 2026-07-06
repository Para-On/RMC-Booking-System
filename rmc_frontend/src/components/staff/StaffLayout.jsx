import { useEffect, useMemo, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { StaffSidebar } from '@/components/staff/StaffSidebar'
import { StaffGlobalSearch } from '@/components/staff/StaffGlobalSearch'
import { StaffSidebarTrigger } from '@/components/staff/StaffSidebarTrigger'
import { StaffNavContext } from '@/components/staff/StaffNavContext'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { TooltipProvider } from '@/components/ui/tooltip'
import { canAccessNavPath } from '@/config/staffNavAccess'
import { DEFAULT_STAFF_MODULES } from '@/config/staffModules'
import { getStaffNav, staffLogout } from '@/staffApi'
import { getStaffAuth } from '@/staffAuth'

/**
 * App shell for all authenticated staff routes.
 * Wrap pages in <StaffPage> for consistent title/actions layout.
 */
export default function StaffLayout() {
  const auth = getStaffAuth()
  const [modules, setModules] = useState(DEFAULT_STAFF_MODULES)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    function loadNav() {
      setLoading(true)
      getStaffNav()
        .then(setModules)
        .catch(() => setModules(DEFAULT_STAFF_MODULES))
        .finally(() => setLoading(false))
    }

    loadNav()
    window.addEventListener('staff-nav-changed', loadNav)
    return () => window.removeEventListener('staff-nav-changed', loadNav)
  }, [])

  const navContextValue = useMemo(
    () => ({
      modules,
      loading,
      canAccess: (path) => canAccessNavPath(modules, path),
    }),
    [modules, loading]
  )

  async function handleLogout() {
    await staffLogout()
    window.location.href = '/staff/login'
  }

  return (
    <StaffNavContext.Provider value={navContextValue}>
      <TooltipProvider>
        <SidebarProvider defaultOpen>
          <StaffSidebar modules={modules} auth={auth} onLogout={handleLogout} />
          <SidebarInset className="min-h-svh">
            <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center gap-3 border-b bg-background/95 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/80 md:px-6">
              <StaffSidebarTrigger />
              <div className="min-w-0 shrink-0">
                <p className="truncate text-sm font-semibold">RMC Staff Portal</p>
                <p className="truncate text-xs text-muted-foreground md:hidden">{auth?.fullName}</p>
              </div>
              <StaffGlobalSearch />
            </header>
            <div className="flex-1 overflow-auto p-4 md:p-6 lg:p-8">
              <Outlet />
            </div>
          </SidebarInset>
        </SidebarProvider>
      </TooltipProvider>
    </StaffNavContext.Provider>
  )
}
