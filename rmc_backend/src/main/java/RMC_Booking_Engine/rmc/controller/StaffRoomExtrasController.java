package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.BrandingAssetUploadResponse;
import RMC_Booking_Engine.rmc.dto.CreateItemAddonRequest;
import RMC_Booking_Engine.rmc.dto.CreateServiceAddonRequest;
import RMC_Booking_Engine.rmc.dto.ItemAddonDto;
import RMC_Booking_Engine.rmc.dto.ServiceAddonDto;
import RMC_Booking_Engine.rmc.dto.UpdateItemAddonRequest;
import RMC_Booking_Engine.rmc.dto.UpdateServiceAddonRequest;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.BrandingStorageService;
import RMC_Booking_Engine.rmc.service.RoomExtrasService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/rooms/extras")
@RequiredArgsConstructor
public class StaffRoomExtrasController {

    private final RoomExtrasService roomExtrasService;
    private final BrandingStorageService brandingStorageService;

    @GetMapping("/services")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public List<ServiceAddonDto> listServices() {
        return roomExtrasService.listStaffServices();
    }

    @PostMapping("/services")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ServiceAddonDto createService(@Valid @RequestBody CreateServiceAddonRequest request) {
        return roomExtrasService.createService(request);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ServiceAddonDto updateService(
            @PathVariable Long id, @Valid @RequestBody UpdateServiceAddonRequest request) {
        return roomExtrasService.updateService(id, request);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        roomExtrasService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services/image")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public BrandingAssetUploadResponse uploadServiceImage(@RequestParam("file") MultipartFile file) {
        return new BrandingAssetUploadResponse(brandingStorageService.storeServiceAddonImage(file));
    }

    @GetMapping("/items")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public List<ItemAddonDto> listItems() {
        return roomExtrasService.listStaffItems();
    }

    @PostMapping("/items")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ItemAddonDto createItem(@Valid @RequestBody CreateItemAddonRequest request) {
        return roomExtrasService.createItem(request);
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ItemAddonDto updateItem(@PathVariable Long id, @Valid @RequestBody UpdateItemAddonRequest request) {
        return roomExtrasService.updateItem(id, request);
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        roomExtrasService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items/image")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_EXTRAS + "')")
    public BrandingAssetUploadResponse uploadItemImage(@RequestParam("file") MultipartFile file) {
        return new BrandingAssetUploadResponse(brandingStorageService.storeItemAddonImage(file));
    }
}
