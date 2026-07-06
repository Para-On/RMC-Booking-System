package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateStaffNavModuleRequest(
        @NotBlank @Size(max = 100) String label,
        @NotBlank @Size(max = 200) String path,
        @NotBlank @Size(max = 50) String icon,
        @NotEmpty List<@NotBlank @Size(max = 20) String> allowedRoles,
        Boolean enabled,
        Integer sortOrder) {
}
