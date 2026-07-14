package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdditionalGuestRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 255) String email,
        @Size(max = 50) String phone) {}
