import { MoreHorizontal } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'

export function StaffTablePanel({ className, children, ...props }) {
  return (
    <div
      className={cn('overflow-hidden rounded-sm border border-border bg-card', className)}
      {...props}
    >
      {children}
    </div>
  )
}

export function StaffTableWrap({ children, className }) {
  return <div className={cn('overflow-x-auto', className)}>{children}</div>
}

export function StaffTable({ className, ...props }) {
  return <Table className={cn('text-sm', className)} {...props} />
}

export function StaffTableHeader({ className, ...props }) {
  return (
    <TableHeader
      className={cn('bg-muted/70 [&_tr]:border-b [&_tr]:border-border', className)}
      {...props}
    />
  )
}

export function StaffTableBody(props) {
  return <TableBody {...props} />
}

export function StaffTableRow({ className, ...props }) {
  return <TableRow className={cn('hover:bg-accent/60', className)} {...props} />
}

export function StaffTableHead({ className, ...props }) {
  return (
    <TableHead
      className={cn(
        'h-10 px-4 py-3 text-left text-xs font-semibold tracking-wide text-foreground/80',
        className
      )}
      {...props}
    />
  )
}

export function StaffTableCell({ className, ...props }) {
  return (
    <TableCell
      className={cn('px-4 py-3.5 align-middle text-sm whitespace-normal', className)}
      {...props}
    />
  )
}

/** Narrow trailing column for row action menus. */
export function StaffTableActionsHead({ className, ...props }) {
  return <StaffTableHead className={cn('w-12 px-2', className)} {...props} />
}

export function StaffTableActionsCell({ className, ...props }) {
  return <StaffTableCell className={cn('w-12 px-2 text-right', className)} {...props} />
}

export function StaffTableEmpty({ className, children, ...props }) {
  return (
    <p
      className={cn('px-4 py-8 text-center text-sm text-muted-foreground', className)}
      {...props}
    >
      {children}
    </p>
  )
}

export function StaffTableRowActions({ label = 'Row actions', align = 'end', children }) {
  const items = Array.isArray(children) ? children : [children]
  const visible = items.filter(Boolean)
  if (visible.length === 0) return null

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 shrink-0"
          aria-label={label}
        >
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align={align}>{visible}</DropdownMenuContent>
    </DropdownMenu>
  )
}

export function StaffTableAction({
  icon: Icon,
  variant,
  className,
  asChild,
  children,
  ...props
}) {
  return (
    <DropdownMenuItem variant={variant} className={className} asChild={asChild} {...props}>
      {asChild ? (
        children
      ) : (
        <>
          {Icon ? <Icon /> : null}
          {children}
        </>
      )}
    </DropdownMenuItem>
  )
}

export function StaffTableActionSeparator(props) {
  return <DropdownMenuSeparator {...props} />
}

export function StaffReferenceLink({ to, children, className }) {
  return (
    <Link
      to={to}
      className={cn(
        'text-sm font-medium text-foreground underline-offset-4 transition-colors hover:text-primary hover:underline',
        className
      )}
    >
      {children}
    </Link>
  )
}

export function StaffGuestIdentity({ guestId, name, email, phone, className }) {
  return (
    <div className={cn('space-y-0.5', className)}>
      {guestId ? (
        <Link
          to={`/staff/guests/${guestId}`}
          className="font-medium text-foreground transition-colors hover:text-primary"
        >
          {name}
        </Link>
      ) : (
        <div className="font-medium text-foreground">{name}</div>
      )}
      {email ? <div className="text-xs text-muted-foreground">{email}</div> : null}
      {phone ? <div className="text-xs text-muted-foreground">{phone}</div> : null}
    </div>
  )
}
