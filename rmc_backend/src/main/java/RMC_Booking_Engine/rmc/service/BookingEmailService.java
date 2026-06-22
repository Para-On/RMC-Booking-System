package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;
    private final BookingRepository bookingRepository;

    @EventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        sendConfirmationAsync(event.bookingId());
    }

    @Async
    public void sendConfirmationAsync(Long bookingId) {
        if (!mailProperties.enabled()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        bookingRepository.findByIdWithDetails(bookingId).ifPresent(booking -> sendConfirmation(booking, mailSender));
    }

    private void sendConfirmation(Booking booking, JavaMailSender mailSender) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(booking.getGuest().getEmail());
            message.setSubject("RMC booking confirmed — " + booking.getReference());
            message.setText(buildConfirmationBody(booking));
            mailSender.send(message);
            log.info("Sent confirmation email for booking {}", booking.getReference());
        } catch (Exception ex) {
            log.warn("Failed to send confirmation email for {}: {}", booking.getReference(), ex.getMessage());
        }
    }

    private String buildConfirmationBody(Booking booking) {
        return """
                Dear %s,

                Your booking at RMC is confirmed.

                Reference: %s
                Room type: %s
                Check-in: %s
                Check-out: %s
                Total: %s %s
                Payment: %s

                To view or manage your booking, visit our website and use Find booking with your email address.

                Thank you for choosing RMC.
                """.formatted(
                booking.getGuest().getFullName(),
                booking.getReference(),
                booking.getRoomType().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getCurrency(),
                booking.getQuotedTotal(),
                booking.getPaymentMethod().name().replace('_', ' '));
    }
}
