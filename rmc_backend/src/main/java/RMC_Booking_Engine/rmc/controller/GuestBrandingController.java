package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.BrandingResponse;
import RMC_Booking_Engine.rmc.service.BrandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest/branding")
@RequiredArgsConstructor
public class GuestBrandingController {

    private final BrandingService brandingService;

    @GetMapping
    public BrandingResponse getBranding() {
        return brandingService.getBranding();
    }
}
