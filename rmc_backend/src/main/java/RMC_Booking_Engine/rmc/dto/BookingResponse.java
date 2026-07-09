package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        String reference,
        String status,
        String paymentMethod,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal quotedTotal,
        String currency,
        String guestName,
        String guestEmail,
        String roomTypeName,
        List<NightlyRateDto> nightlyBreakdown,
        String checkoutRedirectUrl,
        RoomCatalogCardDto catalog,
        List<BookingServiceSelectionDto> serviceAddons,
        List<BookingItemSelectionDto> itemAddons,
        String customExtrasRequest,
        String cancellationTier,
        String refundStatus,
        BigDecimal refundEligibleAmount,
        String cancellationMessage,
        String refundPolicyDescription,
        String refundPreview,
        Integer refundPercentApplied,
        BigDecimal deductionAmount,
        String cancellationReason) {
}
