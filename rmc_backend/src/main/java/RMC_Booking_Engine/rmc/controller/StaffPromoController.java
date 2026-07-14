package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.CreatePromoRequest;
import RMC_Booking_Engine.rmc.dto.PromoDto;
import RMC_Booking_Engine.rmc.dto.UpdatePromoRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.PromoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/promos")
@RequiredArgsConstructor
public class StaffPromoController {

    private final PromoService promoService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_PROMOS + "')")
    public List<PromoDto> list() {
        return promoService.listStaffPromos();
    }

    @PostMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_PROMOS + "')")
    public PromoDto create(@Valid @RequestBody CreatePromoRequest request) {
        return promoService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_PROMOS + "')")
    public PromoDto update(@PathVariable Long id, @Valid @RequestBody UpdatePromoRequest request) {
        return promoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS_PROMOS + "')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
