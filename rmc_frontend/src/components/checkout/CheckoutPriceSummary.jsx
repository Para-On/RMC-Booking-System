import { formatMoney } from '@/api'
import { countStayNights, formatStayRange } from '@/lib/formatDates'

export function CheckoutPriceSummary({
  room,
  roomName,
  imageUrl,
  checkIn,
  checkOut,
  pricing,
  pricingPolicy,
  services,
  cart,
  grandTotal,
}) {
  if (!pricing) return null

  const showServiceCharge = Boolean(pricingPolicy?.serviceChargeEnabled) && pricing.serviceCharge > 0
  const showVat = Boolean(pricingPolicy?.vatEnabled) && pricing.vat > 0

  const heroImage = imageUrl || room?.imageUrls?.[0] || null
  const title = roomName || room?.name || 'Your room'
  const nights = countStayNights(checkIn, checkOut)

  return (
    <aside className="checkout-sidebar-panel" aria-label="Price breakdown">
      {heroImage ? (
        <div className="checkout-sidebar-hero">
          <img src={heroImage} alt="" className="checkout-sidebar-hero-image" />
        </div>
      ) : null}
      <h2 className="checkout-sidebar-title">{title}</h2>
      <p className="checkout-sidebar-dates">{formatStayRange(checkIn, checkOut)}</p>
      {nights > 0 ? (
        <p className="checkout-sidebar-night-count">
          {nights} night{nights === 1 ? '' : 's'}
        </p>
      ) : null}
      <ul className="breakdown checkout-sidebar-nights">
        {room.nightlyBreakdown?.map((night) => (
          <li key={night.date}>
            {night.date}: {formatMoney(night.baseAmount, room.currency)}
          </li>
        ))}
      </ul>
      <dl className="pricing-summary">
        <div>
          <dt>Room rate</dt>
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
        {cart.map((id) => {
          const service = services.find((entry) => entry.id === id)
          if (!service) return null
          return (
            <div key={id}>
              <dt>{service.title}</dt>
              <dd>{service.free ? 'Free' : formatMoney(service.price, room.currency)}</dd>
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
