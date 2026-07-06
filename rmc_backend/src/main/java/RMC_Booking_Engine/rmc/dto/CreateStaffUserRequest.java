package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotNull StaffRole role,
        @NotBlank String password) {
}
