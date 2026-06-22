import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getArrivals, staffLogout } from '../../staffApi'
import { getStaffAuth, isManager } from '../../staffAuth'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export default function StaffArrivalsPage() {
  const auth = getStaffAuth()
  const [date, setDate] = useState(todayIso())
  const [arrivals, setArrivals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadArrivals()
  }, [date])

  async function loadArrivals() {
    setLoading(true)
    setError('')
    try {
      const data = await getArrivals(date)
      setArrivals(data.arrivals || [])
    } catch (err) {
      setError(err.message)
      setArrivals([])
    } finally {
      setLoading(false)
    }
  }

  function handleLogout() {
    staffLogout()
    window.location.href = '/staff/login'
  }

  return (
    <div>
      <div className="staff-toolbar">
        <div>
          <h1>Arrivals</h1>
          <p className="muted">
            Signed in as {auth?.fullName} ({auth?.role})
          </p>
        </div>
        <div className="toolbar-actions">
          {isManager() && (
            <Link to="/staff/settings" className="secondary button-link">
              Settings
            </Link>
          )}
          <button type="button" className="secondary" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </div>

      <div className="card">
        <label>
          Arrival date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p>Loading arrivals…</p>}

      {!loading && arrivals.length === 0 && !error && (
        <div className="card">
          <p>No arrivals for this date.</p>
        </div>
      )}

      {!loading && arrivals.length > 0 && (
        <div className="card table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Guest</th>
                <th>Room type</th>
                <th>Status</th>
                <th>Payment</th>
                <th>Room #</th>
                <th>Check-in</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {arrivals.map((item) => (
                <tr key={item.bookingId}>
                  <td>{item.reference}</td>
                  <td>
                    {item.guestName}
                    <br />
                    <span className="muted">{item.guestEmail}</span>
                  </td>
                  <td>{item.roomTypeName}</td>
                  <td>
                    <span className={`badge badge-${item.status.toLowerCase()}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>{item.paymentMethod}</td>
                  <td>{item.roomNumber || '—'}</td>
                  <td>{item.checkedInAt ? 'Done' : 'Pending'}</td>
                  <td>
                    <Link to={`/staff/bookings/${item.bookingId}`} className="btn btn-sm">
                      Open
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
