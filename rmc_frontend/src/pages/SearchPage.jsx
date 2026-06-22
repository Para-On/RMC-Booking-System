import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { dayAfter, searchAvailability, tomorrowIso } from '../api'

export default function SearchPage() {
  const navigate = useNavigate()
  const [checkIn, setCheckIn] = useState(tomorrowIso())
  const [checkOut, setCheckOut] = useState(dayAfter(tomorrowIso()))
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)

  async function handleSearch(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await searchAvailability(checkIn, checkOut)
      setRooms(data.rooms || [])
      setSearched(true)
    } catch (err) {
      setError(err.message)
      setRooms([])
    } finally {
      setLoading(false)
    }
  }

  function bookRoom(room) {
    navigate('/checkout', {
      state: { room, checkIn, checkOut },
    })
  }

  return (
    <div className="card">
      <h1>Find your stay</h1>
      <p>Search availability and book with pay-at-hotel or pay now with Maya.</p>

      <form className="search-form" onSubmit={handleSearch}>
        <label>
          Check-in
          <input
            type="date"
            value={checkIn}
            min={tomorrowIso()}
            onChange={(e) => {
              setCheckIn(e.target.value)
              if (e.target.value >= checkOut) setCheckOut(dayAfter(e.target.value))
            }}
            required
          />
        </label>
        <label>
          Check-out
          <input
            type="date"
            value={checkOut}
            min={dayAfter(checkIn)}
            onChange={(e) => setCheckOut(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? 'Searching…' : 'Search rooms'}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      {searched && !error && rooms.length === 0 && (
        <p>No rooms available for those dates.</p>
      )}

      <div className="room-list">
        {rooms.map((room) => (
          <div className="room-card" key={room.roomTypeId}>
            <div>
              <h2>{room.name}</h2>
              <p>{room.description}</p>
              <p>
                Up to {room.maxAdults} adults, {room.maxChildren} children ·{' '}
                {room.availableUnits} left
              </p>
            </div>
            <div>
              <div className="price">₱{Number(room.totalTaxInclusive).toLocaleString()}</div>
              <button type="button" onClick={() => bookRoom(room)}>
                Book — pay at hotel
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
