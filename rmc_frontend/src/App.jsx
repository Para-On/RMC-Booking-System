import { BrowserRouter, useLocation } from 'react-router-dom'
import GuestLayout from '@/components/guest/GuestLayout'
import AppRouter from './routers'
import './App.css'

function AppShell() {
  const location = useLocation()
  const isStaffApp =
    location.pathname.startsWith('/staff') && !location.pathname.startsWith('/staff/login')

  if (isStaffApp) {
    return <AppRouter />
  }

  return (
    <GuestLayout>
      <AppRouter />
    </GuestLayout>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AppShell />
    </BrowserRouter>
  )
}

export default App
