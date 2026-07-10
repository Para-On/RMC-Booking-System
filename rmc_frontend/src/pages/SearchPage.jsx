import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BookingFilters from '@/components/booking/BookingFilters'
import HotelHeroSection from '@/components/booking/HotelHeroSection'
import RoomSearchCarousel from '@/components/room/RoomSearchCarousel'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { RoomSearchCarouselSkeleton } from '@/components/guest/GuestPageSkeleton'
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

  function viewRoomDetails(room) {
    if (!appliedSearch) return
    navigate(`/rooms/${room.roomTypeId}`, {
      state: {
        room,
        checkIn: appliedSearch.checkIn,
        checkOut: appliedSearch.checkOut,
        guests: appliedSearch.guests,
      },
    })
  }

  return (
    <div className="search-page">
      <div className="search-page-hero-wrap">
        <section className="search-page-section search-page-section--hero" aria-label="Hotel">
          <HotelHeroSection hotelId={displayHotelId} />
        </section>

        <div className="search-page-filters">
          <div className="search-page-filters-inner">
            <BookingFilters
              embedded
              onSearch={handleSearch}
              onFiltersChange={scheduleSearch}
              loading={loading}
              appliedSearch={appliedSearch}
            />
          </div>
        </div>
      </div>

      <section
        id="search-rooms"
        className="search-page-section search-page-section--rooms"
        aria-label="Available rooms"
      >
        <div className="search-page-rooms-inner">
          <ScrollReveal className="search-page-rooms-intro" variant="slide-up">
            <h2 className="search-page-rooms-title">Explore our Rooms</h2>
          </ScrollReveal>

          <ScrollReveal className="search-page-rooms-content" variant="fade" delay={100}>
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

            {loading && rooms.length === 0 && <RoomSearchCarouselSkeleton />}

            {!loading && rooms.length > 0 && (
              <RoomSearchCarousel
                rooms={rooms}
                appliedSearch={appliedSearch}
                pricingPolicy={pricingPolicy}
                onViewDetails={viewRoomDetails}
              />
            )}
          </ScrollReveal>
        </div>
      </section>
    </div>
  )
}
