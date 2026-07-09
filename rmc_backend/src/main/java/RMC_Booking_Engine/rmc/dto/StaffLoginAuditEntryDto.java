package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffLoginEvent;
import RMC_Booking_Engine.rmc.domain.enums.StaffLoginStatus;
import java.time.Instant;

public record StaffLoginAuditEntryDto(
        Long id,
        Instant createdAt,
        String fullName,
        String email,
        StaffLoginEvent event,
        StaffLoginStatus status,
        String ipAddress,
        String failureReason) {
}
