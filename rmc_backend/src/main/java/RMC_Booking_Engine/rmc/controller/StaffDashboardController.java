package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.StaffDashboardResponse;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.StaffDashboardService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/dashboard")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffDashboardService staffDashboardService;

    @GetMapping
    @PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.DASHBOARD + "')")
    public StaffDashboardResponse getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate from = fromDate != null ? fromDate : date;
        LocalDate to = toDate != null ? toDate : (fromDate != null ? fromDate : date);
        return staffDashboardService.getDashboard(from, to);
    }
}
