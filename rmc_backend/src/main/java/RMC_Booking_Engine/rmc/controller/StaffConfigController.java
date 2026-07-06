package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.CreateRoomNumberRequest;
import RMC_Booking_Engine.rmc.dto.CreateRoomTypeRequest;
import RMC_Booking_Engine.rmc.dto.CreateRoomTypeResponse;
import RMC_Booking_Engine.rmc.dto.ConfigurationAuditEntryDto;
import RMC_Booking_Engine.rmc.dto.CreateRoomUnitRequest;
import RMC_Booking_Engine.rmc.dto.ManagerConfigResponse;
import RMC_Booking_Engine.rmc.dto.RatePlanConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomImageUploadResponse;
import RMC_Booking_Engine.rmc.dto.CreateRoomConfigOptionRequest;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionDto;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionsResponse;
import RMC_Booking_Engine.rmc.dto.RoomNumberDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeDetailDto;
import RMC_Booking_Engine.rmc.dto.RoomUnitConfigDto;
import RMC_Booking_Engine.rmc.dto.SystemConfigItemDto;
import RMC_Booking_Engine.rmc.dto.UpdateDailyRatesRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomConfigOptionRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomNumberRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeCatalogRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomUnitRequest;
import RMC_Booking_Engine.rmc.dto.UpdateSystemConfigRequest;
import java.util.List;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.RoomImageStorageService;
import RMC_Booking_Engine.rmc.service.StaffConfigService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/config")
@RequiredArgsConstructor
public class StaffConfigController {

    private final StaffConfigService staffConfigService;
    private final RoomImageStorageService roomImageStorageService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS + "')")
    public ManagerConfigResponse getConfig() {
        return staffConfigService.getConfig();
    }

    @PutMapping("/system/{key}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS + "')")
    public SystemConfigItemDto updateSystemConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateSystemConfig(key, request.value(), staff);
    }

    @PutMapping("/rate-plans/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS + "')")
    public RatePlanConfigDto updateRatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRatePlanRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRatePlan(id, request, staff);
    }

    @GetMapping("/room-options")
    @PreAuthorize("@staffNavAccessService.canAccessAny(authentication, '"
            + StaffNavPaths.ROOMS_CATALOG + "', '"
            + StaffNavPaths.ROOMS_CONFIG + "', '"
            + StaffNavPaths.SETTINGS + "')")
    public RoomConfigOptionsResponse getRoomConfigOptions() {
        return staffConfigService.getRoomConfigOptions();
    }

    @PostMapping("/room-options")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CONFIG + "')")
    public RoomConfigOptionDto createRoomConfigOption(
            @Valid @RequestBody CreateRoomConfigOptionRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.createRoomConfigOption(request, staff);
    }

    @PutMapping("/room-options/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CONFIG + "')")
    public RoomConfigOptionDto updateRoomConfigOption(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomConfigOptionRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomConfigOption(id, request, staff);
    }

    @DeleteMapping("/room-options/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CONFIG + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoomConfigOption(
            @PathVariable Long id, @AuthenticationPrincipal StaffPrincipal staff) {
        staffConfigService.deleteRoomConfigOption(id, staff);
    }

    @GetMapping("/room-types")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "') or @staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_OPERATIONS + "')")
    public List<RoomTypeDetailDto> listRoomTypes() {
        return staffConfigService.listRoomTypeDetails();
    }

    @GetMapping("/room-numbers")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "') or @staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_OPERATIONS + "')")
    public List<RoomNumberDto> listRoomNumbers(
            @RequestParam(defaultValue = "false") boolean unassignedOnly) {
        return staffConfigService.listRoomNumbers(unassignedOnly);
    }

    @PostMapping("/room-numbers")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public RoomNumberDto createRoomNumber(
            @Valid @RequestBody CreateRoomNumberRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.createRoomNumber(request, staff);
    }

    @PutMapping("/room-numbers/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public RoomNumberDto updateRoomNumber(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomNumberRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomNumber(id, request, staff);
    }

    @DeleteMapping("/room-numbers/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoomNumber(@PathVariable Long id, @AuthenticationPrincipal StaffPrincipal staff) {
        staffConfigService.deleteRoomNumber(id, staff);
    }

    @PostMapping("/room-images")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public RoomImageUploadResponse uploadRoomImage(
            @RequestParam("file") MultipartFile file) {
        return new RoomImageUploadResponse(roomImageStorageService.store(file));
    }

    @PostMapping("/room-types")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public CreateRoomTypeResponse createRoomType(
            @Valid @RequestBody CreateRoomTypeRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.createRoomType(request, staff);
    }

    @PutMapping("/room-types/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "') or @staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_OPERATIONS + "')")
    public RoomTypeConfigDto updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomType(id, request, staff);
    }

    @PutMapping("/room-types/{id}/catalog")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public RoomTypeDetailDto updateRoomTypeCatalog(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeCatalogRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomTypeCatalog(id, request, staff);
    }

    @PostMapping("/room-types/{id}/units")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "')")
    public RoomUnitConfigDto createRoomUnit(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomUnitRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.createRoomUnit(id, request, staff);
    }

    @PutMapping("/rate-plans/{id}/daily-rates")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS + "')")
    public int updateDailyRates(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDailyRatesRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateDailyRates(id, request, staff);
    }

    @GetMapping("/audit")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.SETTINGS + "')")
    public List<ConfigurationAuditEntryDto> getAuditLog() {
        return staffConfigService.getAuditLog();
    }

    @PutMapping("/room-units/{id}")
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_CATALOG + "') or @staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_OPERATIONS + "')")
    public RoomUnitConfigDto updateRoomUnit(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomUnitRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffConfigService.updateRoomUnit(id, request, staff);
    }
}
