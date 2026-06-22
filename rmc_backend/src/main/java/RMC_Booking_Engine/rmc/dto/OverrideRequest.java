package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OverrideRequest(
        @NotNull String targetStatus,
        @NotBlank String reason) {
}
