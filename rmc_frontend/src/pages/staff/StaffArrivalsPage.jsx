import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
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
      title="Arrivals"
      description="Today's expected check-ins and booking status."
    >
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Filter</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid max-w-xs gap-2">
            <Label htmlFor="arrival-date">Arrival date</Label>
            <Input
              id="arrival-date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

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
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Reference</TableHead>
                    <TableHead className="min-w-[140px]">Guest</TableHead>
                    <TableHead className="hidden sm:table-cell">Room type</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="hidden md:table-cell">Payment</TableHead>
                    <TableHead className="hidden lg:table-cell">Room #</TableHead>
                    <TableHead className="hidden md:table-cell">Check-in</TableHead>
                    <TableHead className="text-right"> </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {arrivals.map((item) => (
                    <TableRow key={item.bookingId}>
                      <TableCell className="font-medium">{item.reference}</TableCell>
                      <TableCell>
                        <div>{item.guestName}</div>
                        <div className="text-xs text-muted-foreground">{item.guestEmail}</div>
                        <div className="mt-1 text-xs text-muted-foreground sm:hidden">{item.roomTypeName}</div>
                      </TableCell>
                      <TableCell className="hidden sm:table-cell">{item.roomTypeName}</TableCell>
                      <TableCell>
                        <Badge variant={statusVariant(item.status)}>{item.status}</Badge>
                      </TableCell>
                      <TableCell className="hidden md:table-cell">{item.paymentMethod}</TableCell>
                      <TableCell className="hidden lg:table-cell">{item.roomNumber || '—'}</TableCell>
                      <TableCell className="hidden md:table-cell">
                        {item.checkedInAt ? 'Done' : 'Pending'}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button variant="outline" size="sm" asChild>
                          <Link to={`/staff/bookings/${item.bookingId}`}>Open</Link>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </StaffPageShell>
  )
}
