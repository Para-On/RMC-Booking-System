package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
        Booking booking = bookingRepository.findByReferenceWithDetails(reference)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            throw new BusinessException("This booking is not an online payment booking");
        }
        if (booking.getMayaCheckoutId() == null || booking.getMayaCheckoutId().isBlank()) {
            throw new BusinessException("No Maya checkout associated with this booking");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.FAILED
                || booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        MayaCheckoutStatus checkout;
        try {
            checkout = mayaCheckoutClient.getCheckout(booking.getMayaCheckoutId());
        } catch (BusinessException ex) {
            if (isHoldExpired(booking)) {
                failBooking(booking, BookingStatus.FAILED, "MAYA_CONFIRM_POLL", "Checkout hold expired");
            } else {
                log.warn("Unable to poll Maya checkout for {}: {}", reference, ex.getMessage());
            }
            return;
        }

        processPaymentUpdate(checkout, "MAYA_CONFIRM_POLL", reference);
    }

    private void processPaymentUpdate(MayaCheckoutStatus payload, String trigger, String fallbackReference) {
        String reference = resolveReference(payload, fallbackReference);
        if (reference == null) {
            log.warn("Maya update ignored: missing requestReferenceNumber");
            return;
        }

        Booking booking = bookingRepository.findByReferenceForUpdate(reference).orElse(null);
        if (booking == null) {
            log.warn("Maya update for unknown booking reference {}", reference);
            return;
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        if (payload.isPaymentSuccessful()) {
            confirmBooking(booking, payload, reference, trigger);
        } else if (payload.isPaymentFailed()) {
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

    private void confirmBooking(Booking booking, MayaCheckoutStatus payload, String reference, String trigger) {
        BigDecimal receivedAmount = payload.resolvedAmount();
        if (receivedAmount != null && !amountsMatch(receivedAmount, booking.getQuotedTotal())) {
            log.error("Maya amount mismatch for {}: expected {} got {}",
                    reference, booking.getQuotedTotal(), receivedAmount);
            return;
        }

        String checkoutId = payload.resolvedId() != null ? payload.resolvedId() : booking.getMayaCheckoutId();
        String creditKey = "maya-credit-" + checkoutId;
        if (!bookingLedgerRepository.existsByIdempotencyKey(creditKey)) {
            BookingLedger credit = new BookingLedger();
            credit.setBooking(booking);
            credit.setEntryType(LedgerEntryType.CREDIT);
            credit.setAmount(booking.getQuotedTotal());
            credit.setIdempotencyKey(creditKey);
            credit.setMayaReference(checkoutId);
            credit.setCreatedAt(Instant.now());
            bookingLedgerRepository.save(credit);
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        bookingHoldService.writeAuditLog(booking, previous.name(), BookingStatus.CONFIRMED.name(), trigger, null, null);
        log.info("Booking {} confirmed via {}", reference, trigger);
        eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
    }

    private void failBooking(Booking booking, BookingStatus targetStatus, String trigger, String reason) {
        BookingStatus previous = booking.getStatus();
        booking.setStatus(targetStatus);
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
