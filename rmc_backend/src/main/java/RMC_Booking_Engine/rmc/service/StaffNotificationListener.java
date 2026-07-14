package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StaffNotificationListener {

    private final StaffNotificationService staffNotificationService;
    private final BookingRepository bookingRepository;

    @EventListener
    @Transactional
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        Booking booking = bookingRepository
                .findByIdWithDetails(event.bookingId())
                .orElse(null);
        if (booking == null) {
            return;
        }

        if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA) {
            staffNotificationService.notifyBookingPaymentReceived(event.bookingId());
        } else {
            staffNotificationService.notifyBookingReceived(event.bookingId(), false);
        }
    }

    @EventListener
    @Transactional
    public void onBookingPendingApproval(BookingPendingApprovalEvent event) {
        staffNotificationService.notifyBookingAwaitingApproval(event.bookingId());
    }

    @EventListener
    @Transactional
    public void onBookingPending(BookingPendingEvent event) {
        staffNotificationService.notifyBookingReceived(event.bookingId(), true);
    }
}
