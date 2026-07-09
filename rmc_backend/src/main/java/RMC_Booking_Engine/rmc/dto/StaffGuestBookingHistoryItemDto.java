package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StaffGuestBookingHistoryItemDto(
        Long bookingId,
        String reference,
        String status,
        String roomTypeName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal quotedTotal,
        String currency,
        Instant checkedInAt,
        Instant checkedOutAt,
        Instant createdAt) {}
