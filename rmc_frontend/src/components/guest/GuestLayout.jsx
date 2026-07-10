import { Link, useLocation } from 'react-router-dom'
import GuestNav from '@/components/guest/GuestNav'
import GuestRouteTransition from '@/components/guest/GuestRouteTransition'
import { BrandMark } from '@/components/branding/BrandMark'
import { useBranding } from '@/context/BrandingProvider'
import { useScrollAwareHeader } from '@/hooks/useScrollAwareHeader'
import { cn } from '@/lib/utils'

function GuestBrand() {
  const { branding } = useBranding()

  if (branding.logoUrl) {
    return <BrandMark className="brand-mark" />
  }

  return <span className="text-base font-bold tracking-tight sm:text-lg">RMC Booking</span>
}

export function GuestHeader() {
  const { isFixed, isVisible } = useScrollAwareHeader({ enabled: true })

  return (
    <>
      <header
        className={cn(
          'guest-header-chrome z-50 h-14 shrink-0 shadow-sm transition-transform duration-300 ease-out will-change-transform sm:h-16',
          isFixed ? 'fixed inset-x-0 top-0' : 'relative',
          isFixed && !isVisible && '-translate-y-full'
        )}
      >
        <div className="mx-auto flex h-full w-full max-w-6xl items-center justify-between gap-3 px-4 sm:px-6">
          <Link to="/" className="inline-flex min-w-0 shrink items-center no-underline">
            <GuestBrand />
          </Link>
          <GuestNav />
        </div>
      </header>
      {isFixed ? <div className="guest-header-spacer h-14 shrink-0 sm:h-16" aria-hidden /> : null}
    </>
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
  const { pathname } = useLocation()
  const isSearchPage = pathname === '/'

  return (
    <div
      className={cn(
        'guest-shell flex min-h-svh flex-col bg-background font-sans text-foreground',
        isSearchPage && 'guest-shell--search'
      )}
    >
      <GuestHeader />
      <main
        className={cn(
          'mx-auto w-full flex-1 bg-background',
          isSearchPage ? 'max-w-none p-0' : 'max-w-6xl px-3 py-4 sm:px-5 sm:py-6'
        )}
      >
        <GuestRouteTransition>{children}</GuestRouteTransition>
      </main>
      <GuestFooter />
    </div>
  )
}
