import { forwardRef, useId, useState } from 'react'
import { CalendarDays, ChevronDown, ListFilter, Search } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { formatStayDate } from '@/lib/formatDates'
import { cn } from '@/lib/utils'

const SHELL_CLASS =
  'flex h-9 min-w-0 items-center gap-2 rounded-md border border-input bg-background px-2.5 text-xs transition-colors'
const SHELL_BUTTON_CLASS =
  'cursor-pointer text-left hover:bg-accent/60 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40'
const ICON_CLASS = 'h-3.5 w-3.5 shrink-0 text-muted-foreground'
const CHEVRON_CLASS = 'h-3.5 w-3.5 shrink-0 text-muted-foreground transition-transform'
const PLACEHOLDER_CLASS = 'truncate font-normal text-muted-foreground'
const VALUE_CLASS = 'truncate font-medium text-foreground'
const INPUT_CLASS =
  'w-full min-w-0 bg-transparent text-xs font-medium text-foreground outline-none placeholder:font-normal placeholder:text-muted-foreground'

/** Default width for search filters — compact, not full-row. */
export const STAFF_FILTER_SEARCH_CLASS = 'w-full shrink-0 sm:w-[11rem]'

export function StaffFilterBar({ children, className, meta }) {
  return (
    <div
      className={cn(
        'flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center',
        className,
      )}
    >
      {children}
      {meta ? (
        <div className="text-xs text-muted-foreground sm:ml-auto">{meta}</div>
      ) : null}
    </div>
  )
}

const StaffFilterShell = forwardRef(function StaffFilterShell(
  { icon: Icon, className, trailing, as = 'div', children, ...props },
  ref,
) {
  const Component = as

  return (
    <Component
      ref={ref}
      className={cn(SHELL_CLASS, as === 'button' && SHELL_BUTTON_CLASS, className)}
      {...props}
    >
      <Icon className={ICON_CLASS} aria-hidden />
      <div className="flex min-w-0 flex-1 items-center">{children}</div>
      {trailing}
    </Component>
  )
})

function FilterChevron({ open }) {
  return <ChevronDown className={cn(CHEVRON_CLASS, open && 'rotate-180')} aria-hidden />
}

function FilterDisplay({ name, value, isPlaceholder }) {
  return (
    <span className={cn(isPlaceholder ? PLACEHOLDER_CLASS : VALUE_CLASS)}>
      {isPlaceholder ? name : value}
    </span>
  )
}

export function StaffFilterSearch({
  name = 'Search',
  value,
  onChange,
  className,
  inputClassName,
  icon: Icon = Search,
  ...props
}) {
  const inputId = useId()

  return (
    <StaffFilterShell icon={Icon} className={cn(STAFF_FILTER_SEARCH_CLASS, className)}>
      <input
        id={inputId}
        type="search"
        value={value}
        onChange={onChange}
        placeholder={name}
        className={cn(INPUT_CLASS, inputClassName)}
        aria-label={name}
        {...props}
      />
    </StaffFilterShell>
  )
}

export function StaffFilterSelect({
  name = 'Status',
  value,
  options,
  onChange,
  emptyValue = '',
  icon: Icon = ListFilter,
  className,
  contentClassName,
}) {
  const [open, setOpen] = useState(false)
  const selected = options.find((option) => option.value === value)
  const isPlaceholder = value === emptyValue || !selected

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <StaffFilterShell
          as="button"
          type="button"
          icon={Icon}
          className={cn('w-full sm:w-auto sm:min-w-[8.75rem]', className)}
          aria-label={name}
          trailing={<FilterChevron open={open} />}
        >
          <FilterDisplay
            name={name}
            value={selected?.label}
            isPlaceholder={isPlaceholder}
          />
        </StaffFilterShell>
      </PopoverTrigger>
      <PopoverContent
        className={cn(
          'w-[min(15rem,calc(100vw-2rem))] overflow-hidden rounded-md border p-1 shadow-md',
          contentClassName,
        )}
        align="start"
      >
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            className={cn(
              'flex w-full rounded-sm px-2.5 py-1.5 text-left text-xs transition-colors hover:bg-accent hover:text-accent-foreground',
              option.value === value && 'bg-muted font-medium',
            )}
            onClick={() => {
              onChange(option.value)
              setOpen(false)
            }}
          >
            {option.label}
          </button>
        ))}
      </PopoverContent>
    </Popover>
  )
}

function formatDateLabel(value) {
  if (!value) return ''
  return formatStayDate(value)
}

export function StaffFilterDate({
  name = 'Date',
  value,
  onChange,
  icon: Icon = CalendarDays,
  className,
  min,
  max,
}) {
  const inputId = useId()
  const hasValue = Boolean(value)

  return (
    <StaffFilterShell
      icon={Icon}
      className={cn('relative w-full sm:w-auto sm:min-w-[8.75rem]', className)}
      trailing={<FilterChevron />}
    >
      <div className="relative min-w-0 flex-1">
        <input
          id={inputId}
          type="date"
          value={value}
          min={min}
          max={max}
          onChange={onChange}
          className={cn(
            'absolute inset-0 z-10 w-full cursor-pointer opacity-0',
            '[&::-webkit-calendar-picker-indicator]:absolute [&::-webkit-calendar-picker-indicator]:inset-0 [&::-webkit-calendar-picker-indicator]:w-full [&::-webkit-calendar-picker-indicator]:cursor-pointer',
          )}
          aria-label={name}
        />
        <FilterDisplay
          name={name}
          value={formatDateLabel(value)}
          isPlaceholder={!hasValue}
        />
      </div>
    </StaffFilterShell>
  )
}
