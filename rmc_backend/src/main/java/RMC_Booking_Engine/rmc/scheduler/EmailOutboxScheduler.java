package RMC_Booking_Engine.rmc.scheduler;

import RMC_Booking_Engine.rmc.service.EmailOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxScheduler {

    private final EmailOutboxService emailOutboxService;

    @Scheduled(fixedDelayString = "${app.mail.outbox-interval-ms:60000}")
    public void processOutbox() {
        try {
            int sent = emailOutboxService.processDueEmails();
            if (sent > 0) {
                log.info("Email outbox processed {} message(s)", sent);
            }
        } catch (Exception ex) {
            log.error("Email outbox scheduler failed", ex);
        }
    }
}
