import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Building2,
  CalendarDays,
  Check,
  ConciergeBell,
  CreditCard,
  MessageSquarePlus,
  Package,
  Plus,
  ShieldCheck,
  Sparkles,
  UserRound,
  Users,
  Wallet,
} from 'lucide-react'
import { CheckoutPriceSummary } from '@/components/checkout/CheckoutPriceSummary'
import { CheckoutProgress, STEPS } from '@/components/checkout/CheckoutProgress'
import CheckoutRoomSummary from '@/components/checkout/CheckoutRoomSummary'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { ServiceAddonCard } from '@/components/extras/ServiceAddonCard'
import { catalogFromAvailability, resolveStayPricing } from '@/lib/roomCatalog'
import { loadStoredPromoCodes, clearStoredPromoCodes } from '@/lib/promoCodes'
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

function guestInitials(name) {
  const parts = String(name || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
}

function CheckoutStepIntro({ id, title, description }) {
  return (
    <header className="checkout-step-intro">
      <h2 id={id} className="checkout-step-heading">
        {title}
      </h2>
      {description ? <p className="checkout-step-lead">{description}</p> : null}
    </header>
  )
}

export default function CheckoutPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { pricingPolicy } = usePricingPolicy()
  const initialRoom = state?.room
  const checkIn = state?.checkIn
  const checkOut = state?.checkOut
  const storedPromo = loadStoredPromoCodes()
  const [activePromo, setActivePromo] = useState(() => ({
    promoType:
      state?.promoType || (storedPromo.applied ? storedPromo.promoType : '') || undefined,
    offerCode:
      state?.offerCode || (storedPromo.applied ? storedPromo.offerCode : '') || undefined,
    organizationCode:
      state?.organizationCode ||
      (storedPromo.applied ? storedPromo.organizationCode : '') ||
      undefined,
  }))
  const promoType = activePromo.promoType
  const offerCode = activePromo.offerCode
  const organizationCode = activePromo.organizationCode
  const [room, setRoom] = useState(initialRoom)
  const [quoteLoading, setQuoteLoading] = useState(Boolean(initialRoom && checkIn && checkOut))
  const [quoteError, setQuoteError] = useState('')
  const [step, setStep] = useState(0)
  const [stepAnim, setStepAnim] = useState('forward')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [additionalGuests, setAdditionalGuests] = useState([])
  const [paymentMethod, setPaymentMethod] = useState('PAY_AT_HOTEL')
  const [ageConfirmed, setAgeConfirmed] = useState(false)
  const [dpaConsentAccepted, setDpaConsentAccepted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [services, setServices] = useState([])
  const [items, setItems] = useState([])
  const [extrasLoading, setExtrasLoading] = useState(true)
  const [cart, setCart] = useState([])
  const [itemCart, setItemCart] = useState([])
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
        const result = await checkStayAvailability(
          initialRoom.roomTypeId,
          checkIn,
          checkOut,
          initialRoom.ratePlanId,
          { promoType, offerCode, organizationCode }
        )
        if (!result.available || !result.room) {
          throw new Error(result.message || 'Room is not available for the selected dates.')
        }
        if (!cancelled) {
          const quoted = result.room
          const codesWereSent = Boolean(offerCode || organizationCode || promoType)
          const promoStillApplies = Boolean(quoted.promo || quoted.originalTotalTaxInclusive != null)
          if (codesWereSent && !promoStillApplies) {
            clearStoredPromoCodes()
            setActivePromo({
              promoType: undefined,
              offerCode: undefined,
              organizationCode: undefined,
            })
          }
          // Keep the guest-selected plan fields; refresh totals/promo from server quote.
          setRoom({
            ...initialRoom,
            ...quoted,
            ratePlanId: initialRoom.ratePlanId ?? quoted.ratePlanId,
          })
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
  }, [initialRoom, checkIn, checkOut, promoType, offerCode, organizationCode])

  const cartSet = useMemo(() => new Set(cart), [cart])
  const itemCartSet = useMemo(() => new Set(itemCart), [itemCart])
  const servicesTotal = useMemo(() => {
    return cart.reduce((sum, id) => {
      const service = services.find((entry) => entry.id === id)
      if (!service || service.free || service.price == null) return sum
      return sum + Number(service.price)
    }, 0)
  }, [cart, services])
  const itemsTotal = useMemo(() => {
    return itemCart.reduce((sum, id) => {
      const item = items.find((entry) => entry.id === id)
      if (!item || item.free || item.price == null) return sum
      return sum + Number(item.price)
    }, 0)
  }, [itemCart, items])

  if (!initialRoom || !checkIn || !checkOut || !initialRoom.ratePlanId) {
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
  const grandTotal = pricing ? pricing.total + servicesTotal + itemsTotal : servicesTotal + itemsTotal
  const roomCatalog = catalogFromAvailability(room, {
    taxInclusive: false,
    checkIn,
    checkOut,
    pricingPolicy,
  })
  const stayLabel = formatStayRange(checkIn, checkOut)

  function toggleCart(serviceId) {
    setCart((prev) =>
      prev.includes(serviceId) ? prev.filter((id) => id !== serviceId) : [...prev, serviceId]
    )
  }

  function toggleItemCart(itemId) {
    setItemCart((prev) =>
      prev.includes(itemId) ? prev.filter((id) => id !== itemId) : [...prev, itemId]
    )
  }

  function validateStep(index) {
    setError('')
    if (index === 2) {
      if (!fullName.trim() || !email.trim() || !phone.trim()) {
        setError('Please enter your full name, email, and phone.')
        return false
      }
      for (let i = 0; i < additionalGuests.length; i += 1) {
        const guest = additionalGuests[i]
        if (!guest.fullName?.trim()) {
          setError(`Please enter a name for additional guest ${i + 1}.`)
          return false
        }
        if (guest.email?.trim() && guest.email.trim().toLowerCase() === email.trim().toLowerCase()) {
          setError('Additional guests must use a different email from the primary guest.')
          return false
        }
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

  function addAdditionalGuest() {
    const fromAdultsChildren = Number(room?.maxAdults || 0) + Number(room?.maxChildren || 0)
    const capacity = Math.max(1, fromAdultsChildren || Number(room?.totalCapacity || 1))
    if (1 + additionalGuests.length >= capacity) {
      setError(`This room can accommodate up to ${capacity} guests.`)
      return
    }
    setError('')
    setAdditionalGuests((prev) => [...prev, { fullName: '', email: '', phone: '' }])
  }

  function updateAdditionalGuest(index, field, value) {
    setAdditionalGuests((prev) =>
      prev.map((guest, i) => (i === index ? { ...guest, [field]: value } : guest))
    )
  }

  function removeAdditionalGuest(index) {
    setAdditionalGuests((prev) => prev.filter((_, i) => i !== index))
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
        .filter((item) => itemCart.includes(item.id))
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
        additionalGuests: additionalGuests
          .filter((guest) => guest.fullName?.trim())
          .map((guest) => ({
            fullName: guest.fullName.trim(),
            email: guest.email?.trim() || null,
            phone: guest.phone?.trim() || null,
          })),
        promoType: promoType || null,
        offerCode: offerCode || null,
        organizationCode: organizationCode || null,
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
              <section aria-labelledby="step-extras" className="checkout-panel-stack">
                <CheckoutStepIntro
                  id="step-extras"
                  title="Extras"
                  description="Add optional services or items to your stay. Everything here is optional — you can skip ahead anytime."
                />

                {extrasLoading ? (
                  <div className="checkout-panel checkout-panel--soft">
                    <p className="checkout-empty-hint" style={{ border: 'none', padding: 0, background: 'transparent' }}>
                      Loading extras…
                    </p>
                  </div>
                ) : (
                  <>
                    {services.length === 0 && items.length === 0 ? (
                      <div className="checkout-panel checkout-panel--soft">
                        <div className="checkout-panel__title-row">
                          <span className="checkout-panel__icon" aria-hidden>
                            <Sparkles />
                          </span>
                          <div>
                            <h3 className="checkout-panel__title">No extras listed</h3>
                            <p className="checkout-panel__meta">
                              You can still leave a special request below, or continue to guest details.
                            </p>
                          </div>
                        </div>
                      </div>
                    ) : null}

                    {services.length > 0 ? (
                      <div className="checkout-panel">
                        <div className="checkout-panel__head">
                          <div className="checkout-panel__title-row">
                            <span className="checkout-panel__icon" aria-hidden>
                              <ConciergeBell />
                            </span>
                            <div>
                              <h3 className="checkout-panel__title">Optional services</h3>
                              <p className="checkout-panel__meta">
                                Experiences and add-ons you can include with your stay.
                              </p>
                            </div>
                          </div>
                          {cart.length > 0 ? (
                            <span className="checkout-capacity-chip">
                              {cart.length} selected
                            </span>
                          ) : null}
                        </div>
                        <div className="service-addon-list">
                          {services.map((service, index) => (
                            <ScrollReveal key={service.id} delay={index * 60} variant="scale">
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
                      <div className="checkout-panel">
                        <div className="checkout-panel__head">
                          <div className="checkout-panel__title-row">
                            <span className="checkout-panel__icon" aria-hidden>
                              <Package />
                            </span>
                            <div>
                              <h3 className="checkout-panel__title">Optional items</h3>
                              <p className="checkout-panel__meta">
                                Products and supplies available for your room.
                              </p>
                            </div>
                          </div>
                          {itemCart.length > 0 ? (
                            <span className="checkout-capacity-chip">
                              {itemCart.length} selected
                            </span>
                          ) : null}
                        </div>
                        <div className="service-addon-list">
                          {items.map((item, index) => (
                            <ScrollReveal key={item.id} delay={index * 60} variant="scale">
                              <ServiceAddonCard
                                service={item}
                                inCart={itemCartSet.has(item.id)}
                                onAddToCart={() => toggleItemCart(item.id)}
                                onRemoveFromCart={() => toggleItemCart(item.id)}
                              />
                            </ScrollReveal>
                          ))}
                        </div>
                      </div>
                    ) : null}

                    <div className="checkout-panel checkout-panel--soft">
                      <div className="checkout-panel__title-row">
                        <span className="checkout-panel__icon" aria-hidden>
                          <MessageSquarePlus />
                        </span>
                        <div>
                          <h3 className="checkout-panel__title">Other requests</h3>
                          <p className="checkout-panel__meta">
                            Need something not listed above? Tell us here — optional.
                          </p>
                        </div>
                      </div>
                      <label className="checkout-field checkout-field--full checkout-custom-request">
                        <span className="sr-only">Other requests</span>
                        <textarea
                          rows={3}
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
              <section aria-labelledby="step-guest" className="checkout-panel-stack">
                <CheckoutStepIntro
                  id="step-guest"
                  title="Guest details"
                  description="Tell us who is staying. The primary guest is the booker; you can add companions below."
                />

                <div className="checkout-panel">
                  <div className="checkout-panel__head">
                    <div className="checkout-panel__title-row">
                      <span className="checkout-panel__icon" aria-hidden>
                        <UserRound />
                      </span>
                      <div>
                        <h3 className="checkout-panel__title">Primary guest</h3>
                        <p className="checkout-panel__meta">
                          {1 + additionalGuests.length} guest
                          {1 + additionalGuests.length === 1 ? '' : 's'} on this booking
                        </p>
                      </div>
                    </div>
                    {(room?.maxAdults || room?.maxChildren || room?.totalCapacity) && (
                      <span className="checkout-capacity-chip">
                        <Users aria-hidden />
                        Holds up to{' '}
                        {Number(room.maxAdults || 0) + Number(room.maxChildren || 0) ||
                          room.totalCapacity}
                      </span>
                    )}
                  </div>

                  <div className="checkout-field-grid">
                    <label className="checkout-field checkout-field--full">
                      <span>Full name</span>
                      <input
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="As shown on a valid ID"
                        autoComplete="name"
                        required
                      />
                    </label>
                    <label className="checkout-field">
                      <span>Email</span>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="you@email.com"
                        autoComplete="email"
                        required
                      />
                    </label>
                    <label className="checkout-field">
                      <span>Phone</span>
                      <input
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="+63…"
                        autoComplete="tel"
                        required
                      />
                    </label>
                  </div>
                </div>

                <div className="checkout-panel">
                  <div className="checkout-panel__head">
                    <div className="checkout-panel__title-row">
                      <span className="checkout-panel__icon" aria-hidden>
                        <Users />
                      </span>
                      <div>
                        <h3 className="checkout-panel__title">Other guests</h3>
                        <p className="checkout-panel__meta">
                          Optional. Only the primary booker appears in the staff guest list.
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="checkout-text-btn"
                      onClick={addAdditionalGuest}
                    >
                      <Plus className="size-3.5 shrink-0" aria-hidden />
                      <span>Add guest</span>
                    </button>
                  </div>

                  {additionalGuests.length === 0 ? (
                    <p className="checkout-empty-hint">
                      Traveling alone? You can continue without adding anyone else.
                    </p>
                  ) : (
                    <div className="checkout-companion-list">
                      {additionalGuests.map((guest, index) => (
                        <div key={index} className="checkout-companion">
                          <div className="checkout-companion__head">
                            <span className="checkout-companion__avatar" aria-hidden>
                              {guestInitials(guest.fullName || `G${index + 2}`)}
                            </span>
                            <div className="checkout-companion__label">
                              <strong>Guest {index + 2}</strong>
                              <span>Companion</span>
                            </div>
                            <button
                              type="button"
                              className="checkout-text-btn checkout-text-btn--muted"
                              onClick={() => removeAdditionalGuest(index)}
                            >
                              Remove
                            </button>
                          </div>
                          <div className="checkout-field-grid">
                            <label className="checkout-field checkout-field--full">
                              <span>Full name</span>
                              <input
                                value={guest.fullName}
                                onChange={(e) =>
                                  updateAdditionalGuest(index, 'fullName', e.target.value)
                                }
                                required
                              />
                            </label>
                            <label className="checkout-field">
                              <span>Email (optional)</span>
                              <input
                                type="email"
                                value={guest.email}
                                onChange={(e) =>
                                  updateAdditionalGuest(index, 'email', e.target.value)
                                }
                              />
                            </label>
                            <label className="checkout-field">
                              <span>Phone (optional)</span>
                              <input
                                value={guest.phone}
                                onChange={(e) =>
                                  updateAdditionalGuest(index, 'phone', e.target.value)
                                }
                              />
                            </label>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </section>
            )}

            {stepId === 'payment' && (
              <section aria-labelledby="step-payment" className="checkout-panel-stack">
                <CheckoutStepIntro
                  id="step-payment"
                  title="Payment"
                  description="Choose how you’d like to pay, then confirm the required agreements."
                />

                <fieldset className="checkout-pay-methods">
                  <legend className="sr-only">Payment method</legend>
                  <label
                    className={cn(
                      'checkout-pay-option',
                      paymentMethod === 'PAY_AT_HOTEL' && 'is-selected'
                    )}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="PAY_AT_HOTEL"
                      checked={paymentMethod === 'PAY_AT_HOTEL'}
                      onChange={() => setPaymentMethod('PAY_AT_HOTEL')}
                    />
                    <span className="checkout-pay-option__icon" aria-hidden>
                      <Building2 />
                    </span>
                    <span className="checkout-pay-option__body">
                      <span className="checkout-pay-option__title">Pay at hotel</span>
                      <span className="checkout-pay-option__desc">
                        Confirm now and settle the balance on arrival.
                      </span>
                    </span>
                    <span className="checkout-pay-option__check" aria-hidden>
                      <Check />
                    </span>
                  </label>
                  <label
                    className={cn(
                      'checkout-pay-option',
                      paymentMethod === 'ONLINE_MAYA' && 'is-selected'
                    )}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="ONLINE_MAYA"
                      checked={paymentMethod === 'ONLINE_MAYA'}
                      onChange={() => setPaymentMethod('ONLINE_MAYA')}
                    />
                    <span className="checkout-pay-option__icon" aria-hidden>
                      <Wallet />
                    </span>
                    <span className="checkout-pay-option__body">
                      <span className="checkout-pay-option__title">Pay now with Maya</span>
                      <span className="checkout-pay-option__desc">
                        Card or Maya wallet — you’ll continue to secure checkout.
                      </span>
                    </span>
                    <span className="checkout-pay-option__check" aria-hidden>
                      <Check />
                    </span>
                  </label>
                </fieldset>

                <div className="checkout-panel checkout-panel--soft">
                  <div className="checkout-panel__title-row checkout-panel__title-row--compact">
                    <span className="checkout-panel__icon" aria-hidden>
                      <ShieldCheck />
                    </span>
                    <h3 className="checkout-panel__title">Agreements</h3>
                  </div>
                  <div className="checkout-consent-list">
                    <label
                      className={cn('checkout-consent', ageConfirmed && 'is-checked')}
                    >
                      <input
                        type="checkbox"
                        checked={ageConfirmed}
                        onChange={(e) => setAgeConfirmed(e.target.checked)}
                      />
                      <span className="checkout-consent__box" aria-hidden>
                        <Check />
                      </span>
                      <span>I confirm I am 18 years or older</span>
                    </label>
                    <label
                      className={cn('checkout-consent', dpaConsentAccepted && 'is-checked')}
                    >
                      <input
                        type="checkbox"
                        checked={dpaConsentAccepted}
                        onChange={(e) => setDpaConsentAccepted(e.target.checked)}
                      />
                      <span className="checkout-consent__box" aria-hidden>
                        <Check />
                      </span>
                      <span>I agree to the data privacy policy (v1.0)</span>
                    </label>
                  </div>
                </div>
              </section>
            )}

            {stepId === 'confirm' && (
              <section aria-labelledby="step-confirm" className="checkout-panel-stack">
                <CheckoutStepIntro
                  id="step-confirm"
                  title="Review & confirm"
                  description="Double-check the details below before you finish booking."
                />

                <div className="checkout-review-grid">
                  <article className="checkout-review-block">
                    <header className="checkout-review-block__head">
                      <CalendarDays aria-hidden />
                      <h3>Stay</h3>
                    </header>
                    <p className="checkout-review-block__title">
                      {room.roomTypeName || room.name}
                    </p>
                    {roomCatalog?.ratePlanName && (
                      <p className="checkout-review-block__meta">{roomCatalog.ratePlanName}</p>
                    )}
                    <p className="checkout-review-block__meta">{stayLabel}</p>
                  </article>

                  <article className="checkout-review-block">
                    <header className="checkout-review-block__head">
                      <CreditCard aria-hidden />
                      <h3>Payment</h3>
                    </header>
                    <p className="checkout-review-block__title">
                      {paymentMethod === 'ONLINE_MAYA'
                        ? 'Pay now with Maya'
                        : 'Pay at hotel on arrival'}
                    </p>
                    <p className="checkout-review-block__meta">
                      {paymentMethod === 'ONLINE_MAYA'
                        ? 'You’ll be redirected to Maya after confirming.'
                        : 'No charge until you arrive.'}
                    </p>
                  </article>

                  <article className="checkout-review-block checkout-review-block--wide">
                    <header className="checkout-review-block__head">
                      <Users aria-hidden />
                      <h3>Guests ({1 + additionalGuests.length})</h3>
                    </header>
                    <ul className="checkout-review-people">
                      <li>
                        <span className="checkout-companion__avatar" aria-hidden>
                          {guestInitials(fullName)}
                        </span>
                        <div>
                          <strong>{fullName || 'Primary guest'}</strong>
                          <span className="checkout-review-people__tag">Primary</span>
                          <p>
                            {email}
                            {phone ? ` · ${phone}` : ''}
                          </p>
                        </div>
                      </li>
                      {additionalGuests.map((guest, index) => (
                        <li key={`${guest.fullName}-${index}`}>
                          <span className="checkout-companion__avatar" aria-hidden>
                            {guestInitials(guest.fullName)}
                          </span>
                          <div>
                            <strong>{guest.fullName.trim() || `Guest ${index + 2}`}</strong>
                            <p>
                              {[guest.email?.trim(), guest.phone?.trim()]
                                .filter(Boolean)
                                .join(' · ') || 'No contact details'}
                            </p>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </article>

                  {(cart.length > 0 || itemCart.length > 0 || customExtrasRequest.trim()) && (
                    <article className="checkout-review-block checkout-review-block--wide">
                      <header className="checkout-review-block__head">
                        <Plus aria-hidden />
                        <h3>Extras</h3>
                      </header>
                      <ul className="checkout-review-extras">
                        {cart.map((id) => {
                          const service = services.find((entry) => entry.id === id)
                          if (!service) return null
                          return (
                            <li key={`service-${id}`}>
                              <span>{service.title}</span>
                              <strong>
                                {service.free
                                  ? 'Free'
                                  : formatMoney(service.price, room.currency)}
                              </strong>
                            </li>
                          )
                        })}
                        {itemCart.map((id) => {
                          const item = items.find((entry) => entry.id === id)
                          if (!item) return null
                          return (
                            <li key={`item-${id}`}>
                              <span>{item.title}</span>
                              <strong>
                                {item.free ? 'Free' : formatMoney(item.price, room.currency)}
                              </strong>
                            </li>
                          )
                        })}
                        {customExtrasRequest.trim() ? (
                          <li>
                            <span>Other request</span>
                            <strong className="checkout-review-extras__note">
                              {customExtrasRequest.trim()}
                            </strong>
                          </li>
                        ) : null}
                      </ul>
                    </article>
                  )}
                </div>

                {pricing ? (
                  <div className="checkout-confirm-total" aria-live="polite">
                    <span>Total due</span>
                    <strong>{formatMoney(grandTotal, room.currency)}</strong>
                  </div>
                ) : null}
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
                      : 'Request pay-at-hotel booking'}
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
              ratePlanName={roomCatalog?.ratePlanName}
              imageUrl={roomCatalog?.imageUrls?.[0]}
              checkIn={checkIn}
              checkOut={checkOut}
              pricing={pricing}
              pricingPolicy={pricingPolicy}
              services={services}
              cart={cart}
              items={items}
              itemCart={itemCart}
              grandTotal={grandTotal}
            />
          )}
        </ScrollReveal>
      </div>
    </div>
  )
}
