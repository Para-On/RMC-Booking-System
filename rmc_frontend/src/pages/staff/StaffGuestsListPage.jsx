import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import { StaffFilterBar, StaffFilterSearch } from '@/components/staff/StaffFilters'
import {
  StaffGuestIdentity,
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
import { Button } from '@/components/ui/button'
import { ExternalLink } from 'lucide-react'
import { formatMoney } from '@/lib/formatMoney'
import { getStaffGuests } from '@/staffApi'

export default function StaffGuestsListPage() {
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
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
    loadGuests()
  }, [searchQuery, page])

  async function loadGuests() {
    setLoading(true)
    setError('')
    try {
      const result = await getStaffGuests({
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

  const guests = data?.guests || []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <StaffPageShell
      title="Guests"
      description="Guest profiles, contact details, and booking history."
      filters={
        <StaffFilterBar
          meta={
            !loading && data
              ? `${totalElements.toLocaleString()} guest${totalElements === 1 ? '' : 's'}`
              : null
          }
        >
          <StaffFilterSearch
            name="Search guests"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Name, email, or phone…"
          />
        </StaffFilterBar>
      }
    >
      <StaffAlert>{error}</StaffAlert>
      {loading && <p className="text-sm text-muted-foreground">Loading guests…</p>}

      {!loading && guests.length === 0 && !error && (
        <p className="text-sm text-muted-foreground">No guests match this search.</p>
      )}

      {!loading && guests.length > 0 && (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Guest</StaffTableHead>
                  <StaffTableHead className="hidden sm:table-cell">Phone</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Bookings</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Completed stays</StaffTableHead>
                  <StaffTableHead>Total spent</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {guests.map((guest) => (
                  <StaffTableRow key={guest.guestId}>
                    <StaffTableCell>
                      <StaffGuestIdentity
                        guestId={guest.guestId}
                        name={guest.fullName}
                        email={guest.email}
                      />
                    </StaffTableCell>
                    <StaffTableCell className="hidden text-muted-foreground sm:table-cell">
                      {guest.phone || '—'}
                    </StaffTableCell>
                    <StaffTableCell className="hidden text-foreground md:table-cell">
                      {guest.totalBookings}
                    </StaffTableCell>
                    <StaffTableCell className="hidden text-foreground md:table-cell">
                      {guest.completedStays}
                    </StaffTableCell>
                    <StaffTableCell className="font-medium text-foreground">
                      {formatMoney(guest.totalSpent, guest.currency)}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${guest.fullName}`}>
                        <StaffTableAction icon={ExternalLink} asChild>
                          <Link to={`/staff/guests/${guest.guestId}`}>View profile</Link>
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
