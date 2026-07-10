import { useLayoutEffect, useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'

import { cn } from '@/lib/utils'

const NAV_LINKS = [
  { to: '/', end: true, label: 'Book', shortLabel: 'Book' },
  { to: '/booking/lookup', label: 'Find booking', shortLabel: 'Lookup' },
  { to: '/staff/login', label: 'Staff', shortLabel: 'Staff' },
]

const navLinkClass = ({ isActive }) =>
  cn('guest-chrome-nav-link relative z-[1] rounded-md px-2 py-1.5 text-sm font-medium sm:px-2.5', isActive && 'is-active')

export default function GuestNav() {
  const { pathname } = useLocation()
  const navRef = useRef(null)
  const [indicator, setIndicator] = useState({ left: 0, width: 0, opacity: 0 })

  useLayoutEffect(() => {
    const nav = navRef.current
    if (!nav) return undefined

    function updateIndicator() {
      const activeLink = nav.querySelector('[aria-current="page"]')
      if (!activeLink) {
        setIndicator((value) => ({ ...value, opacity: 0 }))
        return
      }

      setIndicator({
        left: activeLink.offsetLeft,
        width: activeLink.offsetWidth,
        opacity: 1,
      })
    }

    updateIndicator()
    window.addEventListener('resize', updateIndicator)
    return () => window.removeEventListener('resize', updateIndicator)
  }, [pathname])

  return (
    <nav
      ref={navRef}
      className="guest-nav relative flex shrink-0 items-center gap-0.5 sm:gap-1"
      aria-label="Main"
    >
      <span
        className="guest-nav-indicator pointer-events-none absolute bottom-0 left-0 h-0.5 rounded-full"
        style={{
          width: indicator.width,
          transform: `translateX(${indicator.left}px)`,
          opacity: indicator.opacity,
        }}
        aria-hidden
      />
      {NAV_LINKS.map((link) => (
        <NavLink key={link.to} to={link.to} end={link.end} className={navLinkClass}>
          <span className="hidden sm:inline">{link.label}</span>
          <span className="sm:hidden">{link.shortLabel}</span>
        </NavLink>
      ))}
    </nav>
  )
}
