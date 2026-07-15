package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayAdditionalChargeRequest(
        @NotBlank String email,
        @NotNull String paymentMethod) {}
