package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ManualRefundRequest(
        @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,
        @NotBlank String reason,
        @NotBlank String externalReference,
        @NotBlank String method) {
}
