import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

import { cn } from '@/lib/utils'

const ENTER_MS = 280

function StaffRouteEnter({ pathname, children }) {
  const [entered, setEntered] = useState(false)

  useEffect(() => {
    setEntered(false)
    let frame2 = 0
    const frame1 = window.requestAnimationFrame(() => {
      frame2 = window.requestAnimationFrame(() => setEntered(true))
    })
    return () => {
      window.cancelAnimationFrame(frame1)
      if (frame2) window.cancelAnimationFrame(frame2)
    }
  }, [pathname])

  return (
    <div
      className={cn('staff-route-enter', entered && 'staff-route-enter--active')}
      style={{ '--staff-route-enter-ms': `${ENTER_MS}ms` }}
    >
      {children}
    </div>
  )
}

export default function StaffRouteTransition({ children }) {
  const { pathname } = useLocation()

  return (
    <StaffRouteEnter key={pathname} pathname={pathname}>
      {children}
    </StaffRouteEnter>
  )
}
