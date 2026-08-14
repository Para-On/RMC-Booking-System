import { forwardRef, useEffect, useRef, useState } from 'react'
import { addDays, format, parseISO, startOfToday } from 'date-fns'
import {
  AlertCircle,
  Building2,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  Minus,
  Plus,
  Tag,
  Users,
} from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  BOOKING_ACTION_BUTTON_SM_CLASS,
  DEFAULT_GUESTS,
  formatAppliedSummary,
  formatGuestsLabel,
  getDefaultSearchParams,
  HOTELS,
} from '@/lib/bookingFilters'
import { dayAfter } from '@/api'
import {
  clearStoredPromoCodes,
  loadStoredPromoCodes,
  needsOrganizationCode,
  organizationFieldLabel,
  PROMO_TYPE_OPTIONS,
  promoTypeLabel,
  storePromoCodes,
} from '@/lib/promoCodes'
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
  return format(parseISO(iso), 'MMM d, yyyy')
}

function formatFilterDate(iso) {
  if (!iso) return 'Select date'
  return (
    <>
      <span className="sm:hidden">{format(parseISO(iso), 'MMM d')}</span>
      <span className="hidden sm:inline">{formatShortDate(iso)}</span>
    </>
  )
}

const FilterCell = forwardRef(function FilterCell(
  { icon: Icon, label, value, open, className, showDivider = true, ...props },
  ref
) {
  return (
    <button
      ref={ref}
      type="button"
      className={cn(
        'booking-filter-cell group flex min-h-[3.75rem] w-full min-w-0 flex-1 cursor-pointer flex-col justify-center bg-white text-left outline-none transition-all duration-200 hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-slate-300/60 sm:min-h-[4.25rem]',
        showDivider && 'booking-filter-cell--divider',
        className
      )}
      {...props}
    >
      <span className="mb-0.5 flex min-w-0 items-center gap-1 text-[9px] font-semibold uppercase tracking-normal text-muted-foreground sm:mb-1 sm:gap-1.5 sm:text-[11px] sm:tracking-wide">
        <Icon className="hidden h-3.5 w-3.5 shrink-0 sm:inline" aria-hidden />
        <span className="truncate whitespace-nowrap">{label}</span>
      </span>
      <span className="flex min-w-0 items-center justify-between gap-1 sm:gap-3">
        <span className="min-w-0 truncate whitespace-nowrap text-[11px] font-medium leading-tight text-foreground sm:text-sm md:text-[15px]">
          {value}
        </span>
        <ChevronDown
          className={cn(
            'hidden h-4 w-4 shrink-0 text-muted-foreground transition-transform sm:inline',
            open && 'rotate-180'
          )}
          aria-hidden
        />
      </span>
    </button>
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
          value={formatFilterDate(value)}
          open={open}
          showDivider={showDivider}
          className={className}
        />
      </PopoverTrigger>
      <PopoverContent
        className="booking-filters-popover w-auto overflow-hidden rounded-xl border border-border bg-white p-0 shadow-lg"
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
      <div className="flex shrink-0 items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full border-border/80 transition-all duration-200 hover:border-border hover:bg-muted/60 active:scale-95"
          disabled={value <= min}
          onClick={(event) => {
            event.stopPropagation()
            onChange(value - 1)
          }}
          aria-label={`Decrease ${label.toLowerCase()}`}
        >
          <Minus className="h-3.5 w-3.5" />
        </Button>
        <span className="w-6 text-center text-sm font-semibold tabular-nums">{value}</span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full border-border/80 transition-all duration-200 hover:border-border hover:bg-muted/60 active:scale-95"
          disabled={value >= max}
          onClick={(event) => {
            event.stopPropagation()
            onChange(value + 1)
          }}
          aria-label={`Increase ${label.toLowerCase()}`}
        >
          <Plus className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  )
}

function GuestsDropdown({ guests, setGuests, guestsOpen, setGuestsOpen, className }) {
  return (
    <Popover open={guestsOpen} onOpenChange={setGuestsOpen}>
      <PopoverTrigger asChild>
        <FilterCell
          icon={Users}
          label="Guests"
          value={
            <>
              <span className="sm:hidden">
                {guests.adults} ad{guests.children > 0 ? `, ${guests.children} kd` : ''}
              </span>
              <span className="hidden sm:inline">{formatGuestsLabel(guests)}</span>
            </>
          }
          open={guestsOpen}
          showDivider={false}
          className={className}
        />
      </PopoverTrigger>
      <PopoverContent
        className="booking-filters-popover z-[60] w-[min(20rem,calc(100vw-2rem))] rounded-xl border border-border bg-white p-4 shadow-lg sm:w-80"
        align="end"
        sideOffset={6}
        onOpenAutoFocus={(event) => event.preventDefault()}
      >
        <p className="mb-3 text-sm font-medium">Guests</p>
        <GuestCounter
          label="Adults"
          value={guests.adults}
          min={1}
          max={12}
          onChange={(adults) => setGuests((current) => ({ ...current, adults }))}
        />
        <GuestCounter
          label="Kids"
          hint="Ages 0–17"
          value={guests.children}
          min={0}
          max={8}
          onChange={(children) => setGuests((current) => ({ ...current, children }))}
        />
      </PopoverContent>
    </Popover>
  )
}

function PromoCodeFields({
  promoType,
  setPromoType,
  offerCode,
  setOfferCode,
  organizationCode,
  setOrganizationCode,
  promoApplied,
  onApply,
  onClear,
  applying,
  className,
}) {
  const [open, setOpen] = useState(false)
  const needsOrg = needsOrganizationCode(promoType)
  const selectedLabel = !promoType ? (
    <>
      <span className="sm:hidden">None</span>
      <span className="hidden sm:inline">No promo code</span>
    </>
  ) : promoApplied && offerCode ? (
    `${promoTypeLabel(promoType).split(' / ')[0]} · ${offerCode}`
  ) : (
    promoTypeLabel(promoType)
  )

  function selectType(nextType) {
    setPromoType(nextType)
    if (!nextType) {
      setOfferCode('')
      setOrganizationCode('')
      onClear?.()
      setOpen(false)
      return
    }
    if (!needsOrganizationCode(nextType)) {
      setOrganizationCode('')
    }
  }

  function handleApply() {
    setOpen(false)
    onApply?.()
  }

  return (
    <div
      className={cn(
        'booking-filter-cell flex min-h-[3.75rem] min-w-0 flex-1 flex-col justify-center bg-white sm:min-h-[4.25rem]',
        className
      )}
    >
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <FilterCell
            icon={Tag}
            label={
              <>
                <span className="sm:hidden">Promo</span>
                <span className="hidden sm:inline">Promo code</span>
              </>
            }
            value={selectedLabel}
            open={open}
            showDivider={false}
          />
        </PopoverTrigger>
        <PopoverContent
          className="booking-filters-popover w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-xl border border-border bg-white p-1 shadow-lg"
          align="start"
          sideOffset={6}
        >
          <div className="space-y-1 p-1">
            {PROMO_TYPE_OPTIONS.map((opt) => (
              <button
                key={opt.value || 'none'}
                type="button"
                className={cn(
                  'flex w-full rounded-lg px-3 py-2.5 text-left text-sm transition-all duration-200 hover:bg-muted hover:shadow-sm',
                  promoType === opt.value && 'bg-muted font-medium'
                )}
                onClick={() => selectType(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {promoType ? (
            <div className="space-y-2 border-t border-border p-3">
              {needsOrg ? (
                <input
                  type="text"
                  value={organizationCode}
                  onChange={(e) => setOrganizationCode(e.target.value.toUpperCase())}
                  placeholder={organizationFieldLabel(promoType)}
                  className="h-9 w-full rounded-md border border-border bg-background px-2 text-sm uppercase outline-none focus-visible:ring-2 focus-visible:ring-slate-300/60"
                  aria-label={organizationFieldLabel(promoType)}
                />
              ) : null}
              <input
                type="text"
                value={offerCode}
                onChange={(e) => setOfferCode(e.target.value.toUpperCase())}
                placeholder="Offer code"
                className="h-9 w-full rounded-md border border-border bg-background px-2 text-sm uppercase outline-none focus-visible:ring-2 focus-visible:ring-slate-300/60"
                aria-label="Offer code"
              />
              <Button
                type="button"
                size="sm"
                className="w-full"
                disabled={applying}
                onClick={(e) => {
                  e.preventDefault()
                  handleApply()
                }}
              >
                {applying ? 'Applying…' : 'Apply'}
              </Button>
            </div>
          ) : null}
        </PopoverContent>
      </Popover>
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
  promoType,
  setPromoType,
  offerCode,
  setOfferCode,
  organizationCode,
  setOrganizationCode,
  promoApplied,
  onApplyPromo,
  onClearPromo,
  applyingPromo,
}) {
  return (
    <div className="booking-filters-bar flex min-w-0 flex-row items-stretch overflow-hidden bg-white">
      <Popover open={hotelOpen} onOpenChange={setHotelOpen}>
        <PopoverTrigger asChild>
          <FilterCell
            icon={Building2}
            label="Hotel"
            value={selectedHotel.name}
            open={hotelOpen}
            showDivider={false}
            className="min-w-0"
          />
        </PopoverTrigger>
        <PopoverContent
          className="booking-filters-popover w-[min(16rem,calc(100vw-2rem))] overflow-hidden rounded-xl border border-border bg-white p-1 shadow-lg"
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

      <div className="booking-filters-dates flex min-w-0 flex-[1.4] flex-row">
        <DateDropdown
          label={
            <>
              <span className="sm:hidden">In</span>
              <span className="hidden sm:inline">Check-in</span>
            </>
          }
          icon={CalendarDays}
          value={checkIn}
          minDate={today}
          onChange={handleCheckInChange}
          showDivider
          className="min-w-0"
        />
        <DateDropdown
          label={
            <>
              <span className="sm:hidden">Out</span>
              <span className="hidden sm:inline">Check-out</span>
            </>
          }
          icon={CalendarDays}
          value={checkOut}
          minDate={minCheckOut}
          onChange={setCheckOut}
          showDivider
          className="min-w-0"
        />
      </div>

      <GuestsDropdown
        guests={guests}
        setGuests={setGuests}
        guestsOpen={guestsOpen}
        setGuestsOpen={setGuestsOpen}
        className="booking-filter-cell--divider min-w-0"
      />

      <PromoCodeFields
        promoType={promoType}
        setPromoType={setPromoType}
        offerCode={offerCode}
        setOfferCode={setOfferCode}
        organizationCode={organizationCode}
        setOrganizationCode={setOrganizationCode}
        promoApplied={promoApplied}
        onApply={onApplyPromo}
        onClear={onClearPromo}
        applying={applyingPromo}
        className="booking-filter-cell--divider min-w-0"
      />

      <div className="booking-filters-search flex min-w-0 shrink-0 items-center justify-center border-l border-border px-1.5 py-2 sm:px-4 sm:py-3">
        <Button
          type="button"
          size="lg"
          disabled={!datesValid || loading}
          onClick={handleSearch}
          className={cn(
            'w-full',
            BOOKING_ACTION_BUTTON_SM_CLASS,
            'min-w-0 px-2 sm:h-12 sm:min-w-[8rem] sm:px-5 sm:text-base'
          )}
        >
          {loading ? 'Searching…' : 'Search'}
        </Button>
      </div>
    </div>
  )
}

function PromoApplyAlert({ notice, onDismiss }) {
  if (!notice) return null
  const isError = notice.variant === 'error'
  return (
    <Alert
      variant={isError ? 'destructive' : 'default'}
      className={
        isError
          ? undefined
          : 'border-emerald-200 bg-emerald-50 text-emerald-950 *:data-[slot=alert-description]:text-emerald-900/90'
      }
    >
      {isError ? <AlertCircle aria-hidden /> : <CheckCircle2 aria-hidden />}
      <AlertTitle>{notice.title}</AlertTitle>
      <AlertDescription>{notice.message}</AlertDescription>
      {onDismiss ? (
        <button
          type="button"
          className="absolute right-2 top-2 text-xs text-muted-foreground underline-offset-2 hover:underline"
          onClick={onDismiss}
        >
          Dismiss
        </button>
      ) : null}
    </Alert>
  )
}

export default function BookingFilters({
  onSearch,
  onFiltersChange,
  loading = false,
  resultCount = null,
  appliedSearch = null,
  embedded = false,
  promoClearNonce = 0,
  promoSessionError = '',
  className,
}) {
  const today = startOfToday()
  const stored = loadStoredPromoCodes()
  const [hotelId, setHotelId] = useState(defaults.hotelId)
  const [checkIn, setCheckIn] = useState(defaults.checkIn)
  const [checkOut, setCheckOut] = useState(defaults.checkOut)
  const [guests, setGuests] = useState(DEFAULT_GUESTS)
  const [hotelOpen, setHotelOpen] = useState(false)
  const [guestsOpen, setGuestsOpen] = useState(false)
  const [mobileExpanded, setMobileExpanded] = useState(false)
  const [offerCode, setOfferCode] = useState(stored.offerCode)
  const [organizationCode, setOrganizationCode] = useState(stored.organizationCode)
  const [promoType, setPromoType] = useState(stored.promoType || '')
  const [promoApplied, setPromoApplied] = useState(Boolean(stored.applied))
  const [promoNotice, setPromoNotice] = useState(null)
  const successNoticeTimer = useRef(null)

  function clearPromoNotice() {
    if (successNoticeTimer.current) {
      clearTimeout(successNoticeTimer.current)
      successNoticeTimer.current = null
    }
    setPromoNotice(null)
  }

  function showPromoNotice(notice) {
    if (successNoticeTimer.current) {
      clearTimeout(successNoticeTimer.current)
      successNoticeTimer.current = null
    }
    setPromoNotice(notice)
    if (notice?.variant === 'success') {
      successNoticeTimer.current = setTimeout(() => {
        setPromoNotice(null)
        successNoticeTimer.current = null
      }, 6000)
    }
  }

  function showPromoError(message) {
    showPromoNotice({
      variant: 'error',
      title: 'Promo code not applied',
      message,
    })
  }

  function showPromoSuccess(offer) {
    showPromoNotice({
      variant: 'success',
      title: 'Promo code applied',
      message: offer
        ? `“${offer}” is applied. Eligible rooms now show the discounted rate.`
        : 'Eligible rooms now show the discounted rate.',
    })
  }

  const selectedHotel = HOTELS.find((h) => h.id === hotelId) ?? HOTELS[0]
  const minCheckOut = checkIn ? addDays(parseISO(checkIn), 1) : addDays(today, 1)
  const datesValid = checkIn && checkOut && checkIn < checkOut
  const needsOrg = needsOrganizationCode(promoType)

  function promoPayload(appliedOnly = true) {
    if (appliedOnly && !promoApplied) {
      return { promoType: undefined, offerCode: undefined, organizationCode: undefined }
    }
    if (!promoType || !offerCode.trim()) {
      return { promoType: undefined, offerCode: undefined, organizationCode: undefined }
    }
    return {
      promoType,
      offerCode: offerCode.trim(),
      organizationCode: needsOrg ? organizationCode.trim() || undefined : undefined,
    }
  }

  const summary = formatAppliedSummary(appliedSearch) ?? {
    hotel: selectedHotel.name,
    dates: `${formatShortDate(checkIn)} – ${formatShortDate(checkOut)}`,
    guests: formatGuestsLabel(guests),
  }

  useEffect(() => {
    if (!onFiltersChange || !datesValid) return
    // Stay filters only — promo Apply/Clear call onSearch explicitly (avoids flicker on type pick).
    onFiltersChange({
      hotelId,
      checkIn,
      checkOut,
      guests,
      ...promoPayload(true),
    })
  }, [hotelId, checkIn, checkOut, guests, datesValid, onFiltersChange])

  useEffect(() => {
    if (!promoClearNonce) return
    setPromoApplied(false)
    setPromoType('')
    setOfferCode('')
    setOrganizationCode('')
    showPromoError(
      promoSessionError ||
        'Promo code is no longer available. Prices shown are without that code.'
    )
    clearStoredPromoCodes()
  }, [promoClearNonce])

  useEffect(() => {
    return () => {
      if (successNoticeTimer.current) clearTimeout(successNoticeTimer.current)
    }
  }, [])

  function handleCheckInChange(nextIn) {
    setCheckIn(nextIn)
    if (!checkOut || checkOut <= nextIn) {
      setCheckOut(dayAfter(nextIn))
    }
  }

  async function persistAndSearch(nextApplied) {
    const codes = nextApplied
      ? {
          promoType,
          offerCode: offerCode.trim(),
          organizationCode: needsOrg ? organizationCode.trim() : '',
          applied: true,
        }
      : { promoType: '', offerCode: '', organizationCode: '', applied: false }
    storePromoCodes(codes)
    const payload = {
      hotelId,
      checkIn,
      checkOut,
      guests,
      promoType: nextApplied ? promoType : undefined,
      offerCode: nextApplied ? offerCode.trim() || undefined : undefined,
      organizationCode:
        nextApplied && needsOrg ? organizationCode.trim() || undefined : undefined,
    }
    const result = await onSearch?.(payload)
    if (result?.promoCodeError) {
      setPromoApplied(false)
      setPromoType('')
      setOfferCode('')
      setOrganizationCode('')
      showPromoError(result.promoCodeError)
      clearStoredPromoCodes()
    }
    setMobileExpanded(false)
  }

  function handleSearch() {
    if (!datesValid || loading) return
    persistAndSearch(promoApplied && Boolean(promoType && offerCode.trim()))
  }

  async function handleApplyPromo() {
    clearPromoNotice()
    if (!promoType) return
    if (!offerCode.trim()) {
      showPromoError('Enter an offer code')
      return
    }
    if (needsOrg && !organizationCode.trim()) {
      showPromoError(`Enter a ${organizationFieldLabel(promoType).toLowerCase()}`)
      return
    }
    if (!datesValid) {
      showPromoError('Select valid dates before applying a promo')
      return
    }

    const payload = {
      hotelId,
      checkIn,
      checkOut,
      guests,
      promoType,
      offerCode: offerCode.trim(),
      organizationCode: needsOrg ? organizationCode.trim() || undefined : undefined,
    }
    const result = await onSearch?.(payload)
    if (result?.promoCodeError) {
      setPromoApplied(false)
      setPromoType('')
      setOfferCode('')
      setOrganizationCode('')
      showPromoError(result.promoCodeError)
      clearStoredPromoCodes()
      return
    }
    setPromoApplied(true)
    storePromoCodes({
      promoType,
      offerCode: payload.offerCode,
      organizationCode: needsOrg ? organizationCode.trim() : '',
      applied: true,
    })
    showPromoSuccess(payload.offerCode)
    setMobileExpanded(false)
  }

  function handleClearPromo() {
    setPromoApplied(false)
    clearPromoNotice()
    storePromoCodes({ promoType: '', offerCode: '', organizationCode: '', applied: false })
    if (datesValid) {
      onSearch({
        hotelId,
        checkIn,
        checkOut,
        guests,
        promoType: undefined,
        offerCode: undefined,
        organizationCode: undefined,
      })
    }
  }

  function toggleMobileExpanded() {
    setMobileExpanded((open) => !open)
  }

  function setPromoTypeAndReset(next) {
    const wasApplied = promoApplied
    setPromoType(next)
    setPromoApplied(false)
    clearPromoNotice()
    // Only re-fetch when dropping an already-applied code (not when first picking a type).
    if (wasApplied && next && datesValid) {
      storePromoCodes({
        promoType: next,
        offerCode: '',
        organizationCode: '',
        applied: false,
      })
      onSearch({
        hotelId,
        checkIn,
        checkOut,
        guests,
        promoType: undefined,
        offerCode: undefined,
        organizationCode: undefined,
      })
    }
  }

  function setOfferCodeAndReset(next) {
    setOfferCode(next)
    if (promoApplied) setPromoApplied(false)
  }

  function setOrganizationCodeAndReset(next) {
    setOrganizationCode(next)
    if (promoApplied) setPromoApplied(false)
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
    promoType,
    setPromoType: setPromoTypeAndReset,
    offerCode,
    setOfferCode: setOfferCodeAndReset,
    organizationCode,
    setOrganizationCode: setOrganizationCodeAndReset,
    promoApplied,
    onApplyPromo: handleApplyPromo,
    onClearPromo: handleClearPromo,
    applyingPromo: loading,
  }

  const promoAlert = (
    <PromoApplyAlert notice={promoNotice} onDismiss={clearPromoNotice} />
  )

  if (embedded) {
    return (
      <div className={cn('w-full space-y-3', className)}>
        <div className="booking-filters-bar w-full overflow-hidden rounded-xl border border-border bg-white shadow-lg">
          <FilterBar {...filterBarProps} />
        </div>
        {promoAlert}
      </div>
    )
  }

  return (
    <div className={cn('w-full space-y-3', className)}>
      {/* Mobile: collapsible filter card */}
      <div className="lg:hidden">
        <div className="booking-filters-bar overflow-hidden rounded-xl border border-border bg-white shadow-lg">
          <button
            type="button"
            className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-muted/40 sm:px-5"
            onClick={toggleMobileExpanded}
            aria-expanded={mobileExpanded}
            aria-controls="booking-filters-mobile-panel"
          >
            <div className="flex min-w-0 flex-1 items-start gap-3">
              <span className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <CalendarDays className="size-4" aria-hidden />
              </span>
              <div className="min-w-0 space-y-0.5">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {mobileExpanded ? 'Modify your stay' : 'Book your stay'}
                </p>
                <p className="truncate text-sm font-semibold text-foreground">{summary.hotel}</p>
                <p className="truncate text-sm text-foreground">{summary.dates}</p>
                <p className="truncate text-xs text-muted-foreground">{summary.guests}</p>
              </div>
            </div>
            <ChevronDown
              className={cn(
                'size-5 shrink-0 text-muted-foreground transition-transform duration-200',
                mobileExpanded && 'rotate-180'
              )}
              aria-hidden
            />
          </button>

          <div
            id="booking-filters-mobile-panel"
            className={cn(
              'grid border-t border-border transition-[grid-template-rows] duration-300 ease-out',
              mobileExpanded ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
            )}
          >
            <div className="overflow-hidden">
              <FilterBar {...filterBarProps} />
            </div>
          </div>
        </div>
      </div>

      {/* Desktop: full filter bar */}
      <div className="booking-filters-bar hidden w-full overflow-hidden rounded-xl border border-border bg-white lg:block shadow-md sticky top-0 z-20">
        <FilterBar {...filterBarProps} />
      </div>

      {promoAlert}

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
