package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateRatePlanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.01") BigDecimal baseNightlyRate,
        @NotNull Long refundPolicyId,
        @Min(1) Integer holdTtlMinutes,
        @Min(0) Integer payLaterCutoffHours,
        Boolean active) {
}
