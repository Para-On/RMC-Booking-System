package RMC_Booking_Engine.rmc.dto;

import java.util.List;
import java.math.BigDecimal;

public record RoomTypeDetailDto(
        Long id,
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        Integer totalCapacity,
        Integer overbookingBuffer,
        Integer minAdvanceBookingHours,
        Integer maxAdvanceBookingDays,
        boolean active,
        Long ratePlanId,
        String ratePlanName,
        BigDecimal baseNightlyRate,
        int unitCount,
        BigDecimal squareMeters,
        List<String> amenities,
        boolean refundable,
        boolean freeCancellation,
        List<String> imageUrls,
        List<RoomNumberDto> assignedRoomNumbers,
        Long roomCategoryId,
        String roomCategoryLabel,
        Long roomViewId,
        String roomViewLabel,
        Long bedTypeId,
        String bedTypeLabel) {
}
