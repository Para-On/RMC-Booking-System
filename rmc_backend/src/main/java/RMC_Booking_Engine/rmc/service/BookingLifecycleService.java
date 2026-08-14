package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleService {

    private static final int HOLD_EXTENSION_MINUTES = 15;

    private static final Set<BookingStatus> NO_SHOW_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingHoldService bookingHoldService;
    private final BookingCancellationService bookingCancellationService;
    private final MayaCheckoutClient mayaCheckoutClient;
    private final MayaPaymentService mayaPaymentService;
    private final PromoCodeService promoCodeService;

    @Transactional
    public int expirePendingPayments() {
        Instant now = Instant.now();
        int count = 0;
        for (Booking booking : bookingRepository.findExpiredByStatus(BookingStatus.PENDING_PAYMENT, now)) {
            RequestCorrelation.setBookingReference(booking.getReference());
            if (tryReconcileOrExtendMayaHold(booking)) {
                continue;
            }
            transition(booking, BookingStatus.FAILED, "SCHEDULER_HOLD_EXPIRY", "Payment hold expired");
            count++;
        }
        if (count > 0) {
            log.info("Expired {} pending-payment booking(s)", count);
        }
        return count;
    }

    private boolean tryReconcileOrExtendMayaHold(Booking booking) {
        String checkoutId = booking.getMayaCheckoutId();
        if (checkoutId == null || checkoutId.isBlank()) {
            return false;
        }
        try {
            MayaCheckoutStatus checkout = mayaCheckoutClient.getCheckout(checkoutId);
            if (checkout.isPaymentSuccessful()) {
                mayaPaymentService.handleWebhookPayload(checkout);
                log.info("Reconciled successful Maya payment during hold expiry for {}", booking.getReference());
                return true;
            }
            if (!checkout.isPaymentFailed() && !checkout.isCheckoutCancelled()) {
                bookingHoldService.extendPaymentHold(booking, HOLD_EXTENSION_MINUTES);
                bookingRepository.save(booking);
                log.info("Extended payment hold for booking {}", booking.getReference());
                return true;
            }
        } catch (BusinessException ex) {
            log.warn("Maya poll failed during hold expiry for {}: {}", booking.getReference(), ex.getMessage());
        }
        return false;
    }

    @Transactional
    public int applyPayLaterCutoffs() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Booking booking : bookingRepository.findUncheckedInByStatus(BookingStatus.CONFIRMED_PAY_LATER)) {
            if (!booking.getCheckInDate().isBefore(today)) {
                continue;
            }
            bookingCancellationService.cancelBooking(
                    booking,
                    "Pay-later booking past check-in date",
                    null,
                    "SCHEDULER_PAY_LATER_CUTOFF",
                    "SCHEDULER_PAY_LATER_CUTOFF_REFUND_PENDING",
                    false);
            count++;
        }
        for (Booking booking : bookingRepository.findUncheckedInByStatus(BookingStatus.PENDING_APPROVAL)) {
            if (!booking.getCheckInDate().isBefore(today)) {
                continue;
            }
            bookingCancellationService.cancelBooking(
                    booking,
                    "Pending approval booking past check-in date",
                    null,
                    "SCHEDULER_PAY_LATER_CUTOFF",
                    "SCHEDULER_PAY_LATER_CUTOFF_REFUND_PENDING",
                    false);
            count++;
        }
        if (count > 0) {
            log.info("Cancelled {} pay-later booking(s) past check-in", count);
        }
        return count;
    }

    @Transactional
    public int markNoShows() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Booking booking : bookingRepository.findUncheckedInBefore(NO_SHOW_STATUSES, today)) {
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            transition(booking, BookingStatus.NO_SHOW, "SCHEDULER_NO_SHOW", "Guest did not check in");
            addNoShowLedgerEntry(booking);
            count++;
        }
        if (count > 0) {
            log.info("Marked {} booking(s) as no-show", count);
        }
        return count;
    }

    @Transactional
    public int retryPendingMayaRefunds() {
        List<Long> ids = bookingRepository.findIdsByStatusAndPaymentMethodAndRefundStatus(
                BookingStatus.CANCELLED, PaymentMethod.ONLINE_MAYA, RefundStatus.PENDING);
        int completed = 0;
        for (Long id : ids) {
            try {
                if (bookingCancellationService.retryQueuedMayaRefund(id)) {
                    completed++;
                }
            } catch (Exception ex) {
                log.warn("Maya refund retry failed for booking id={}: {}", id, ex.getMessage());
            }
        }
        if (completed > 0) {
            log.info("Completed {} pending Maya refund(s) after cutoff", completed);
        }
        return completed;
    }

    private void transition(Booking booking, BookingStatus targetStatus, String trigger, String reason) {
        RequestCorrelation.setBookingReference(booking.getReference());
        if (booking.getStatus() == targetStatus) {
            return;
        }
        BookingStatus previous = booking.getStatus();
        booking.setStatus(targetStatus);
        if (targetStatus == BookingStatus.FAILED
                && (previous == BookingStatus.PENDING_PAYMENT || previous == BookingStatus.PENDING_APPROVAL)) {
            promoCodeService.releaseUsageIfAttached(booking);
        }
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);
        bookingHoldService.writeAuditLog(booking, previous.name(), targetStatus.name(), trigger, null, reason);
    }

    private void addNoShowLedgerEntry(Booking booking) {
        String key = "noshow-" + booking.getReference();
        if (bookingLedgerRepository.existsByIdempotencyKey(key)) {
            return;
        }
        BookingLedger entry = new BookingLedger();
        entry.setBooking(booking);
        entry.setEntryType(LedgerEntryType.NO_SHOW_RECORD);
        entry.setAmount(BigDecimal.ZERO);
        entry.setIdempotencyKey(key);
        entry.setCreatedAt(Instant.now());
        bookingLedgerRepository.save(entry);
    }
}
