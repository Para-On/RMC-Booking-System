import { cn } from '@/lib/utils'
import { useMotionReveal } from '@/hooks/useMotionReveal'

const VARIANT_CLASS = {
  fade: 'motion--fade',
  'slide-up': 'motion--slide-up',
  'slide-down': 'motion--slide-down',
  'slide-left': 'motion--slide-left',
  'slide-right': 'motion--slide-right',
  scale: 'motion--scale',
  blur: 'motion--blur',
}

const DIRECTION_TO_VARIANT = {
  up: 'slide-up',
  down: 'slide-down',
  left: 'slide-left',
  right: 'slide-right',
}

export function resolveMotionVariant(variant, direction = 'up') {
  if (variant) return VARIANT_CLASS[variant] ? variant : 'slide-up'
  return DIRECTION_TO_VARIANT[direction] ?? 'slide-up'
}

export default function MotionReveal({
  children,
  className,
  delay = 0,
  duration = 400,
  variant,
  direction = 'up',
  trigger = 'scroll',
  active = true,
  as: Component = 'div',
  ...props
}) {
  const { ref, visible } = useMotionReveal({ trigger, active })
  const resolved = resolveMotionVariant(variant, direction)

  return (
    <Component
      ref={ref}
      className={cn(
        'motion-reveal',
        VARIANT_CLASS[resolved],
        visible && 'motion-reveal--visible',
        className
      )}
      style={{
        transitionDelay: `${delay}ms`,
        '--motion-duration': `${duration}ms`,
      }}
      {...props}
    >
      {children}
    </Component>
  )
}
