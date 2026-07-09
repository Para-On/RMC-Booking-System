package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.ChangeStaffPasswordRequest;
import RMC_Booking_Engine.rmc.dto.StaffProfileResponse;
import RMC_Booking_Engine.rmc.dto.UpdateStaffProfileRequest;
import RMC_Booking_Engine.rmc.dto.UpdateStaffThemeRequest;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/profile")
@RequiredArgsConstructor
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    @GetMapping
    public StaffProfileResponse getProfile(@AuthenticationPrincipal StaffPrincipal staff) {
        return staffProfileService.getProfile(staff.id());
    }

    @PutMapping
    public StaffProfileResponse updateProfile(
            @Valid @RequestBody UpdateStaffProfileRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffProfileService.updateProfile(staff.id(), request);
    }

    @PutMapping("/theme")
    public StaffProfileResponse updateTheme(
            @Valid @RequestBody UpdateStaffThemeRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffProfileService.updateTheme(staff.id(), request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StaffProfileResponse uploadAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffProfileService.uploadAvatar(staff.id(), file);
    }

    @PutMapping("/password")
    public void changePassword(
            @Valid @RequestBody ChangeStaffPasswordRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        staffProfileService.changePassword(staff.id(), request);
    }
}
