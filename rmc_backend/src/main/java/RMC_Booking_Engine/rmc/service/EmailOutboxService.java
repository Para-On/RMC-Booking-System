package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.EmailOutbox;
import RMC_Booking_Engine.rmc.domain.enums.EmailOutboxStatus;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.EmailOutboxRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final BookingRepository bookingRepository;
    private final AppMailProperties mailProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Transactional
    public void enqueueConfirmation(Long bookingId) {
        if (!mailProperties.enabled()) {
            return;
        }
        if (emailOutboxRepository.existsByBookingId(bookingId)) {
            return;
        }

        bookingRepository.findByIdWithDetails(bookingId).ifPresent(booking -> {
            EmailOutbox entry = new EmailOutbox();
            entry.setBookingId(booking.getId());
            entry.setRecipient(booking.getGuest().getEmail());
            entry.setSubject("RMC booking confirmed — " + booking.getReference());
            entry.setBody(buildConfirmationBody(booking));
            entry.setStatus(EmailOutboxStatus.PENDING);
            entry.setAttempts(0);
            entry.setMaxAttempts(mailProperties.maxAttempts());
            entry.setNextRetryAt(Instant.now());
            entry.setCreatedAt(Instant.now());
            emailOutboxRepository.save(entry);
            log.info("Queued confirmation email for booking {}", booking.getReference());
        });
    }

    @Transactional
    public int processDueEmails() {
        if (!mailProperties.enabled()) {
            return 0;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return 0;
        }

        List<EmailOutbox> due = emailOutboxRepository.findDueForProcessing(
                EmailOutboxStatus.PENDING, Instant.now());
        int sent = 0;
        for (EmailOutbox entry : due) {
            if (sendEntry(entry, mailSender)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean sendEntry(EmailOutbox entry, JavaMailSender mailSender) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(entry.getRecipient());
            message.setSubject(entry.getSubject());
            message.setText(entry.getBody());
            mailSender.send(message);

            entry.setStatus(EmailOutboxStatus.SENT);
            entry.setSentAt(Instant.now());
            entry.setLastError(null);
            emailOutboxRepository.save(entry);
            log.info("Sent outbox email id={} bookingId={}", entry.getId(), entry.getBookingId());
            return true;
        } catch (Exception ex) {
            entry.setAttempts(entry.getAttempts() + 1);
            entry.setLastError(truncate(ex.getMessage(), 500));
            if (entry.getAttempts() >= entry.getMaxAttempts()) {
                entry.setStatus(EmailOutboxStatus.FAILED);
                log.warn("Email outbox id={} failed permanently after {} attempts: {}",
                        entry.getId(), entry.getAttempts(), ex.getMessage());
            } else {
                long delayMinutes = (long) Math.pow(2, entry.getAttempts())
                        * mailProperties.retryBaseMinutes();
                entry.setNextRetryAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
                log.warn("Email outbox id={} attempt {} failed, retry in {} min: {}",
                        entry.getId(), entry.getAttempts(), delayMinutes, ex.getMessage());
            }
            emailOutboxRepository.save(entry);
            return false;
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
