import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
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
    <div className="card">
      <h1>Checkout</h1>
      <p>
        {room.name} · {checkIn} → {checkOut}
      </p>
      <p className="price">{formatMoney(room.totalTaxInclusive, room.currency)} total (tax inclusive)</p>

      <ul className="breakdown">
        {room.nightlyBreakdown.map((night) => (
          <li key={night.date}>
            {night.date}: {formatMoney(night.taxInclusiveTotal, room.currency)}
          </li>
        ))}
      </ul>

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
