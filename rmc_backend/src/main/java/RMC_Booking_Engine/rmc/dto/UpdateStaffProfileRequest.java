package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStaffProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 40) String phone) {}
