import { Navigate, Route, Routes } from 'react-router-dom'

import NavModuleRoute from './components/NavModuleRoute'

import StaffLayout from './components/staff/StaffLayout'

import StaffRoute from './components/StaffRoute'

import { STAFF_NAV_PATHS } from './config/staffNavAccess'

import BookDatesPage from './pages/BookDatesPage'

import RoomDetailPage from './pages/RoomDetailPage'

import BookingPage from './pages/BookingPage'

import BookingSuccessPage from './pages/BookingSuccessPage'

import CheckoutPage from './pages/CheckoutPage'

import ConfirmationPage from './pages/ConfirmationPage'

import LookupPage from './pages/LookupPage'

import SearchPage from './pages/SearchPage'

import StaffDashboardPage from './pages/staff/StaffDashboardPage'

import StaffArrivalsPage from './pages/staff/StaffArrivalsPage'

import StaffBookingsListPage from './pages/staff/StaffBookingsListPage'

import StaffGuestsListPage from './pages/staff/StaffGuestsListPage'

import StaffGuestProfilePage from './pages/staff/StaffGuestProfilePage'

import StaffBookingPage from './pages/staff/StaffBookingPage'

import StaffLoginPage from './pages/staff/StaffLoginPage'

import StaffModulesPage from './pages/staff/StaffModulesPage'

import StaffRoomsCatalogPage from './pages/staff/StaffRoomsCatalogPage'

import StaffRoomsConfigPage from './pages/staff/StaffRoomsConfigPage'

import StaffRoomsOperationsPage from './pages/staff/StaffRoomsOperationsPage'

import StaffRoomExtrasPage from './pages/staff/StaffRoomExtrasPage'

import StaffSettingsPage from './pages/staff/StaffSettingsPage'
import StaffRefundPolicyPage from './pages/staff/StaffRefundPolicyPage'
import StaffAuditPage from './pages/staff/StaffAuditPage'

import StaffBrandingPage from './pages/staff/StaffBrandingPage'

import StaffUsersPage from './pages/staff/StaffUsersPage'

import StaffProfilePage from './pages/staff/StaffProfilePage'



export default function AppRouter() {

  return (

    <Routes>

      <Route path="/" element={<SearchPage />} />

      <Route path="/rooms/:roomTypeId" element={<RoomDetailPage />} />

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

          path="arrivals/bookings"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ARRIVALS_BOOKINGS}>

              <StaffBookingsListPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="guests/:id"

          element={

            <NavModuleRoute>

              <StaffGuestProfilePage />

            </NavModuleRoute>

          }

        />

        <Route

          path="guests"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.GUESTS}>

              <StaffGuestsListPage />

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

          path="rooms/extras"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.ROOMS_EXTRAS}>

              <StaffRoomExtrasPage />

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

          path="settings/refund-policy"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.SETTINGS_REFUND_POLICY}>

              <StaffRefundPolicyPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="settings/audit"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.SETTINGS_AUDIT}>

              <StaffAuditPage />

            </NavModuleRoute>

          }

        />

        <Route

          path="branding"

          element={

            <NavModuleRoute path={STAFF_NAV_PATHS.BRANDING}>

              <StaffBrandingPage />

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

        <Route path="profile" element={<StaffProfilePage />} />

      </Route>

    </Routes>

  )

}

