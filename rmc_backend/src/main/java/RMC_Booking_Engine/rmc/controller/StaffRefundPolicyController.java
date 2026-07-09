package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.RefundPolicyConfigDto;
import RMC_Booking_Engine.rmc.dto.UpdateRefundPolicyRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.RefundPolicyConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/refund-policy")
@RequiredArgsConstructor
public class StaffRefundPolicyController {

    private final RefundPolicyConfigService refundPolicyConfigService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public RefundPolicyConfigDto getPolicy() {
        return refundPolicyConfigService.getActivePolicy();
    }

    @PutMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public RefundPolicyConfigDto updatePolicy(
            @Valid @RequestBody UpdateRefundPolicyRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        refundPolicyConfigService.assertValidTimezone(request.timezone());
        return refundPolicyConfigService.updateActivePolicy(request, staff);
    }
}
