package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class BookingEmailService {

    private final AppMailProperties mailProperties;
    private final EmailOutboxService emailOutboxService;

    /** Guest email with booking reference as soon as the booking is created (Maya pending payment). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void onBookingPending(BookingPendingEvent event) {
        if (!mailProperties.enabled()) {
            return;
        }
        emailOutboxService.enqueueBookingReceived(event.bookingId());
    }

    /**
     * Guest email with booking reference when pay-at-hotel booking is created (pending approval).
     * Idempotent: Maya payment moving to PENDING_APPROVAL does not enqueue a second BOOKING_RECEIVED.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void onBookingPendingApproval(BookingPendingApprovalEvent event) {
        if (!mailProperties.enabled()) {
            return;
        }
        emailOutboxService.enqueueBookingReceived(event.bookingId());
    }

    @EventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        if (!mailProperties.enabled()) {
            return;
        }
        emailOutboxService.enqueueConfirmation(event.bookingId());
    }

    @EventListener
    @Async
    public void onBookingRejected(BookingRejectedEvent event) {
        if (!mailProperties.enabled()) {
            return;
        }
        emailOutboxService.enqueueRejection(event.bookingId(), event.reason());
    }
}
