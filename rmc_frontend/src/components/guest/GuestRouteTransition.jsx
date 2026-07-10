import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

import { useBranding } from '@/context/BrandingProvider'
import { GuestPageSkeleton } from '@/components/guest/GuestPageSkeleton'
import { cn } from '@/lib/utils'

const ENTER_MS = 400

export default function GuestRouteTransition({ children }) {
  const { pathname } = useLocation()
  const { loading: brandingLoading } = useBranding()
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

  if (brandingLoading) {
    return <GuestPageSkeleton pathname={pathname} />
  }

  return (
    <div
      key={pathname}
      className={cn('guest-route-enter', entered && 'guest-route-enter--active')}
      style={{ '--guest-route-enter-ms': `${ENTER_MS}ms` }}
    >
      {children}
    </div>
  )
}
