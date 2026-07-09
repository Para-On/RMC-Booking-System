import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { getStaffAuth, patchStaffAuth, applyStaffTheme } from '@/staffAuth'
import { getStaffProfile, updateStaffTheme } from '@/staffApi'

const StaffProfileContext = createContext(null)

function profileFromAuth(auth) {
  if (!auth) return null
  return {
    fullName: auth.fullName || '',
    email: auth.email || '',
    role: auth.role || '',
    profileImageUrl: auth.profileImageUrl || null,
    themePreference: auth.themePreference || 'LIGHT',
    phone: auth.phone || '',
  }
}

export function StaffProfileProvider({ children }) {
  const [profile, setProfile] = useState(() => profileFromAuth(getStaffAuth()))
  const [themeLoading, setThemeLoading] = useState(false)

  const syncProfile = useCallback((next) => {
    setProfile(next)
    patchStaffAuth({
      fullName: next.fullName,
      email: next.email,
      role: next.role,
      profileImageUrl: next.profileImageUrl,
      themePreference: next.themePreference,
      phone: next.phone,
    })
    if (next.themePreference) {
      applyStaffTheme(next.themePreference)
    }
  }, [])

  const refreshProfile = useCallback(async () => {
    const data = await getStaffProfile()
    syncProfile({
      id: data.id,
      fullName: data.fullName,
      email: data.email,
      role: data.role,
      phone: data.phone || '',
      profileImageUrl: data.profileImageUrl,
      themePreference: data.themePreference || 'LIGHT',
      mfaEnabled: data.mfaEnabled,
    })
    return data
  }, [syncProfile])

  useEffect(() => {
    const auth = getStaffAuth()
    if (auth?.themePreference) {
      applyStaffTheme(auth.themePreference)
    }
    refreshProfile().catch(() => {
      if (auth?.themePreference) {
        applyStaffTheme(auth.themePreference)
      }
    })
  }, [refreshProfile])

  const toggleTheme = useCallback(async () => {
    const nextTheme = profile?.themePreference === 'DARK' ? 'LIGHT' : 'DARK'
    setThemeLoading(true)
    try {
      const data = await updateStaffTheme(nextTheme)
      syncProfile({
        ...profile,
        themePreference: data.themePreference || nextTheme,
      })
    } finally {
      setThemeLoading(false)
    }
  }, [profile, syncProfile])

  const value = useMemo(
    () => ({
      profile,
      setProfile: syncProfile,
      refreshProfile,
      toggleTheme,
      themeLoading,
      isDarkMode: profile?.themePreference === 'DARK',
    }),
    [profile, refreshProfile, syncProfile, themeLoading, toggleTheme]
  )

  return <StaffProfileContext.Provider value={value}>{children}</StaffProfileContext.Provider>
}

export function useStaffProfile() {
  const ctx = useContext(StaffProfileContext)
  if (!ctx) {
    throw new Error('useStaffProfile must be used within StaffProfileProvider')
  }
  return ctx
}
