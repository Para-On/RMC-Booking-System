import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'

import { useBranding } from '@/context/BrandingProvider'
import { GuestPageSkeleton } from '@/components/guest/GuestPageSkeleton'
import { cn } from '@/lib/utils'

const ENTER_MS = 400
const HEADER_MOTION_MS = 700

const GuestRouteMotionContext = createContext({
  entered: true,
  pathname: '/',
  enterMs: ENTER_MS,
  headerMotionMs: HEADER_MOTION_MS,
})

export function useGuestRouteMotion() {
  return useContext(GuestRouteMotionContext)
}

export function guestRouteEnterClass(entered, extra) {
  return cn('guest-route-enter', entered && 'guest-route-enter--active', extra)
}

export function GuestRouteMotionProvider({ children }) {
  const { pathname } = useLocation()
  const { loading: brandingLoading } = useBranding()
  const [entered, setEntered] = useState(false)
  const ready = !brandingLoading

  // Replay enter only once chrome is actually mounted (after branding),
  // otherwise refresh finishes with entered=true and skips the CSS transition.
  useEffect(() => {
    if (!ready) {
      setEntered(false)
      return undefined
    }

    window.scrollTo(0, 0)
    document.documentElement.scrollTop = 0
    document.body.scrollTop = 0

    setEntered(false)
    let frame2 = 0
    const frame1 = window.requestAnimationFrame(() => {
      frame2 = window.requestAnimationFrame(() => setEntered(true))
    })
    return () => {
      window.cancelAnimationFrame(frame1)
      if (frame2) window.cancelAnimationFrame(frame2)
    }
  }, [pathname, ready])

  const value = useMemo(
    () => ({ entered, pathname, enterMs: ENTER_MS, headerMotionMs: HEADER_MOTION_MS }),
    [entered, pathname]
  )

  if (!ready) {
    return <GuestPageSkeleton pathname={pathname} />
  }

  return (
    <GuestRouteMotionContext.Provider value={value}>
      {children}
    </GuestRouteMotionContext.Provider>
  )
}

/** Wraps page body content with the shared enter animation class. */
export default function GuestRouteTransition({ children, className }) {
  const { entered, enterMs } = useGuestRouteMotion()

  return (
    <div
      className={guestRouteEnterClass(entered, className)}
      style={{ '--guest-route-enter-ms': `${enterMs}ms` }}
      data-route-entered={entered ? 'true' : 'false'}
    >
      {children}
    </div>
  )
}
