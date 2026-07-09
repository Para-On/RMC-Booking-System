import { Alert, AlertDescription } from '@/components/ui/alert'
import { cn } from '@/lib/utils'

/**
 * Page wrapper for staff routes — use inside StaffLayout on every staff page.
 */
export function StaffPage({ title, description, actions, filters, children, className }) {
  return (
    <div className={cn('mx-auto w-full max-w-6xl space-y-4', className)}>
      {(title || actions) && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-0.5">
            {title && <h1 className="text-xl font-semibold tracking-tight sm:text-2xl">{title}</h1>}
            {description && (
              <p className="text-sm text-muted-foreground">{description}</p>
            )}
          </div>
          {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
        </div>
      )}
      {filters}
      {children}
    </div>
  )
}

/** @deprecated Use StaffPage */
export const StaffPageShell = StaffPage

export function StaffAlert({ variant = 'error', children }) {
  if (!children) return null

  const isSuccess = variant === 'success'

  return (
    <Alert
      variant={isSuccess ? 'default' : 'destructive'}
      className={isSuccess ? 'border-emerald-200 bg-emerald-50 text-emerald-900' : undefined}
    >
      <AlertDescription className={isSuccess ? 'text-emerald-800' : undefined}>{children}</AlertDescription>
    </Alert>
  )
}
