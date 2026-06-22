package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.ManagerConfigResponse;
import RMC_Booking_Engine.rmc.dto.RatePlanConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeConfigDto;
import RMC_Booking_Engine.rmc.dto.SystemConfigItemDto;
import RMC_Booking_Engine.rmc.dto.UpdateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeRequest;
import RMC_Booking_Engine.rmc.dto.UpdateSystemConfigRequest;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class StaffConfigController {

    private final StaffConfigService staffConfigService;

    @GetMapping
    public ManagerConfigResponse getConfig() {
        return staffConfigService.getConfig();
    }

    @PutMapping("/system/{key}")
    public SystemConfigItemDto updateSystemConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateSystemConfig(key, request.value(), staff);
    }

    @PutMapping("/rate-plans/{id}")
    public RatePlanConfigDto updateRatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRatePlanRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRatePlan(id, request, staff);
    }

    @PutMapping("/room-types/{id}")
    public RoomTypeConfigDto updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomType(id, request, staff);
    }
}
