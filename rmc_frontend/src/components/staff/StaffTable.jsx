import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'

/** Room / staff list tables with more padding and readable wrapping. */
export function StaffTable({ className, ...props }) {
  return <Table className={cn('text-sm', className)} {...props} />
}

export function StaffTableHeader(props) {
  return <TableHeader {...props} />
}

export function StaffTableBody(props) {
  return <TableBody {...props} />
}

export function StaffTableRow({ className, ...props }) {
  return <TableRow className={cn('hover:bg-muted/40', className)} {...props} />
}

export function StaffTableHead({ className, ...props }) {
  return (
    <TableHead
      className={cn(
        'h-12 px-4 py-3 text-left text-xs font-medium tracking-wide text-muted-foreground',
        className
      )}
      {...props}
    />
  )
}

export function StaffTableCell({ className, ...props }) {
  return (
    <TableCell
      className={cn('px-4 py-4 align-top whitespace-normal', className)}
      {...props}
    />
  )
}

export function StaffTableWrap({ children, className }) {
  return <div className={cn('overflow-x-auto', className)}>{children}</div>
}
