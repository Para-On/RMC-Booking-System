package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.dto.AuthResponse;
import RMC_Booking_Engine.rmc.dto.LoginRequest;
import RMC_Booking_Engine.rmc.dto.MfaConfirmRequest;
import RMC_Booking_Engine.rmc.dto.MfaSetupResponse;
import RMC_Booking_Engine.rmc.dto.MfaVerifyRequest;
import RMC_Booking_Engine.rmc.dto.RefreshRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffAuthService;
import RMC_Booking_Engine.rmc.service.StaffLoginAuditService;
import RMC_Booking_Engine.rmc.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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
    private final StaffLoginAuditService staffLoginAuditService;
    private final StaffUserRepository staffUserRepository;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        try {
            AuthResponse response = staffAuthService.login(request);
            if (!Boolean.TRUE.equals(response.mfaRequired())) {
                findUser(request.email())
                        .ifPresent(user -> staffLoginAuditService.recordLoginSuccess(user, ip));
            }
            return response;
        } catch (BusinessException ex) {
            recordLoginFailure(request.email(), ex.getMessage(), ip);
            throw ex;
        }
    }

    @PostMapping("/mfa/verify")
    public AuthResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        try {
            AuthResponse response = staffAuthService.verifyMfa(request);
            if (response.email() != null) {
                findUser(response.email())
                        .ifPresent(user -> staffLoginAuditService.recordLoginSuccess(user, ip));
            }
            return response;
        } catch (BusinessException ex) {
            recordLoginFailure(null, ex.getMessage(), ip);
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return staffAuthService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(
            @RequestBody(required = false) RefreshRequest request,
            @AuthenticationPrincipal StaffPrincipal staff,
            HttpServletRequest httpRequest) {
        staffAuthService.logout(request, staff);
        if (staff != null) {
            staffLoginAuditService.recordLogoutSuccess(staff, ClientIpResolver.resolve(httpRequest));
        }
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

    private void recordLoginFailure(String email, String reason, String ip) {
        if (email == null || email.isBlank()) {
            staffLoginAuditService.recordLoginFailure(null, null, null, reason, ip);
            return;
        }
        findUser(email).ifPresentOrElse(
                user -> staffLoginAuditService.recordLoginFailure(
                        email, user.getFullName(), user.getId(), reason, ip),
                () -> staffLoginAuditService.recordLoginFailure(email, null, null, reason, ip));
    }

    private java.util.Optional<StaffUser> findUser(String email) {
        if (email == null || email.isBlank()) {
            return java.util.Optional.empty();
        }
        return staffUserRepository.findByEmailIgnoreCase(email.trim());
    }
}
