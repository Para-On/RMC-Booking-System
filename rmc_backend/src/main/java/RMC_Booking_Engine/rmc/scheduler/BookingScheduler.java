package RMC_Booking_Engine.rmc.scheduler;

import RMC_Booking_Engine.rmc.obs.LogRedaction;
import RMC_Booking_Engine.rmc.obs.OpsAlertSignals;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
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
    private final OpsAlertSignals opsAlertSignals;

    @Scheduled(fixedDelayString = "${app.scheduler.interval-ms:60000}")
    public void runLifecycleJobs() {
        try {
            bookingLifecycleService.expirePendingPayments();
            bookingLifecycleService.applyPayLaterCutoffs();
            bookingLifecycleService.markNoShows();
            bookingLifecycleService.retryPendingMayaRefunds();
        } catch (Exception ex) {
            opsAlertSignals.holdFailure(null, ex.getMessage());
            log.error(
                    "Booking lifecycle scheduler failed [{}]",
                    RequestCorrelation.describe(),
                    LogRedaction.forLogging(ex));
        }
    }
}
