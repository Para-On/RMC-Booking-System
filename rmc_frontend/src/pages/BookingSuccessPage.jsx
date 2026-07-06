import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmPayment, getBookingStatus } from '../api'

export default function BookingSuccessPage() {
  const [params] = useSearchParams()
  const reference = params.get('reference')
  const urlStatus = params.get('status')
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const confirmInFlight = useRef(false)

  useEffect(() => {
    if (!reference) {
      setLoading(false)
      setError('Missing booking reference.')
      return
    }

    let attempts = 0
    let timer
    let cancelled = false

    async function poll() {
      try {
        if (!confirmInFlight.current) {
          confirmInFlight.current = true
          try {
            await confirmPayment(reference)
          } finally {
            confirmInFlight.current = false
          }
        }
        const data = await getBookingStatus(reference)
        if (cancelled) return
        setStatus(data.status)
        setError('')
        if (data.status === 'CONFIRMED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
          setLoading(false)
          return
        }
        if (urlStatus === 'failed' || urlStatus === 'cancelled') {
          setLoading(false)
          return
        }
      } catch (err) {
        if (cancelled) return
        try {
          const data = await getBookingStatus(reference)
          if (cancelled) return
          setStatus(data.status)
          if (data.status === 'CONFIRMED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
            setError('')
            setLoading(false)
            return
          }
        } catch {
          // keep original confirm error below
        }
        if (attempts === 0) {
          setError(err.message)
        }
      }

      attempts += 1
      if (attempts < 12) {
        timer = setTimeout(poll, 3000)
      } else {
        setLoading(false)
      }
    }

    poll()
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [reference, urlStatus])

  if (!reference) {
    return (
      <div className="card">
        <p className="error">No booking reference provided.</p>
        <Link to="/">Back to search</Link>
      </div>
    )
  }

  const failed =
    urlStatus === 'failed' ||
    urlStatus === 'cancelled' ||
    status === 'FAILED' ||
    status === 'CANCELLED'

  return (
    <div className="card">
      <h1>Payment status</h1>
      <p>
        Reference: <strong>{reference}</strong>
      </p>

      {loading && <p>Confirming your payment…</p>}
      {error && !loading && <p className="error">{error}</p>}

      {!loading && status === 'CONFIRMED' && (
        <div className="success">
          <p>Payment successful. Your booking is confirmed.</p>
        </div>
      )}

      {!loading && status === 'PENDING_PAYMENT' && (
        <p>Payment is still processing. Check again shortly from Find booking.</p>
      )}

      {!loading && failed && (
        <p className="error">Payment was not completed. You can search again to rebook.</p>
      )}

      <p style={{ marginTop: '1.5rem' }}>
        <Link to={`/booking/${reference}`}>View booking</Link>
        {' · '}
        <Link to="/">Book another stay</Link>
      </p>
    </div>
  )
}
