package RMC_Booking_Engine.rmc.dto;

import java.time.LocalDate;

public record StaffSearchResultDto(
        Long bookingId,
        String reference,
        String guestName,
        String guestEmail,
        String roomTypeName,
        String status,
        LocalDate checkInDate,
        LocalDate checkOutDate) {
}
