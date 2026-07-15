package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Shared cancel flow: free the room, snapshot policy, and queue a staff refund when money is owed
 * (Maya capture or hotel folio credits for pay-later).
 */
@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingHoldService bookingHoldService;
    private final RefundPolicyService refundPolicyService;
    private final MayaRefundService mayaRefundService;
    private final ApplicationEventPublisher eventPublisher;

    public record CancelResult(String auditTrigger, boolean refundPending) {}

    public CancelResult cancelBooking(
            Booking booking,
            String reason,
            Long staffUserId,
            String unpaidAuditTrigger,
            String refundPendingAuditTrigger) {
        return cancelBooking(
                booking,
                reason,
                staffUserId,
                unpaidAuditTrigger,
                refundPendingAuditTrigger,
                true);
    }

    /**
     * @param enforcePolicy when false (staff override), cancel is always allowed; refund may still be queued
     *                      from remaining paid balance if the guest policy window has passed
     */
    public CancelResult cancelBooking(
            Booking booking,
            String reason,
            Long staffUserId,
            String unpaidAuditTrigger,
            String refundPendingAuditTrigger,
            boolean enforcePolicy) {

        BookingStatus previous = booking.getStatus();
        if (previous == BookingStatus.CANCELLED) {
            return new CancelResult(null, booking.getRefundStatus() == RefundStatus.PENDING);
        }

        Instant now = Instant.now();
        String trimmedReason = reason != null && !reason.isBlank() ? reason.trim() : null;

        if (previous == BookingStatus.PENDING_PAYMENT) {
            return cancelUnpaid(booking, previous, now, trimmedReason, unpaidAuditTrigger, staffUserId);
        }

        if (previous == BookingStatus.PENDING_APPROVAL) {
            List<BookingLedger> ledger =
                    bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
            BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledger);
            if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                    && mayaRefundable.compareTo(BigDecimal.ZERO) > 0) {
                // Paid Maya awaiting approval — still apply cancellation / refund policy windows
                return cancelPaidWithPolicy(
                        booking,
                        previous,
                        now,
                        trimmedReason,
                        unpaidAuditTrigger,
                        refundPendingAuditTrigger,
                        staffUserId,
                        enforcePolicy,
                        ledger,
                        mayaRefundable);
            }
            return cancelUnpaid(booking, previous, now, trimmedReason, unpaidAuditTrigger, staffUserId);
        }

        if (previous != BookingStatus.CONFIRMED && previous != BookingStatus.CONFIRMED_PAY_LATER) {
            throw new BusinessException("Booking cannot be cancelled from status " + previous);
        }

        List<BookingLedger> ledger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledger);
        return cancelPaidWithPolicy(
                booking,
                previous,
                now,
                trimmedReason,
                unpaidAuditTrigger,
                refundPendingAuditTrigger,
                staffUserId,
                enforcePolicy,
                ledger,
                mayaRefundable);
    }

    private CancelResult cancelPaidWithPolicy(
            Booking booking,
            BookingStatus previous,
            Instant now,
            String trimmedReason,
            String unpaidAuditTrigger,
            String refundPendingAuditTrigger,
            Long staffUserId,
            boolean enforcePolicy,
            List<BookingLedger> ledger,
            BigDecimal mayaRefundable) {
        BigDecimal paidAmount = sumCredits(ledger);
        CancellationEvaluation evaluation = refundPolicyService.evaluateCancellation(booking, paidAmount);
        if (enforcePolicy && !evaluation.allowed()) {
            throw new BusinessException(
                    evaluation.blockReason() != null
                            ? evaluation.blockReason()
                            : "This booking cannot be cancelled online");
        }

        CancellationTier tier;
        BigDecimal refundEligible;
        Integer refundPercent;
        BigDecimal deduction;

        if (evaluation.allowed()) {
            tier = evaluation.tier() != null ? evaluation.tier() : CancellationTier.NONE;
            refundEligible = evaluation.refundEligibleAmount() != null
                    ? evaluation.refundEligibleAmount().min(mayaRefundable)
                    : BigDecimal.ZERO;
            refundPercent = evaluation.refundPercentApplied();
            deduction = evaluation.deductionAmount() != null
                    ? evaluation.deductionAmount()
                    : paidAmount.subtract(refundEligible).max(BigDecimal.ZERO);
        } else {
            // Staff force-cancel after check-in / outside window: no automatic refund entitlement
            tier = CancellationTier.NONE;
            refundEligible = BigDecimal.ZERO;
            refundPercent = 0;
            deduction = paidAmount;
        }

        return cancelPaidMaya(
                booking,
                previous,
                now,
                trimmedReason,
                unpaidAuditTrigger,
                refundPendingAuditTrigger,
                staffUserId,
                mayaRefundable,
                tier,
                refundEligible,
                refundPercent,
                deduction);
    }

    private CancelResult cancelUnpaid(
            Booking booking,
            BookingStatus previous,
            Instant now,
            String trimmedReason,
            String unpaidAuditTrigger,
            Long staffUserId) {
        applyCancelledShell(booking, now, trimmedReason);
        booking.setRefundStatus(RefundStatus.NOT_APPLICABLE);
        booking.setCancellationTier(CancellationTier.NONE);
        booking.setRefundEligibleAmount(BigDecimal.ZERO);
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);
        bookingHoldService.writeAuditLog(
                booking,
                previous.name(),
                BookingStatus.CANCELLED.name(),
                unpaidAuditTrigger,
                staffUserId,
                trimmedReason);
        eventPublisher.publishEvent(new BookingCancelledEvent(booking.getId(), false));
        return new CancelResult(unpaidAuditTrigger, false);
    }

    private CancelResult cancelPaidMaya(
            Booking booking,
            BookingStatus previous,
            Instant now,
            String trimmedReason,
            String unpaidAuditTrigger,
            String refundPendingAuditTrigger,
            Long staffUserId,
            BigDecimal mayaRefundable,
            CancellationTier tier,
            BigDecimal refundEligible,
            Integer refundPercent,
            BigDecimal deduction) {
        applyCancelledShell(booking, now, trimmedReason);
        booking.setCancellationTier(tier);
        booking.setRefundEligibleAmount(refundEligible);
        booking.setRefundPercentApplied(refundPercent);
        booking.setDeductionAmount(deduction);

        // Queue for Maya or pay-later when policy leaves a refundable paid balance (hotel credits count).
        boolean queueRefund = refundEligible.compareTo(BigDecimal.ZERO) > 0
                && mayaRefundable.compareTo(BigDecimal.ZERO) > 0
                && (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                        || booking.getPaymentMethod() == PaymentMethod.PAY_AT_HOTEL);

        booking.setRefundStatus(queueRefund ? RefundStatus.PENDING : RefundStatus.NOT_APPLICABLE);
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);

        String trigger = queueRefund ? refundPendingAuditTrigger : unpaidAuditTrigger;
        bookingHoldService.writeAuditLog(
                booking,
                previous.name(),
                BookingStatus.CANCELLED.name(),
                trigger,
                staffUserId,
                trimmedReason);
        eventPublisher.publishEvent(new BookingCancelledEvent(booking.getId(), queueRefund));
        return new CancelResult(trigger, queueRefund);
    }

    /**
     * Repairs paid bookings that were cancelled without queuing a refund (Maya or pay-later folio).
     */
    public boolean healStuckPendingRefund(Booking booking) {
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            return false;
        }
        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA
                && booking.getPaymentMethod() != PaymentMethod.PAY_AT_HOTEL) {
            return false;
        }
        RefundStatus status = booking.getRefundStatus();
        if (status == RefundStatus.PENDING
                || status == RefundStatus.FAILED
                || status == RefundStatus.COMPLETED) {
            return false;
        }

        List<BookingLedger> ledger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledger);
        if (mayaRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal alreadyDue = mayaRefundService.remainingRefundDue(booking, ledger);
        if (status == RefundStatus.NOT_APPLICABLE
                && alreadyDue.compareTo(BigDecimal.ZERO) > 0
                && booking.getRefundEligibleAmount() != null
                && booking.getRefundEligibleAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Previously cancelled pay-later (or stuck) with snapshot but never queued
            booking.setRefundStatus(RefundStatus.PENDING);
            bookingRepository.save(booking);
            return true;
        }

        if (status == RefundStatus.NOT_APPLICABLE) {
            return false;
        }

        BigDecimal paidAmount = sumCredits(ledger);
        CancellationEvaluation evaluation = refundPolicyService.evaluateCancellation(booking, paidAmount);
        if (!evaluation.allowed()
                || evaluation.refundEligibleAmount() == null
                || evaluation.refundEligibleAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal due = evaluation.refundEligibleAmount().min(mayaRefundable);
        if (booking.getCancellationTier() == null) {
            booking.setCancellationTier(
                    evaluation.tier() != null ? evaluation.tier() : CancellationTier.NONE);
        }
        if (booking.getRefundEligibleAmount() == null
                || booking.getRefundEligibleAmount().compareTo(BigDecimal.ZERO) <= 0) {
            booking.setRefundEligibleAmount(due);
        }
        if (booking.getRefundPercentApplied() == null) {
            booking.setRefundPercentApplied(evaluation.refundPercentApplied());
        }
        if (booking.getDeductionAmount() == null) {
            booking.setDeductionAmount(evaluation.deductionAmount());
        }
        if (booking.getCancelledAt() == null) {
            booking.setCancelledAt(Instant.now());
        }
        booking.setRefundStatus(RefundStatus.PENDING);
        bookingRepository.save(booking);
        return true;
    }

    private void applyCancelledShell(Booking booking, Instant now, String reason) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        if (reason != null) {
            booking.setCancellationReason(reason);
        }
    }

    private BigDecimal sumCredits(List<BookingLedger> entries) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
