import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ScrollReveal from '@/components/motion/ScrollReveal'

export default function LookupPage() {
  const navigate = useNavigate()
  const [reference, setReference] = useState('')
  const [email, setEmail] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    navigate(`/booking/${encodeURIComponent(reference.trim())}?email=${encodeURIComponent(email.trim())}`)
  }

  return (
    <ScrollReveal variant="slide-up" trigger="mount" delay={100}>
      <div className="card">
        <h1>Find your booking</h1>
        <form className="search-form" onSubmit={handleSubmit}>
          <label>
            Booking reference
            <input value={reference} onChange={(e) => setReference(e.target.value)} required />
          </label>
          <label>
            Email used at booking
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <button type="submit">View booking</button>
        </form>
      </div>
    </ScrollReveal>
  )
}
