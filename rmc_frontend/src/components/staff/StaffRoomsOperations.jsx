import { useEffect, useMemo, useState } from 'react'
import { format, startOfMonth, addMonths } from 'date-fns'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import RoomOpsBookingDialog from '@/components/staff/RoomOpsBookingDialog'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  StaffTable,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTableRow,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import { getRoomCalendar, getRoomDailyStatus, listRoomNumbers, listRoomTypes } from '@/staffApi'

const TABLE_CLASS =
  '[&_th]:h-10 [&_th]:px-4 [&_th]:py-3 [&_th]:text-left [&_th]:text-xs [&_th]:font-semibold [&_th]:text-foreground/80 [&_td]:px-4 [&_td]:py-3.5 [&_td]:text-left [&_td]:align-middle [&_td]:text-sm'

const TABLE_PANEL_CLASS = 'overflow-hidden rounded-sm border border-border bg-card'

const TABLE_HEADER_CLASS = 'bg-muted/70 [&_tr]:border-b [&_tr]:border-border'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function dayStatusVariant(status) {
  switch (status) {
    case 'AVAILABLE':
      return 'outline'
    case 'RESERVED':
      return 'secondary'
    case 'OCCUPIED':
      return 'default'
    case 'OUT_OF_ORDER':
      return 'destructive'
    default:
      return 'outline'
  }
}

function dayStatusLabel(status) {
  switch (status) {
    case 'AVAILABLE':
      return 'Available'
    case 'RESERVED':
      return 'Reserved'
    case 'OCCUPIED':
      return 'Occupied'
    case 'OUT_OF_ORDER':
      return 'Out of order'
    default:
      return status
  }
}

function checkoutNote(alert, checkOut) {
  if (alert === 'TODAY') return `Checkout today (${checkOut})`
  if (alert === 'TOMORROW') return `Checkout tomorrow (${checkOut})`
  return '—'
}

export default function StaffRoomsOperations() {
  const [activeTab, setActiveTab] = useState('daily')
  const [date, setDate] = useState(todayIso())
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [statusFilter, setStatusFilter] = useState('all')
  const [roomTypeFilter, setRoomTypeFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [roomTypes, setRoomTypes] = useState([])
  const [allRooms, setAllRooms] = useState([])
  const [dailyData, setDailyData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedRoomId, setSelectedRoomId] = useState('')
  const [calendarMonth, setCalendarMonth] = useState(() => startOfMonth(new Date()))
  const [calendarData, setCalendarData] = useState(null)
  const [calendarLoading, setCalendarLoading] = useState(false)
  const [calendarError, setCalendarError] = useState('')

  const [detailRow, setDetailRow] = useState(null)
  const [detailOpen, setDetailOpen] = useState(false)

  useEffect(() => {
    Promise.all([listRoomTypes(), listRoomNumbers(false)])
      .then(([types, numbers]) => {
        setRoomTypes(types)
        setAllRooms(numbers)
      })
      .catch(() => {
        setRoomTypes([])
        setAllRooms([])
      })
  }, [])

  useEffect(() => {
    loadDailyStatus()
  }, [date, page, size, statusFilter, roomTypeFilter, search])

  useEffect(() => {
    if (!selectedRoomId && allRooms.length > 0) {
      setSelectedRoomId(String(allRooms[0].id))
    }
  }, [allRooms, selectedRoomId])

  useEffect(() => {
    if (selectedRoomId) {
      loadCalendar()
    } else {
      setCalendarData(null)
    }
  }, [selectedRoomId, calendarMonth])

  async function loadDailyStatus() {
    setLoading(true)
    setError('')
    try {
      const data = await getRoomDailyStatus({
        date,
        page,
        size,
        status: statusFilter === 'all' ? undefined : statusFilter,
        roomTypeId: roomTypeFilter === 'all' ? undefined : Number(roomTypeFilter),
        search: search || undefined,
      })
      setDailyData(data)
    } catch (err) {
      setError(err.message)
      setDailyData(null)
    } finally {
      setLoading(false)
    }
  }

  async function loadCalendar() {
    setCalendarLoading(true)
    setCalendarError('')
    try {
      const from = format(startOfMonth(calendarMonth), 'yyyy-MM-dd')
      const to = format(addMonths(startOfMonth(calendarMonth), 1), 'yyyy-MM-dd')
      const data = await getRoomCalendar(Number(selectedRoomId), from, to)
      setCalendarData(data)
    } catch (err) {
      setCalendarError(err.message)
      setCalendarData(null)
    } finally {
      setCalendarLoading(false)
    }
  }

  function handleSearchSubmit(e) {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function handleFilterChange(setter, value) {
    setPage(0)
    setter(value)
  }

  const occupiedDates = useMemo(() => {
    if (!calendarData?.occupiedDates) return new Set()
    return new Set(calendarData.occupiedDates)
  }, [calendarData])

  const summaryLine = dailyData
    ? [
        `${dailyData.availableCount} available`,
        `${dailyData.occupiedCount} occupied`,
        `${dailyData.reservedCount} reserved`,
        `${dailyData.outOfOrderCount} out of order`,
      ].join(' · ')
    : ''

  return (
    <>
      <Tabs value={activeTab} onValueChange={setActiveTab} className="gap-4">
        <TabsList variant="line" className="h-8 w-fit max-w-full self-start rounded-none border-b bg-transparent p-0">
          <TabsTrigger value="daily" className="h-8 rounded-none px-3 text-xs sm:text-sm">
            Daily view
          </TabsTrigger>
          <TabsTrigger value="calendar" className="h-8 rounded-none px-3 text-xs sm:text-sm">
            Room calendar
          </TabsTrigger>
        </TabsList>

        <TabsContent value="daily" className="mt-4 space-y-3">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="grid gap-2">
              <Label htmlFor="ops-date">Date</Label>
              <Input
                id="ops-date"
                type="date"
                className="h-8 text-sm"
                value={date}
                onChange={(e) => {
                  setPage(0)
                  setDate(e.target.value)
                }}
              />
            </div>
            <div className="grid gap-2">
              <Label>Status</Label>
              <Select
                value={statusFilter}
                onValueChange={(value) => handleFilterChange(setStatusFilter, value)}
              >
                <SelectTrigger className="h-8 text-sm">
                  <SelectValue placeholder="All statuses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All statuses</SelectItem>
                  <SelectItem value="AVAILABLE">Available</SelectItem>
                  <SelectItem value="RESERVED">Reserved</SelectItem>
                  <SelectItem value="OCCUPIED">Occupied</SelectItem>
                  <SelectItem value="OUT_OF_ORDER">Out of order</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Room type</Label>
              <Select
                value={roomTypeFilter}
                onValueChange={(value) => handleFilterChange(setRoomTypeFilter, value)}
              >
                <SelectTrigger className="h-8 text-sm">
                  <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All types</SelectItem>
                  {roomTypes.map((type) => (
                    <SelectItem key={type.id} value={String(type.id)}>
                      {type.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <form className="grid gap-2" onSubmit={handleSearchSubmit}>
              <Label htmlFor="ops-search">Search</Label>
              <div className="flex gap-2">
                <Input
                  id="ops-search"
                  className="h-8 text-sm"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Room, guest, reference"
                />
                <Button type="submit" variant="secondary" size="sm">
                  Go
                </Button>
              </div>
            </form>
          </div>

          {summaryLine && <p className="text-sm text-muted-foreground">{summaryLine}</p>}

          <StaffAlert>{error}</StaffAlert>

          {dailyData?.unassignedReservations?.length > 0 && (
            <Alert>
              <AlertTitle>Unassigned reservations</AlertTitle>
              <AlertDescription>
                {dailyData.unassignedReservations
                  .map((item) => `${item.count} for ${item.roomTypeName}`)
                  .join(' · ')}
                {' — '}no room number assigned yet.
              </AlertDescription>
            </Alert>
          )}

          {loading && <p className="text-sm text-muted-foreground">Loading rooms…</p>}

          {!loading && (!dailyData?.content || dailyData.content.length === 0) && (
            <p className="text-sm text-muted-foreground">No rooms match these filters.</p>
          )}

          {!loading && dailyData?.content?.length > 0 && (
            <div className={TABLE_PANEL_CLASS}>
              <StaffTableWrap>
                <StaffTable className={TABLE_CLASS}>
                  <StaffTableHeader className={TABLE_HEADER_CLASS}>
                    <StaffTableRow>
                      <StaffTableHead>Room</StaffTableHead>
                      <StaffTableHead className="hidden sm:table-cell">Type</StaffTableHead>
                      <StaffTableHead>Status</StaffTableHead>
                      <StaffTableHead className="min-w-[10rem]">Guest</StaffTableHead>
                      <StaffTableHead className="hidden md:table-cell">Stay</StaffTableHead>
                      <StaffTableHead className="hidden lg:table-cell">Checkout</StaffTableHead>
                      <StaffTableHead className="w-20"> </StaffTableHead>
                    </StaffTableRow>
                  </StaffTableHeader>
                  <StaffTableBody>
                    {dailyData.content.map((row) => (
                      <StaffTableRow key={row.roomUnitId}>
                        <StaffTableCell>
                          <div className="font-medium">{row.roomNumber}</div>
                          {row.floorLabel && (
                            <div className="mt-0.5 text-sm text-muted-foreground">Floor {row.floorLabel}</div>
                          )}
                        </StaffTableCell>
                        <StaffTableCell className="hidden sm:table-cell">
                          {row.roomTypeName || '—'}
                        </StaffTableCell>
                        <StaffTableCell>
                          <Badge variant={dayStatusVariant(row.dayStatus)} className="text-xs">
                            {row.statusLabel || dayStatusLabel(row.dayStatus)}
                          </Badge>
                        </StaffTableCell>
                        <StaffTableCell>
                          {row.guestName ? (
                            <>
                              <div>{row.guestName}</div>
                              <div className="mt-0.5 text-sm text-muted-foreground">{row.bookingReference}</div>
                            </>
                          ) : (
                            <span className="text-muted-foreground">—</span>
                          )}
                        </StaffTableCell>
                        <StaffTableCell className="hidden md:table-cell text-muted-foreground">
                          {row.stay ? `${row.stay.checkIn} → ${row.stay.checkOut}` : '—'}
                        </StaffTableCell>
                        <StaffTableCell className="hidden lg:table-cell">
                          {row.checkoutAlert ? (
                            <span
                              className={
                                row.checkoutAlert === 'TODAY'
                                  ? 'text-sm font-medium text-destructive'
                                  : 'text-sm text-muted-foreground'
                              }
                            >
                              {checkoutNote(row.checkoutAlert, row.stay?.checkOut)}
                            </span>
                          ) : (
                            '—'
                          )}
                        </StaffTableCell>
                        <StaffTableCell>
                          {row.bookingId ? (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setDetailRow(row)
                                setDetailOpen(true)
                              }}
                            >
                              View
                            </Button>
                          ) : (
                            <span className="text-sm text-muted-foreground">—</span>
                          )}
                        </StaffTableCell>
                      </StaffTableRow>
                    ))}
                  </StaffTableBody>
                </StaffTable>
              </StaffTableWrap>
              {dailyData.totalPages > 1 && (
                <div className="border-t px-3 py-2">
                  <Pagination>
                    <PaginationContent>
                      <PaginationItem>
                        <PaginationPrevious
                          href="#"
                          onClick={(e) => {
                            e.preventDefault()
                            if (page > 0) setPage(page - 1)
                          }}
                          className={page === 0 ? 'pointer-events-none opacity-50' : ''}
                        />
                      </PaginationItem>
                      <PaginationItem>
                        <span className="px-3 text-sm text-muted-foreground">
                          Page {page + 1} of {dailyData.totalPages}
                        </span>
                      </PaginationItem>
                      <PaginationItem>
                        <PaginationNext
                          href="#"
                          onClick={(e) => {
                            e.preventDefault()
                            if (page + 1 < dailyData.totalPages) setPage(page + 1)
                          }}
                          className={page + 1 >= dailyData.totalPages ? 'pointer-events-none opacity-50' : ''}
                        />
                      </PaginationItem>
                    </PaginationContent>
                  </Pagination>
                </div>
              )}
            </div>
          )}
        </TabsContent>

        <TabsContent value="calendar" className="mt-4 space-y-3">
          <p className="text-sm text-muted-foreground">
            View occupancy for a single room across the month.
          </p>

          <div className="grid max-w-xs gap-2">
            <Label>Room</Label>
            <Select value={selectedRoomId} onValueChange={setSelectedRoomId}>
              <SelectTrigger className="h-8 text-sm">
                <SelectValue placeholder="Select a room" />
              </SelectTrigger>
              <SelectContent>
                {allRooms.map((row) => (
                  <SelectItem key={row.id} value={String(row.id)}>
                    {row.roomNumber}
                    {row.roomTypeName ? ` — ${row.roomTypeName}` : ''}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <StaffAlert>{calendarError}</StaffAlert>

          {selectedRoomId && (
            <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
              <div className="rounded-sm border border-border p-2">
                {calendarLoading ? (
                  <p className="p-4 text-sm text-muted-foreground">Loading…</p>
                ) : (
                  <Calendar
                    mode="single"
                    month={calendarMonth}
                    onMonthChange={setCalendarMonth}
                    modifiers={{
                      occupied: (day) => occupiedDates.has(format(day, 'yyyy-MM-dd')),
                    }}
                    modifiersClassNames={{
                      occupied: 'bg-muted font-medium',
                    }}
                  />
                )}
              </div>

              {calendarData?.blocks?.length > 0 && (
                <div className={`${TABLE_PANEL_CLASS} min-w-0 flex-1`}>
                  <StaffTableWrap>
                    <StaffTable className={TABLE_CLASS}>
                      <StaffTableHeader className={TABLE_HEADER_CLASS}>
                        <StaffTableRow>
                          <StaffTableHead>Reference</StaffTableHead>
                          <StaffTableHead>Guest</StaffTableHead>
                          <StaffTableHead>Stay</StaffTableHead>
                        </StaffTableRow>
                      </StaffTableHeader>
                      <StaffTableBody>
                        {calendarData.blocks.map((block) => (
                          <StaffTableRow key={block.bookingId}>
                            <StaffTableCell className="font-medium">{block.reference}</StaffTableCell>
                            <StaffTableCell>{block.guestName}</StaffTableCell>
                            <StaffTableCell className="text-muted-foreground">
                              {block.checkInDate} → {block.checkOutDate}
                            </StaffTableCell>
                          </StaffTableRow>
                        ))}
                      </StaffTableBody>
                    </StaffTable>
                  </StaffTableWrap>
                </div>
              )}

              {calendarData && !calendarLoading && calendarData.blocks?.length === 0 && (
                <p className="text-sm text-muted-foreground">No bookings this month.</p>
              )}
            </div>
          )}
        </TabsContent>
      </Tabs>

      <RoomOpsBookingDialog
        row={detailRow}
        open={detailOpen}
        onOpenChange={setDetailOpen}
        onUpdated={loadDailyStatus}
      />
    </>
  )
}
