import { createContext, useContext } from 'react'

export const StaffNavContext = createContext({
  modules: [],
  loading: true,
  canAccess: () => false,
})

export function useStaffNav() {
  return useContext(StaffNavContext)
}
