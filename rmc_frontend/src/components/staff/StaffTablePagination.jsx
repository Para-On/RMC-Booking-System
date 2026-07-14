import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'

export const STAFF_PAGE_SIZE_OPTIONS = [10, 25, 50]

/**
 * Shared staff table pager.
 * `page` is 0-based (API / React state). Display shows 1-based page numbers.
 */
export default function StaffTablePagination({
  page = 0,
  pageSize = 10,
  totalElements = 0,
  totalPages = 0,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = STAFF_PAGE_SIZE_OPTIONS,
  showPageSize = true,
  showPageLabel = true,
  className,
  bordered = true,
}) {
  const safeTotalPages = Math.max(0, totalPages)
  const rangeStart = totalElements === 0 ? 0 : page * pageSize + 1
  const rangeEnd = Math.min((page + 1) * pageSize, totalElements)
  const canPrev = page > 0
  const canNext = safeTotalPages > 0 && page + 1 < safeTotalPages

  return (
    <div
      className={cn(
        'flex flex-wrap items-center justify-between gap-2 px-3 py-2',
        bordered && 'border-t',
        className
      )}
    >
      {showPageSize && onPageSizeChange ? (
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span className="whitespace-nowrap">Rows per page</span>
          <Select
            value={String(pageSize)}
            onValueChange={(value) => onPageSizeChange(Number(value))}
          >
            <SelectTrigger className="h-7 w-14 text-xs" aria-label="Rows per page">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {pageSizeOptions.map((size) => (
                <SelectItem key={size} value={String(size)}>
                  {size}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ) : (
        <div />
      )}

      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <span className="whitespace-nowrap tabular-nums">
          {rangeStart}–{rangeEnd} of {totalElements}
        </span>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          disabled={!canPrev}
          aria-label="Previous page"
          onClick={() => onPageChange?.(page - 1)}
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        {showPageLabel ? (
          <span className="min-w-16 text-center tabular-nums">
            Page {safeTotalPages === 0 ? 0 : page + 1} of {safeTotalPages}
          </span>
        ) : null}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          disabled={!canNext}
          aria-label="Next page"
          onClick={() => onPageChange?.(page + 1)}
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  )
}
