package RMC_Booking_Engine.rmc.dto;

public record RoomUnitConfigDto(
        Long id,
        Long roomTypeId,
        String roomTypeName,
        String roomNumber,
        String floorLabel,
        String dayStatus,
        String statusLabel,
        Long statusOptionId) {
}
