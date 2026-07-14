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
import { ExternalLink } from 'lucide-react'
import StaffTablePagination, { STAFF_PAGE_SIZE_OPTIONS } from '@/components/staff/StaffTablePagination'
import { formatMoney } from '@/lib/formatMoney'
import { getStaffGuests } from '@/staffApi'

export default function StaffGuestsListPage() {
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
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
  }, [searchQuery, page, pageSize])

  async function loadGuests() {
    setLoading(true)
    setError('')
    try {
      const result = await getStaffGuests({
        q: searchQuery || undefined,
        page,
        size: pageSize,
      })
      setData(result)
      if (result?.totalPages > 0 && page >= result.totalPages) {
        setPage(Math.max(0, result.totalPages - 1))
      }
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
          <StaffTablePagination
            page={page}
            pageSize={data?.size || pageSize}
            totalElements={data?.totalElements || 0}
            totalPages={totalPages}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size)
              setPage(0)
            }}
            pageSizeOptions={STAFF_PAGE_SIZE_OPTIONS}
          />
        </StaffTablePanel>
      )}
    </StaffPageShell>
  )
}
