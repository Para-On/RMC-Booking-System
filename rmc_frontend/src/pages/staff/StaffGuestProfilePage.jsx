import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import {
  StaffGuestAvatar,
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
import StaffTablePagination, { STAFF_PAGE_SIZE_OPTIONS } from '@/components/staff/StaffTablePagination'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ExternalLink, Mail, Phone, UserRound } from 'lucide-react'
import { formatMoney } from '@/lib/formatMoney'
import { formatStaffDateTime, formatStayRange } from '@/lib/formatDates'
import { getStaffGuest } from '@/staffApi'

function statusVariant(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'CONFIRMED_PAY_LATER':
      return 'default'
    case 'CANCELLED':
    case 'NO_SHOW':
      return 'destructive'
    default:
      return 'outline'
  }
}

function StatCard({ label, value, hint }) {
  return (
    <Card className="min-w-0">
      <CardHeader className="pb-2">
        <CardDescription className="text-xs sm:text-sm">{label}</CardDescription>
        <CardTitle className="truncate text-xl sm:text-2xl">{value}</CardTitle>
      </CardHeader>
      {hint ? (
        <CardContent className="pt-0 text-xs text-muted-foreground sm:text-sm">{hint}</CardContent>
      ) : null}
    </Card>
  )
}

function paginateBookings(list, page, pageSize) {
  const totalElements = list.length
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize) || 1)
  const safePage = Math.min(Math.max(page, 0), Math.max(0, totalPages - 1))
  const start = safePage * pageSize
  return {
    items: list.slice(start, start + pageSize),
    page: safePage,
    totalPages: totalElements === 0 ? 0 : totalPages,
    totalElements,
  }
}

export default function StaffGuestProfilePage() {
  const { id } = useParams()
  const [guest, setGuest] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  useEffect(() => {
    setPage(0)
    loadGuest()
  }, [id])

  async function loadGuest() {
    setLoading(true)
    setError('')
    try {
      const data = await getStaffGuest(id)
      setGuest(data)
    } catch (err) {
      setGuest(null)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const bookings = guest?.bookings || []
  const pagination = useMemo(
    () => paginateBookings(bookings, page, pageSize),
    [bookings, page, pageSize]
  )

  useEffect(() => {
    if (pagination.totalPages > 0 && page >= pagination.totalPages) {
      setPage(Math.max(0, pagination.totalPages - 1))
    }
  }, [page, pagination.totalPages])

  if (loading) {
    return (
      <StaffPageShell title="Guest profile">
        <p className="text-sm text-muted-foreground">Loading guest profile…</p>
      </StaffPageShell>
    )
  }

  if (!guest) {
    return (
      <StaffPageShell title="Guest profile">
        <StaffAlert>{error || 'Guest not found.'}</StaffAlert>
      </StaffPageShell>
    )
  }

  return (
    <StaffPageShell
      title={
        <span className="inline-flex min-w-0 items-center gap-2.5">
          <StaffGuestAvatar name={guest.fullName} className="size-8" />
          <span className="truncate">{guest.fullName}</span>
        </span>
      }
      description="Guest contact details, spend summary, and booking history."
    >
      <StaffAlert>{error}</StaffAlert>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4 xl:grid-cols-4">
        <StatCard label="Total bookings" value={guest.totalBookings} />
        <StatCard label="Completed stays" value={guest.completedStays} />
        <StatCard
          label="Total spent"
          value={formatMoney(guest.totalSpent, guest.currency)}
          hint="Only bookings where this guest was the primary booker"
        />
        <StatCard
          label="Completed stays spent"
          value={formatMoney(guest.completedStaysSpent, guest.currency)}
          hint="Primary-booker stays with a recorded checkout"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)]">
        <Card className="min-w-0">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <UserRound className="size-4 shrink-0" />
              Contact details
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex min-w-0 items-start gap-2">
              <Mail className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <div className="min-w-0">
                <p className="text-muted-foreground">Email</p>
                <a
                  href={`mailto:${guest.email}`}
                  className="break-all font-medium text-foreground transition-colors hover:text-primary"
                >
                  {guest.email}
                </a>
              </div>
            </div>
            <div className="flex min-w-0 items-start gap-2">
              <Phone className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <div className="min-w-0">
                <p className="text-muted-foreground">Phone</p>
                <a
                  href={`tel:${guest.phone}`}
                  className="break-all font-medium text-foreground transition-colors hover:text-primary"
                >
                  {guest.phone}
                </a>
              </div>
            </div>
            <div className="space-y-1 border-t border-border pt-3 text-xs text-muted-foreground">
              <p>DPA consent: {guest.dpaConsentVersion}</p>
              <p>Consented: {formatStaffDateTime(guest.consentTimestamp)}</p>
              <p>Age confirmed: {formatStaffDateTime(guest.ageConfirmedAt)}</p>
            </div>
          </CardContent>
        </Card>

        <Card className="min-w-0 overflow-hidden">
          <CardHeader className="gap-1">
            <CardTitle className="text-base">Booking history</CardTitle>
            <CardDescription>
              {bookings.length} booking{bookings.length === 1 ? '' : 's'} on file
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            {bookings.length === 0 ? (
              <p className="px-4 pb-6 text-sm text-muted-foreground sm:px-6">No bookings yet.</p>
            ) : (
              <StaffTablePanel className="rounded-none border-0 border-t border-border shadow-none">
                <StaffTableWrap>
                  <StaffTable>
                    <StaffTableHeader>
                      <StaffTableRow>
                        <StaffTableHead>Reference</StaffTableHead>
                        <StaffTableHead className="hidden sm:table-cell">Stay</StaffTableHead>
                        <StaffTableHead>Status</StaffTableHead>
                        <StaffTableHead className="hidden md:table-cell">Total</StaffTableHead>
                        <StaffTableActionsHead />
                      </StaffTableRow>
                    </StaffTableHeader>
                    <StaffTableBody>
                      {pagination.items.map((booking) => (
                        <StaffTableRow key={booking.bookingId}>
                          <StaffTableCell className="min-w-0">
                            <StaffReferenceLink to={`/staff/bookings/${booking.bookingId}`}>
                              {booking.reference}
                            </StaffReferenceLink>
                            <div className="mt-0.5 truncate text-xs text-muted-foreground">
                              {booking.roomTypeName}
                            </div>
                            <div className="mt-0.5 text-xs text-muted-foreground sm:hidden">
                              {formatStayRange(booking.checkInDate, booking.checkOutDate)}
                            </div>
                            <div className="mt-0.5 text-xs font-medium text-foreground md:hidden">
                              {formatMoney(booking.quotedTotal, booking.currency)}
                            </div>
                          </StaffTableCell>
                          <StaffTableCell className="hidden whitespace-nowrap text-muted-foreground sm:table-cell">
                            {formatStayRange(booking.checkInDate, booking.checkOutDate)}
                          </StaffTableCell>
                          <StaffTableCell>
                            <Badge
                              variant={statusVariant(booking.status)}
                              className="max-w-[9.5rem] truncate sm:max-w-none"
                            >
                              {booking.status}
                            </Badge>
                          </StaffTableCell>
                          <StaffTableCell className="hidden font-medium text-foreground md:table-cell">
                            {formatMoney(booking.quotedTotal, booking.currency)}
                          </StaffTableCell>
                          <StaffTableActionsCell>
                            <StaffTableRowActions label={`Actions for ${booking.reference}`}>
                              <StaffTableAction icon={ExternalLink} asChild>
                                <Link to={`/staff/bookings/${booking.bookingId}`}>Open booking</Link>
                              </StaffTableAction>
                            </StaffTableRowActions>
                          </StaffTableActionsCell>
                        </StaffTableRow>
                      ))}
                    </StaffTableBody>
                  </StaffTable>
                </StaffTableWrap>
                <StaffTablePagination
                  page={pagination.page}
                  pageSize={pageSize}
                  totalElements={pagination.totalElements}
                  totalPages={pagination.totalPages}
                  onPageChange={setPage}
                  onPageSizeChange={(size) => {
                    setPageSize(size)
                    setPage(0)
                  }}
                  pageSizeOptions={STAFF_PAGE_SIZE_OPTIONS}
                />
              </StaffTablePanel>
            )}
          </CardContent>
        </Card>
      </div>
    </StaffPageShell>
  )
}
