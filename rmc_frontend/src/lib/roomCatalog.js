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

export function catalogFromWizardForm(form, amenities, imageUrls, roomConfig, baseNightlyRate) {
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
    refundable: Boolean(form.refundable),
    freeCancellation: form.freeCancellation !== false,
    availableUnits: null,
    totalPrice: baseNightlyRate != null ? Number(baseNightlyRate) : null,
    currency: 'PHP',
    priceNote: 'Sample nightly rate — guest total depends on search dates',
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
      total: acc.total + Number(night.taxInclusiveTotal || 0),
    }),
    { base: 0, serviceCharge: 0, vat: 0, total: 0 }
  )
}

export function resolveStayPricing(room, checkIn, checkOut) {
  if (!room) return null

  const expectedNights = countStayNights(checkIn, checkOut)
  const breakdown = room.nightlyBreakdown
  const fromBreakdown = sumNightlyPricing(breakdown)

  if (
    fromBreakdown &&
    (!expectedNights || !breakdown?.length || breakdown.length === expectedNights)
  ) {
    return fromBreakdown
  }

  if (room.totalTaxInclusive != null) {
    const total = Number(room.totalTaxInclusive)
    const base =
      room.totalBase != null ? Number(room.totalBase) : fromBreakdown?.base ?? total
    const serviceCharge = fromBreakdown?.serviceCharge ?? 0
    const vat = fromBreakdown?.vat ?? 0

    return {
      base,
      serviceCharge,
      vat,
      total,
    }
  }

  return fromBreakdown
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
  const excludedTax = !taxInclusive && hasGuestFees(pricingPolicy)

  return {
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
    totalPrice: taxInclusive ? taxTotal : baseTotal,
    currency: room.currency || 'PHP',
    priceNote: null,
    excludedTax,
    taxInclusive,
    roomTypeId: room.roomTypeId,
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
  { id: 'policies', label: 'Policy' },
  { id: 'preview', label: 'Preview' },
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
    description: 'Set the name, description, capacity, and nightly rate.',
  },
  {
    title: 'Amenities & images',
    description: 'Add amenities and photos shown on the guest room card.',
  },
  {
    title: 'Policies',
    description: 'Configure refund rules and whether this type is visible to guests.',
  },
  {
    title: 'Preview',
    description: 'Review how this room appears on the public booking site.',
  },
]

export function validateWizardStep(step, { form, selectedUnitIds, amenities }) {
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
      if (!form.baseNightlyRate || Number(form.baseNightlyRate) <= 0) {
        return 'Enter a valid base nightly rate.'
      }
      return null
    case 3:
      return null
    case 4:
      return null
    default:
      return null
  }
}
