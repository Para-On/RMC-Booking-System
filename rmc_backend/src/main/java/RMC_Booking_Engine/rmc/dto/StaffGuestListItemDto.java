package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record StaffGuestListItemDto(
        Long guestId,
        String fullName,
        String email,
        String phone,
        long totalBookings,
        long completedStays,
        BigDecimal totalSpent,
        String currency) {}
