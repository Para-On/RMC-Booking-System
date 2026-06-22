package RMC_Booking_Engine.rmc.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ArrivalItemDto(
        Long bookingId,
        String reference,
        String guestName,
        String guestEmail,
        String roomTypeName,
        String status,
        String paymentMethod,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String roomNumber,
        Instant checkedInAt,
        Instant checkedOutAt) {
}
