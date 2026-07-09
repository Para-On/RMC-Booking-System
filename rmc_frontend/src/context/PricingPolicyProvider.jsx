import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { getPricingPolicy } from '@/api'
import { DEFAULT_PRICING_POLICY } from '@/lib/pricingPolicy'

const PricingPolicyContext = createContext({
  pricingPolicy: DEFAULT_PRICING_POLICY,
  refreshPricingPolicy: async () => DEFAULT_PRICING_POLICY,
})

export function PricingPolicyProvider({ children }) {
  const [pricingPolicy, setPricingPolicy] = useState(DEFAULT_PRICING_POLICY)

  const refreshPricingPolicy = useCallback(async () => {
    try {
      const data = await getPricingPolicy()
      const next = { ...DEFAULT_PRICING_POLICY, ...data }
      setPricingPolicy(next)
      return next
    } catch {
      setPricingPolicy(DEFAULT_PRICING_POLICY)
      return DEFAULT_PRICING_POLICY
    }
  }, [])

  useEffect(() => {
    refreshPricingPolicy()
  }, [refreshPricingPolicy])

  return (
    <PricingPolicyContext.Provider value={{ pricingPolicy, refreshPricingPolicy }}>
      {children}
    </PricingPolicyContext.Provider>
  )
}

export function usePricingPolicy() {
  return useContext(PricingPolicyContext)
}
