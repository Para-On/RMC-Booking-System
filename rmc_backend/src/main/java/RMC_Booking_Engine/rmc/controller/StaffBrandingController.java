package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.BrandingAssetUploadResponse;
import RMC_Booking_Engine.rmc.dto.BrandingResponse;
import RMC_Booking_Engine.rmc.dto.UpdateBrandingRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.BrandingService;
import RMC_Booking_Engine.rmc.service.BrandingStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/branding")
@RequiredArgsConstructor
public class StaffBrandingController {

    private final BrandingService brandingService;
    private final BrandingStorageService brandingStorageService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.BRANDING + "')")
    public BrandingResponse getBranding() {
        return brandingService.getBranding();
    }

    @PutMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.BRANDING + "')")
    public BrandingResponse updateBranding(
            @Valid @RequestBody UpdateBrandingRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return brandingService.updateBranding(request, staff);
    }

    @PostMapping("/logo")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.BRANDING + "')")
    public BrandingAssetUploadResponse uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal staff) {
        String assetUrl = brandingStorageService.storeLogo(file);
        brandingService.updateLogoUrl(assetUrl, staff);
        return new BrandingAssetUploadResponse(assetUrl);
    }
}
