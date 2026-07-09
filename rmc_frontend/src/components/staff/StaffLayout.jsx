import { useEffect, useMemo, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { StaffSidebar } from '@/components/staff/StaffSidebar'
import { StaffGlobalSearch } from '@/components/staff/StaffGlobalSearch'
import { StaffNotifications } from '@/components/staff/StaffNotifications'
import { StaffSidebarTrigger } from '@/components/staff/StaffSidebarTrigger'
import { StaffNavContext } from '@/components/staff/StaffNavContext'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { TooltipProvider } from '@/components/ui/tooltip'
import { canAccessNavPath } from '@/config/staffNavAccess'
import { DEFAULT_STAFF_MODULES } from '@/config/staffModules'
import { getStaffNav, staffLogout } from '@/staffApi'
import { StaffProfileProvider } from '@/context/StaffProfileProvider'

/**
 * App shell for all authenticated staff routes.
 * Wrap pages in <StaffPage> for consistent title/actions layout.
 */
export default function StaffLayout() {
  const mainRef = useRef(null)
  const { pathname } = useLocation()
  const [modules, setModules] = useState(DEFAULT_STAFF_MODULES)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const el = mainRef.current
    if (!el) return
    el.scrollTop = 0
    el.scrollLeft = 0
  }, [pathname])

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
    <StaffProfileProvider>
      <StaffNavContext.Provider value={navContextValue}>
        <TooltipProvider>
          <SidebarProvider defaultOpen>
            <StaffSidebar modules={modules} onLogout={handleLogout} />
            <SidebarInset className="flex h-svh min-h-0 flex-col overflow-hidden">
              <header className="sticky top-0 z-20 flex h-12 shrink-0 items-center gap-2 border-b bg-background/95 px-3 backdrop-blur supports-[backdrop-filter]:bg-background/80 md:px-4">
                <StaffSidebarTrigger />
                <div className="ml-auto flex min-w-0 flex-1 items-center justify-end gap-1 sm:gap-2">
                  <StaffGlobalSearch className="w-full max-w-md" />
                  <StaffNotifications />
                </div>
              </header>
              <div
                ref={mainRef}
                className="staff-main-scroll flex-1 overflow-y-auto overflow-x-hidden p-3 md:p-4"
              >
                <Outlet />
              </div>
            </SidebarInset>
          </SidebarProvider>
        </TooltipProvider>
      </StaffNavContext.Provider>
    </StaffProfileProvider>
  )
}
