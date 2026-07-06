package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.StaffSearchResultDto;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.StaffSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/search")
@RequiredArgsConstructor
public class StaffSearchController {

    private final StaffSearchService staffSearchService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccessAny(authentication, '"
            + StaffNavPaths.DASHBOARD + "', '"
            + StaffNavPaths.ARRIVALS + "', '"
            + StaffNavPaths.ROOMS_CATALOG + "', '"
            + StaffNavPaths.ROOMS_OPERATIONS + "', '"
            + StaffNavPaths.SETTINGS + "')")
    public List<StaffSearchResultDto> search(@RequestParam("q") String query) {
        return staffSearchService.search(query);
    }
}
