import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Label,
  Pie,
  PieChart,
  XAxis,
} from 'recharts'
import {
  ArrowDownLeft,
  ArrowUpRight,
  BedDouble,
  CircleDollarSign,
  ExternalLink,
} from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { StaffFilterBar, StaffFilterDate } from '@/components/staff/StaffFilters'
import {
  StaffTable,
  StaffTableAction,
  StaffTableActionsCell,
  StaffTableActionsHead,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTablePanel,
  StaffTableRow,
  StaffTableRowActions,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart'
import { formatStayRange } from '@/lib/formatDates'
import { getStaffDashboard } from '@/staffApi'

const RANGE_PRESETS = [
  { id: '1d', label: 'Today', days: 1 },
  { id: '3d', label: '3 days', days: 3 },
  { id: '7d', label: '7 days', days: 7 },
  { id: '30d', label: '30 days', days: 30 },
]

const roomStatusChartConfig = {
  rooms: { label: 'Rooms' },
  available: { label: 'Available', color: 'var(--chart-2)' },
  reserved: { label: 'Reserved', color: 'var(--chart-4)' },
  occupied: { label: 'Occupied', color: 'var(--chart-1)' },
  outOfOrder: { label: 'Out of order', color: 'var(--chart-5)' },
}

const movementChartConfig = {
  guests: { label: 'Guests' },
  checkIns: { label: 'Check-ins', color: 'var(--chart-2)' },
  checkOuts: { label: 'Check-outs', color: 'var(--chart-4)' },
}

const revenueChartConfig = {
  amount: { label: 'Amount' },
  revenue: { label: 'Net revenue', color: 'var(--chart-1)' },
  grossPayments: { label: 'Payments', color: 'var(--chart-2)' },
  refunds: { label: 'Refunds', color: 'var(--chart-5)' },
}

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

function formatDayTick(isoDate) {
  const date = new Date(`${isoDate}T12:00:00`)
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function formatDayTooltip(isoDate) {
  const date = new Date(`${isoDate}T12:00:00`)
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
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
  const [activeMovement, setActiveMovement] = useState('checkIns')

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
    const interval = setInterval(loadDashboard, 30000)
    return () => clearInterval(interval)
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

  const roomStatusData = useMemo(() => {
    if (!data) return []
    return [
      {
        status: 'available',
        rooms: data.availableRooms,
        fill: 'var(--color-available)',
      },
      {
        status: 'reserved',
        rooms: data.reservedRooms,
        fill: 'var(--color-reserved)',
      },
      {
        status: 'occupied',
        rooms: data.occupiedRooms,
        fill: 'var(--color-occupied)',
      },
      {
        status: 'outOfOrder',
        rooms: data.outOfOrderRooms,
        fill: 'var(--color-outOfOrder)',
      },
    ].filter((item) => item.rooms > 0)
  }, [data])

  const totalRooms = useMemo(
    () => roomStatusData.reduce((sum, item) => sum + item.rooms, 0),
    [roomStatusData],
  )

  const seriesData = useMemo(() => {
    if (!data?.series?.length) return []
    return data.series.map((point) => ({
      ...point,
      revenue: Number(point.revenue || 0),
      grossPayments: Number(point.grossPayments || 0),
      refunds: Number(point.refunds || 0),
      checkIns: Number(point.checkIns || 0),
      checkOuts: Number(point.checkOuts || 0),
    }))
  }, [data])

  const movementTotals = useMemo(
    () => ({
      checkIns: seriesData.reduce((sum, point) => sum + point.checkIns, 0),
      checkOuts: seriesData.reduce((sum, point) => sum + point.checkOuts, 0),
    }),
    [seriesData],
  )

  const revenueTotals = useMemo(
    () => ({
      revenue: seriesData.reduce((sum, point) => sum + point.revenue, 0),
      grossPayments: seriesData.reduce((sum, point) => sum + point.grossPayments, 0),
      refunds: seriesData.reduce((sum, point) => sum + point.refunds, 0),
    }),
    [seriesData],
  )

  const hasMovement = movementTotals.checkIns > 0 || movementTotals.checkOuts > 0
  const hasRevenue =
    revenueTotals.revenue !== 0 ||
    revenueTotals.grossPayments > 0 ||
    revenueTotals.refunds > 0

  return (
    <StaffPage
      title="Dashboard"
      description="Occupancy, movement, revenue, and bookings."
      actions={
        <Button variant="outline" size="sm" asChild>
          <Link to="/staff/arrivals">View arrivals</Link>
        </Button>
      }
      filters={
        <StaffFilterBar
          meta={
            !loading && data ? (
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="outline" className="h-6 px-2 text-[11px] font-normal">
                  {data.occupancyPercent}% occupied
                </Badge>
                <Badge variant="secondary" className="h-6 px-2 text-[11px] font-normal">
                  {data.bookings?.length ?? 0} bookings
                </Badge>
              </div>
            ) : null
          }
        >
          <div className="flex flex-wrap items-center gap-1">
            {RANGE_PRESETS.map((preset) => (
              <Button
                key={preset.id}
                type="button"
                size="sm"
                variant={activePreset === preset.id ? 'secondary' : 'ghost'}
                className="h-9 rounded-md px-2.5 text-xs"
                onClick={() => applyPreset(preset)}
              >
                {preset.label}
              </Button>
            ))}
          </div>
          <StaffFilterDate
            name="From"
            value={fromDate}
            onChange={(e) => handleFromChange(e.target.value)}
          />
          <StaffFilterDate
            name="To"
            value={toDate}
            onChange={(e) => handleToChange(e.target.value)}
            min={fromDate}
          />
        </StaffFilterBar>
      }
    >
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
              title={isSingleDay ? 'Net revenue today' : 'Net revenue in period'}
              value={formatMoney(data.revenueTotal, data.currency)}
              hint={
                data.refundsTotal > 0
                  ? `${formatMoney(data.grossPayments, data.currency)} payments − ${formatMoney(data.refundsTotal, data.currency)} refunds`
                  : isSingleDay
                    ? 'Payments minus refunds recorded today'
                    : `Payments minus refunds recorded ${rangeLabel}`
              }
              icon={CircleDollarSign}
              accent="bg-sky-500/10 text-sky-600"
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <Card className="flex flex-col">
              <CardHeader className="items-center pb-0">
                <CardTitle>Room status</CardTitle>
                <CardDescription>Live mix as of {data.toDate}</CardDescription>
              </CardHeader>
              <CardContent className="flex-1 pb-0">
                {roomStatusData.length === 0 ? (
                  <p className="py-10 text-center text-sm text-muted-foreground">
                    No rooms configured.
                  </p>
                ) : (
                  <ChartContainer
                    config={roomStatusChartConfig}
                    className="mx-auto aspect-square max-h-[250px]"
                  >
                    <PieChart>
                      <ChartTooltip
                        cursor={false}
                        content={<ChartTooltipContent hideLabel nameKey="status" />}
                      />
                      <Pie
                        data={roomStatusData}
                        dataKey="rooms"
                        nameKey="status"
                        innerRadius={60}
                        strokeWidth={5}
                      >
                        <Label
                          content={({ viewBox }) => {
                            if (viewBox && 'cx' in viewBox && 'cy' in viewBox) {
                              return (
                                <text
                                  x={viewBox.cx}
                                  y={viewBox.cy}
                                  textAnchor="middle"
                                  dominantBaseline="middle"
                                >
                                  <tspan
                                    x={viewBox.cx}
                                    y={viewBox.cy}
                                    className="fill-foreground text-3xl font-bold"
                                  >
                                    {totalRooms.toLocaleString()}
                                  </tspan>
                                  <tspan
                                    x={viewBox.cx}
                                    y={(viewBox.cy || 0) + 24}
                                    className="fill-muted-foreground"
                                  >
                                    Rooms
                                  </tspan>
                                </text>
                              )
                            }
                            return null
                          }}
                        />
                      </Pie>
                    </PieChart>
                  </ChartContainer>
                )}
              </CardContent>
              <CardFooter className="flex-col gap-2 text-sm">
                <div className="flex items-center gap-2 font-medium leading-none">
                  {data.occupancyPercent}% occupied tonight
                </div>
                <div className="leading-none text-muted-foreground">
                  Available, reserved, occupied, and out of order
                </div>
              </CardFooter>
            </Card>

            <Card className="py-0 lg:col-span-2">
              <CardHeader className="flex flex-col items-stretch border-b !p-0 sm:flex-row">
                <div className="flex flex-1 flex-col justify-center gap-1 px-6 pt-4 pb-3 sm:!py-0">
                  <CardTitle>Guest movement</CardTitle>
                  <CardDescription>
                    Check-ins and check-outs{isSingleDay ? ' today' : ` · ${rangeLabel}`}
                  </CardDescription>
                </div>
                <div className="flex">
                  {['checkIns', 'checkOuts'].map((key) => (
                    <button
                      key={key}
                      type="button"
                      data-active={activeMovement === key}
                      className="relative z-30 flex flex-1 flex-col justify-center gap-1 border-t px-6 py-4 text-left even:border-l data-[active=true]:bg-muted/50 sm:border-t-0 sm:border-l sm:px-8 sm:py-6"
                      onClick={() => setActiveMovement(key)}
                    >
                      <span className="text-xs text-muted-foreground">
                        {movementChartConfig[key].label}
                      </span>
                      <span className="text-lg font-bold leading-none sm:text-3xl">
                        {movementTotals[key].toLocaleString()}
                      </span>
                    </button>
                  ))}
                </div>
              </CardHeader>
              <CardContent className="px-2 sm:p-6">
                {!hasMovement ? (
                  <p className="py-16 text-center text-sm text-muted-foreground">
                    No check-ins or check-outs in this period yet.
                  </p>
                ) : (
                  <ChartContainer
                    config={movementChartConfig}
                    className="aspect-auto h-[250px] w-full"
                  >
                    <BarChart
                      accessibilityLayer
                      data={seriesData}
                      margin={{ left: 12, right: 12 }}
                    >
                      <CartesianGrid vertical={false} />
                      <XAxis
                        dataKey="date"
                        tickLine={false}
                        axisLine={false}
                        tickMargin={8}
                        minTickGap={32}
                        tickFormatter={formatDayTick}
                      />
                      <ChartTooltip
                        content={
                          <ChartTooltipContent
                            className="w-[150px]"
                            nameKey="guests"
                            labelFormatter={formatDayTooltip}
                          />
                        }
                      />
                      <Bar
                        dataKey={activeMovement}
                        fill={`var(--color-${activeMovement})`}
                        radius={8}
                      />
                    </BarChart>
                  </ChartContainer>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Revenue trend</CardTitle>
                <CardDescription>
                  Ledger activity{isSingleDay ? ' for today' : ` by day · ${rangeLabel}`}
                </CardDescription>
              </CardHeader>
              <CardContent className="px-2 pt-4 sm:px-6 sm:pt-6">
                {!hasRevenue ? (
                  <p className="py-16 text-center text-sm text-muted-foreground">
                    No ledger payments or refunds recorded in this period.
                  </p>
                ) : (
                  <ChartContainer
                    config={revenueChartConfig}
                    className="aspect-auto h-[250px] w-full"
                  >
                    <AreaChart
                      accessibilityLayer
                      data={seriesData}
                      margin={{ left: 12, right: 12 }}
                    >
                      <defs>
                        <linearGradient id="fillGrossPayments" x1="0" y1="0" x2="0" y2="1">
                          <stop
                            offset="5%"
                            stopColor="var(--color-grossPayments)"
                            stopOpacity={0.8}
                          />
                          <stop
                            offset="95%"
                            stopColor="var(--color-grossPayments)"
                            stopOpacity={0.1}
                          />
                        </linearGradient>
                        <linearGradient id="fillRevenue" x1="0" y1="0" x2="0" y2="1">
                          <stop
                            offset="5%"
                            stopColor="var(--color-revenue)"
                            stopOpacity={0.8}
                          />
                          <stop
                            offset="95%"
                            stopColor="var(--color-revenue)"
                            stopOpacity={0.1}
                          />
                        </linearGradient>
                        <linearGradient id="fillRefunds" x1="0" y1="0" x2="0" y2="1">
                          <stop
                            offset="5%"
                            stopColor="var(--color-refunds)"
                            stopOpacity={0.8}
                          />
                          <stop
                            offset="95%"
                            stopColor="var(--color-refunds)"
                            stopOpacity={0.1}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid vertical={false} />
                      <XAxis
                        dataKey="date"
                        tickLine={false}
                        axisLine={false}
                        tickMargin={8}
                        minTickGap={32}
                        tickFormatter={formatDayTick}
                      />
                      <ChartTooltip
                        cursor={false}
                        content={
                          <ChartTooltipContent
                            labelFormatter={formatDayTooltip}
                            indicator="dot"
                            formatter={(value, name) => (
                              <div className="flex w-full items-center justify-between gap-4">
                                <span className="text-muted-foreground">
                                  {revenueChartConfig[name]?.label ?? name}
                                </span>
                                <span className="font-mono font-medium tabular-nums text-foreground">
                                  {formatMoney(value, data.currency)}
                                </span>
                              </div>
                            )}
                          />
                        }
                      />
                      <ChartLegend content={<ChartLegendContent />} />
                      <Area
                        dataKey="grossPayments"
                        type="natural"
                        fill="url(#fillGrossPayments)"
                        fillOpacity={0.4}
                        stroke="var(--color-grossPayments)"
                        strokeWidth={2}
                      />
                      <Area
                        dataKey="revenue"
                        type="natural"
                        fill="url(#fillRevenue)"
                        fillOpacity={0.4}
                        stroke="var(--color-revenue)"
                        strokeWidth={2}
                      />
                      <Area
                        dataKey="refunds"
                        type="natural"
                        fill="url(#fillRefunds)"
                        fillOpacity={0.4}
                        stroke="var(--color-refunds)"
                        strokeWidth={2}
                      />
                    </AreaChart>
                  </ChartContainer>
                )}
              </CardContent>
              <CardFooter className="flex-col items-start gap-1 border-t text-sm sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-wrap gap-4">
                  <span>
                    <span className="text-muted-foreground">Net </span>
                    <span className="font-medium tabular-nums">
                      {formatMoney(revenueTotals.revenue, data.currency)}
                    </span>
                  </span>
                  <span>
                    <span className="text-muted-foreground">Paid </span>
                    <span className="font-medium tabular-nums">
                      {formatMoney(revenueTotals.grossPayments, data.currency)}
                    </span>
                  </span>
                  <span>
                    <span className="text-muted-foreground">Refunds </span>
                    <span className="font-medium tabular-nums">
                      {formatMoney(revenueTotals.refunds, data.currency)}
                    </span>
                  </span>
                </div>
              </CardFooter>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Quick links</CardTitle>
                <CardDescription>Jump into operational work.</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                <Button variant="secondary" asChild>
                  <Link to="/staff/bookings">All bookings</Link>
                </Button>
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
                <StaffTablePanel>
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
                          <StaffTableActionsHead />
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
                              {formatStayRange(booking.checkInDate, booking.checkOutDate)}
                            </StaffTableCell>
                            <StaffTableCell className="hidden sm:table-cell">
                              {booking.roomNumber || '—'}
                            </StaffTableCell>
                            <StaffTableCell>
                              <Badge variant={statusVariant(booking.status)}>{booking.status}</Badge>
                            </StaffTableCell>
                            <StaffTableActionsCell>
                              <StaffTableRowActions label={`Actions for ${booking.reference}`}>
                                <StaffTableAction icon={ExternalLink} asChild>
                                  <Link to={`/staff/bookings/${booking.bookingId}`}>
                                    Open booking
                                  </Link>
                                </StaffTableAction>
                              </StaffTableRowActions>
                            </StaffTableActionsCell>
                          </StaffTableRow>
                        ))}
                      </StaffTableBody>
                    </StaffTable>
                  </StaffTableWrap>
                </StaffTablePanel>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </StaffPage>
  )
}
