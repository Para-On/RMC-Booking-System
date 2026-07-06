package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record RefundResponse(
        String reference,
        String status,
        BigDecimal refundedAmount,
        String currency,
        String mayaRefundId,
        BigDecimal ledgerBalance) {
}
