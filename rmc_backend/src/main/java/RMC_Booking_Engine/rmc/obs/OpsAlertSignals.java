package RMC_Booking_Engine.rmc.obs;

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Grepable, countable signals for webhook and hold failures ({@code RMC-SPEC-OBS-001.2}).
 */
@Component
@Slf4j
public class OpsAlertSignals {

    public static final String WEBHOOK_FAILURE = "webhook_failure";
    public static final String HOLD_FAILURE = "hold_failure";

    private final AtomicLong webhookFailures = new AtomicLong();
    private final AtomicLong holdFailures = new AtomicLong();

    public void webhookFailure(String bookingReference, String reason) {
        long count = webhookFailures.incrementAndGet();
        emit(WEBHOOK_FAILURE, count, bookingReference, reason);
    }

    public void holdFailure(String bookingReference, String reason) {
        long count = holdFailures.incrementAndGet();
        emit(HOLD_FAILURE, count, bookingReference, reason);
    }

    public long webhookFailureCount() {
        return webhookFailures.get();
    }

    public long holdFailureCount() {
        return holdFailures.get();
    }

    private void emit(String signal, long count, String bookingReference, String reason) {
        if (bookingReference != null && !bookingReference.isBlank()) {
            RequestCorrelation.setBookingReference(bookingReference);
        }
        log.warn(
                "ALERT signal={} count={} {} reason={}",
                signal,
                count,
                RequestCorrelation.describe(),
                LogRedaction.redact(reason));
    }
}
