package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record RatePlanConfigDto(
        Long id,
        Long roomTypeId,
        String roomTypeName,
        String name,
        Long refundPolicyId,
        String refundPolicyName,
        Boolean policyEnabled,
        Boolean refundable,
        Integer holdTtlMinutes,
        Integer payLaterCutoffHours,
        boolean active,
        BigDecimal sampleNightlyRate) {
}
