import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import { StaffFilterBar, StaffFilterSearch, StaffFilterSelect } from '@/components/staff/StaffFilters'
import {
  StaffGuestIdentity,
  StaffReferenceLink,
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
import { ExternalLink, UserRound } from 'lucide-react'
import { formatMoney } from '@/lib/formatMoney'
import { formatStayRange } from '@/lib/formatDates'
import { getStaffBookings } from '@/staffApi'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'PENDING_PAYMENT', label: 'Pending payment' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'CONFIRMED_PAY_LATER', label: 'Confirmed (pay later)' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'NO_SHOW', label: 'No show' },
  { value: 'FAILED', label: 'Failed' },
]

function statusVariant(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'CONFIRMED_PAY_LATER':
      return 'default'
    case 'CANCELLED':
    case 'NO_SHOW':
    case 'FAILED':
      return 'destructive'
    default:
      return 'outline'
  }
}

export default function StaffBookingsListPage() {
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    loadBookings()
    const interval = setInterval(loadBookings, 30000)
    return () => clearInterval(interval)
  }, [status, searchQuery, page])

  async function loadBookings() {
    setLoading(true)
    setError('')
    try {
      const result = await getStaffBookings({
        status: status === 'ALL' ? undefined : status,
        q: searchQuery || undefined,
        page,
        size: 25,
      })
      setData(result)
    } catch (err) {
      setError(err.message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  const bookings = data?.bookings || []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <StaffPageShell
      title="All bookings"
      description="Every booking created in the system, newest first."
      filters={
        <StaffFilterBar
          meta={
            !loading && data
              ? `${totalElements.toLocaleString()} booking${totalElements === 1 ? '' : 's'}`
              : null
          }
        >
          <StaffFilterSearch
            name="Search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <StaffFilterSelect
            name="Status"
            value={status}
            emptyValue="ALL"
            options={STATUS_OPTIONS}
            onChange={(value) => {
              setStatus(value)
              setPage(0)
            }}
          />
        </StaffFilterBar>
      }
    >
      <StaffAlert>{error}</StaffAlert>
      {loading && <p className="text-sm text-muted-foreground">Loading bookings…</p>}

      {!loading && bookings.length === 0 && !error && (
        <p className="text-sm text-muted-foreground">No bookings match this filter.</p>
      )}

      {!loading && bookings.length > 0 && (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Reference</StaffTableHead>
                  <StaffTableHead className="min-w-[140px]">Guest</StaffTableHead>
                  <StaffTableHead className="hidden sm:table-cell">Room type</StaffTableHead>
                  <StaffTableHead>Status</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Stay</StaffTableHead>
                  <StaffTableHead className="hidden lg:table-cell">Total</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {bookings.map((item) => (
                  <StaffTableRow key={item.bookingId}>
                    <StaffTableCell>
                      <StaffReferenceLink to={`/staff/bookings/${item.bookingId}`}>
                        {item.reference}
                      </StaffReferenceLink>
                    </StaffTableCell>
                    <StaffTableCell>
                      <StaffGuestIdentity
                        guestId={item.guestId}
                        name={item.guestName}
                        email={item.guestEmail}
                      />
                    </StaffTableCell>
                    <StaffTableCell className="hidden text-foreground sm:table-cell">
                      {item.roomTypeName}
                    </StaffTableCell>
                    <StaffTableCell>
                      <Badge variant={statusVariant(item.status)}>{item.status}</Badge>
                    </StaffTableCell>
                    <StaffTableCell className="hidden whitespace-nowrap text-muted-foreground md:table-cell">
                      {formatStayRange(item.checkInDate, item.checkOutDate)}
                    </StaffTableCell>
                    <StaffTableCell className="hidden text-foreground lg:table-cell">
                      {formatMoney(item.quotedTotal, item.currency)}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${item.reference}`}>
                        <StaffTableAction icon={ExternalLink} asChild>
                          <Link to={`/staff/bookings/${item.bookingId}`}>Open booking</Link>
                        </StaffTableAction>
                        {item.guestId ? (
                          <StaffTableAction icon={UserRound} asChild>
                            <Link to={`/staff/guests/${item.guestId}`}>View guest</Link>
                          </StaffTableAction>
                        ) : null}
                      </StaffTableRowActions>
                    </StaffTableActionsCell>
                  </StaffTableRow>
                ))}
              </StaffTableBody>
            </StaffTable>
          </StaffTableWrap>
        </StaffTablePanel>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between gap-4">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={page <= 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              Previous
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((current) => current + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </StaffPageShell>
  )
}
