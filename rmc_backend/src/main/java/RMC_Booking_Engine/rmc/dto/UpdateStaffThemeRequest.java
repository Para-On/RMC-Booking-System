package RMC_Booking_Engine.rmc.dto;

import RMC_Booking_Engine.rmc.domain.enums.StaffThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffThemeRequest(@NotNull StaffThemePreference themePreference) {}
