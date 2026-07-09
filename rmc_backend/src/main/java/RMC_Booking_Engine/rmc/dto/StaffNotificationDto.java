package RMC_Booking_Engine.rmc.dto;

import java.time.Instant;

public record StaffNotificationDto(
        Long id,
        String type,
        String title,
        String message,
        Long bookingId,
        String linkPath,
        Instant createdAt,
        boolean unread) {}
