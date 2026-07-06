package RMC_Booking_Engine.rmc.dto;

public record RoomUnitOptionDto(
        Long id,
        String roomNumber,
        String floorLabel,
        String dayStatus,
        String statusLabel) {
}
