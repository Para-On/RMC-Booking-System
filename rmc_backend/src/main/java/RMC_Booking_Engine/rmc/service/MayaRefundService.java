package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaRefundResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MayaRefundService {

    private final BookingLedgerRepository bookingLedgerRepository;
    private final MayaCheckoutClient mayaCheckoutClient;

    public record RefundExecution(BigDecimal amount, String mayaRefundId) {
    }

    public RefundExecution executeRefund(Booking booking, BigDecimal requestedAmount, String reason) {
        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            throw new BusinessException("Refunds are only supported for online Maya payments");
        }

        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal refundable = refundableAmount(ledgerEntries);
        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No refundable payment found on this booking");
        }

        BigDecimal refundAmount = requestedAmount != null ? requestedAmount : refundable;
        if (refundAmount.compareTo(refundable) > 0) {
            throw new BusinessException("Refund amount exceeds refundable balance of " + refundable);
        }

        String checkoutId = booking.getMayaCheckoutId();
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new BusinessException("No Maya checkout associated with this booking");
        }

        MayaRefundResponse mayaRefund = mayaCheckoutClient.refundCheckout(
                checkoutId, refundAmount, booking.getCurrency(), reason.trim());

        String idempotencyKey = "maya-refund-"
                + (mayaRefund.id() != null ? mayaRefund.id() : Instant.now().toEpochMilli());
        if (!bookingLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            BookingLedger refundEntry = new BookingLedger();
            refundEntry.setBooking(booking);
            refundEntry.setEntryType(LedgerEntryType.REFUND);
            refundEntry.setAmount(refundAmount);
            refundEntry.setIdempotencyKey(idempotencyKey);
            refundEntry.setMayaReference(mayaRefund.id());
            refundEntry.setCreatedAt(Instant.now());
            bookingLedgerRepository.save(refundEntry);
        }

        return new RefundExecution(refundAmount, mayaRefund.id());
    }

    public BigDecimal refundableAmount(List<BookingLedger> entries) {
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        BigDecimal refunds = sumByType(entries, LedgerEntryType.REFUND);
        return credits.subtract(refunds).max(BigDecimal.ZERO);
    }

    public BigDecimal calculateBalance(List<BookingLedger> entries) {
        BigDecimal debits = sumByType(entries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        BigDecimal refunds = sumByType(entries, LedgerEntryType.REFUND);
        return debits.subtract(credits).subtract(refunds);
    }

    private BigDecimal sumByType(List<BookingLedger> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
