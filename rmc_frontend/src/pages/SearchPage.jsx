import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BookingFilters from '@/components/booking/BookingFilters'
import HotelHeroSection from '@/components/booking/HotelHeroSection'
import RoomSearchCarousel from '@/components/room/RoomSearchCarousel'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { filterRoomsByGuests, getDefaultSearchParams } from '@/lib/bookingFilters'
import { usePricingPolicy } from '@/context/PricingPolicyProvider'
import { searchAvailability } from '../api'

const defaultSearch = getDefaultSearchParams()

export default function SearchPage() {
  const navigate = useNavigate()
  const { pricingPolicy } = usePricingPolicy()
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [hasSearched, setHasSearched] = useState(false)
  const [appliedSearch, setAppliedSearch] = useState(null)
  const [displayHotelId, setDisplayHotelId] = useState(defaultSearch.hotelId)
  const searchTimerRef = useRef(null)

  const handleSearch = useCallback(async ({ hotelId, checkIn, checkOut, guests }) => {
    if (!checkIn || !checkOut || checkIn >= checkOut) return

    setLoading(true)
    setError('')
    setDisplayHotelId(hotelId)
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

  const scheduleSearch = useCallback(
    (params) => {
      setDisplayHotelId(params.hotelId)
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }
      searchTimerRef.current = setTimeout(() => {
        handleSearch(params)
      }, 350)
    },
    [handleSearch]
  )

  useEffect(() => {
    return () => {
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }
    }
  }, [])

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
    <div className="catalog-page mx-auto w-full max-w-6xl px-0 sm:px-1">
      <HotelHeroSection hotelId={displayHotelId} />

      <div className="search-page-filters my-5 sm:my-6">
        <BookingFilters
          embedded
          onSearch={handleSearch}
          onFiltersChange={scheduleSearch}
          loading={loading}
          appliedSearch={appliedSearch}
        />
      </div>

      <div className="search-page-rooms-intro mb-3 space-y-1 sm:mb-4">
        <h2 className="text-lg font-semibold tracking-tight sm:text-xl">Our rooms</h2>
        <p className="text-sm text-muted-foreground">
          Browse available rooms and book in a few clicks.
        </p>
        {hasSearched && !loading && (
          <p className="text-sm text-muted-foreground">
            {rooms.length === 0
              ? 'No rooms match your search. Try different dates or guest counts.'
              : `${rooms.length} room${rooms.length === 1 ? '' : 's'} available`}
          </p>
        )}
      </div>

      <section className="search-page-panel search-page-panel--rooms" aria-label="Available rooms">
        <div className="search-page-panel-body search-page-panel-body--rooms">
          {error && (
            <Alert variant="destructive" className="m-4">
              <AlertTitle>Could not load availability</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {!loading && !error && hasSearched && rooms.length === 0 && (
            <div className="flex h-full items-center justify-center p-4">
              <Alert className="max-w-lg">
                <AlertTitle>No rooms available</AlertTitle>
                <AlertDescription>
                  Nothing matches your stay
                  {appliedSearch
                    ? ` from ${appliedSearch.checkIn} to ${appliedSearch.checkOut} for ${appliedSearch.guests.rooms} room(s), ${appliedSearch.guests.adults} adult(s)${appliedSearch.guests.children ? ` and ${appliedSearch.guests.children} child(ren)` : ''}`
                    : ''}
                  . Try different dates or guest counts.
                </AlertDescription>
              </Alert>
            </div>
          )}

          {loading && rooms.length === 0 && (
            <p className="flex h-full items-center justify-center text-sm text-muted-foreground">
              Loading available rooms…
            </p>
          )}

          {!loading && rooms.length > 0 && (
            <RoomSearchCarousel
              rooms={rooms}
              appliedSearch={appliedSearch}
              pricingPolicy={pricingPolicy}
              onBook={startBooking}
            />
          )}
        </div>
      </section>
    </div>
  )
}
