package RMC_Booking_Engine.rmc.dto;

public record RoomCalendarBlockDto(
        Long bookingId,
        String reference,
        String guestName,
        String checkInDate,
        String checkOutDate,
        String blockType) {
}
