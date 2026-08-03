export const DEFAULT_PRICING_POLICY = {
  serviceChargeEnabled: true,
  vatEnabled: true,
  municipalTaxEnabled: false,
}

export function hasGuestFees(pricingPolicy = DEFAULT_PRICING_POLICY) {
  return Boolean(
    pricingPolicy?.serviceChargeEnabled
      || pricingPolicy?.vatEnabled
      || pricingPolicy?.municipalTaxEnabled
  )
}

export function buildStayPriceNote({
  taxInclusive = false,
  pricingPolicy = DEFAULT_PRICING_POLICY,
} = {}) {
  const includesFees = hasGuestFees(pricingPolicy)

  if (taxInclusive) {
    return includesFees ? 'Total for your stay · includes taxes & fees' : 'Total for your stay'
  }

  return includesFees
    ? 'Room rate for selected stay · taxes & fees at checkout'
    : 'Room rate for your stay'
}

export function buildBookingPriceNote(pricingPolicy = DEFAULT_PRICING_POLICY) {
  return hasGuestFees(pricingPolicy)
    ? 'Total paid · includes taxes & fees'
    : 'Total paid'
}
