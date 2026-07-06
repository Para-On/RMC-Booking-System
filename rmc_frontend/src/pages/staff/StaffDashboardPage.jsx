import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDownLeft, ArrowUpRight, BedDouble, CircleDollarSign } from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import {
  StaffTable,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTableRow,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { getStaffDashboard } from '@/staffApi'

const RANGE_PRESETS = [
  { id: '1d', label: 'Today', days: 1 },
  { id: '3d', label: '3 days', days: 3 },
  { id: '7d', label: '7 days', days: 7 },
  { id: '30d', label: '30 days', days: 30 },
]

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function addDays(isoDate, days) {
  const date = new Date(`${isoDate}T12:00:00`)
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

function presetRange(days) {
  const to = todayIso()
  const from = addDays(to, -(days - 1))
  return { from, to }
}

function formatMoney(amount, currency = 'PHP') {
  const value = Number(amount || 0)
  return `${currency === 'PHP' ? '₱' : currency}${value.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

function formatRangeLabel(fromDate, toDate) {
  if (fromDate === toDate) return fromDate
  return `${fromDate} – ${toDate}`
}

function statusVariant(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'CONFIRMED_PAY_LATER':
      return 'default'
    case 'NO_SHOW':
      return 'destructive'
    default:
      return 'outline'
  }
}

function MetricCard({ title, value, hint, icon: Icon, accent }) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
        <div>
          <CardDescription>{title}</CardDescription>
          <CardTitle className="mt-2 text-3xl tabular-nums">{value}</CardTitle>
          {hint && <p className="mt-1 text-sm text-muted-foreground">{hint}</p>}
        </div>
        <div className={`rounded-lg p-2 ${accent}`}>
          <Icon className="h-5 w-5" />
        </div>
      </CardHeader>
    </Card>
  )
}

export default function StaffDashboardPage() {
  const initial = presetRange(1)
  const [fromDate, setFromDate] = useState(initial.from)
  const [toDate, setToDate] = useState(initial.to)
  const [activePreset, setActivePreset] = useState('1d')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getStaffDashboard({ fromDate, toDate })
      setData(result)
    } catch (err) {
      setError(err.message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [fromDate, toDate])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  function applyPreset(preset) {
    const range = presetRange(preset.days)
    setFromDate(range.from)
    setToDate(range.to)
    setActivePreset(preset.id)
  }

  function handleFromChange(value) {
    setFromDate(value)
    setActivePreset('')
    if (value > toDate) setToDate(value)
  }

  function handleToChange(value) {
    setToDate(value)
    setActivePreset('')
    if (value < fromDate) setFromDate(value)
  }

  const sellable = data ? data.availableRooms + data.reservedRooms + data.occupiedRooms : 0
  const isSingleDay = fromDate === toDate
  const rangeLabel = formatRangeLabel(fromDate, toDate)

  const dateRangeBar = (
    <div className="-mt-2 flex flex-wrap items-center gap-x-2 gap-y-1.5 text-xs text-muted-foreground">
      <span className="shrink-0 font-medium text-foreground">Period</span>
      {RANGE_PRESETS.map((preset) => (
        <Button
          key={preset.id}
          type="button"
          size="sm"
          variant={activePreset === preset.id ? 'secondary' : 'ghost'}
          className="h-7 px-2 text-xs"
          onClick={() => applyPreset(preset)}
        >
          {preset.label}
        </Button>
      ))}
      <span className="hidden sm:inline text-muted-foreground/70">·</span>
      <Input
        id="dashboard-from"
        type="date"
        value={fromDate}
        onChange={(e) => handleFromChange(e.target.value)}
        className="h-7 w-[8.75rem] px-2 text-xs"
        aria-label="From date"
      />
      <span>–</span>
      <Input
        id="dashboard-to"
        type="date"
        value={toDate}
        onChange={(e) => handleToChange(e.target.value)}
        className="h-7 w-[8.75rem] px-2 text-xs"
        aria-label="To date"
      />
      {!loading && data && (
        <>
          <span className="hidden md:inline text-muted-foreground/70">·</span>
          <Badge variant="outline" className="h-6 px-2 text-[11px] font-normal">
            {data.occupancyPercent}% occupied
          </Badge>
          <Badge variant="secondary" className="h-6 px-2 text-[11px] font-normal">
            {data.bookings?.length ?? 0} bookings
          </Badge>
        </>
      )}
    </div>
  )

  return (
    <StaffPage
      title="Dashboard"
      description="Occupancy, movement, revenue, and bookings."
      actions={
        <Button variant="outline" size="sm" asChild>
          <Link to="/staff/arrivals">View arrivals</Link>
        </Button>
      }
    >
      {dateRangeBar}
      {error && <StaffAlert variant="destructive">{error}</StaffAlert>}

      {loading && <p className="text-sm text-muted-foreground">Loading dashboard…</p>}

      {!loading && data && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              title={isSingleDay ? 'Occupied tonight' : 'Occupied (as of end date)'}
              value={data.occupiedRooms}
              hint={`${data.reservedRooms} reserved · ${data.availableRooms} open of ${sellable} sellable`}
              icon={BedDouble}
              accent="bg-primary/10 text-primary"
            />
            <MetricCard
              title={isSingleDay ? 'Check-ins today' : 'Check-ins in period'}
              value={data.checkInsCount}
              hint={isSingleDay ? 'Arrivals scheduled for today' : `Arrivals between ${rangeLabel}`}
              icon={ArrowDownLeft}
              accent="bg-emerald-500/10 text-emerald-600"
            />
            <MetricCard
              title={isSingleDay ? 'Check-outs today' : 'Check-outs in period'}
              value={data.checkOutsCount}
              hint={
                isSingleDay
                  ? 'In-house guests departing today'
                  : `Departures between ${rangeLabel}`
              }
              icon={ArrowUpRight}
              accent="bg-amber-500/10 text-amber-600"
            />
            <MetricCard
              title={isSingleDay ? 'Revenue today' : 'Revenue in period'}
              value={formatMoney(data.revenueTotal, data.currency)}
              hint={
                isSingleDay ? 'Payments recorded today' : `Payments recorded ${rangeLabel}`
              }
              icon={CircleDollarSign}
              accent="bg-sky-500/10 text-sky-600"
            />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Room status</CardTitle>
                <CardDescription>
                  Live counts as of {data.toDate}, derived from bookings and maintenance flags.
                </CardDescription>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-3 text-sm">
                <div className="rounded-lg border p-3">
                  <p className="text-muted-foreground">Available</p>
                  <p className="text-2xl font-semibold tabular-nums">{data.availableRooms}</p>
                </div>
                <div className="rounded-lg border p-3">
                  <p className="text-muted-foreground">Reserved</p>
                  <p className="text-2xl font-semibold tabular-nums">{data.reservedRooms}</p>
                </div>
                <div className="rounded-lg border p-3">
                  <p className="text-muted-foreground">Occupied</p>
                  <p className="text-2xl font-semibold tabular-nums">{data.occupiedRooms}</p>
                </div>
                <div className="rounded-lg border p-3">
                  <p className="text-muted-foreground">Out of order</p>
                  <p className="text-2xl font-semibold tabular-nums">{data.outOfOrderRooms}</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Quick links</CardTitle>
                <CardDescription>Jump into operational work.</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                <Button variant="secondary" asChild>
                  <Link to="/staff/arrivals">Arrivals</Link>
                </Button>
                <Button variant="secondary" asChild>
                  <Link to="/staff/rooms/operations">Room operations</Link>
                </Button>
                <Button variant="secondary" asChild>
                  <Link to="/staff/rooms/catalog">Room catalog</Link>
                </Button>
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Bookings</CardTitle>
              <CardDescription>
                Stays overlapping {rangeLabel} (confirmed, pay-later, and no-show).
              </CardDescription>
            </CardHeader>
            <CardContent>
              {!data.bookings?.length && (
                <p className="text-sm text-muted-foreground">No bookings in this period.</p>
              )}
              {data.bookings?.length > 0 && (
                <StaffTableWrap>
                  <StaffTable>
                    <StaffTableHeader>
                      <StaffTableRow>
                        <StaffTableHead>Reference</StaffTableHead>
                        <StaffTableHead>Guest</StaffTableHead>
                        <StaffTableHead className="hidden md:table-cell">Room type</StaffTableHead>
                        <StaffTableHead>Stay</StaffTableHead>
                        <StaffTableHead className="hidden sm:table-cell">Room</StaffTableHead>
                        <StaffTableHead>Status</StaffTableHead>
                      </StaffTableRow>
                    </StaffTableHeader>
                    <StaffTableBody>
                      {data.bookings.map((booking) => (
                        <StaffTableRow key={booking.bookingId}>
                          <StaffTableCell>
                            <Link
                              to={`/staff/bookings/${booking.bookingId}`}
                              className="font-medium text-primary hover:underline"
                            >
                              {booking.reference}
                            </Link>
                          </StaffTableCell>
                          <StaffTableCell>{booking.guestName}</StaffTableCell>
                          <StaffTableCell className="hidden md:table-cell">
                            {booking.roomTypeName}
                          </StaffTableCell>
                          <StaffTableCell className="whitespace-nowrap text-muted-foreground">
                            {booking.checkInDate} → {booking.checkOutDate}
                          </StaffTableCell>
                          <StaffTableCell className="hidden sm:table-cell">
                            {booking.roomNumber || '—'}
                          </StaffTableCell>
                          <StaffTableCell>
                            <Badge variant={statusVariant(booking.status)}>{booking.status}</Badge>
                          </StaffTableCell>
                        </StaffTableRow>
                      ))}
                    </StaffTableBody>
                  </StaffTable>
                </StaffTableWrap>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </StaffPage>
  )
}
