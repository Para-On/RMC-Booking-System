package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record RoomDailyStatusPageResponse(
        String date,
        List<RoomDailyStatusDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        int availableCount,
        int reservedCount,
        int occupiedCount,
        int outOfOrderCount,
        List<UnassignedReservationDto> unassignedReservations) {
}
