package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffActivityAction;
import java.time.Instant;

public record StaffActivityAuditEntryDto(
        Long id,
        Instant createdAt,
        String fullName,
        String role,
        StaffActivityAction action,
        String moduleKey,
        String moduleLabel,
        String requestMethod,
        String requestPath) {
}
