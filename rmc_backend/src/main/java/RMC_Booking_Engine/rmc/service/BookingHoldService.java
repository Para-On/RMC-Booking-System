package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.InventoryHold;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingHoldService {

    private final InventoryHoldRepository inventoryHoldRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;

    public void releaseActiveHolds(Booking booking) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBookingIdAndStatus(
                booking.getId(), HoldStatus.ACTIVE);
        for (InventoryHold hold : holds) {
            hold.setStatus(HoldStatus.RELEASED);
        }
        if (!holds.isEmpty()) {
            inventoryHoldRepository.saveAll(holds);
        }
    }

    public void writeAuditLog(
            Booking booking,
            String fromStatus,
            String toStatus,
            String trigger,
            Long staffUserId,
            String reason) {
        BookingAuditLog logEntry = new BookingAuditLog();
        logEntry.setBooking(booking);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setTriggerSource(trigger);
        logEntry.setStaffUserId(staffUserId);
        logEntry.setReason(reason);
        logEntry.setCreatedAt(Instant.now());
        bookingAuditLogRepository.save(logEntry);
    }
}
