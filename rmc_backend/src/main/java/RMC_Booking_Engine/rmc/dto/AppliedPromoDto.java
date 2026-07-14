package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record AppliedPromoDto(
        Long id,
        String name,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal amountOff,
        String label) {
}
