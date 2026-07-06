package RMC_Booking_Engine.rmc.dto;

public record UnassignedReservationDto(
        Long roomTypeId,
        String roomTypeName,
        int count) {
}
