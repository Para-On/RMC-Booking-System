import { forwardRef, useEffect, useState } from 'react'
import { addDays, format, parseISO, startOfToday } from 'date-fns'
import {
  Building2,
  CalendarDays,
  ChevronDown,
  Minus,
  Pencil,
  Plus,
  Users,
  X,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  DEFAULT_GUESTS,
  formatAppliedSummary,
  formatGuestsLabel,
  getDefaultSearchParams,
  HOTELS,
} from '@/lib/bookingFilters'
import { dayAfter } from '@/api'
import { cn } from '@/lib/utils'

const defaults = getDefaultSearchParams()

function isoToDate(iso) {
  if (!iso) return undefined
  return parseISO(iso)
}

function dateToIso(date) {
  if (!date) return ''
  return format(date, 'yyyy-MM-dd')
}

function formatShortDate(iso) {
  if (!iso) return 'Select date'
  return format(parseISO(iso), 'EEE, MMM d, yyyy')
}

const FilterCell = forwardRef(function FilterCell(
  { icon: Icon, label, value, open, className, showDivider = true, ...props },
  ref
) {
  return (
    <div
      ref={ref}
      role="button"
      tabIndex={0}
      className={cn(
        'group flex min-h-[4.5rem] min-w-0 flex-1 cursor-pointer flex-col justify-center px-4 py-3 text-left outline-none transition-all duration-200 hover:bg-muted/50 focus-visible:ring-2 focus-visible:ring-ring/40 sm:px-5 sm:py-4',
        showDivider && 'border-border border-l',
        className
      )}
      {...props}
    >
      <span className="mb-1 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
        <Icon className="h-3.5 w-3.5 shrink-0" aria-hidden />
        {label}
      </span>
      <span className="flex items-center justify-between gap-3">
        <span className="truncate text-sm font-medium text-foreground sm:text-base">{value}</span>
        <ChevronDown
          className={cn(
            'h-4 w-4 shrink-0 text-muted-foreground transition-transform',
            open && 'rotate-180'
          )}
          aria-hidden
        />
      </span>
    </div>
  )
})

function DateDropdown({ label, value, onChange, minDate, icon: Icon, showDivider = true, className }) {
  const [open, setOpen] = useState(false)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <FilterCell
          icon={Icon}
          label={label}
          value={formatShortDate(value)}
          open={open}
          showDivider={showDivider}
          className={className}
        />
      </PopoverTrigger>
      <PopoverContent
        className="w-auto overflow-hidden rounded-xl border bg-popover p-0 shadow-lg"
        align="start"
        sideOffset={6}
      >
        <Calendar
          mode="single"
          selected={isoToDate(value)}
          onSelect={(date) => {
            if (!date) return
            onChange(dateToIso(date))
            setOpen(false)
          }}
          disabled={{ before: minDate }}
          defaultMonth={isoToDate(value) ?? minDate}
          className="p-3 [--cell-size:2.25rem] sm:p-4"
        />
      </PopoverContent>
    </Popover>
  )
}

function GuestCounter({ label, hint, value, min, max, onChange }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-border/60 py-3 last:border-b-0">
      <div className="min-w-0">
        <p className="text-sm font-medium">{label}</p>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </div>
      <div className="flex shrink-0 items-center gap-2.5">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full transition-all duration-200 hover:border-primary/30 hover:bg-primary/5 hover:shadow-sm active:scale-95"
          disabled={value <= min}
          onClick={() => onChange(value - 1)}
        >
          <Minus className="h-3.5 w-3.5" />
        </Button>
        <span className="w-6 text-center text-sm font-semibold tabular-nums">{value}</span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full transition-all duration-200 hover:border-primary/30 hover:bg-primary/5 hover:shadow-sm active:scale-95"
          disabled={value >= max}
          onClick={() => onChange(value + 1)}
        >
          <Plus className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  )
}

function FilterBar({
  hotelId,
  setHotelId,
  checkIn,
  checkOut,
  guests,
  setGuests,
  handleCheckInChange,
  setCheckOut,
  handleSearch,
  loading,
  datesValid,
  today,
  minCheckOut,
  selectedHotel,
  hotelOpen,
  setHotelOpen,
  guestsOpen,
  setGuestsOpen,
}) {
  return (
    <div className="flex flex-col lg:flex-row lg:items-stretch">
      <Popover open={hotelOpen} onOpenChange={setHotelOpen}>
        <PopoverTrigger asChild>
          <FilterCell
            icon={Building2}
            label="Hotel"
            value={selectedHotel.name}
            open={hotelOpen}
            showDivider={false}
            className="lg:min-w-[11rem] lg:max-w-[13rem]"
          />
        </PopoverTrigger>
        <PopoverContent
          className="w-[min(16rem,calc(100vw-2rem))] overflow-hidden rounded-xl border p-1 shadow-lg"
          align="start"
          sideOffset={6}
        >
          {HOTELS.map((hotel) => (
            <button
              key={hotel.id}
              type="button"
              className={cn(
                'flex w-full rounded-lg px-3 py-2.5 text-left text-sm transition-all duration-200 hover:bg-muted hover:shadow-sm',
                hotel.id === hotelId && 'bg-muted font-medium'
              )}
              onClick={() => {
                setHotelId(hotel.id)
                setHotelOpen(false)
              }}
            >
              {hotel.name}
            </button>
          ))}
        </PopoverContent>
      </Popover>

      <div className="grid grid-cols-1 border-t border-border sm:grid-cols-2 lg:flex lg:flex-1 lg:border-t-0">
        <DateDropdown
          label="Check-in"
          icon={CalendarDays}
          value={checkIn}
          minDate={today}
          onChange={handleCheckInChange}
          showDivider={false}
          className="sm:border-l-0 lg:border-l lg:border-border"
        />
        <DateDropdown
          label="Check-out"
          icon={CalendarDays}
          value={checkOut}
          minDate={minCheckOut}
          onChange={setCheckOut}
        />
      </div>

      <Popover open={guestsOpen} onOpenChange={setGuestsOpen}>
        <PopoverTrigger asChild>
          <FilterCell
            icon={Users}
            label="Guests"
            value={formatGuestsLabel(guests)}
            open={guestsOpen}
            className="border-t border-border lg:min-w-[12rem] lg:max-w-[15rem] lg:border-t-0"
          />
        </PopoverTrigger>
        <PopoverContent
          className="w-[min(20rem,calc(100vw-2rem))] rounded-xl border p-4 shadow-lg sm:w-80"
          align="end"
          sideOffset={6}
        >
          <p className="mb-1 text-sm font-medium">Guests</p>
          <GuestCounter
            label="Rooms"
            hint="How many rooms you need"
            value={guests.rooms}
            min={1}
            max={5}
            onChange={(rooms) => setGuests((g) => ({ ...g, rooms }))}
          />
          <GuestCounter
            label="Adults"
            value={guests.adults}
            min={1}
            max={12}
            onChange={(adults) => setGuests((g) => ({ ...g, adults }))}
          />
          <GuestCounter
            label="Children"
            hint="Ages 0–17"
            value={guests.children}
            min={0}
            max={8}
            onChange={(children) => setGuests((g) => ({ ...g, children }))}
          />
        </PopoverContent>
      </Popover>

      <div className="flex items-center justify-center border-t border-border p-3 sm:px-4 lg:min-w-[10.5rem] lg:border-l lg:border-t-0">
        <Button
          type="button"
          size="lg"
          disabled={!datesValid || loading}
          onClick={handleSearch}
          className="h-12 w-full min-w-[9rem] justify-center px-5 text-center text-base shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:brightness-110 active:translate-y-0 active:shadow-sm disabled:hover:translate-y-0 disabled:hover:shadow-sm disabled:hover:brightness-100"
        >
          {loading ? 'Searching…' : 'Search'}
        </Button>
      </div>
    </div>
  )
}

export default function BookingFilters({
  onSearch,
  loading = false,
  resultCount = null,
  appliedSearch = null,
  className,
}) {
  const today = startOfToday()
  const [hotelId, setHotelId] = useState(defaults.hotelId)
  const [checkIn, setCheckIn] = useState(defaults.checkIn)
  const [checkOut, setCheckOut] = useState(defaults.checkOut)
  const [guests, setGuests] = useState(DEFAULT_GUESTS)
  const [hotelOpen, setHotelOpen] = useState(false)
  const [guestsOpen, setGuestsOpen] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  const selectedHotel = HOTELS.find((h) => h.id === hotelId) ?? HOTELS[0]
  const minCheckOut = checkIn ? addDays(parseISO(checkIn), 1) : addDays(today, 1)
  const datesValid = checkIn && checkOut && checkIn < checkOut
  const summary = formatAppliedSummary(appliedSearch) ?? {
    hotel: selectedHotel.name,
    dates: `${formatShortDate(checkIn)} – ${formatShortDate(checkOut)}`,
    guests: formatGuestsLabel(guests),
  }

  useEffect(() => {
    if (!mobileOpen) return undefined
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [mobileOpen])

  function handleCheckInChange(nextIn) {
    setCheckIn(nextIn)
    if (!checkOut || checkOut <= nextIn) {
      setCheckOut(dayAfter(nextIn))
    }
  }

  function handleSearch() {
    if (!datesValid || loading) return
    onSearch({ hotelId, checkIn, checkOut, guests })
    setMobileOpen(false)
  }

  const filterBarProps = {
    hotelId,
    setHotelId,
    checkIn,
    checkOut,
    guests,
    setGuests,
    handleCheckInChange,
    setCheckOut,
    handleSearch,
    loading,
    datesValid,
    today,
    minCheckOut,
    selectedHotel,
    hotelOpen,
    setHotelOpen,
    guestsOpen,
    setGuestsOpen,
  }

  return (
    <div className={cn('w-full space-y-3', className)}>
      {/* Mobile: compact summary + Modify */}
      <div className="lg:hidden">
        <div className="rounded-lg border border-border bg-card p-4 shadow-sm">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0 space-y-1.5">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                Your search
              </p>
              <p className="truncate text-sm font-semibold">{summary.hotel}</p>
              <p className="text-sm text-foreground">{summary.dates}</p>
              <p className="text-sm text-muted-foreground">{summary.guests}</p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0 gap-1.5 transition-all duration-200 hover:border-primary/40 hover:bg-primary/5 hover:shadow-sm active:scale-[0.98]"
              onClick={() => setMobileOpen(true)}
            >
              <Pencil className="h-3.5 w-3.5" />
              Modify
            </Button>
          </div>
        </div>
      </div>

      {/* Mobile: slide-down filter panel */}
      <div
        className={cn(
          'pointer-events-none fixed inset-0 z-50 lg:hidden',
          mobileOpen && 'pointer-events-auto'
        )}
        aria-hidden={!mobileOpen}
      >
        <button
          type="button"
          className={cn(
            'absolute inset-0 bg-black/40 transition-opacity duration-300',
            mobileOpen ? 'opacity-100' : 'opacity-0'
          )}
          aria-label="Close filters"
          onClick={() => setMobileOpen(false)}
        />
        <div
          className={cn(
            'absolute inset-x-0 top-0 max-h-[92vh] overflow-y-auto border-b border-border bg-card shadow-xl transition-transform duration-300 ease-out',
            mobileOpen ? 'translate-y-0' : '-translate-y-full'
          )}
        >
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <p className="text-sm font-semibold">Modify search</p>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8 transition-colors duration-200 hover:bg-muted"
              aria-label="Close"
              onClick={() => setMobileOpen(false)}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
          <FilterBar {...filterBarProps} />
        </div>
      </div>

      {/* Desktop: full filter bar */}
      <div className="sticky top-0 z-20 hidden w-full overflow-hidden rounded-lg border border-border bg-card shadow-md lg:block">
        <FilterBar {...filterBarProps} />
      </div>

      {resultCount != null && !loading && (
        <p className="px-1 text-sm text-muted-foreground">
          {resultCount === 0
            ? 'No rooms match your search. Try different dates or guest counts.'
            : `${resultCount} room${resultCount === 1 ? '' : 's'} available`}
        </p>
      )}
    </div>
  )
}
