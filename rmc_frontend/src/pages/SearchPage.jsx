import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BookingFilters from '@/components/booking/BookingFilters'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { filterRoomsByGuests, getDefaultSearchParams } from '@/lib/bookingFilters'
import { catalogFromAvailability } from '@/lib/roomCatalog'
import { searchAvailability } from '../api'

export default function SearchPage() {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [hasSearched, setHasSearched] = useState(false)
  const [appliedSearch, setAppliedSearch] = useState(null)

  const handleSearch = useCallback(async ({ hotelId, checkIn, checkOut, guests }) => {
    if (!checkIn || !checkOut || checkIn >= checkOut) return

    setLoading(true)
    setError('')
    try {
      const data = await searchAvailability(checkIn, checkOut)
      const filtered = filterRoomsByGuests(data.rooms || [], guests)
      setRooms(filtered)
      setAppliedSearch({ hotelId, checkIn, checkOut, guests })
      setHasSearched(true)
    } catch (err) {
      setError(err.message)
      setRooms([])
      setAppliedSearch(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    handleSearch(getDefaultSearchParams())
  }, [handleSearch])

  function startBooking(room) {
    if (!appliedSearch) return
    navigate('/checkout', {
      state: {
        room,
        checkIn: appliedSearch.checkIn,
        checkOut: appliedSearch.checkOut,
        guests: appliedSearch.guests,
      },
    })
  }

  return (
    <div className="catalog-page mx-auto w-full max-w-5xl space-y-6 px-0 sm:px-1">
      <BookingFilters
        onSearch={handleSearch}
        loading={loading}
        appliedSearch={appliedSearch}
        resultCount={hasSearched ? rooms.length : null}
      />

      <div className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">Our rooms</h1>
        <p className="text-muted-foreground">
          Browse available rooms and book in a few clicks.
        </p>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertTitle>Could not load availability</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {!loading && !error && hasSearched && rooms.length === 0 && (
        <Alert>
          <AlertTitle>No rooms available</AlertTitle>
          <AlertDescription>
            Nothing matches your stay
            {appliedSearch
              ? ` from ${appliedSearch.checkIn} to ${appliedSearch.checkOut} for ${appliedSearch.guests.rooms} room(s), ${appliedSearch.guests.adults} adult(s)${appliedSearch.guests.children ? ` and ${appliedSearch.guests.children} child(ren)` : ''}`
              : ''}
            . Try different dates or guest counts.
          </AlertDescription>
        </Alert>
      )}

      {loading && rooms.length === 0 && (
        <p className="text-center text-sm text-muted-foreground">Loading available rooms…</p>
      )}

      <div className="room-catalog-list flex flex-col gap-6 pt-2">
        {rooms.map((room) => (
          <RoomCatalogCard
            key={room.roomTypeId}
            layout="vertical"
            {...catalogFromAvailability(room)}
            bookLabel="Book now"
            onBook={() => startBooking(room)}
          />
        ))}
      </div>
    </div>
  )
}
