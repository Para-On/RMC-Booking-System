import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { staffLogin } from '../../staffApi'

export default function StaffLoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('frontdesk@rmc.local')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await staffLogin(email, password)
      navigate('/staff/arrivals')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card staff-login">
      <h1>Staff login</h1>
      <p className="muted">Front desk and manager access</p>
      {error && <p className="error">{error}</p>}
      <form className="checkout-form" onSubmit={handleSubmit}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="username"
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="hint">
        Dev accounts: <code>frontdesk@rmc.local</code> / <code>staff123</code> or{' '}
        <code>manager@rmc.local</code> / <code>manager123</code>
      </p>
    </div>
  )
}
