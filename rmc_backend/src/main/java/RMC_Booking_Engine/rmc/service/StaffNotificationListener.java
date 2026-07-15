package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffNotificationListener {

    private final StaffNotificationService staffNotificationService;
    private final BookingRepository bookingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            Booking booking = bookingRepository
                    .findByIdWithDetails(event.bookingId())
                    .orElse(null);
            if (booking == null) {
                return;
            }
            // Maya payment was already notified at PENDING_APPROVAL; skip duplicate "payment received"
            if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA) {
                return;
            }
            staffNotificationService.notifyBookingReceived(event.bookingId(), false);
        } catch (Exception ex) {
            log.warn(
                    "Staff notification failed for confirmed booking {}: {}",
                    event.bookingId(),
                    ex.getMessage(),
                    ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingPendingApproval(BookingPendingApprovalEvent event) {
        try {
            staffNotificationService.notifyBookingAwaitingApproval(event.bookingId());
        } catch (Exception ex) {
            log.warn(
                    "Staff notification failed for pending-approval booking {}: {}",
                    event.bookingId(),
                    ex.getMessage(),
                    ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingPending(BookingPendingEvent event) {
        try {
            staffNotificationService.notifyBookingReceived(event.bookingId(), true);
        } catch (Exception ex) {
            log.warn(
                    "Staff notification failed for pending booking {}: {}",
                    event.bookingId(),
                    ex.getMessage(),
                    ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCancelled(BookingCancelledEvent event) {
        try {
            staffNotificationService.notifyBookingCancelled(event.bookingId(), event.refundPending());
        } catch (Exception ex) {
            log.warn(
                    "Staff notification failed for cancelled booking {}: {}",
                    event.bookingId(),
                    ex.getMessage(),
                    ex);
        }
    }
}
