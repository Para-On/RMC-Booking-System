package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String mfaToken,
        @NotBlank String code) {
}
