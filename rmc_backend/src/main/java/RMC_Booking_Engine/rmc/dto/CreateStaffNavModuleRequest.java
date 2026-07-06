package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateStaffNavModuleRequest(
        @NotBlank @Size(max = 50) String moduleKey,
        @NotBlank @Size(max = 100) String label,
        @NotBlank @Size(max = 200) String path,
        @NotBlank @Size(max = 50) String icon,
        @NotEmpty List<@NotBlank @Size(max = 20) String> allowedRoles,
        Long parentId,
        Integer sortOrder,
        Boolean enabled) {
}
