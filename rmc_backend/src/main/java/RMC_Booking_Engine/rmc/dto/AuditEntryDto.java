package RMC_Booking_Engine.rmc.dto;

import java.time.Instant;

public record AuditEntryDto(
        String fromStatus,
        String toStatus,
        String triggerSource,
        String reason,
        Instant createdAt) {
}
