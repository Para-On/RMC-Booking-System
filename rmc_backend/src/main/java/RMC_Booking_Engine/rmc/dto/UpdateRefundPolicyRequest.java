package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRefundPolicyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Boolean enabled,
        @NotNull @Min(0) @Max(365) Integer fullCutoffValue,
        @NotBlank @Pattern(regexp = "HOURS|DAYS") String fullCutoffUnit,
        @NotNull Boolean partialEnabled,
        @NotNull @Min(0) @Max(100) Integer partialRefundPercent,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String checkInTime,
        @NotBlank @Size(max = 50) String timezone,
        @Size(max = 2000) String description) {
}
