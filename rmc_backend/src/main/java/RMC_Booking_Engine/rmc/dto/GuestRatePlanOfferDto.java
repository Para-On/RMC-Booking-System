package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import java.math.BigDecimal;
import java.util.List;

public record GuestRatePlanOfferDto(
        Long ratePlanId,
        String name,
        BigDecimal totalBase,
        BigDecimal totalTaxInclusive,
        BigDecimal originalTotalTaxInclusive,
        AppliedPromoDto promo,
        List<NightlyRateDto> nightlyBreakdown,
        boolean refundable,
        boolean freeCancellation,
        String policySummary,
        Integer fullCutoffValue,
        CutoffUnit fullCutoffUnit,
        boolean partialEnabled,
        Integer partialRefundPercent) {
}
