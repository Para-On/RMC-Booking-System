package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.CreateStaffUserRequest;
import RMC_Booking_Engine.rmc.dto.ResetStaffPasswordRequest;
import RMC_Booking_Engine.rmc.dto.StaffUserDto;
import RMC_Booking_Engine.rmc.dto.UpdateStaffUserRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/users")
@RequiredArgsConstructor
@PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.USERS + "')")
public class StaffUserController {

    private final StaffUserService staffUserService;

    @GetMapping
    public List<StaffUserDto> listUsers() {
        return staffUserService.listUsers();
    }

    @PostMapping
    public StaffUserDto createUser(
            @Valid @RequestBody CreateStaffUserRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffUserService.createUser(request, staff);
    }

    @PutMapping("/{id}")
    public StaffUserDto updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffUserRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffUserService.updateUser(id, request, staff);
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetStaffPasswordRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        staffUserService.resetPassword(id, request.password(), staff);
    }
}
