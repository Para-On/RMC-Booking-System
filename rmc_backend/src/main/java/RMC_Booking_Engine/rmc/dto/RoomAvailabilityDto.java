package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomAvailabilityDto(
        Long roomTypeId,
        Long ratePlanId,
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        Integer availableUnits,
        BigDecimal totalBase,
        BigDecimal totalTaxInclusive,
        BigDecimal fromTotalTaxInclusive,
        BigDecimal originalTotalTaxInclusive,
        AppliedPromoDto promo,
        String currency,
        List<NightlyRateDto> nightlyBreakdown,
        List<GuestRatePlanOfferDto> ratePlans,
        BigDecimal squareMeters,
        List<String> imageUrls,
        List<String> amenities,
        String roomCategoryLabel,
        String roomViewLabel,
        String bedTypeLabel,
        boolean refundable,
        boolean freeCancellation,
        boolean policiesVary) {
}
