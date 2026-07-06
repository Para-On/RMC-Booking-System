package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetStaffPasswordRequest(
        @NotBlank String password) {
}
