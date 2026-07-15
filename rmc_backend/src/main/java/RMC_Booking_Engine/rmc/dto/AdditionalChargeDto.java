package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdditionalChargeDto(
        Long id,
        String description,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        Instant createdAt,
        Instant paidAt,
        Instant approvedAt,
        String rejectionReason,
        boolean canPayMaya,
        boolean canRequestPayAtHotel,
        String checkoutRedirectUrl) {}
