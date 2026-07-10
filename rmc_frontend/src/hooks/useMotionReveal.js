import { useEffect, useRef, useState } from 'react'

export function useMotionReveal({
  trigger = 'scroll',
  active = true,
  threshold = 0.12,
  rootMargin = '0px 0px -6% 0px',
  once = true,
} = {}) {
  const ref = useRef(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (!active) {
      setVisible(false)
      return undefined
    }

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setVisible(true)
      return undefined
    }

    if (trigger === 'mount') {
      setVisible(false)
      let frame2 = 0
      const frame1 = window.requestAnimationFrame(() => {
        frame2 = window.requestAnimationFrame(() => setVisible(true))
      })
      return () => {
        window.cancelAnimationFrame(frame1)
        if (frame2) window.cancelAnimationFrame(frame2)
      }
    }

    const element = ref.current
    if (!element) return undefined

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          if (once) observer.disconnect()
        } else if (!once) {
          setVisible(false)
        }
      },
      { threshold, rootMargin }
    )

    observer.observe(element)

    // Above-the-fold content should animate on page load, not only after scrolling.
    let frame2 = 0
    const frame1 = window.requestAnimationFrame(() => {
      frame2 = window.requestAnimationFrame(() => {
        const records = observer.takeRecords()
        if (records.some((entry) => entry.isIntersecting)) {
          setVisible(true)
          if (once) observer.disconnect()
          return
        }

        const rect = element.getBoundingClientRect()
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight
        if (rect.top < viewportHeight && rect.bottom > 0) {
          setVisible(true)
          if (once) observer.disconnect()
        }
      })
    })

    return () => {
      window.cancelAnimationFrame(frame1)
      if (frame2) window.cancelAnimationFrame(frame2)
      observer.disconnect()
    }
  }, [trigger, active, threshold, rootMargin, once])

  return { ref, visible }
}
