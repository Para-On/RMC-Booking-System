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
        BigDecimal totalTaxInclusive,
        String currency,
        List<NightlyRateDto> nightlyBreakdown) {
}
