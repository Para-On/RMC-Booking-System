import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import {
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
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{label}</CardDescription>
        <CardTitle className="text-2xl">{value}</CardTitle>
      </CardHeader>
      {hint ? <CardContent className="pt-0 text-sm text-muted-foreground">{hint}</CardContent> : null}
    </Card>
  )
}

export default function StaffGuestProfilePage() {
  const { id } = useParams()
  const [guest, setGuest] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
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
      title={guest.fullName}
      description="Guest contact details, spend summary, and booking history."
    >
      <StaffAlert>{error}</StaffAlert>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total bookings" value={guest.totalBookings} />
        <StatCard label="Completed stays" value={guest.completedStays} />
        <StatCard
          label="Total spent"
          value={formatMoney(guest.totalSpent, guest.currency)}
          hint="Confirmed, pay-later, cancelled, and no-show bookings"
        />
        <StatCard
          label="Completed stays spent"
          value={formatMoney(guest.completedStaysSpent, guest.currency)}
          hint="Bookings with a recorded checkout"
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <UserRound className="size-4" />
              Contact details
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex items-start gap-2">
              <Mail className="mt-0.5 size-4 text-muted-foreground" />
              <div>
                <p className="text-muted-foreground">Email</p>
                <a
                  href={`mailto:${guest.email}`}
                  className="font-medium text-foreground transition-colors hover:text-primary"
                >
                  {guest.email}
                </a>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <Phone className="mt-0.5 size-4 text-muted-foreground" />
              <div>
                <p className="text-muted-foreground">Phone</p>
                <a
                  href={`tel:${guest.phone}`}
                  className="font-medium text-foreground transition-colors hover:text-primary"
                >
                  {guest.phone}
                </a>
              </div>
            </div>
            <div className="border-t border-border pt-3 text-xs text-muted-foreground">
              <p>DPA consent: {guest.dpaConsentVersion}</p>
              <p>Consented: {formatStaffDateTime(guest.consentTimestamp)}</p>
              <p>Age confirmed: {formatStaffDateTime(guest.ageConfirmedAt)}</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Booking history</CardTitle>
            <CardDescription>
              {guest.bookings.length} booking{guest.bookings.length === 1 ? '' : 's'} on file
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            {guest.bookings.length === 0 ? (
              <p className="px-6 pb-6 text-sm text-muted-foreground">No bookings yet.</p>
            ) : (
              <StaffTablePanel className="rounded-none border-0 border-t border-border shadow-none">
                <StaffTableWrap>
                  <StaffTable>
                    <StaffTableHeader>
                      <StaffTableRow>
                        <StaffTableHead>Reference</StaffTableHead>
                        <StaffTableHead>Stay</StaffTableHead>
                        <StaffTableHead>Status</StaffTableHead>
                        <StaffTableHead>Total</StaffTableHead>
                        <StaffTableActionsHead />
                      </StaffTableRow>
                    </StaffTableHeader>
                    <StaffTableBody>
                      {guest.bookings.map((booking) => (
                        <StaffTableRow key={booking.bookingId}>
                          <StaffTableCell>
                            <StaffReferenceLink to={`/staff/bookings/${booking.bookingId}`}>
                              {booking.reference}
                            </StaffReferenceLink>
                            <div className="mt-0.5 text-xs text-muted-foreground">
                              {booking.roomTypeName}
                            </div>
                          </StaffTableCell>
                          <StaffTableCell className="whitespace-nowrap text-muted-foreground">
                            {formatStayRange(booking.checkInDate, booking.checkOutDate)}
                          </StaffTableCell>
                          <StaffTableCell>
                            <Badge variant={statusVariant(booking.status)}>{booking.status}</Badge>
                          </StaffTableCell>
                          <StaffTableCell className="font-medium text-foreground">
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
              </StaffTablePanel>
            )}
          </CardContent>
        </Card>
      </div>
    </StaffPageShell>
  )
}
