package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.domain.enums.StaffThemePreference;

public record StaffProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        StaffRole role,
        StaffThemePreference themePreference,
        boolean mfaEnabled) {}
