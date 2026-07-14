package RMC_Booking_Engine.rmc.dto;

public record BookingOccupantDto(
        Long guestId,
        String fullName,
        String email,
        String phone,
        boolean primary) {}
