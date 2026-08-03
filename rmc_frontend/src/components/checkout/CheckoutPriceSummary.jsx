import { formatMoney } from '@/api'
import { countStayNights, formatStayRange } from '@/lib/formatDates'

export function CheckoutPriceSummary({
  room,
  roomName,
  ratePlanName,
  imageUrl,
  checkIn,
  checkOut,
  pricing,
  pricingPolicy,
  services,
  cart,
  items = [],
  itemCart = [],
  grandTotal,
}) {
  if (!pricing) return null

  const showServiceCharge = Boolean(pricingPolicy?.serviceChargeEnabled) && pricing.serviceCharge > 0
  const showVat = Boolean(pricingPolicy?.vatEnabled) && pricing.vat > 0
  const showMunicipalTax =
    Boolean(pricingPolicy?.municipalTaxEnabled) && Number(pricing.municipalTax || 0) > 0
  const promoLabel = pricing.promoLabel || pricing.promoName || room?.promo?.label || null
  const promoOff = pricing.promoAmountOff != null ? Number(pricing.promoAmountOff) : 0
  const originalRoomTotal =
    pricing.originalTotal != null ? Number(pricing.originalTotal) : null

  const heroImage = imageUrl || room?.imageUrls?.[0] || null
  const title = roomName || room?.name || 'Your room'
  const nights = countStayNights(checkIn, checkOut)
  const roomRateLabel =
    nights > 0 ? `Room rate (${nights} night${nights === 1 ? '' : 's'})` : 'Room rate'

  return (
    <aside className="checkout-sidebar-panel" aria-label="Price breakdown">
      {heroImage ? (
        <div className="checkout-sidebar-hero">
          <img src={heroImage} alt="" className="checkout-sidebar-hero-image" />
        </div>
      ) : null}
      <h2 className="checkout-sidebar-title">{title}</h2>
      {ratePlanName && <p className="checkout-sidebar-dates">{ratePlanName}</p>}
      <p className="checkout-sidebar-dates">{formatStayRange(checkIn, checkOut)}</p>
      <dl className="pricing-summary checkout-sidebar-pricing">
        {promoOff > 0 && originalRoomTotal != null ? (
          <>
            <div>
              <dt>{roomRateLabel}</dt>
              <dd>
                <span className="checkout-price-with-strike">
                  <span className="checkout-price-promo">{formatMoney(pricing.total, room.currency)}</span>
                  <span className="checkout-price-strike">
                    {formatMoney(originalRoomTotal, room.currency)}
                  </span>
                </span>
              </dd>
            </div>
            <div className="checkout-promo-line">
              <dt>{promoLabel || 'Promo discount'}</dt>
              <dd>−{formatMoney(promoOff, room.currency)}</dd>
            </div>
          </>
        ) : (
          <>
            <div>
              <dt>{roomRateLabel}</dt>
              <dd>{formatMoney(pricing.base, room.currency)}</dd>
            </div>
            {showServiceCharge && (
              <div>
                <dt>Service charge</dt>
                <dd>{formatMoney(pricing.serviceCharge, room.currency)}</dd>
              </div>
            )}
            {showVat && (
              <div>
                <dt>VAT</dt>
                <dd>{formatMoney(pricing.vat, room.currency)}</dd>
              </div>
            )}
            {showMunicipalTax && (
              <div>
                <dt>Municipal tax</dt>
                <dd>{formatMoney(pricing.municipalTax, room.currency)}</dd>
              </div>
            )}
          </>
        )}
        {cart.map((id) => {
          const service = services.find((entry) => entry.id === id)
          if (!service) return null
          return (
            <div key={`service-${id}`}>
              <dt>{service.title}</dt>
              <dd>{service.free ? 'Free' : formatMoney(service.price, room.currency)}</dd>
            </div>
          )
        })}
        {itemCart.map((id) => {
          const item = items.find((entry) => entry.id === id)
          if (!item) return null
          return (
            <div key={`item-${id}`}>
              <dt>{item.title}</dt>
              <dd>{item.free ? 'Free' : formatMoney(item.price, room.currency)}</dd>
            </div>
          )
        })}
        <div className="pricing-total">
          <dt>Total</dt>
          <dd>{formatMoney(grandTotal, room.currency)}</dd>
        </div>
      </dl>
    </aside>
  )
}
