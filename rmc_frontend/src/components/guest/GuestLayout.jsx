import { Link, NavLink } from 'react-router-dom'
import { BrandMark } from '@/components/branding/BrandMark'
import { useBranding } from '@/context/BrandingProvider'
import { cn } from '@/lib/utils'

function GuestBrand() {
  const { branding } = useBranding()

  if (branding.logoUrl) {
    return <BrandMark className="brand-mark" />
  }

  return <span className="text-base font-bold tracking-tight sm:text-lg">RMC Booking</span>
}

const navLinkClass = ({ isActive }) =>
  cn('guest-chrome-nav-link rounded-md px-2 py-1 text-sm font-medium sm:px-2.5', isActive && 'is-active')

export function GuestHeader() {
  return (
    <header className="guest-header-chrome sticky top-0 z-40 shadow-sm">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-2.5 sm:px-6 sm:py-3">
        <Link to="/" className="inline-flex min-w-0 shrink items-center no-underline">
          <GuestBrand />
        </Link>
        <nav className="flex shrink-0 items-center gap-0.5 sm:gap-1" aria-label="Main">
          <NavLink to="/" end className={navLinkClass}>
            Book
          </NavLink>
          <NavLink to="/booking/lookup" className={navLinkClass}>
            <span className="hidden sm:inline">Find booking</span>
            <span className="sm:hidden">Lookup</span>
          </NavLink>
          <NavLink to="/staff/login" className={navLinkClass}>
            Staff
          </NavLink>
        </nav>
      </div>
    </header>
  )
}

export function GuestFooter() {
  const { branding } = useBranding()
  const year = new Date().getFullYear()
  const copyright =
    branding.footerCopyright?.trim() || `© ${year} RMC Booking. All rights reserved.`

  return (
    <footer className="guest-footer-chrome mt-auto">
      <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3 lg:gap-10">
          <div className="space-y-3 sm:col-span-2 lg:col-span-1">
            <GuestBrand />
            <p className="guest-chrome-muted max-w-sm text-sm leading-relaxed">
              {branding.footerText?.trim() || 'Book your stay with confidence.'}
            </p>
          </div>
          <div>
            <h3 className="guest-chrome-subtle mb-3 text-xs font-semibold uppercase tracking-wide">
              Quick links
            </h3>
            <ul className="space-y-2 text-sm">
              <li>
                <Link to="/" className="guest-chrome-link">
                  Book a room
                </Link>
              </li>
              <li>
                <Link to="/booking/lookup" className="guest-chrome-link">
                  Find booking
                </Link>
              </li>
              <li>
                <Link to="/staff/login" className="guest-chrome-link">
                  Staff login
                </Link>
              </li>
            </ul>
          </div>
          <div>
            <h3 className="guest-chrome-subtle mb-3 text-xs font-semibold uppercase tracking-wide">
              Contact
            </h3>
            <ul className="space-y-2 text-sm">
              {branding.footerContactEmail ? (
                <li>
                  <a href={`mailto:${branding.footerContactEmail}`} className="guest-chrome-link break-all">
                    {branding.footerContactEmail}
                  </a>
                </li>
              ) : null}
              {branding.footerContactPhone ? (
                <li>
                  <a href={`tel:${branding.footerContactPhone}`} className="guest-chrome-link">
                    {branding.footerContactPhone}
                  </a>
                </li>
              ) : null}
              {!branding.footerContactEmail && !branding.footerContactPhone ? (
                <li className="guest-chrome-faint">Contact details coming soon.</li>
              ) : null}
            </ul>
          </div>
        </div>
        <p className="guest-chrome-subtle guest-chrome-divider mt-8 border-t pt-4 text-xs sm:text-sm">
          {copyright}
        </p>
      </div>
    </footer>
  )
}

export default function GuestLayout({ children }) {
  return (
    <div className="guest-shell flex min-h-svh flex-col bg-secondary font-sans text-secondary-foreground">
      <GuestHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-3 py-4 sm:px-5 sm:py-6">{children}</main>
      <GuestFooter />
    </div>
  )
}
