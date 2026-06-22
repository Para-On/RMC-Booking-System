import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import SearchPage from './pages/SearchPage'
import CheckoutPage from './pages/CheckoutPage'
import ConfirmationPage from './pages/ConfirmationPage'
import LookupPage from './pages/LookupPage'
import BookingPage from './pages/BookingPage'
import BookingSuccessPage from './pages/BookingSuccessPage'
import StaffLoginPage from './pages/staff/StaffLoginPage'
import StaffArrivalsPage from './pages/staff/StaffArrivalsPage'
import StaffBookingPage from './pages/staff/StaffBookingPage'
import StaffSettingsPage from './pages/staff/StaffSettingsPage'
import StaffRoute from './components/StaffRoute'
import ManagerRoute from './components/ManagerRoute'
import './App.css'

function App() {
  return (
    <BrowserRouter>
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
          <Routes>
            <Route path="/" element={<SearchPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/booking/confirmed" element={<ConfirmationPage />} />
            <Route path="/booking/success" element={<BookingSuccessPage />} />
            <Route path="/booking/lookup" element={<LookupPage />} />
            <Route path="/booking/:reference" element={<BookingPage />} />
            <Route path="/staff/login" element={<StaffLoginPage />} />
            <Route
              path="/staff/arrivals"
              element={
                <StaffRoute>
                  <StaffArrivalsPage />
                </StaffRoute>
              }
            />
            <Route
              path="/staff/bookings/:id"
              element={
                <StaffRoute>
                  <StaffBookingPage />
                </StaffRoute>
              }
            />
            <Route
              path="/staff/settings"
              element={
                <ManagerRoute>
                  <StaffSettingsPage />
                </ManagerRoute>
              }
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
