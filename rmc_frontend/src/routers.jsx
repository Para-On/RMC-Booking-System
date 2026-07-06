import { Navigate, Route, Routes } from 'react-router-dom'

import NavModuleRoute from './components/NavModuleRoute'

import StaffLayout from './components/staff/StaffLayout'

import StaffRoute from './components/StaffRoute'

import { STAFF_NAV_PATHS } from './config/staffNavAccess'

import BookDatesPage from './pages/BookDatesPage'

import BookingPage from './pages/BookingPage'

import BookingSuccessPage from './pages/BookingSuccessPage'

import CheckoutPage from './pages/CheckoutPage'

import ConfirmationPage from './pages/ConfirmationPage'

import LookupPage from './pages/LookupPage'

import SearchPage from './pages/SearchPage'

import StaffDashboardPage from './pages/staff/StaffDashboardPage'

import StaffArrivalsPage from './pages/staff/StaffArrivalsPage'

import StaffBookingPage from './pages/staff/StaffBookingPage'

import StaffLoginPage from './pages/staff/StaffLoginPage'

import StaffModulesPage from './pages/staff/StaffModulesPage'

import StaffRoomsCatalogPage from './pages/staff/StaffRoomsCatalogPage'

import StaffRoomsConfigPage from './pages/staff/StaffRoomsConfigPage'

import StaffRoomsOperationsPage from './pages/staff/StaffRoomsOperationsPage'

import StaffSettingsPage from './pages/staff/StaffSettingsPage'

import StaffUsersPage from './pages/staff/StaffUsersPage'



export default function AppRouter() {

  return (

    <Routes>

      <Route path="/" element={<SearchPage />} />

      <Route path="/rooms/:roomTypeId/dates" element={<BookDatesPage />} />

      <Route path="/checkout" element={<CheckoutPage />} />

      <Route path="/booking/confirmed" element={<ConfirmationPage />} />

      <Route path="/booking/success" element={<BookingSuccessPage />} />

      <Route path="/booking/lookup" element={<LookupPage />} />

      <Route path="/booking/:reference" element={<BookingPage />} />

      <Route path="/staff/login" element={<StaffLoginPage />} />

      <Route

        path="/staff"

        element={

          <StaffRoute>

            <StaffLayout />

          </StaffRoute>

        }

      >

        <Route index element={<Navigate to="dashboard" replace />} />

        <Route

          path="dashboard"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.DASHBOARD}>

              <StaffDashboardPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="arrivals"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ARRIVALS}>

              <StaffArrivalsPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="bookings/:id"

          element={

            <NavModuleRoute>

              <StaffBookingPage />

            </NavModuleRoute>

          }

        />

        <Route path="rooms" element={<Navigate to="rooms/operations" replace />} />

        <Route

          path="rooms/catalog"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ROOMS_CATALOG}>

              <StaffRoomsCatalogPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="rooms/config"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ROOMS_CONFIG}>

              <StaffRoomsConfigPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="rooms/operations"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ROOMS_OPERATIONS}>

              <StaffRoomsOperationsPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="settings"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.SETTINGS}>

              <StaffSettingsPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="users"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.USERS}>

              <StaffUsersPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="modules"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.MODULES}>

              <StaffModulesPage />

            </NavModuleRoute>

          }

        />

      </Route>

    </Routes>

  )

}

