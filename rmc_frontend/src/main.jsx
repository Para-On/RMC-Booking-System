import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrandingProvider } from '@/context/BrandingProvider'
import { PricingPolicyProvider } from '@/context/PricingPolicyProvider'
import { applyStaffTheme, getStaffAuth } from '@/staffAuth'
import './index.css'
import App from './App.jsx'

const storedAuth = getStaffAuth()
if (storedAuth?.themePreference) {
  applyStaffTheme(storedAuth.themePreference)
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrandingProvider>
      <PricingPolicyProvider>
        <App />
      </PricingPolicyProvider>
    </BrandingProvider>
  </StrictMode>,
)
