package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateStaffNavModulesRequest(
        @NotEmpty @Valid List<StaffNavModuleUpdateItem> modules) {

    public record StaffNavModuleUpdateItem(
            Long id,
            Boolean enabled,
            Integer sortOrder) {
    }
}
