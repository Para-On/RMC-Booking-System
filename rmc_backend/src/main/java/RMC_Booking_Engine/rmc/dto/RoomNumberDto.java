package RMC_Booking_Engine.rmc.dto;

public record RoomNumberDto(
        Long id,
        String roomNumber,
        String floorLabel,
        String dayStatus,
        String statusLabel,
        Long statusOptionId,
        Long roomTypeId,
        String roomTypeName) {
}
