package RMC_Booking_Engine.rmc.dto;

public record RoomUnitBookingDto(
        Long bookingId,
        String reference,
        String guestName,
        String status,
        String checkInDate,
        String checkOutDate,
        String paymentMethod) {}
