package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckOutResponse(
        Long bookingId,
        String reference,
        Instant checkedOutAt,
        BigDecimal quotedTotal,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        String currency,
        List<LedgerEntryDto> ledger) {
}
