package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingListItemDto(
        Long bookingId,
        Long guestId,
        String reference,
        String guestName,
        String guestEmail,
        String roomTypeName,
        String status,
        String paymentMethod,
        String refundStatus,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal quotedTotal,
        String currency,
        Instant createdAt) {
}
