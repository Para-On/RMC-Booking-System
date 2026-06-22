package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.AuthResponse;
import RMC_Booking_Engine.rmc.dto.LoginRequest;
import RMC_Booking_Engine.rmc.dto.RefreshRequest;
import RMC_Booking_Engine.rmc.service.StaffAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return staffAuthService.refresh(request);
    }
}
