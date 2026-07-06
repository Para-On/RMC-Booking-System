package RMC_Booking_Engine.rmc.dto;

import java.time.Instant;

public record ConfigurationAuditEntryDto(
        Long id,
        String entityType,
        Long entityId,
        String configKey,
        String oldValue,
        String newValue,
        Long staffUserId,
        String staffEmail,
        Instant createdAt) {
}
