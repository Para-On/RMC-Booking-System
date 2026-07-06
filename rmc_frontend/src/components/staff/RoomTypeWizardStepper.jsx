import { Fragment } from 'react'
import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'

function StepConnector({ active }) {
  return (
    <div
      className={cn(
        'h-0.5 w-full rounded-full bg-muted/80 transition-colors duration-500 ease-out',
        active && 'bg-primary'
      )}
    />
  )
}

function StepCircle({ done, active, upcoming, index }) {
  return (
    <span
      className={cn(
        'relative z-10 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 bg-background text-[10px] font-semibold shadow-sm transition-all duration-300 ease-out',
        done && 'border-primary bg-primary text-primary-foreground shadow-md shadow-primary/30',
        active &&
          'border-primary bg-primary/10 text-primary shadow-md shadow-primary/20 ring-2 ring-primary/25 ring-offset-2 ring-offset-background',
        upcoming && 'border-muted-foreground/20 text-muted-foreground shadow-[0_2px_8px_rgba(0,0,0,0.06)]'
      )}
    >
      {done ? (
        <Check className="h-3 w-3 transition-transform duration-300 ease-out" />
      ) : (
        <span>{index + 1}</span>
      )}
    </span>
  )
}

export default function RoomTypeWizardStepper({ steps, currentStep }) {
  return (
    <nav aria-label="Progress" className="mb-3 shrink-0 overflow-visible px-2 pt-0.5">
      <ol className="flex w-full items-start overflow-visible">
        {steps.map((step, index) => {
          const done = index < currentStep
          const active = index === currentStep
          const upcoming = index > currentStep

          return (
            <Fragment key={step.id}>
              {index > 0 ? (
                <li
                  className="flex min-w-5 flex-1 items-center self-start px-1.5 pt-[11px] sm:px-2"
                  aria-hidden
                >
                  <StepConnector active={index <= currentStep} />
                </li>
              ) : (
                <li className="min-w-2 flex-1 self-start pt-[11px] sm:min-w-3" aria-hidden />
              )}
              <li className="relative z-10 flex shrink-0 flex-col items-center gap-1.5">
                <StepCircle done={done} active={active} upcoming={upcoming} index={index} />
                <span
                  className={cn(
                    'max-w-[3.25rem] truncate text-center text-[10px] font-medium leading-tight transition-colors duration-300 ease-out sm:max-w-[3.5rem] sm:text-[11px]',
                    done && 'text-primary/80',
                    active && 'font-semibold text-primary',
                    upcoming && 'text-muted-foreground'
                  )}
                >
                  {step.label}
                </span>
              </li>
              {index === steps.length - 1 && (
                <li className="min-w-2 flex-1 self-start pt-[11px] sm:min-w-3" aria-hidden />
              )}
            </Fragment>
          )
        })}
      </ol>
    </nav>
  )
}
