package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDailyRatesRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
