import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { getBranding } from '@/api'
import { applyBranding, DEFAULT_BRANDING } from '@/lib/applyBranding'

const BrandingContext = createContext({
  branding: DEFAULT_BRANDING,
  loading: true,
  refreshBranding: async () => {},
})

export function BrandingProvider({ children }) {
  const [branding, setBranding] = useState(DEFAULT_BRANDING)
  const [loading, setLoading] = useState(true)

  const refreshBranding = useCallback(async () => {
    try {
      const data = await getBranding()
      setBranding(data)
      applyBranding(data)
    } catch {
      setBranding(DEFAULT_BRANDING)
      applyBranding(DEFAULT_BRANDING)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refreshBranding()
  }, [refreshBranding])

  const value = useMemo(
    () => ({
      branding,
      loading,
      refreshBranding,
    }),
    [branding, loading, refreshBranding]
  )

  return <BrandingContext.Provider value={value}>{children}</BrandingContext.Provider>
}

export function useBranding() {
  return useContext(BrandingContext)
}
