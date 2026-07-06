package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record StaffNavModuleDto(
        Long id,
        Long parentId,
        String moduleKey,
        String label,
        String path,
        String icon,
        List<String> allowedRoles,
        int sortOrder,
        boolean enabled,
        List<StaffNavModuleDto> children) {

    public StaffNavModuleDto withoutChildren() {
        return new StaffNavModuleDto(
                id, parentId, moduleKey, label, path, icon, allowedRoles, sortOrder, enabled, List.of());
    }
}
