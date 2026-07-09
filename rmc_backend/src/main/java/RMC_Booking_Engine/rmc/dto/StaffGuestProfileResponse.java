package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StaffGuestProfileResponse(
        Long guestId,
        String fullName,
        String email,
        String phone,
        Instant consentTimestamp,
        String dpaConsentVersion,
        Instant ageConfirmedAt,
        long totalBookings,
        long completedStays,
        BigDecimal totalSpent,
        BigDecimal completedStaysSpent,
        String currency,
        List<StaffGuestBookingHistoryItemDto> bookings) {}
