package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromoCodeDto(
        Long id,
        String name,
        String description,
        String promoType,
        String offerCode,
        String organizationCode,
        String discountType,
        BigDecimal discountValue,
        LocalDate startsOn,
        LocalDate endsOn,
        int maxUses,
        int usedCount,
        int remainingUses,
        boolean active,
        boolean currentlyApplicable,
        List<Long> ratePlanIds,
        List<String> ratePlanNames) {
}
