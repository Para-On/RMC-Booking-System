import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { staffGlobalSearch } from '@/staffApi'

export function StaffGlobalSearch() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const containerRef = useRef(null)

  useEffect(() => {
    if (query.trim().length < 2) {
      setResults([])
      setError('')
      return undefined
    }

    const timer = setTimeout(() => {
      setLoading(true)
      setError('')
      staffGlobalSearch(query.trim())
        .then((items) => {
          setResults(items)
          setOpen(true)
        })
        .catch((err) => {
          setResults([])
          setError(err.message)
          setOpen(true)
        })
        .finally(() => setLoading(false))
    }, 300)

    return () => clearTimeout(timer)
  }, [query])

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function selectResult(bookingId) {
    setOpen(false)
    setQuery('')
    navigate(`/staff/bookings/${bookingId}`)
  }

  return (
    <div ref={containerRef} className="relative ml-auto w-full max-w-md">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => query.trim().length >= 2 && setOpen(true)}
        placeholder="Search guest or booking reference…"
        className="pl-9"
        aria-label="Global booking search"
      />
      {open && (
        <div className="absolute z-50 mt-2 w-full overflow-hidden rounded-lg border bg-popover shadow-lg">
          {loading && <p className="px-3 py-2 text-sm text-muted-foreground">Searching…</p>}
          {!loading && error && <p className="px-3 py-2 text-sm text-destructive">{error}</p>}
          {!loading && !error && results.length === 0 && query.trim().length >= 2 && (
            <p className="px-3 py-2 text-sm text-muted-foreground">No bookings found.</p>
          )}
          {!loading && results.length > 0 && (
            <ul className="max-h-72 overflow-auto py-1">
              {results.map((item) => (
                <li key={item.bookingId}>
                  <button
                    type="button"
                    className="flex w-full flex-col items-start gap-0.5 px-3 py-2 text-left text-sm hover:bg-muted"
                    onClick={() => selectResult(item.bookingId)}
                  >
                    <span className="font-medium">{item.reference}</span>
                    <span className="text-muted-foreground">
                      {item.guestName} · {item.roomTypeName} · {item.status}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
