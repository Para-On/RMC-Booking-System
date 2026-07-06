package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record RefundRequest(
        @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String reason) {
}
