package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaRefundResponse;
import RMC_Booking_Engine.rmc.dto.MayaVoidResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaRefundService {

    private static final ZoneId PROPERTY_ZONE = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final BookingLedgerRepository bookingLedgerRepository;
    private final MayaCheckoutClient mayaCheckoutClient;

    public enum ReversalMethod {
        VOID,
        REFUND
    }

    public record RefundExecution(BigDecimal amount, String mayaReferenceId, ReversalMethod method) {
    }

    public record MayaRefundTiming(
            boolean canProcessNow,
            String blockedReason,
            Instant availableAt,
            ReversalMethod methodWhenAllowed) {
    }

    public RefundExecution executeRefund(Booking booking, BigDecimal requestedAmount, String reason) {
        return reversePayment(booking, requestedAmount, reason, false);
    }

    public RefundExecution reversePayment(
            Booking booking,
            BigDecimal requestedAmount,
            String reason,
            boolean preferVoid) {
        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            throw new BusinessException("Refunds are only supported for online Maya payments");
        }

        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal mayaRefundable = mayaRefundableAmount(ledgerEntries);
        if (mayaRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No refundable Maya payment remains on this booking");
        }

        BigDecimal refundAmount = requestedAmount != null ? requestedAmount : mayaRefundable;
        if (refundAmount.compareTo(mayaRefundable) > 0) {
            throw new BusinessException("Refund amount exceeds Maya refundable balance of " + mayaRefundable);
        }

        MayaRefundTiming timing = assessRefundTiming(ledgerEntries, refundAmount, mayaRefundable);
        if (!timing.canProcessNow()) {
            throw new BusinessException(timing.blockedReason());
        }

        String checkoutId = booking.getMayaCheckoutId();
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new BusinessException("No Maya checkout associated with this booking");
        }

        boolean useVoid = preferVoid
                && isSamePaymentDay(ledgerEntries)
                && refundAmount.compareTo(mayaRefundable) >= 0
                && sumByType(ledgerEntries, LedgerEntryType.MANUAL_REFUND).compareTo(BigDecimal.ZERO) == 0;

        if (useVoid) {
            try {
                MayaVoidResponse voidResponse = mayaCheckoutClient.voidCheckout(checkoutId, reason.trim());
                recordLedgerRefund(booking, refundAmount, voidResponse.id(), "maya-void-", LedgerEntryType.REFUND);
                return new RefundExecution(refundAmount, voidResponse.id(), ReversalMethod.VOID);
            } catch (BusinessException voidEx) {
                if (!isVoidUnavailable(voidEx)) {
                    throw voidEx;
                }
                log.warn(
                        "Maya void unavailable for checkout {}; checking refund eligibility: {}",
                        checkoutId,
                        voidEx.getMessage());
                // Maya: refunds only after 12:00 AM Asia/Manila on the day after payment.
                if (isSamePaymentDay(ledgerEntries)) {
                    throw sameDayRefundNotReady(ledgerEntries);
                }
            }
        }

        try {
            return executeMayaRefund(booking, checkoutId, refundAmount, reason.trim());
        } catch (BusinessException refundEx) {
            if (isRefundNotYetEligible(refundEx)) {
                throw sameDayRefundNotReady(ledgerEntries);
            }
            throw refundEx;
        }
    }

    private BusinessException sameDayRefundNotReady(List<BookingLedger> ledgerEntries) {
        Instant availableAt = refundAvailableAt(ledgerEntries);
        String availableLabel = ZonedDateTime.ofInstant(availableAt, PROPERTY_ZONE).format(DISPLAY_FORMAT);
        return new BusinessException(
                "Maya void is not available for this payment, and API refunds are only allowed after "
                        + availableLabel
                        + " (Asia/Manila). Leave the booking as PENDING and retry then, "
                        + "or process the refund in Maya Business Manager.");
    }

    private RefundExecution executeMayaRefund(
            Booking booking, String checkoutId, BigDecimal refundAmount, String reason) {
        MayaRefundResponse mayaRefund = mayaCheckoutClient.refundCheckout(
                checkoutId, refundAmount, booking.getCurrency(), reason);
        recordLedgerRefund(booking, refundAmount, mayaRefund.id(), "maya-refund-", LedgerEntryType.REFUND);
        return new RefundExecution(refundAmount, mayaRefund.id(), ReversalMethod.REFUND);
    }

    /** True when Maya rejected void but a refund may still succeed later (e.g. PY0045). */
    static boolean isVoidUnavailable(BusinessException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("py0045") || lower.contains("not available for void");
    }

    /** True when Maya rejected refund as not yet eligible (e.g. PY0047 same-day). */
    static boolean isRefundNotYetEligible(BusinessException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("py0047") || lower.contains("ineligible for refund");
    }

    /** True when the caller should leave refund PENDING and retry after the Manila cutoff. */
    public static boolean isRetryLater(BusinessException ex) {
        if (ex == null) {
            return false;
        }
        if (isRefundNotYetEligible(ex)) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("only allowed after")
                || lower.contains("partial maya refunds are not available on the same day");
    }

    public MayaRefundTiming assessRefundTiming(
            List<BookingLedger> entries, BigDecimal refundAmount, BigDecimal mayaRefundableAmount) {
        BigDecimal mayaRefundable = mayaRefundableAmount != null ? mayaRefundableAmount : mayaRefundableAmount(entries);
        BigDecimal amount = refundAmount != null ? refundAmount : mayaRefundable;
        boolean sameDay = isSamePaymentDay(entries);
        boolean fullMayaAmount = amount.compareTo(mayaRefundable) >= 0;

        if (sameDay && fullMayaAmount && sumByType(entries, LedgerEntryType.MANUAL_REFUND).compareTo(BigDecimal.ZERO) == 0) {
            return new MayaRefundTiming(true, null, null, ReversalMethod.VOID);
        }
        if (sameDay && !fullMayaAmount) {
            Instant availableAt = refundAvailableAt(entries);
            String availableLabel = ZonedDateTime.ofInstant(availableAt, PROPERTY_ZONE).format(DISPLAY_FORMAT);
            return new MayaRefundTiming(
                    false,
                    "Partial Maya refunds are not available on the same day as payment. "
                            + "Maya only supports full voids same day. Try again after "
                            + availableLabel
                            + " (Asia/Manila), or record a manual refund if enabled.",
                    availableAt,
                    ReversalMethod.REFUND);
        }
        return new MayaRefundTiming(true, null, null, ReversalMethod.REFUND);
    }

    public Instant refundAvailableAt(List<BookingLedger> entries) {
        return firstCreditAt(entries)
                .map(paymentAt -> paymentAt.atZone(PROPERTY_ZONE).toLocalDate().plusDays(1)
                        .atStartOfDay(PROPERTY_ZONE)
                        .toInstant())
                .orElse(Instant.now());
    }

    public void recordManualRefund(
            Booking booking, BigDecimal refundAmount, String externalReference, String idempotencySuffix) {
        String idempotencyKey = "manual-refund-" + booking.getId() + "-" + idempotencySuffix;
        if (bookingLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        BookingLedger refundEntry = new BookingLedger();
        refundEntry.setBooking(booking);
        refundEntry.setEntryType(LedgerEntryType.MANUAL_REFUND);
        refundEntry.setAmount(refundAmount);
        refundEntry.setIdempotencyKey(idempotencyKey);
        refundEntry.setMayaReference(externalReference);
        refundEntry.setCreatedAt(Instant.now());
        bookingLedgerRepository.save(refundEntry);
    }

    public boolean isSamePaymentDay(List<BookingLedger> entries) {
        Optional<Instant> paymentAt = firstCreditAt(entries);
        if (paymentAt.isEmpty()) {
            return false;
        }
        LocalDate paymentDate = paymentAt.get().atZone(PROPERTY_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(PROPERTY_ZONE);
        return paymentDate.equals(today);
    }

    public Optional<Instant> firstCreditAt(List<BookingLedger> entries) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .map(BookingLedger::getCreatedAt)
                .findFirst();
    }

    /** Amount still refundable via Maya API (excludes manual refunds). */
    public BigDecimal mayaRefundableAmount(List<BookingLedger> entries) {
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        BigDecimal mayaRefunds = sumByType(entries, LedgerEntryType.REFUND);
        return credits.subtract(mayaRefunds).max(BigDecimal.ZERO);
    }

    public BigDecimal refundableAmount(List<BookingLedger> entries) {
        return mayaRefundableAmount(entries);
    }

    public BigDecimal sumPolicyRefunds(List<BookingLedger> entries) {
        return sumByType(entries, LedgerEntryType.REFUND)
                .add(sumByType(entries, LedgerEntryType.MANUAL_REFUND));
    }

    public BigDecimal sumManualRefunds(List<BookingLedger> entries) {
        return sumByType(entries, LedgerEntryType.MANUAL_REFUND);
    }

    public BigDecimal calculateBalance(List<BookingLedger> entries) {
        BigDecimal debits = sumByType(entries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        BigDecimal refunds = sumPolicyRefunds(entries);
        return debits.subtract(credits).subtract(refunds);
    }

    public BigDecimal remainingRefundDue(Booking booking, List<BookingLedger> entries) {
        if (booking.getRefundEligibleAmount() == null) {
            return mayaRefundableAmount(entries);
        }
        BigDecimal alreadyRefunded = sumPolicyRefunds(entries);
        return booking.getRefundEligibleAmount().subtract(alreadyRefunded).max(BigDecimal.ZERO);
    }

    private void recordLedgerRefund(
            Booking booking,
            BigDecimal refundAmount,
            String mayaRef,
            String idempotencyPrefix,
            LedgerEntryType entryType) {
        String idempotencyKey = idempotencyPrefix
                + (mayaRef != null ? mayaRef : String.valueOf(Instant.now().toEpochMilli()));
        if (!bookingLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            BookingLedger refundEntry = new BookingLedger();
            refundEntry.setBooking(booking);
            refundEntry.setEntryType(entryType);
            refundEntry.setAmount(refundAmount);
            refundEntry.setIdempotencyKey(idempotencyKey);
            refundEntry.setMayaReference(mayaRef);
            refundEntry.setCreatedAt(Instant.now());
            bookingLedgerRepository.save(refundEntry);
        }
    }

    private BigDecimal sumByType(List<BookingLedger> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
