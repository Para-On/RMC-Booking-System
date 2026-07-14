package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEmailService {

    private final AppMailProperties mailProperties;
    private final EmailOutboxService emailOutboxService;

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
