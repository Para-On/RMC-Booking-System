package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundPolicyPreviewDto(
        String policySummary,
        String tierIfCancelledNow,
        BigDecimal refundAmountIfCancelledNow,
        Integer refundPercentIfCancelledNow,
        BigDecimal deductionAmountIfCancelledNow,
        Instant fullCutoffAt,
        Instant checkInAt,
        boolean cancellationAllowedNow,
        String blockReason) {
}
