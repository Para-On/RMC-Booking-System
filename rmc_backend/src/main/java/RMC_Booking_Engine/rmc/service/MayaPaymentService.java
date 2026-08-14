package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.obs.LogRedaction;
import RMC_Booking_Engine.rmc.obs.OpsAlertSignals;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingHoldService bookingHoldService;
    private final ApplicationEventPublisher eventPublisher;
    private final MayaCheckoutClient mayaCheckoutClient;
    private final MayaProperties mayaProperties;
    private final MayaRefundService mayaRefundService;
    private final BookingRefundPolicySnapshotService bookingRefundPolicySnapshotService;
    private final EntityManager entityManager;
    private final AdditionalChargeService additionalChargeService;
    private final PromoCodeService promoCodeService;
    private final OpsAlertSignals opsAlertSignals;
    private final MayaPaymentEvidenceService mayaPaymentEvidenceService;

    @Transactional
    public void handleWebhookPayload(MayaCheckoutStatus payload) {
        if (payload == null) {
            log.warn("Maya webhook ignored: empty payload");
            return;
        }
        processPaymentUpdate(payload, "MAYA_WEBHOOK", null);
    }

    @Transactional
    public void confirmPaymentByReference(String reference) {
        RequestCorrelation.setBookingReference(reference);
        Booking booking = bookingRepository.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            throw new BusinessException("This booking is not an online payment booking");
        }
        if (booking.getMayaCheckoutId() == null || booking.getMayaCheckoutId().isBlank()) {
            throw new BusinessException("No Maya checkout associated with this booking");
        }

        entityManager.refresh(booking);
        if (isPaidAwaitingOrConfirmed(booking)) {
            return;
        }

        MayaCheckoutStatus checkout;
        try {
            checkout = mayaCheckoutClient.getCheckout(booking.getMayaCheckoutId());
        } catch (BusinessException ex) {
            entityManager.refresh(booking);
            if (isPaidAwaitingOrConfirmed(booking)) {
                return;
            }
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT && isHoldExpired(booking)) {
                failBooking(booking, BookingStatus.FAILED, "MAYA_CONFIRM_POLL", "Checkout hold expired");
            } else {
                log.warn("Unable to poll Maya checkout for {}: {}", reference, ex.getMessage());
            }
            return;
        }

        processPaymentUpdate(checkout, "MAYA_CONFIRM_POLL", reference);
    }

    private void processPaymentUpdate(MayaCheckoutStatus payload, String trigger, String fallbackReference) {
        try {
            mayaPaymentEvidenceService.record(payload, trigger, fallbackReference);
        } catch (Exception ex) {
            log.error(
                    "Maya payment evidence failed [{}]",
                    RequestCorrelation.describe(),
                    LogRedaction.forLogging(ex));
        }

        if (additionalChargeService.tryHandleMayaPayload(payload, trigger)) {
            return;
        }

        String reference = resolveReference(payload, fallbackReference);
        RequestCorrelation.setBookingReference(reference);
        if (reference == null) {
            log.warn("Maya update ignored: missing requestReferenceNumber");
            return;
        }

        if (reference.startsWith(AdditionalChargeService.MAYA_REQUEST_PREFIX)) {
            log.warn("Maya update for unknown additional charge reference {}", reference);
            return;
        }

        Booking booking = bookingRepository.findByReferenceForUpdate(reference).orElse(null);
        if (booking == null) {
            log.warn("Maya update for unknown booking reference {}", reference);
            return;
        }
        entityManager.refresh(booking);

        if (isPaidAwaitingOrConfirmed(booking)) {
            return;
        }

        if (payload.isPaymentSuccessful()) {
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                markPaidAwaitingApproval(booking, payload, reference, trigger);
            } else {
                handleLateSuccessfulPayment(booking, payload, reference, trigger);
            }
            return;
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        if (payload.isPaymentFailed()) {
            failBooking(booking, BookingStatus.FAILED, trigger, payload.resolvedFailureReason());
        } else if (payload.isCheckoutCancelled()) {
            failBooking(booking, BookingStatus.CANCELLED, trigger, payload.resolvedFailureReason());
        } else if (isHoldExpired(booking)) {
            failBooking(booking, BookingStatus.FAILED, trigger, "Checkout hold expired");
        } else {
            log.info("Maya payment still pending: status={} paymentStatus={} ref={}",
                    payload.status(), payload.paymentStatus(), reference);
        }
    }

    private void markPaidAwaitingApproval(
            Booking booking, MayaCheckoutStatus payload, String reference, String trigger) {
        BigDecimal receivedAmount = payload.resolvedAmount();
        if (receivedAmount != null && !amountsMatch(receivedAmount, booking.getQuotedTotal())) {
            log.error("Maya amount mismatch for {}: expected {} got {}",
                    reference, booking.getQuotedTotal(), receivedAmount);
            opsAlertSignals.webhookFailure(reference, "amount mismatch");
            return;
        }

        String checkoutId = resolveCheckoutId(booking, payload);
        recordMayaCreditIfAbsent(booking, checkoutId);

        entityManager.refresh(booking);
        if (isPaidAwaitingOrConfirmed(booking)) {
            log.debug("Booking {} already paid/confirmed; skipping duplicate Maya success ({})", reference, trigger);
            return;
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.PENDING_APPROVAL);
        bookingHoldService.clearPaymentHoldExpiry(booking);
        bookingRefundPolicySnapshotService.attachSnapshotIfAbsent(booking, booking.getRoomType());
        bookingRepository.save(booking);

        bookingHoldService.writeAuditLog(
                booking, previous.name(), BookingStatus.PENDING_APPROVAL.name(), trigger, null, null);
        log.info("Booking {} paid via {}; awaiting staff approval", reference, trigger);
        eventPublisher.publishEvent(new BookingPendingApprovalEvent(booking.getId()));
    }

    private void handleLateSuccessfulPayment(
            Booking booking,
            MayaCheckoutStatus payload,
            String reference,
            String trigger) {
        BigDecimal receivedAmount = payload.resolvedAmount();
        if (receivedAmount != null && !amountsMatch(receivedAmount, booking.getQuotedTotal())) {
            log.error("Late Maya amount mismatch for {}: expected {} got {}",
                    reference, booking.getQuotedTotal(), receivedAmount);
            opsAlertSignals.webhookFailure(reference, "late amount mismatch");
            return;
        }

        String checkoutId = resolveCheckoutId(booking, payload);
        recordMayaCreditIfAbsent(booking, checkoutId);

        entityManager.refresh(booking);
        if (isPaidAwaitingOrConfirmed(booking)) {
            return;
        }

        BookingStatus previous = booking.getStatus();
        try {
            bookingHoldService.restoreHolds(booking);
        } catch (BusinessException ex) {
            log.warn("Late payment for {} but room unavailable: {}", reference, ex.getMessage());
            refundLatePayment(booking, reference, previous, ex.getMessage());
            return;
        }

        booking.setStatus(BookingStatus.PENDING_APPROVAL);
        bookingHoldService.clearPaymentHoldExpiry(booking);
        bookingRefundPolicySnapshotService.attachSnapshotIfAbsent(booking, booking.getRoomType());
        bookingRepository.save(booking);
        bookingHoldService.writeAuditLog(
                booking,
                previous.name(),
                BookingStatus.PENDING_APPROVAL.name(),
                "LATE_PAYMENT_AWAITING_APPROVAL",
                null,
                trigger);
        log.info("Booking {} paid via late payment ({}); awaiting staff approval", reference, trigger);
        eventPublisher.publishEvent(new BookingPendingApprovalEvent(booking.getId()));
    }

    private boolean isPaidAwaitingOrConfirmed(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || (booking.getStatus() == BookingStatus.PENDING_APPROVAL
                        && booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA);
    }

    private void refundLatePayment(Booking booking, String reference, BookingStatus previous, String reason) {
        try {
            mayaRefundService.executeRefund(booking, null, "Late payment - room no longer available");
            bookingHoldService.writeAuditLog(
                    booking,
                    previous.name(),
                    previous.name(),
                    "LATE_PAYMENT_REFUND",
                    null,
                    reason);
            log.info("Refunded late payment for booking {}", reference);
        } catch (Exception ex) {
            log.error("Failed to refund late payment for {}: {}", reference, ex.getMessage());
            bookingHoldService.writeAuditLog(
                    booking,
                    previous.name(),
                    previous.name(),
                    "LATE_PAYMENT_REFUND_FAILED",
                    null,
                    ex.getMessage());
        }
    }

    private String resolveCheckoutId(Booking booking, MayaCheckoutStatus payload) {
        String checkoutId = booking.getMayaCheckoutId();
        if (checkoutId == null || checkoutId.isBlank()) {
            checkoutId = payload.resolvedId();
        }
        return checkoutId;
    }

    private void recordMayaCreditIfAbsent(Booking booking, String checkoutId) {
        if (checkoutId == null || checkoutId.isBlank()) {
            return;
        }
        String creditKey = "maya-credit-" + checkoutId;
        if (bookingLedgerRepository.existsByIdempotencyKey(creditKey)
                || bookingLedgerRepository.existsByBookingIdAndEntryType(booking.getId(), LedgerEntryType.CREDIT)) {
            return;
        }

        BookingLedger credit = new BookingLedger();
        credit.setBooking(booking);
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(booking.getQuotedTotal());
        credit.setIdempotencyKey(creditKey);
        credit.setMayaReference(checkoutId);
        credit.setCreatedAt(Instant.now());
        try {
            bookingLedgerRepository.saveAndFlush(credit);
        } catch (DataIntegrityViolationException ex) {
            entityManager.detach(credit);
            log.info("Idempotent skip duplicate Maya credit ledger for {}", creditKey);
        }
    }

    private void failBooking(Booking booking, BookingStatus targetStatus, String trigger, String reason) {
        BookingStatus previous = booking.getStatus();
        booking.setStatus(targetStatus);
        if (previous == BookingStatus.PENDING_PAYMENT || previous == BookingStatus.PENDING_APPROVAL) {
            promoCodeService.releaseUsageIfAttached(booking);
        }
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);
        bookingHoldService.writeAuditLog(booking, previous.name(), targetStatus.name(), trigger, null, reason);
        log.info("Booking {} marked {} via {} ({})", booking.getReference(), targetStatus, trigger, reason);
    }

    private String resolveReference(MayaCheckoutStatus payload, String fallbackReference) {
        String reference = payload.requestReferenceNumber();
        if (reference == null || reference.isBlank()) {
            reference = fallbackReference;
        }
        return (reference == null || reference.isBlank()) ? null : reference;
    }

    private boolean isHoldExpired(Booking booking) {
        return booking.getExpiresAt() != null && Instant.now().isAfter(booking.getExpiresAt());
    }

    private boolean amountsMatch(BigDecimal received, BigDecimal expected) {
        if (received == null || expected == null) {
            return false;
        }
        return received.compareTo(expected) == 0;
    }

    public void assertMayaConfigured() {
        if (!mayaProperties.isConfigured()) {
            throw new BusinessException("Online payment is not configured. Please choose pay at hotel.");
        }
    }
}
