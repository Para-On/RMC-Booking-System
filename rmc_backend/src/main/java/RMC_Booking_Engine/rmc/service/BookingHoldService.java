package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.InventoryHold;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingHoldService {

    private final InventoryHoldRepository inventoryHoldRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final AvailabilityService availabilityService;

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

    public void extendPaymentHold(Booking booking, int extensionMinutes) {
        Instant newExpiry = Instant.now().plusSeconds(extensionMinutes * 60L);
        booking.setExpiresAt(newExpiry);
        List<InventoryHold> holds = inventoryHoldRepository.findByBookingIdAndStatus(
                booking.getId(), HoldStatus.ACTIVE);
        for (InventoryHold hold : holds) {
            hold.setExpiresAt(newExpiry);
        }
        if (!holds.isEmpty()) {
            inventoryHoldRepository.saveAll(holds);
        }
    }

    /** After Maya payment succeeds, keep inventory reserved until staff approve/reject. */
    public void clearPaymentHoldExpiry(Booking booking) {
        booking.setExpiresAt(null);
        List<InventoryHold> holds = inventoryHoldRepository.findByBookingIdAndStatus(
                booking.getId(), HoldStatus.ACTIVE);
        for (InventoryHold hold : holds) {
            hold.setExpiresAt(null);
        }
        if (!holds.isEmpty()) {
            inventoryHoldRepository.saveAll(holds);
        }
    }

    public void restoreHolds(Booking booking) {
        LocalDate checkIn = booking.getCheckInDate();
        LocalDate checkOut = booking.getCheckOutDate();
        availabilityService.assertAvailable(booking.getRoomType(), checkIn, checkOut);

        Map<LocalDate, InventoryHold> holdsByDate = inventoryHoldRepository.findByBookingId(booking.getId())
                .stream()
                .collect(Collectors.toMap(InventoryHold::getHoldDate, hold -> hold, (left, right) -> left));

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            InventoryHold hold = holdsByDate.get(date);
            if (hold != null) {
                if (hold.getStatus() != HoldStatus.ACTIVE) {
                    hold.setStatus(HoldStatus.ACTIVE);
                    hold.setExpiresAt(null);
                    inventoryHoldRepository.save(hold);
                }
            } else {
                InventoryHold created = new InventoryHold();
                created.setBooking(booking);
                created.setRoomType(booking.getRoomType());
                created.setHoldDate(date);
                created.setHeldCount(1);
                created.setStatus(HoldStatus.ACTIVE);
                inventoryHoldRepository.save(created);
            }
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
