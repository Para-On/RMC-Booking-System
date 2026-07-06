package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.AuthResponse;
import RMC_Booking_Engine.rmc.dto.LoginRequest;
import RMC_Booking_Engine.rmc.dto.MfaConfirmRequest;
import RMC_Booking_Engine.rmc.dto.MfaSetupResponse;
import RMC_Booking_Engine.rmc.dto.MfaVerifyRequest;
import RMC_Booking_Engine.rmc.dto.RefreshRequest;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/auth")
@RequiredArgsConstructor
public class StaffAuthController {

    private final StaffAuthService staffAuthService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return staffAuthService.login(request);
    }

    @PostMapping("/mfa/verify")
    public AuthResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return staffAuthService.verifyMfa(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return staffAuthService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(
            @RequestBody(required = false) RefreshRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        staffAuthService.logout(request, staff);
    }

    @PostMapping("/mfa/setup")
    public MfaSetupResponse startMfaSetup(@AuthenticationPrincipal StaffPrincipal staff) {
        return staffAuthService.startMfaSetup(staff);
    }

    @PostMapping("/mfa/confirm")
    public void confirmMfaSetup(
            @Valid @RequestBody MfaConfirmRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        staffAuthService.confirmMfaSetup(staff, request);
    }

    @PostMapping("/mfa/disable")
    public void disableMfa(@AuthenticationPrincipal StaffPrincipal staff) {
        staffAuthService.disableMfa(staff);
    }
}
