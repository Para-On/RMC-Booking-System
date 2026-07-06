package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomCatalogCardDto(
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        BigDecimal squareMeters,
        List<String> imageUrls,
        List<String> amenities,
        String roomCategoryLabel,
        String roomViewLabel,
        String bedTypeLabel,
        boolean refundable,
        boolean freeCancellation) {
}
