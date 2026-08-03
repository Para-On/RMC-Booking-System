/**
 * Normalize catalog card props from staff room type, availability API, or wizard form.
 */
import { countStayNights } from '@/lib/formatDates'
import { buildBookingPriceNote } from '@/lib/pricingPolicy'

export function labelForOption(options, id) {
  if (!id) return null
  const match = (options || []).find((o) => String(o.id) === String(id))
  return match?.label ?? null
}

export function catalogFromStaffRoom(room) {
  if (!room) return null
  return {
    name: room.name || 'Room name',
    description: room.description || '',
    maxAdults: room.maxAdults ?? 2,
    maxChildren: room.maxChildren ?? 0,
    squareMeters: room.squareMeters != null ? Number(room.squareMeters) : null,
    imageUrls: room.imageUrls || [],
    amenities: room.amenities || [],
    roomCategoryLabel: room.roomCategoryLabel || null,
    roomViewLabel: room.roomViewLabel || null,
    bedTypeLabel: room.bedTypeLabel || null,
    refundable: Boolean(room.refundable),
    freeCancellation: room.freeCancellation !== false,
    availableUnits: room.unitCount ?? null,
    totalPrice: room.baseNightlyRate != null ? Number(room.baseNightlyRate) : null,
    currency: 'PHP',
    priceNote: 'Sample nightly rate — search dates determine total',
  }
}

export function catalogFromWizardForm(form, amenities, imageUrls, roomConfig) {
  return {
    name: form.name?.trim() || 'Room name',
    description: form.description?.trim() || '',
    maxAdults: Number(form.maxAdults) || 2,
    maxChildren: Number(form.maxChildren) || 0,
    squareMeters: form.squareMeters ? Number(form.squareMeters) : null,
    imageUrls: imageUrls || [],
    amenities: amenities || [],
    roomCategoryLabel: labelForOption(roomConfig.categories, form.roomCategoryId),
    roomViewLabel: labelForOption(roomConfig.views, form.roomViewId),
    bedTypeLabel: labelForOption(roomConfig.bedTypes, form.bedTypeId),
    availableUnits: null,
    totalPrice: null,
    currency: 'PHP',
    priceNote: 'Add a rate plan after creating this room type to set pricing',
  }
}

export function catalogFromGuestCatalogItem(item) {
  if (!item?.catalog) return null
  const c = item.catalog
  return {
    name: c.name,
    description: c.description || '',
    maxAdults: c.maxAdults,
    maxChildren: c.maxChildren,
    squareMeters: c.squareMeters != null ? Number(c.squareMeters) : null,
    imageUrls: c.imageUrls || [],
    amenities: c.amenities || [],
    roomCategoryLabel: c.roomCategoryLabel || null,
    roomViewLabel: c.roomViewLabel || null,
    bedTypeLabel: c.bedTypeLabel || null,
    refundable: Boolean(c.refundable),
    freeCancellation: c.freeCancellation !== false,
    availableUnits: null,
    totalPrice: item.fromNightlyRate != null ? Number(item.fromNightlyRate) : null,
    currency: item.currency || 'PHP',
    priceNote: 'From per night · choose dates for your total',
  }
}

export function sumNightlyPricing(nights) {
  if (!nights?.length) return null
  return nights.reduce(
    (acc, night) => ({
      base: acc.base + Number(night.baseAmount || 0),
      serviceCharge: acc.serviceCharge + Number(night.serviceCharge || 0),
      vat: acc.vat + Number(night.vat || 0),
      municipalTax: acc.municipalTax + Number(night.municipalTax || 0),
      total: acc.total + Number(night.taxInclusiveTotal || 0),
    }),
    { base: 0, serviceCharge: 0, vat: 0, municipalTax: 0, total: 0 }
  )
}

export function resolveOfferPricing(offer) {
  if (!offer) return null
  const fromBreakdown = sumNightlyPricing(offer.nightlyBreakdown)
  const total = offer.totalTaxInclusive != null ? Number(offer.totalTaxInclusive) : fromBreakdown?.total ?? null
  const base = offer.totalBase != null ? Number(offer.totalBase) : fromBreakdown?.base ?? total
  return {
    base,
    serviceCharge: fromBreakdown?.serviceCharge ?? 0,
    vat: fromBreakdown?.vat ?? 0,
    municipalTax: fromBreakdown?.municipalTax ?? 0,
    total,
    originalTotal: offer.originalTotalTaxInclusive != null ? Number(offer.originalTotalTaxInclusive) : null,
    promoAmountOff: offer.promo?.amountOff != null ? Number(offer.promo.amountOff) : null,
    promoLabel: offer.promo?.label || offer.promo?.name || null,
    promoName: offer.promo?.name || null,
  }
}

/** Find the rate-plan offer a room's top-level pricing currently reflects (selected, else From/cheapest). */
export function selectedRatePlanOffer(room) {
  const ratePlans = room?.ratePlans || []
  if (!ratePlans.length) return null
  if (room?.ratePlanId != null) {
    const match = ratePlans.find((p) => String(p.ratePlanId) === String(room.ratePlanId))
    if (match) return match
  }
  return ratePlans[0]
}

/** Merge a chosen rate-plan offer's pricing/policy fields onto a room so downstream pricing helpers reflect that plan. */
export function mergeRoomWithRatePlanOffer(room, offer) {
  if (!room || !offer) return room
  return {
    ...room,
    ratePlanId: offer.ratePlanId,
    totalBase: offer.totalBase,
    totalTaxInclusive: offer.totalTaxInclusive,
    fromTotalTaxInclusive: offer.totalTaxInclusive,
    originalTotalTaxInclusive: offer.originalTotalTaxInclusive,
    promo: offer.promo,
    nightlyBreakdown: offer.nightlyBreakdown,
    refundable: offer.refundable,
    freeCancellation: offer.freeCancellation,
  }
}

export function resolveStayPricing(room, checkIn, checkOut) {
  if (!room) return null

  const expectedNights = countStayNights(checkIn, checkOut)
  const breakdown = room.nightlyBreakdown
  const fromBreakdown = sumNightlyPricing(breakdown)
  const hasPromo = Boolean(room.promo || room.originalTotalTaxInclusive != null)

  if (hasPromo && room.totalTaxInclusive != null) {
    const total = Number(room.totalTaxInclusive)
    const base =
      room.totalBase != null ? Number(room.totalBase) : fromBreakdown?.base ?? total
    return {
      base,
      serviceCharge: fromBreakdown?.serviceCharge ?? 0,
      vat: fromBreakdown?.vat ?? 0,
      municipalTax: fromBreakdown?.municipalTax ?? 0,
      total,
      originalTotal:
        room.originalTotalTaxInclusive != null
          ? Number(room.originalTotalTaxInclusive)
          : null,
      promoAmountOff: room.promo?.amountOff != null ? Number(room.promo.amountOff) : null,
      promoLabel: room.promo?.label || room.promo?.name || null,
      promoName: room.promo?.name || null,
    }
  }

  if (
    fromBreakdown &&
    (!expectedNights || !breakdown?.length || breakdown.length === expectedNights)
  ) {
    return {
      ...fromBreakdown,
      originalTotal: null,
      promoAmountOff: null,
      promoLabel: null,
      promoName: null,
    }
  }

  if (room.totalTaxInclusive != null) {
    const total = Number(room.totalTaxInclusive)
    const base =
      room.totalBase != null ? Number(room.totalBase) : fromBreakdown?.base ?? total
    const serviceCharge = fromBreakdown?.serviceCharge ?? 0
    const vat = fromBreakdown?.vat ?? 0
    const municipalTax = fromBreakdown?.municipalTax ?? 0

    return {
      base,
      serviceCharge,
      vat,
      municipalTax,
      total,
      originalTotal:
        room.originalTotalTaxInclusive != null
          ? Number(room.originalTotalTaxInclusive)
          : null,
      promoAmountOff: room.promo?.amountOff != null ? Number(room.promo.amountOff) : null,
      promoLabel: room.promo?.label || room.promo?.name || null,
      promoName: room.promo?.name || null,
    }
  }

  return fromBreakdown
    ? {
        ...fromBreakdown,
        originalTotal: null,
        promoAmountOff: null,
        promoLabel: null,
        promoName: null,
      }
    : null
}

import { hasGuestFees } from '@/lib/pricingPolicy'

export function catalogFromAvailability(
  room,
  { taxInclusive = false, checkIn, checkOut, pricingPolicy } = {}
) {
  if (!room) return null
  const pricing = resolveStayPricing(room, checkIn, checkOut)
  const baseTotal = pricing?.base ?? null
  const taxTotal = pricing?.total ?? null
  const hasPromo = Boolean(room.promo)
  const showTaxInclusive = taxInclusive || hasPromo
  const excludedTax = !showTaxInclusive && hasGuestFees(pricingPolicy)
  const promoLabel = room.promo?.label || room.promo?.name || null
  const originalTaxTotal =
    room.originalTotalTaxInclusive != null ? Number(room.originalTotalTaxInclusive) : null
  const ratePlans = room.ratePlans || []
  const offer = selectedRatePlanOffer(room)
  const fromPrice =
    room.fromTotalTaxInclusive != null
      ? Number(room.fromTotalTaxInclusive)
      : room.totalTaxInclusive != null
        ? Number(room.totalTaxInclusive)
        : null

  return {
    // Card media/meta from room type; From price from cheapest plan
    name: room.name,
    description: room.description || '',
    maxAdults: room.maxAdults,
    maxChildren: room.maxChildren,
    squareMeters: room.squareMeters != null ? Number(room.squareMeters) : null,
    imageUrls: room.imageUrls || [],
    amenities: room.amenities || [],
    roomCategoryLabel: room.roomCategoryLabel || null,
    roomViewLabel: room.roomViewLabel || null,
    bedTypeLabel: room.bedTypeLabel || null,
    refundable: Boolean(room.refundable),
    freeCancellation: room.freeCancellation !== false,
    availableUnits: room.availableUnits,
    promoLabel,
    totalPrice: showTaxInclusive ? taxTotal : baseTotal,
    originalPrice: showTaxInclusive && originalTaxTotal != null ? originalTaxTotal : null,
    currency: room.currency || 'PHP',
    priceNote: hasPromo ? 'Promo applied · taxes included' : null,
    excludedTax,
    taxInclusive: showTaxInclusive,
    roomTypeId: room.roomTypeId,
    ratePlanId: room.ratePlanId ?? null,
    ratePlanName: offer?.name || null,
    policySummary: offer?.policySummary || null,
    ratePlans,
    hasMultipleRatePlans: ratePlans.length > 1,
    policiesVary: Boolean(room.policiesVary),
    fromPrice,
  }
}

export function catalogFromBooking(booking, { pricingPolicy } = {}) {
  if (!booking?.catalog) return null
  const c = booking.catalog
  return {
    name: c.name || booking.roomTypeName,
    description: c.description || '',
    maxAdults: c.maxAdults,
    maxChildren: c.maxChildren,
    squareMeters: c.squareMeters != null ? Number(c.squareMeters) : null,
    imageUrls: c.imageUrls || [],
    amenities: c.amenities || [],
    roomCategoryLabel: c.roomCategoryLabel || null,
    roomViewLabel: c.roomViewLabel || null,
    bedTypeLabel: c.bedTypeLabel || null,
    refundable: Boolean(c.refundable),
    freeCancellation: c.freeCancellation !== false,
    availableUnits: null,
    totalPrice: booking.quotedTotal != null ? Number(booking.quotedTotal) : null,
    currency: booking.currency || 'PHP',
    priceNote: buildBookingPriceNote(pricingPolicy),
    taxInclusive: true,
  }
}

export const WIZARD_STEPS = [
  { id: 'numbers', label: 'Numbers' },
  { id: 'classification', label: 'Class' },
  { id: 'details', label: 'Details' },
  { id: 'media', label: 'Media' },
  { id: 'visibility', label: 'Visibility' },
  { id: 'preview', label: 'Preview' },
]

/** Create-only: name + which room numbers (product extras on edit). */
export const SLIM_CREATE_WIZARD_STEPS = [
  { id: 'details', label: 'Name' },
  { id: 'numbers', label: 'Rooms' },
]

export const WIZARD_STEP_CONTENT = [
  {
    title: 'Assign room numbers',
    description: 'Select which rooms belong to this type. Add new room numbers from the catalog page first.',
  },
  {
    title: 'Classification',
    description: 'Choose the room category, view, and bed type.',
  },
  {
    title: 'Room details',
    description: 'Set the name, description, and capacity. Pricing is managed separately via rate plans after this room type is created.',
  },
  {
    title: 'Amenities & images',
    description: 'Add amenities and photos shown on the guest room card.',
  },
  {
    title: 'Visibility',
    description: 'Choose whether this room type is visible to guests. Pricing and refund policy are configured under rate plans.',
  },
  {
    title: 'Preview',
    description: 'Review how this room appears on the public booking site.',
  },
]

export const SLIM_CREATE_STEP_CONTENT = [
  {
    title: 'Room catalog name',
    description: 'Name this inventory group. Guest photos, amenities, and classification are set on rate plans.',
  },
  {
    title: 'Assign room numbers',
    description: 'Select which physical rooms belong to this catalog. Rate plans under this catalog inherit these rooms for availability.',
  },
]

export function validateWizardStep(step, { form, selectedUnitIds, amenities }, { slimCreate = false } = {}) {
  if (slimCreate) {
    switch (step) {
      case 0:
        if (!form.name?.trim()) return 'Room name is required.'
        return null
      case 1:
        if (selectedUnitIds.length === 0) return 'Select at least one room number.'
        return null
      default:
        return null
    }
  }
  switch (step) {
    case 0:
      if (selectedUnitIds.length === 0) return 'Select at least one room number.'
      return null
    case 1:
      if (!form.roomCategoryId || !form.roomViewId || !form.bedTypeId) {
        return 'Select room category, view, and bed type.'
      }
      return null
    case 2:
      if (!form.name?.trim()) return 'Room name is required.'
      return null
    case 3:
      return null
    case 4:
      return null
    default:
      return null
  }
}

export const RATE_PLAN_WIZARD_STEPS = [
  { id: 'catalog', label: 'Catalog' },
  { id: 'policy', label: 'Policy' },
  { id: 'details', label: 'Details' },
  { id: 'pricing', label: 'Pricing' },
  { id: 'preview', label: 'Preview' },
]

export const RATE_PLAN_STEP_CONTENT = [
  {
    title: 'Room type',
    description: 'Choose which room type this plan belongs to. Room numbers and guest product details come from that type.',
  },
  {
    title: 'Refund policy',
    description: 'Select a named refund policy created under Settings → Refund policy.',
  },
  {
    title: 'Plan details',
    description: 'Name this rate plan and set hold / pay-later rules.',
  },
  {
    title: 'Pricing',
    description: 'Set the base nightly rate (create) or apply daily rates for a date range (edit). Taxes use General settings.',
  },
  {
    title: 'Preview',
    description: 'Review the room-type card with this plan’s From price and policy.',
  },
]

export function emptyRatePlanForm() {
  return {
    name: 'Standard Flexible',
    baseNightlyRate: '',
    refundPolicyId: '',
    holdTtlMinutes: '30',
    payLaterCutoffHours: '24',
    active: true,
  }
}

export function ratePlanFormFromConfig(plan) {
  if (!plan) return emptyRatePlanForm()
  return {
    name: plan.name || '',
    baseNightlyRate: '',
    refundPolicyId: plan.refundPolicyId != null ? String(plan.refundPolicyId) : '',
    holdTtlMinutes: plan.holdTtlMinutes != null ? String(plan.holdTtlMinutes) : '30',
    payLaterCutoffHours: plan.payLaterCutoffHours != null ? String(plan.payLaterCutoffHours) : '24',
    active: plan.active !== false,
  }
}

export function buildCreateRatePlanPayload(form) {
  return {
    name: form.name.trim(),
    baseNightlyRate: Number(form.baseNightlyRate),
    refundPolicyId: Number(form.refundPolicyId),
    holdTtlMinutes: Number(form.holdTtlMinutes) || 30,
    payLaterCutoffHours: Number(form.payLaterCutoffHours) || 0,
    active: Boolean(form.active),
  }
}

export function buildUpdateRatePlanPayload(form) {
  return {
    name: form.name.trim(),
    refundPolicyId: form.refundPolicyId ? Number(form.refundPolicyId) : undefined,
    holdTtlMinutes: Number(form.holdTtlMinutes) || 30,
    payLaterCutoffHours: Number(form.payLaterCutoffHours) || 0,
    active: Boolean(form.active),
  }
}

export function validateRatePlanWizardStep(step, { form, roomTypeId, isEdit }) {
  switch (step) {
    case 0:
      if (!isEdit && !roomTypeId) return 'Select a room type.'
      return null
    case 1:
      if (!form.refundPolicyId) return 'Select a refund policy.'
      return null
    case 2:
      if (!form.name?.trim()) return 'Rate plan name is required.'
      return null
    case 3:
      if (!isEdit && (!form.baseNightlyRate || Number(form.baseNightlyRate) <= 0)) {
        return 'Enter a valid base nightly rate.'
      }
      return null
    default:
      return null
  }
}
