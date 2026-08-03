package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.RefundPolicyConfigDto;
import RMC_Booking_Engine.rmc.dto.UpdateRefundPolicyRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.RefundPolicyConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/staff/refund-policy")
@RequiredArgsConstructor
public class StaffRefundPolicyController {

    private final RefundPolicyConfigService refundPolicyConfigService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public List<RefundPolicyConfigDto> listPolicies() {
        return refundPolicyConfigService.listPolicies();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public RefundPolicyConfigDto createPolicy(
            @Valid @RequestBody UpdateRefundPolicyRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return refundPolicyConfigService.createPolicy(request, staff);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public RefundPolicyConfigDto updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRefundPolicyRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return refundPolicyConfigService.updatePolicy(id, request, staff);
    }

    /** Legacy single-policy update — updates the first active policy. */
    @PutMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public RefundPolicyConfigDto updatePolicyLegacy(
            @Valid @RequestBody UpdateRefundPolicyRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return refundPolicyConfigService.updateActivePolicy(request, staff);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_REFUND_POLICY + "')")
    public Map<String, Object> deactivatePolicy(
            @PathVariable Long id, @AuthenticationPrincipal StaffPrincipal staff) {
        refundPolicyConfigService.deactivatePolicy(id, staff);
        return Map.of("deactivated", true, "id", id);
    }
}
