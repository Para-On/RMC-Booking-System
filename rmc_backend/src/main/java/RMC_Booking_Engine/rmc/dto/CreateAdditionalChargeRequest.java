package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAdditionalChargeRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}
