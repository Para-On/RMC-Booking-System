package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NightlyRateDto(
        LocalDate date,
        BigDecimal baseAmount,
        BigDecimal serviceCharge,
        BigDecimal vat,
        BigDecimal taxInclusiveTotal) {
}
