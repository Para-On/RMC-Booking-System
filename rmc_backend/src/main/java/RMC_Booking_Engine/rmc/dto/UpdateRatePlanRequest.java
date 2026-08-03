package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRatePlanRequest(
        @Size(max = 100) String name,
        Long refundPolicyId,
        @Min(1) Integer holdTtlMinutes,
        @Min(0) Integer payLaterCutoffHours,
        Boolean active) {
}
