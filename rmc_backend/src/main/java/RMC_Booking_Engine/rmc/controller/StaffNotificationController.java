package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.StaffNotificationsResponse;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/notifications")
@RequiredArgsConstructor
public class StaffNotificationController {

    private final StaffNotificationService staffNotificationService;

    @GetMapping
    public StaffNotificationsResponse list(@AuthenticationPrincipal StaffPrincipal staff) {
        return staffNotificationService.listForStaff(staff.id());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal StaffPrincipal staff) {
        return new UnreadCountResponse(staffNotificationService.countUnread(staff.id()));
    }

    @PostMapping("/mark-seen")
    public StaffNotificationsResponse markSeen(@AuthenticationPrincipal StaffPrincipal staff) {
        return staffNotificationService.markAllSeen(staff.id());
    }

    public record UnreadCountResponse(long unreadCount) {}
}
