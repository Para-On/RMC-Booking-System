/** Session helpers for guest-applied promo codes (distinct from automatic promos). */

const STORAGE_KEY = 'rmc.promoCodes'

/** @typedef {'SPECIAL_RATE' | 'CORPORATE' | 'AGENCY' | ''} PromoType */

export const PROMO_TYPE_OPTIONS = [
  { value: '', label: 'No promo code' },
  { value: 'SPECIAL_RATE', label: 'Promotion / special rate' },
  { value: 'CORPORATE', label: 'Corporate rate' },
  { value: 'AGENCY', label: 'Agency rate' },
]

export function promoTypeLabel(type) {
  return PROMO_TYPE_OPTIONS.find((o) => o.value === type)?.label || 'Promo code'
}

export function needsOrganizationCode(type) {
  return type === 'CORPORATE' || type === 'AGENCY'
}

export function organizationFieldLabel(type) {
  return type === 'AGENCY' ? 'Agency code' : 'Corporate code'
}

export function loadStoredPromoCodes() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return { promoType: '', offerCode: '', organizationCode: '', applied: false }
    }
    const parsed = JSON.parse(raw)
    const promoType = parsed.promoType || (parsed.needsOrganization ? 'CORPORATE' : '')
    return {
      promoType: promoType || '',
      offerCode: parsed.offerCode || '',
      organizationCode: parsed.organizationCode || '',
      applied: Boolean(parsed.applied && (parsed.offerCode || '').trim()),
    }
  } catch {
    return { promoType: '', offerCode: '', organizationCode: '', applied: false }
  }
}

export function storePromoCodes({ promoType, offerCode, organizationCode, applied }) {
  sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      promoType: promoType || '',
      offerCode: offerCode || '',
      organizationCode: organizationCode || '',
      applied: Boolean(applied),
    })
  )
}

export function clearStoredPromoCodes() {
  sessionStorage.removeItem(STORAGE_KEY)
}
