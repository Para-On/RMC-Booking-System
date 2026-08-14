import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, Info, Loader2 } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

const AppFeedbackContext = createContext(null)

export function useAppFeedback() {
  const value = useContext(AppFeedbackContext)
  if (!value) {
    throw new Error('useAppFeedback must be used within AppFeedbackProvider')
  }
  return value
}

export function AppFeedbackProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const [loading, setLoading] = useState(null)
  const [confirmState, setConfirmState] = useState(null)
  const [promptValue, setPromptValue] = useState('')
  const toastTimers = useRef(new Map())
  const toastId = useRef(0)

  const dismissToast = useCallback((id) => {
    const timer = toastTimers.current.get(id)
    if (timer) {
      clearTimeout(timer)
      toastTimers.current.delete(id)
    }
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const toast = useCallback(
    ({ variant = 'info', title, message, duration } = {}) => {
      const id = ++toastId.current
      setToasts((current) => [...current.slice(-4), { id, variant, title, message }])
      const ms = duration ?? (variant === 'error' ? 8000 : 5000)
      if (ms > 0) {
        toastTimers.current.set(
          id,
          setTimeout(() => dismissToast(id), ms)
        )
      }
      return id
    },
    [dismissToast]
  )

  const confirm = useCallback((options = {}) => {
    return new Promise((resolve) => {
      setPromptValue('')
      setConfirmState({
        title: options.title || 'Please confirm',
        description: options.description || '',
        confirmLabel: options.confirmLabel || 'Confirm',
        cancelLabel: options.cancelLabel || 'Cancel',
        variant: options.variant || 'default',
        promptLabel: options.promptLabel || null,
        promptRequired: Boolean(options.promptRequired),
        promptKind: options.promptKind || 'textarea',
        resolve,
      })
    })
  }, [])

  const closeConfirm = useCallback((result) => {
    setConfirmState((current) => {
      current?.resolve(result)
      return null
    })
    setPromptValue('')
  }, [])

  const withLoading = useCallback(async (message, fn) => {
    setLoading({ message: message || 'Please wait…' })
    try {
      return await fn()
    } finally {
      setLoading(null)
    }
  }, [])

  const value = useMemo(
    () => ({ toast, confirm, withLoading, dismissToast }),
    [toast, confirm, withLoading, dismissToast]
  )

  const destructive = confirmState?.variant === 'destructive'
  const promptMissing = Boolean(confirmState?.promptRequired && !promptValue.trim())

  return (
    <AppFeedbackContext.Provider value={value}>
      {children}

      {loading ? (
        <div
          className="fixed inset-0 z-[80] flex items-center justify-center bg-black/40 px-4 supports-backdrop-filter:backdrop-blur-[2px]"
          role="alertdialog"
          aria-busy="true"
          aria-live="assertive"
        >
          <div className="flex max-w-sm items-center gap-3 rounded-xl border border-border bg-background px-4 py-3 shadow-lg">
            <Loader2 className="size-5 shrink-0 animate-spin text-primary" aria-hidden />
            <p className="text-sm font-medium text-foreground">{loading.message}</p>
          </div>
        </div>
      ) : null}

      <div className="pointer-events-none fixed top-4 right-4 z-[90] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2">
        {toasts.map((item) => (
          <Alert
            key={item.id}
            variant={item.variant === 'error' ? 'destructive' : 'default'}
            className={`pointer-events-auto shadow-lg ${
              item.variant === 'success'
                ? 'border-emerald-200 bg-emerald-50 text-emerald-950 *:data-[slot=alert-description]:text-emerald-900/90'
                : ''
            }`}
          >
            {item.variant === 'error' ? (
              <AlertCircle aria-hidden />
            ) : item.variant === 'success' ? (
              <CheckCircle2 aria-hidden />
            ) : (
              <Info aria-hidden />
            )}
            {item.title ? <AlertTitle>{item.title}</AlertTitle> : null}
            {item.message ? <AlertDescription>{item.message}</AlertDescription> : null}
            <button
              type="button"
              className="absolute right-2 top-2 text-xs text-muted-foreground underline-offset-2 hover:underline"
              onClick={() => dismissToast(item.id)}
            >
              Dismiss
            </button>
          </Alert>
        ))}
      </div>

      <Dialog
        open={Boolean(confirmState)}
        onOpenChange={(open) => {
          if (!open && confirmState) {
            closeConfirm({ confirmed: false, value: '' })
          }
        }}
      >
        <DialogContent showCloseButton={false} className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{confirmState?.title}</DialogTitle>
            {confirmState?.description ? (
              <DialogDescription>{confirmState.description}</DialogDescription>
            ) : null}
          </DialogHeader>
          {confirmState?.promptLabel ? (
            <label className="grid gap-1.5 text-sm">
              <span className="text-muted-foreground">{confirmState.promptLabel}</span>
              {confirmState.promptKind === 'textarea' ? (
                <textarea
                  value={promptValue}
                  onChange={(e) => setPromptValue(e.target.value)}
                  rows={3}
                  className="w-full rounded-md border border-border bg-background px-2 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
                />
              ) : (
                <input
                  type={confirmState.promptKind === 'password' ? 'password' : 'text'}
                  value={promptValue}
                  onChange={(e) => setPromptValue(e.target.value)}
                  className="h-9 w-full rounded-md border border-border bg-background px-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
                />
              )}
            </label>
          ) : null}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => closeConfirm({ confirmed: false, value: '' })}
            >
              {confirmState?.cancelLabel}
            </Button>
            <Button
              type="button"
              variant={destructive ? 'destructive' : 'default'}
              disabled={promptMissing}
              onClick={() =>
                closeConfirm({ confirmed: true, value: promptValue.trim() })
              }
            >
              {confirmState?.confirmLabel}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </AppFeedbackContext.Provider>
  )
}
