package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.RoomCalendarResponse;
import RMC_Booking_Engine.rmc.dto.RoomDailyStatusPageResponse;
import RMC_Booking_Engine.rmc.dto.RoomUnitBookingPageResponse;
import RMC_Booking_Engine.rmc.security.StaffNavPaths;
import RMC_Booking_Engine.rmc.service.StaffRoomOccupancyService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/rooms")
@RequiredArgsConstructor
@PreAuthorize("@staffNavAccessService.canAccess(authentication, '" + StaffNavPaths.ROOMS_OPERATIONS + "')")
public class StaffRoomController {

    private final StaffRoomOccupancyService staffRoomOccupancyService;

    @GetMapping("/daily-status")
    public RoomDailyStatusPageResponse dailyStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String search) {
        return staffRoomOccupancyService.getDailyStatus(date, page, size, status, roomTypeId, search);
    }

    @GetMapping("/{roomUnitId}/calendar")
    public RoomCalendarResponse roomCalendar(
            @PathVariable Long roomUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return staffRoomOccupancyService.getRoomCalendar(roomUnitId, from, to);
    }

    @GetMapping("/{roomUnitId}/bookings")
    public RoomUnitBookingPageResponse roomBookings(
            @PathVariable Long roomUnitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return staffRoomOccupancyService.getRoomUnitBookings(roomUnitId, page, size);
    }
}
