import { fontOptionForFamily } from '@/lib/brandingFonts'
import { resolveBrandingPalette } from '@/lib/brandingColors'

const BRAND_COLOR_TOKENS = [
  ['primary', 'primaryColor'],
  ['primary-foreground', 'primaryForegroundColor'],
  ['secondary', 'secondaryColor'],
  ['secondary-foreground', 'secondaryForegroundColor'],
]

const GUEST_CHROME_TOKENS = [
  ['guest-header-bg', 'headerBackgroundColor'],
  ['guest-header-fg', 'headerForegroundColor'],
  ['guest-footer-bg', 'footerBackgroundColor'],
  ['guest-footer-fg', 'footerForegroundColor'],
]

const SIDEBAR_COLOR_TOKENS = [
  ['sidebar-primary', 'primaryColor'],
  ['sidebar-primary-foreground', 'primaryForegroundColor'],
]

let loadedFontHref = null

function setToken(token, value) {
  if (!value) return
  document.documentElement.style.setProperty(`--${token}`, value)
}

function clearBrandingOverrides() {
  const root = document.documentElement
  const toRemove = [
    ...BRAND_COLOR_TOKENS.map(([token]) => `--${token}`),
    ...GUEST_CHROME_TOKENS.map(([token]) => `--${token}`),
    ...SIDEBAR_COLOR_TOKENS.map(([token]) => `--${token}`),
    '--brand-font-sans',
  ]
  for (const property of toRemove) {
    root.style.removeProperty(property)
  }
}

function ensureFontLoaded(fontFamily) {
  const option = fontOptionForFamily(fontFamily)
  setToken('brand-font-sans', option.cssFamily)

  if (!option.googleFonts) {
    return
  }
  if (loadedFontHref === option.googleFonts) {
    return
  }
  loadedFontHref = option.googleFonts
  const existing = document.getElementById('branding-font-link')
  if (existing) {
    existing.href = option.googleFonts
    return
  }
  const link = document.createElement('link')
  link.id = 'branding-font-link'
  link.rel = 'stylesheet'
  link.href = option.googleFonts
  document.head.appendChild(link)
}

export function applyBranding(branding) {
  if (!branding) return

  clearBrandingOverrides()

  const palette = resolveBrandingPalette(branding)

  for (const [token, colorKey] of BRAND_COLOR_TOKENS) {
    setToken(token, palette[colorKey])
  }

  for (const [token, colorKey] of GUEST_CHROME_TOKENS) {
    setToken(token, palette[colorKey])
  }

  for (const [token, colorKey] of SIDEBAR_COLOR_TOKENS) {
    setToken(token, palette[colorKey])
  }

  ensureFontLoaded(branding.fontFamily)
}

export const DEFAULT_BRANDING = {
  logoUrl: null,
  fontFamily: 'Geist Variable',
  primaryColor: '#1a1a1a',
  secondaryColor: '#f4f4f5',
  footerText: null,
  footerContactEmail: null,
  footerContactPhone: null,
  footerCopyright: null,
}
