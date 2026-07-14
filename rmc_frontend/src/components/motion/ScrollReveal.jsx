import MotionReveal from '@/components/motion/MotionReveal'

export default function ScrollReveal({
  variant,
  direction = 'up',
  trigger = 'scroll',
  once = false,
  ...props
}) {
  return (
    <MotionReveal
      trigger={trigger}
      variant={variant}
      direction={direction}
      once={once}
      {...props}
    />
  )
}
