import { BrowserRouter, Link, useLocation } from 'react-router-dom'
import AppRouter from './routers'
import './App.css'

function AppShell() {
  const location = useLocation()
  const isStaffApp = location.pathname.startsWith('/staff') && !location.pathname.startsWith('/staff/login')

  if (isStaffApp) {
    return <AppRouter />
  }

  return (
    <div className="app">
      <header className="app-header">
        <Link to="/" className="brand">
          RMC Booking
        </Link>
        <nav>
          <Link to="/">Book</Link>
          <Link to="/booking/lookup">Find booking</Link>
          <Link to="/staff/login">Staff</Link>
        </nav>
      </header>
      <main className="app-main">
        <AppRouter />
      </main>
    </div>
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
