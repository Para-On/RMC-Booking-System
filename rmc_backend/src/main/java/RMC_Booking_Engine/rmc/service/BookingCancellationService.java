package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.service.MayaRefundService.RefundExecution;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared cancel flow: free the room, snapshot policy, auto-reverse paid Maya on guest cancel,
 * and queue staff refund when money is owed (hotel folio / Maya retry).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingHoldService bookingHoldService;
    private final RefundPolicyService refundPolicyService;
    private final MayaRefundService mayaRefundService;
    private final ApplicationEventPublisher eventPublisher;
    private final PromoCodeService promoCodeService;
    private final EmailOutboxService emailOutboxService;

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
        if (previous == BookingStatus.PENDING_PAYMENT || previous == BookingStatus.PENDING_APPROVAL) {
            promoCodeService.releaseUsageIfAttached(booking);
        }
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

        boolean autoMaya = staffUserId == null
                && "GUEST_CANCEL_REFUND_PENDING".equals(refundPendingAuditTrigger)
                && booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                && refundEligible.compareTo(BigDecimal.ZERO) > 0
                && mayaRefundable.compareTo(BigDecimal.ZERO) > 0;

        boolean queueRefund = refundEligible.compareTo(BigDecimal.ZERO) > 0
                && mayaRefundable.compareTo(BigDecimal.ZERO) > 0
                && (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                        || booking.getPaymentMethod() == PaymentMethod.PAY_AT_HOTEL);

        booking.setRefundStatus(queueRefund ? RefundStatus.PENDING : RefundStatus.NOT_APPLICABLE);
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);

        if (autoMaya) {
            return initiateGuestMayaReverse(
                    booking,
                    previous,
                    trimmedReason,
                    refundPendingAuditTrigger,
                    refundEligible);
        }

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

    private CancelResult initiateGuestMayaReverse(
            Booking booking,
            BookingStatus previous,
            String trimmedReason,
            String refundPendingAuditTrigger,
            BigDecimal refundEligible) {
        RequestCorrelation.setBookingReference(booking.getReference());
        String mayaReason = trimmedReason != null ? trimmedReason : "Guest cancellation";
        try {
            RefundExecution refund = mayaRefundService.reversePayment(
                    booking, refundEligible, mayaReason, preferVoid(booking, refundEligible));
            markRefundProgress(booking, refund.amount());
            String trigger = refund.method() == MayaRefundService.ReversalMethod.VOID
                    ? "GUEST_CANCEL_VOID"
                    : "GUEST_CANCEL_REFUND";
            bookingHoldService.writeAuditLog(
                    booking,
                    previous.name(),
                    BookingStatus.CANCELLED.name(),
                    trigger,
                    null,
                    trimmedReason);
            if (booking.getRefundStatus() == RefundStatus.COMPLETED) {
                emailOutboxService.enqueueRefundProcessed(booking.getId(), refund.amount());
            }
            boolean pending = booking.getRefundStatus() == RefundStatus.PENDING;
            eventPublisher.publishEvent(new BookingCancelledEvent(booking.getId(), pending));
            log.info(
                    "Guest cancel auto-{} {} for booking {}",
                    refund.method().name().toLowerCase(),
                    refund.amount(),
                    booking.getReference());
            return new CancelResult(trigger, pending);
        } catch (BusinessException ex) {
            return handleGuestMayaReverseFailure(
                    booking, previous, trimmedReason, refundPendingAuditTrigger, ex);
        }
    }

    private CancelResult handleGuestMayaReverseFailure(
            Booking booking,
            BookingStatus previous,
            String trimmedReason,
            String refundPendingAuditTrigger,
            BusinessException ex) {
        boolean retryLater = MayaRefundService.isRetryLater(ex);
        booking.setRefundStatus(retryLater ? RefundStatus.PENDING : RefundStatus.FAILED);
        bookingRepository.save(booking);
        String trigger = retryLater ? refundPendingAuditTrigger : "GUEST_CANCEL_REFUND_FAILED";
        bookingHoldService.writeAuditLog(
                booking,
                previous.name(),
                BookingStatus.CANCELLED.name(),
                trigger,
                null,
                ex.getMessage());
        eventPublisher.publishEvent(new BookingCancelledEvent(booking.getId(), true));
        if (retryLater) {
            log.info(
                    "Guest cancel Maya reverse deferred for {}: {}",
                    booking.getReference(),
                    ex.getMessage());
        } else {
            log.warn(
                    "Guest cancel Maya reverse failed for {}: {}",
                    booking.getReference(),
                    ex.getMessage());
        }
        return new CancelResult(trigger, true);
    }

    /**
     * Retries a cancelled online-Maya booking whose refund is still {@code PENDING}
     * (same-day PY0045/PY0047 wait). Booking stays cancelled on failure.
     */
    @Transactional
    public boolean retryQueuedMayaRefund(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null
                || booking.getStatus() != BookingStatus.CANCELLED
                || booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA
                || booking.getRefundStatus() != RefundStatus.PENDING) {
            return false;
        }
        RequestCorrelation.setBookingReference(booking.getReference());
        List<BookingLedger> ledger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal remaining = mayaRefundService.remainingRefundDue(booking, ledger);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            booking.setRefundStatus(RefundStatus.COMPLETED);
            bookingRepository.save(booking);
            return true;
        }
        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledger);
        if (mayaRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal amount = remaining.min(mayaRefundable);
        var timing = mayaRefundService.assessRefundTiming(ledger, amount, mayaRefundable);
        if (!timing.canProcessNow()) {
            return false;
        }
        boolean preferVoid = booking.getCancellationTier() == CancellationTier.FULL
                && amount.compareTo(mayaRefundable) >= 0
                && mayaRefundService.isSamePaymentDay(ledger)
                && mayaRefundService.sumManualRefunds(ledger).compareTo(BigDecimal.ZERO) == 0;
        try {
            RefundExecution refund = mayaRefundService.reversePayment(
                    booking, amount, "Scheduled guest refund retry", preferVoid);
            markRefundProgress(booking, refund.amount());
            String trigger = refund.method() == MayaRefundService.ReversalMethod.VOID
                    ? "SCHEDULER_MAYA_VOID"
                    : "SCHEDULER_MAYA_REFUND";
            bookingHoldService.writeAuditLog(
                    booking,
                    BookingStatus.CANCELLED.name(),
                    BookingStatus.CANCELLED.name(),
                    trigger,
                    null,
                    null);
            if (booking.getRefundStatus() == RefundStatus.COMPLETED) {
                emailOutboxService.enqueueRefundProcessed(booking.getId(), refund.amount());
            }
            log.info(
                    "Scheduler auto-{} {} for booking {}",
                    refund.method().name().toLowerCase(),
                    refund.amount(),
                    booking.getReference());
            return booking.getRefundStatus() == RefundStatus.COMPLETED;
        } catch (BusinessException ex) {
            if (MayaRefundService.isRetryLater(ex)) {
                log.info("Scheduler Maya reverse still deferred for {}: {}", booking.getReference(), ex.getMessage());
                return false;
            }
            booking.setRefundStatus(RefundStatus.FAILED);
            bookingRepository.save(booking);
            bookingHoldService.writeAuditLog(
                    booking,
                    BookingStatus.CANCELLED.name(),
                    BookingStatus.CANCELLED.name(),
                    "SCHEDULER_MAYA_REFUND_FAILED",
                    null,
                    ex.getMessage());
            log.warn("Scheduler Maya reverse failed for {}: {}", booking.getReference(), ex.getMessage());
            return false;
        }
    }

    private boolean preferVoid(Booking booking, BigDecimal refundEligible) {
        List<BookingLedger> ledger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledger);
        return booking.getCancellationTier() == CancellationTier.FULL
                && refundEligible.compareTo(mayaRefundable) >= 0
                && mayaRefundService.isSamePaymentDay(ledger)
                && mayaRefundService.sumManualRefunds(ledger).compareTo(BigDecimal.ZERO) == 0;
    }

    private void markRefundProgress(Booking booking, BigDecimal refundedAmount) {
        List<BookingLedger> ledger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal remaining = mayaRefundService.remainingRefundDue(booking, ledger);
        booking.setRefundStatus(
                remaining.compareTo(BigDecimal.ZERO) <= 0 ? RefundStatus.COMPLETED : RefundStatus.PENDING);
        bookingRepository.save(booking);
        if (refundedAmount != null) {
            log.debug("Refund progress for {}: remaining {}", booking.getReference(), remaining);
        }
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
