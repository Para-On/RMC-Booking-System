import { Link, useLocation } from 'react-router-dom'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import { catalogFromBooking } from '@/lib/roomCatalog'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { formatMoney } from '../api'

export default function ConfirmationPage() {
  const { state } = useLocation()
  const { pricingPolicy } = usePricingPolicy()
  const booking = state?.booking

  if (!booking) {
    return (
      <div className="card">
        <p>No booking details.</p>
        <Link to="/" className="btn">
          Search rooms
        </Link>
      </div>
    )
  }

  const catalog = catalogFromBooking(booking, { pricingPolicy })
  const pendingApproval = booking.status === 'PENDING_APPROVAL'
  const headline = pendingApproval
    ? 'Booking request received — awaiting hotel approval.'
    : booking.paymentMethod === 'ONLINE_MAYA'
      ? 'Booking confirmed — payment received.'
      : 'Booking confirmed — pay at hotel on arrival.'

  return (
    <div className="card booking-layout">
      <div className="success">{headline}</div>
      <h1>Reference: {booking.reference}</h1>

      {pendingApproval ? (
        <p className="muted">
          Your pay-at-hotel request is pending staff approval. You will receive an email once it is
          confirmed.
        </p>
      ) : null}

      {catalog && (
        <div className="booking-room-card">
          <RoomCatalogCard {...catalog} compact />
        </div>
      )}

      <div className="meta-grid">
        <p>
          <strong>Guests ({booking.guestCount || booking.occupants?.length || 1}):</strong>{' '}
          {booking.guestName} ({booking.guestEmail})
        </p>
        {booking.occupants?.filter((occupant) => !occupant.primary).length > 0 ? (
          <p>
            <strong>Other guests:</strong>{' '}
            {booking.occupants
              .filter((occupant) => !occupant.primary)
              .map((occupant) => occupant.fullName)
              .join(', ')}
          </p>
        ) : null}
        <p>
          <strong>Room:</strong> {booking.roomTypeName}
        </p>
        <p>
          <strong>Stay:</strong> {booking.checkIn} → {booking.checkOut}
        </p>
        <p>
          <strong>Total:</strong> {formatMoney(booking.quotedTotal, booking.currency)}
        </p>
        <p>
          <strong>Status:</strong> {booking.status}
        </p>
      </div>

      {booking.serviceAddons?.length > 0 ? (
        <div className="checkout-extras-section">
          <h2>Services</h2>
          <ul className="breakdown">
            {booking.serviceAddons.map((service, index) => (
              <li key={`${service.title}-${index}`}>
                {service.title}
                {service.lineTotal > 0
                  ? ` — ${formatMoney(service.lineTotal, booking.currency)}`
                  : ' — Free'}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {booking.itemAddons?.length > 0 ? (
        <div className="checkout-extras-section">
          <h2>Requested items</h2>
          <ul className="breakdown">
            {booking.itemAddons.map((item, index) => (
              <li key={`${item.itemName}-${index}`}>
                {item.itemName}
                {item.unitPrice != null && Number(item.unitPrice) > 0
                  ? ` — ${formatMoney(item.unitPrice, booking.currency || 'PHP')}`
                  : ' (Free)'}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {booking.customExtrasRequest ? (
        <div className="checkout-extras-section">
          <h2>Other requests</h2>
          <p>{booking.customExtrasRequest}</p>
        </div>
      ) : null}

      <p>Save your reference and email — you'll need both to look up or cancel this booking.</p>
      <Link to="/booking/lookup" className="btn">
        Look up booking
      </Link>
    </div>
  )
}
