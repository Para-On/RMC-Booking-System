package RMC_Booking_Engine.rmc.scheduler;

import RMC_Booking_Engine.rmc.service.BookingLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingLifecycleService bookingLifecycleService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval-ms:60000}")
    public void runLifecycleJobs() {
        try {
            bookingLifecycleService.expirePendingPayments();
            bookingLifecycleService.applyPayLaterCutoffs();
            bookingLifecycleService.markNoShows();
        } catch (Exception ex) {
            log.error("Booking lifecycle scheduler failed", ex);
        }
    }
}
