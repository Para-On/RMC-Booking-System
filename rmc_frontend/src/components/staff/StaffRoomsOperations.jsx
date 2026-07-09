import { useEffect, useMemo, useState } from 'react'
import { format, startOfMonth, addMonths } from 'date-fns'
import { BedDouble } from 'lucide-react'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import {
  StaffFilterBar,
  StaffFilterDate,
  StaffFilterSearch,
  StaffFilterSelect,
} from '@/components/staff/StaffFilters'
import RoomOpsBookingDialog from '@/components/staff/RoomOpsBookingDialog'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import {
  StaffPageTabContent,
  StaffPageTabList,
  StaffPageTabs,
  StaffPageTabTrigger,
} from '@/components/staff/StaffPageTabs'
import { formatStayRange } from '@/lib/formatDates'
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
import { getRoomCalendar, getRoomDailyStatus, listRoomNumbers, listRoomTypes } from '@/staffApi'
import { Eye } from 'lucide-react'

const STATUS_OPTIONS = [
  { value: 'all', label: 'All statuses' },
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'RESERVED', label: 'Reserved' },
  { value: 'OCCUPIED', label: 'Occupied' },
  { value: 'OUT_OF_ORDER', label: 'Out of order' },
]

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
    const timer = setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

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

  function handleFilterChange(setter, value) {
    setPage(0)
    setter(value)
  }

  const roomTypeOptions = useMemo(
    () => [
      { value: 'all', label: 'All types' },
      ...roomTypes.map((type) => ({ value: String(type.id), label: type.name })),
    ],
    [roomTypes],
  )

  const roomOptions = useMemo(
    () =>
      allRooms.map((row) => ({
        value: String(row.id),
        label: `${row.roomNumber}${row.roomTypeName ? ` — ${row.roomTypeName}` : ''}`,
      })),
    [allRooms],
  )

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
      <StaffPageTabs value={activeTab} onValueChange={setActiveTab}>
        <StaffPageTabList>
          <StaffPageTabTrigger value="daily">Daily view</StaffPageTabTrigger>
          <StaffPageTabTrigger value="calendar">Room calendar</StaffPageTabTrigger>
        </StaffPageTabList>

        <StaffPageTabContent value="daily" className="space-y-3">
          <StaffFilterBar meta={summaryLine || null}>
            <StaffFilterSearch
              name="Search"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            <StaffFilterDate
              name="Date"
              value={date}
              onChange={(e) => {
                setPage(0)
                setDate(e.target.value)
              }}
            />
            <StaffFilterSelect
              name="Status"
              value={statusFilter}
              emptyValue="all"
              options={STATUS_OPTIONS}
              onChange={(value) => handleFilterChange(setStatusFilter, value)}
            />
            <StaffFilterSelect
              name="Room type"
              value={roomTypeFilter}
              emptyValue="all"
              options={roomTypeOptions}
              icon={BedDouble}
              onChange={(value) => handleFilterChange(setRoomTypeFilter, value)}
            />
          </StaffFilterBar>

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
            <StaffTablePanel>
              <StaffTableWrap>
                <StaffTable>
                  <StaffTableHeader>
                    <StaffTableRow>
                      <StaffTableHead>Room</StaffTableHead>
                      <StaffTableHead className="hidden sm:table-cell">Type</StaffTableHead>
                      <StaffTableHead>Status</StaffTableHead>
                      <StaffTableHead className="min-w-[10rem]">Guest</StaffTableHead>
                      <StaffTableHead className="hidden md:table-cell">Stay</StaffTableHead>
                      <StaffTableHead className="hidden lg:table-cell">Checkout</StaffTableHead>
                      <StaffTableActionsHead />
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
                          {row.stay
                            ? formatStayRange(row.stay.checkIn, row.stay.checkOut)
                            : '—'}
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
                        <StaffTableActionsCell>
                          {row.bookingId ? (
                            <StaffTableRowActions label={`Actions for room ${row.roomNumber}`}>
                              <StaffTableAction
                                icon={Eye}
                                onClick={() => {
                                  setDetailRow(row)
                                  setDetailOpen(true)
                                }}
                              >
                                View booking
                              </StaffTableAction>
                            </StaffTableRowActions>
                          ) : (
                            <span className="text-sm text-muted-foreground">—</span>
                          )}
                        </StaffTableActionsCell>
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
            </StaffTablePanel>
          )}
        </StaffPageTabContent>

        <StaffPageTabContent value="calendar" className="space-y-3">
          <p className="text-sm text-muted-foreground">
            View occupancy for a single room across the month.
          </p>

          <StaffFilterBar>
            <StaffFilterSelect
              name="Room"
              value={selectedRoomId}
              emptyValue=""
              options={roomOptions}
              icon={BedDouble}
              onChange={setSelectedRoomId}
              className="w-full sm:w-auto sm:min-w-[12rem]"
            />
          </StaffFilterBar>

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
                <StaffTablePanel className="min-w-0 flex-1">
                  <StaffTableWrap>
                    <StaffTable>
                      <StaffTableHeader>
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
                              {formatStayRange(block.checkInDate, block.checkOutDate)}
                            </StaffTableCell>
                          </StaffTableRow>
                        ))}
                      </StaffTableBody>
                    </StaffTable>
                  </StaffTableWrap>
                </StaffTablePanel>
              )}

              {calendarData && !calendarLoading && calendarData.blocks?.length === 0 && (
                <p className="text-sm text-muted-foreground">No bookings this month.</p>
              )}
            </div>
          )}
        </StaffPageTabContent>
      </StaffPageTabs>

      <RoomOpsBookingDialog
        row={detailRow}
        open={detailOpen}
        onOpenChange={setDetailOpen}
        onUpdated={loadDailyStatus}
      />
    </>
  )
}
