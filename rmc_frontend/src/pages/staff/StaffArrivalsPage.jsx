import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
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
import { ExternalLink } from 'lucide-react'
import { getArrivals } from '@/staffApi'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function statusVariant(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'CONFIRMED_PAY_LATER':
      return 'default'
    case 'CHECKED_IN':
      return 'secondary'
    case 'CANCELLED':
    case 'NO_SHOW':
      return 'destructive'
    default:
      return 'outline'
  }
}

export default function StaffArrivalsPage() {
  const [date, setDate] = useState(todayIso())
  const [arrivals, setArrivals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadArrivals()
    const interval = setInterval(loadArrivals, 30000)
    return () => clearInterval(interval)
  }, [date])

  async function loadArrivals() {
    setLoading(true)
    setError('')
    try {
      const data = await getArrivals(date)
      setArrivals(data.arrivals || [])
    } catch (err) {
      setError(err.message)
      setArrivals([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <StaffPageShell
      title="Today's arrivals"
      description="Expected check-ins and booking status for the selected date."
      filters={
        <StaffFilterBar
          meta={
            !loading
              ? `${arrivals.length} arrival${arrivals.length === 1 ? '' : 's'}`
              : null
          }
        >
          <StaffFilterDate
            name="Date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full sm:w-auto sm:min-w-[11rem]"
          />
        </StaffFilterBar>
      }
    >
      <StaffAlert>{error}</StaffAlert>
      {loading && <p className="text-sm text-muted-foreground">Loading arrivals…</p>}

      {!loading && arrivals.length === 0 && !error && (
        <Card>
          <CardContent className="py-6">
            <p className="text-sm text-muted-foreground">No arrivals for this date.</p>
          </CardContent>
        </Card>
      )}

      {!loading && arrivals.length > 0 && (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Reference</StaffTableHead>
                  <StaffTableHead className="min-w-[140px]">Guest</StaffTableHead>
                  <StaffTableHead className="hidden sm:table-cell">Room type</StaffTableHead>
                  <StaffTableHead>Status</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Payment</StaffTableHead>
                  <StaffTableHead className="hidden lg:table-cell">Room #</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Check-in</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {arrivals.map((item) => (
                  <StaffTableRow key={item.bookingId}>
                    <StaffTableCell className="font-medium">{item.reference}</StaffTableCell>
                    <StaffTableCell>
                      <div>{item.guestName}</div>
                      <div className="text-xs text-muted-foreground">{item.guestEmail}</div>
                      <div className="mt-1 text-xs text-muted-foreground sm:hidden">{item.roomTypeName}</div>
                    </StaffTableCell>
                    <StaffTableCell className="hidden sm:table-cell">{item.roomTypeName}</StaffTableCell>
                    <StaffTableCell>
                      <Badge variant={statusVariant(item.status)}>{item.status}</Badge>
                    </StaffTableCell>
                    <StaffTableCell className="hidden md:table-cell">{item.paymentMethod}</StaffTableCell>
                    <StaffTableCell className="hidden lg:table-cell">{item.roomNumber || '—'}</StaffTableCell>
                    <StaffTableCell className="hidden md:table-cell">
                      {item.checkedInAt ? 'Done' : 'Pending'}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${item.reference}`}>
                        <StaffTableAction icon={ExternalLink} asChild>
                          <Link to={`/staff/bookings/${item.bookingId}`}>Open booking</Link>
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
    </StaffPageShell>
  )
}
