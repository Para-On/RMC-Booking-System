package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record GuestRoomCatalogItemDto(
        Long roomTypeId,
        Long ratePlanId,
        RoomCatalogCardDto catalog,
        BigDecimal fromNightlyRate,
        String currency) {
}
