import { StaffPage } from '@/components/staff/StaffPageShell'
import StaffRoomsOperations from '@/components/staff/StaffRoomsOperations'

export default function StaffRoomsOperationsPage() {
  return (
    <StaffPage
      title="View and update room"
      description="Monitor daily room availability, calendars, and in-stay updates."
    >
      <StaffRoomsOperations />
    </StaffPage>
  )
}
