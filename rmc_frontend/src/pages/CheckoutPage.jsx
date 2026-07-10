import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { CheckoutPriceSummary } from '@/components/checkout/CheckoutPriceSummary'
import { CheckoutProgress, STEPS } from '@/components/checkout/CheckoutProgress'
import CheckoutRoomSummary from '@/components/checkout/CheckoutRoomSummary'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { ServiceAddonCard } from '@/components/extras/ServiceAddonCard'
import { catalogFromAvailability, resolveStayPricing } from '@/lib/roomCatalog'
import { formatStayRange } from '@/lib/formatDates'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { cn } from '@/lib/utils'
import {
  checkStayAvailability,
  createBooking,
  formatMoney,
  listItemAddons,
  listServiceAddons,
} from '../api'

export default function CheckoutPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { pricingPolicy } = usePricingPolicy()
  const initialRoom = state?.room
  const checkIn = state?.checkIn
  const checkOut = state?.checkOut
  const [room, setRoom] = useState(initialRoom)
  const [quoteLoading, setQuoteLoading] = useState(Boolean(initialRoom && checkIn && checkOut))
  const [quoteError, setQuoteError] = useState('')
  const [step, setStep] = useState(0)
  const [stepAnim, setStepAnim] = useState('forward')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('PAY_AT_HOTEL')
  const [ageConfirmed, setAgeConfirmed] = useState(false)
  const [dpaConsentAccepted, setDpaConsentAccepted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [services, setServices] = useState([])
  const [items, setItems] = useState([])
  const [extrasLoading, setExtrasLoading] = useState(true)
  const [cart, setCart] = useState([])
  const [selectedItems, setSelectedItems] = useState({})
  const [customExtrasRequest, setCustomExtrasRequest] = useState('')

  useEffect(() => {
    let cancelled = false
    async function loadExtras() {
      setExtrasLoading(true)
      try {
        const [serviceData, itemData] = await Promise.all([
          listServiceAddons(),
          listItemAddons(),
        ])
        if (!cancelled) {
          setServices(serviceData)
          setItems(itemData)
          setSelectedItems(Object.fromEntries(itemData.map((item) => [item.id, false])))
        }
      } catch {
        if (!cancelled) {
          setServices([])
          setItems([])
        }
      } finally {
        if (!cancelled) setExtrasLoading(false)
      }
    }
    loadExtras()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!initialRoom?.roomTypeId || !checkIn || !checkOut) return undefined

    let cancelled = false
    async function loadQuote() {
      setQuoteLoading(true)
      setQuoteError('')
      try {
        const result = await checkStayAvailability(initialRoom.roomTypeId, checkIn, checkOut)
        if (!result.available || !result.room) {
          throw new Error(result.message || 'Room is not available for the selected dates.')
        }
        if (!cancelled) {
          setRoom({ ...initialRoom, ...result.room })
        }
      } catch (err) {
        if (!cancelled) {
          setQuoteError(err.message)
          setRoom(initialRoom)
        }
      } finally {
        if (!cancelled) setQuoteLoading(false)
      }
    }

    loadQuote()
    return () => {
      cancelled = true
    }
  }, [initialRoom, checkIn, checkOut])

  const cartSet = useMemo(() => new Set(cart), [cart])
  const servicesTotal = useMemo(() => {
    return cart.reduce((sum, id) => {
      const service = services.find((entry) => entry.id === id)
      if (!service || service.free || service.price == null) return sum
      return sum + Number(service.price)
    }, 0)
  }, [cart, services])

  if (!initialRoom || !checkIn || !checkOut) {
    return (
      <div className="card">
        <p>No room selected.</p>
        <Link to="/" className="btn">
          Back to search
        </Link>
      </div>
    )
  }

  const pricing = resolveStayPricing(room, checkIn, checkOut)
  const grandTotal = pricing ? pricing.total + servicesTotal : servicesTotal
  const roomCatalog = catalogFromAvailability(room, {
    taxInclusive: false,
    checkIn,
    checkOut,
    pricingPolicy,
  })
  const stayLabel = formatStayRange(checkIn, checkOut)
  const selectedItemNames = items.filter((item) => selectedItems[item.id]).map((item) => item.name)

  function toggleCart(serviceId) {
    setCart((prev) =>
      prev.includes(serviceId) ? prev.filter((id) => id !== serviceId) : [...prev, serviceId]
    )
  }

  function toggleItem(itemId) {
    setSelectedItems((prev) => ({ ...prev, [itemId]: !prev[itemId] }))
  }

  function validateStep(index) {
    setError('')
    if (index === 2) {
      if (!fullName.trim() || !email.trim() || !phone.trim()) {
        setError('Please enter your full name, email, and phone.')
        return false
      }
    }
    if (index === 3) {
      if (!ageConfirmed || !dpaConsentAccepted) {
        setError('Please confirm your age and accept the data privacy policy.')
        return false
      }
    }
    return true
  }

  function goNext() {
    if (!validateStep(step)) return
    setStepAnim('forward')
    setStep((prev) => Math.min(prev + 1, STEPS.length - 1))
  }

  function goBack() {
    setError('')
    setStepAnim('back')
    setStep((prev) => Math.max(prev - 1, 0))
  }

  async function handleSubmit() {
    if (!validateStep(2) || !validateStep(3)) {
      setStep(2)
      return
    }
    setLoading(true)
    setError('')
    try {
      const itemAddons = items
        .filter((item) => selectedItems[item.id])
        .map((item) => ({
          itemId: item.id,
          selected: true,
        }))

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
        serviceAddonIds: cart,
        itemAddons,
        customExtrasRequest: customExtrasRequest.trim() || null,
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

  const stepId = STEPS[step].id

  return (
    <div className="checkout-page">
      <ScrollReveal variant="fade" trigger="mount">
        <CheckoutProgress currentStep={step} />
      </ScrollReveal>

      <div className="checkout-shell mb-2">
        <ScrollReveal className="checkout-main" variant="slide-up" delay={80} trigger="mount">
          <div
            key={stepId}
            className={cn(
              'card checkout-step-card',
              stepAnim === 'forward' ? 'motion-step-forward' : 'motion-step-back'
            )}
          >
            {stepId === 'room' && (
              <section aria-labelledby="step-room">
                <h2 id="step-room" className="checkout-step-heading">Room details</h2>
                <div className="checkout-room-card">
                  <CheckoutRoomSummary catalog={roomCatalog} />
                </div>
              </section>
            )}

            {stepId === 'extras' && (
              <section aria-labelledby="step-extras">
                {extrasLoading ? (
                  <p className="muted">Loading extras…</p>
                ) : (
                  <>
                    {services.length > 0 ? (
                      <div className="checkout-extras-section">
                        <h3 className='font-bold'>Optional services</h3>
                        <div className="service-addon-list">
                          {services.map((service, index) => (
                            <ScrollReveal
                              key={service.id}
                              delay={index * 80}
                              variant="scale"
                            >
                              <ServiceAddonCard
                                service={service}
                                inCart={cartSet.has(service.id)}
                                onAddToCart={() => toggleCart(service.id)}
                                onRemoveFromCart={() => toggleCart(service.id)}
                              />
                            </ScrollReveal>
                          ))}
                        </div>
                      </div>
                    ) : null}

                    {items.length > 0 ? (
                      <div className="checkout-extras-section">
                        <h3 className='font-bold'>Optional items</h3>
                        <p className="muted text-sm">Select any that apply. All choices are optional.</p>
                        <div className="item-addon-inline-list">
                          {items.map((item) => (
                            <label key={item.id} className="item-addon-inline">
                              <input
                                type="checkbox"
                                checked={Boolean(selectedItems[item.id])}
                                onChange={() => toggleItem(item.id)}
                              />
                              <span>{item.name}</span>
                            </label>
                          ))}
                        </div>
                      </div>
                    ) : null}

                    <div className="checkout-extras-section">
                      <h3 className='font-bold'>Other requests</h3>
                      <p className="muted">
                        Need something not listed above? Describe it here (optional).
                      </p>
                      <label className="checkout-custom-request">
                        <span className="sr-only">Other requests</span>
                        <input
                          type="text"
                          placeholder="e.g. extra pillows, late check-in, dietary needs…"
                          value={customExtrasRequest}
                          onChange={(e) => setCustomExtrasRequest(e.target.value)}
                        />
                      </label>
                    </div>
                  </>
                )}
              </section>
            )}

            {stepId === 'guest' && (
              <section aria-labelledby="step-guest" className="checkout-form">
                <h2 id="step-guest">Guest details</h2>
                <label>
                  Full name
                  <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
                </label>
                <label>
                  Email
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Phone
                  <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
                </label>
              </section>
            )}

            {stepId === 'payment' && (
              <section aria-labelledby="step-payment" className="checkout-form">
                <h2 id="step-payment">Payment</h2>
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
              </section>
            )}

            {stepId === 'confirm' && (
              <section aria-labelledby="step-confirm">
                <h2 id="step-confirm">Review &amp; confirm</h2>
                <div className="checkout-review">
                  <h3>Room</h3>
                  <p>{room.roomTypeName || room.name}</p>
                  <p className="muted">{stayLabel}</p>

                  {(cart.length > 0 || selectedItemNames.length > 0 || customExtrasRequest.trim()) && (
                    <>
                      <h3>Extras</h3>
                      <ul className="breakdown">
                        {cart.map((id) => {
                          const service = services.find((entry) => entry.id === id)
                          if (!service) return null
                          return (
                            <li key={id}>
                              {service.title}
                              {service.free
                                ? ' (Free)'
                                : ` — ${formatMoney(service.price, room.currency)}`}
                            </li>
                          )
                        })}
                        {selectedItemNames.map((name) => (
                          <li key={name}>{name}</li>
                        ))}
                        {customExtrasRequest.trim() ? (
                          <li>Other: {customExtrasRequest.trim()}</li>
                        ) : null}
                      </ul>
                    </>
                  )}

                  <h3>Guest</h3>
                  <p>
                    {fullName}
                    <br />
                    {email}
                    <br />
                    {phone}
                  </p>

                  <h3>Payment</h3>
                  <p>
                    {paymentMethod === 'ONLINE_MAYA'
                      ? 'Pay now with Maya'
                      : 'Pay at hotel on arrival'}
                  </p>
                </div>
              </section>
            )}

            {error && <p className="error">{error}</p>}
            {quoteError && <p className="error">{quoteError}</p>}

            <div className="checkout-step-actions">
              {step > 0 ? (
                <button type="button" className="secondary" onClick={goBack} disabled={loading}>
                  Back
                </button>
              ) : (
                <span />
              )}
              {step < STEPS.length - 1 ? (
                <button type="button" onClick={goNext} disabled={quoteLoading}>
                  Next
                </button>
              ) : (
                <button type="button" onClick={handleSubmit} disabled={loading || quoteLoading || !pricing}>
                  {loading
                    ? 'Processing…'
                    : paymentMethod === 'ONLINE_MAYA'
                      ? 'Continue to Maya payment'
                      : 'Confirm pay-at-hotel booking'}
                </button>
              )}
            </div>
          </div>
        </ScrollReveal>

        <ScrollReveal className="checkout-sidebar" variant="scale" delay={140} trigger="mount">
          {quoteLoading ? (
            <aside className="checkout-sidebar-panel">
              <p className="muted text-sm">Updating price for your stay…</p>
            </aside>
          ) : (
            <CheckoutPriceSummary
              room={room}
              roomName={roomCatalog?.name}
              imageUrl={roomCatalog?.imageUrls?.[0]}
              checkIn={checkIn}
              checkOut={checkOut}
              pricing={pricing}
              pricingPolicy={pricingPolicy}
              services={services}
              cart={cart}
              grandTotal={grandTotal}
            />
          )}
        </ScrollReveal>
      </div>
    </div>
  )
}
