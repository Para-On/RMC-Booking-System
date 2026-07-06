package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.CreateStaffNavModuleRequest;
import RMC_Booking_Engine.rmc.dto.StaffNavModuleDto;
import RMC_Booking_Engine.rmc.dto.StaffRoleOptionDto;
import RMC_Booking_Engine.rmc.dto.UpdateStaffNavModuleRequest;
import RMC_Booking_Engine.rmc.dto.UpdateStaffNavModulesRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffNavService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/nav")
@RequiredArgsConstructor
public class StaffNavController {

    private final StaffNavService staffNavService;

    @GetMapping
    public List<StaffNavModuleDto> getNav(@AuthenticationPrincipal StaffPrincipal staff) {
        return staffNavService.getNavForStaff(staff);
    }

    @GetMapping("/roles")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    public List<StaffRoleOptionDto> getRoles() {
        return staffNavService.getRoleOptions();
    }

    @GetMapping("/all")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    public List<StaffNavModuleDto> getAllModules() {
        return staffNavService.getAllModules();
    }

    @PostMapping("/modules")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffNavModuleDto createModule(@Valid @RequestBody CreateStaffNavModuleRequest request) {
        return staffNavService.createModule(request);
    }

    @PutMapping("/modules/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    public StaffNavModuleDto updateModule(
            @PathVariable Long id, @Valid @RequestBody UpdateStaffNavModuleRequest request) {
        return staffNavService.updateModule(id, request);
    }

    @DeleteMapping("/modules/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable Long id) {
        staffNavService.deleteModule(id);
    }

    @PutMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.MODULES + "')")
    public List<StaffNavModuleDto> updateModules(
            @Valid @RequestBody UpdateStaffNavModulesRequest request) {
        return staffNavService.updateModules(request);
    }
}
