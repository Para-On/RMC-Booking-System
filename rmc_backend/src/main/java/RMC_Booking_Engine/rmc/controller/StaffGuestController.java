package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.StaffGuestListResponse;
import RMC_Booking_Engine.rmc.dto.StaffGuestProfileResponse;
import RMC_Booking_Engine.rmc.security.RequireArrivalsAccess;
import RMC_Booking_Engine.rmc.service.StaffGuestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/guests")
public class StaffGuestController {

    private final StaffGuestService staffGuestService;

    public StaffGuestController(StaffGuestService staffGuestService) {
        this.staffGuestService = staffGuestService;
    }

    @RequireArrivalsAccess
    @GetMapping
    public StaffGuestListResponse listGuests(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return staffGuestService.listGuests(q, page, size);
    }

    @RequireArrivalsAccess
    @GetMapping("/{id}")
    public StaffGuestProfileResponse getGuest(@PathVariable Long id) {
        return staffGuestService.getGuestProfile(id);
    }
}
