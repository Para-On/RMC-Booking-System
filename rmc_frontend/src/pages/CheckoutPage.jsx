import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import { catalogFromAvailability, sumNightlyPricing } from '@/lib/roomCatalog'
import { createBooking, formatMoney } from '../api'

export default function CheckoutPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('PAY_AT_HOTEL')
  const [ageConfirmed, setAgeConfirmed] = useState(false)
  const [dpaConsentAccepted, setDpaConsentAccepted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (!state?.room) {
    return (
      <div className="card">
        <p>No room selected.</p>
        <Link to="/" className="btn">
          Back to search
        </Link>
      </div>
    )
  }

  const { room, checkIn, checkOut } = state
  const pricing = sumNightlyPricing(room.nightlyBreakdown)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const booking = await createBooking({
        roomTypeId: room.roomTypeId,
        ratePlanId: room.ratePlanId,
        checkIn,
        checkOut,
        paymentMethod,
        fullName,
        email,
        phone,
        ageConfirmed,
        dpaConsentAccepted,
      })

      if (paymentMethod === 'ONLINE_MAYA' && booking.checkoutRedirectUrl) {
        window.location.href = booking.checkoutRedirectUrl
        return
      }

      navigate('/booking/confirmed', { state: { booking } })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card checkout-layout">
      <h1>Checkout</h1>
      <p>
        {checkIn} → {checkOut}
      </p>

      <div className="checkout-room-card">
        <RoomCatalogCard {...catalogFromAvailability(room, { taxInclusive: true })} compact />
      </div>

      {pricing && (
        <div className="checkout-pricing">
          <h2 className="text-lg font-semibold">Price breakdown</h2>
          <ul className="breakdown">
            {room.nightlyBreakdown.map((night) => (
              <li key={night.date}>
                {night.date}: room rate {formatMoney(night.baseAmount, room.currency)}
              </li>
            ))}
          </ul>
          <dl className="pricing-summary">
            <div>
              <dt>Room rate</dt>
              <dd>{formatMoney(pricing.base, room.currency)}</dd>
            </div>
            {pricing.serviceCharge > 0 && (
              <div>
                <dt>Service charge</dt>
                <dd>{formatMoney(pricing.serviceCharge, room.currency)}</dd>
              </div>
            )}
            {pricing.vat > 0 && (
              <div>
                <dt>VAT</dt>
                <dd>{formatMoney(pricing.vat, room.currency)}</dd>
              </div>
            )}
            <div className="pricing-total">
              <dt>Total</dt>
              <dd>{formatMoney(pricing.total, room.currency)}</dd>
            </div>
          </dl>
        </div>
      )}

      <form className="checkout-form" onSubmit={handleSubmit}>
        <label>
          Full name
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Phone
          <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
        </label>
        <fieldset className="payment-options">
          <legend>Payment method</legend>
          <label className="checkbox-row">
            <input
              type="radio"
              name="paymentMethod"
              value="PAY_AT_HOTEL"
              checked={paymentMethod === 'PAY_AT_HOTEL'}
              onChange={() => setPaymentMethod('PAY_AT_HOTEL')}
            />
            Pay at hotel (confirm now, pay on arrival)
          </label>
          <label className="checkbox-row">
            <input
              type="radio"
              name="paymentMethod"
              value="ONLINE_MAYA"
              checked={paymentMethod === 'ONLINE_MAYA'}
              onChange={() => setPaymentMethod('ONLINE_MAYA')}
            />
            Pay now with Maya (card / Maya wallet)
          </label>
        </fieldset>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={ageConfirmed}
            onChange={(e) => setAgeConfirmed(e.target.checked)}
          />
          I confirm I am 18 years or older
        </label>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={dpaConsentAccepted}
            onChange={(e) => setDpaConsentAccepted(e.target.checked)}
          />
          I agree to the data privacy policy (v1.0)
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading
            ? 'Processing…'
            : paymentMethod === 'ONLINE_MAYA'
              ? 'Continue to Maya payment'
              : 'Confirm pay-at-hotel booking'}
        </button>
      </form>
    </div>
  )
}
