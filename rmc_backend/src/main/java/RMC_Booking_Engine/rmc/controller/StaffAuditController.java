package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.PagedResponse;
import RMC_Booking_Engine.rmc.dto.StaffActivityAuditEntryDto;
import RMC_Booking_Engine.rmc.dto.StaffLoginAuditEntryDto;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.StaffActivityAuditService;
import RMC_Booking_Engine.rmc.service.StaffLoginAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/audit")
@RequiredArgsConstructor
public class StaffAuditController {

    private final StaffLoginAuditService staffLoginAuditService;
    private final StaffActivityAuditService staffActivityAuditService;

    @GetMapping("/login")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_AUDIT + "')")
    public PagedResponse<StaffLoginAuditEntryDto> getLoginAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return staffLoginAuditService.list(page, size);
    }

    @GetMapping("/activity")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_AUDIT + "')")
    public PagedResponse<StaffActivityAuditEntryDto> getActivityAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return staffActivityAuditService.list(page, size);
    }
}
