package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryDto(
        String entryType,
        BigDecimal amount,
        Instant createdAt,
        String paymentReference) {
}
