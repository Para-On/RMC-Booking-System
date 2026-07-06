package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import java.time.Instant;

public record StaffUserDto(
        Long id,
        String email,
        String fullName,
        StaffRole role,
        boolean active,
        boolean mfaEnabled,
        Instant lastLoginAt,
        Instant createdAt) {
}
