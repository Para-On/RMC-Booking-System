package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffUserRequest(
        @NotNull Boolean active,
        StaffRole role) {
}
