package RMC_Booking_Engine.rmc.dto;

public record RoomTypeConfigDto(
        Long id,
        String name,
        Integer totalCapacity,
        Integer overbookingBuffer,
        Integer minAdvanceBookingHours,
        Integer maxAdvanceBookingDays,
        boolean active) {
}
