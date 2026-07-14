import { useEffect, useRef, useState } from 'react'

const DEFAULT_THRESHOLD = 72

export function useScrollAwareHeader({ enabled = false, threshold = DEFAULT_THRESHOLD } = {}) {
  const [isScrolled, setIsScrolled] = useState(false)
  const [isVisible, setIsVisible] = useState(true)
  const lastYRef = useRef(0)
  const tickingRef = useRef(false)

  useEffect(() => {
    if (!enabled) {
      setIsScrolled(false)
      setIsVisible(true)
      return undefined
    }

    lastYRef.current = window.scrollY

    const update = () => {
      const y = window.scrollY
      const lastY = lastYRef.current

      if (y <= threshold) {
        setIsScrolled(false)
        setIsVisible(true)
      } else {
        setIsScrolled(true)
        if (y < lastY - 4) {
          setIsVisible(true)
        } else if (y > lastY + 4) {
          setIsVisible(false)
        }
      }

      lastYRef.current = y
      tickingRef.current = false
    }

    const onScroll = () => {
      if (tickingRef.current) return
      tickingRef.current = true
      requestAnimationFrame(update)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    update()

    return () => window.removeEventListener('scroll', onScroll)
  }, [enabled, threshold])

  return { isScrolled, isVisible, isFixed: isScrolled }
}
