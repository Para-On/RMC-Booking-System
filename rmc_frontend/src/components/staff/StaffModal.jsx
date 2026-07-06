import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

const SIZE_CLASS = {
  sm: 'sm:max-w-md',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-2xl',
  xl: 'sm:max-w-4xl',
  '2xl': 'sm:max-w-5xl',
  wizard: 'flex h-[min(720px,92dvh)] w-full max-w-[min(calc(100vw-1rem),48rem)] flex-col sm:max-w-3xl',
}

/**
 * Shared staff modal layout: fixed header, scrollable body, optional footer.
 * Overrides the default small dialog width for responsive forms.
 */
export function StaffModal({
  open,
  onOpenChange,
  trigger,
  title,
  description,
  size = 'md',
  children,
  footer,
  className,
  bodyClassName,
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {trigger}
      <StaffModalContent
        title={title}
        description={description}
        size={size}
        footer={footer}
        className={className}
        bodyClassName={bodyClassName}
      >
        {children}
      </StaffModalContent>
    </Dialog>
  )
}

export function StaffModalContent({
  title,
  description,
  size = 'md',
  children,
  footer,
  className,
  bodyClassName,
}) {
  return (
    <DialogContent
      className={cn(
        'flex w-full max-w-[calc(100%-1.5rem)] flex-col gap-0 overflow-hidden p-0 sm:max-w-none',
        size === 'wizard' ? SIZE_CLASS.wizard : 'max-h-[min(92dvh,920px)]',
        size !== 'wizard' && SIZE_CLASS[size],
        className
      )}
    >
      {(title || description) && (
        <DialogHeader className="shrink-0 space-y-1.5 border-b px-4 py-4 text-left sm:px-6 sm:py-5">
          {title && <DialogTitle className="text-lg sm:text-xl">{title}</DialogTitle>}
          {description && <DialogDescription className="text-sm">{description}</DialogDescription>}
        </DialogHeader>
      )}

      <div
        className={cn(
          'min-h-0 flex-1 px-4 py-4 sm:px-6 sm:py-5',
          size === 'wizard' ? 'overflow-hidden' : 'overflow-y-auto overscroll-contain',
          bodyClassName
        )}
      >
        {children}
      </div>

      {footer && (
        <div className="flex shrink-0 flex-wrap justify-end gap-2 border-t bg-muted/30 px-4 py-4 sm:px-6">
          {footer}
        </div>
      )}
    </DialogContent>
  )
}

export { DialogTrigger as StaffModalTrigger }
