package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StaffBookingDetailResponse(
        Long bookingId,
        String reference,
        String status,
        String paymentMethod,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal quotedTotal,
        String currency,
        String guestName,
        String guestEmail,
        String guestPhone,
        String roomTypeName,
        String roomNumber,
        Instant checkedInAt,
        Instant checkedOutAt,
        BigDecimal ledgerBalance,
        List<LedgerEntryDto> ledger,
        List<AuditEntryDto> auditLog,
        List<RoomUnitOptionDto> availableRooms) {
}
