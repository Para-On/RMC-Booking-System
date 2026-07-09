package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Min;

public record UpdateRatePlanRequest(
        String cancellationPolicy,
        @Min(0) Integer refundWindowHours,
        @Min(0) Integer lateCancelRefundPercent,
        Boolean allowLateCancellation,
        @Min(1) Integer holdTtlMinutes,
        @Min(0) Integer payLaterCutoffHours,
        Boolean active) {
}
