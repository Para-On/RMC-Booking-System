package RMC_Booking_Engine.rmc.obs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import RMC_Booking_Engine.rmc.controller.MayaWebhookController;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.service.AvailabilityService;
import RMC_Booking_Engine.rmc.service.BookingHoldService;
import RMC_Booking_Engine.rmc.service.MayaPaymentService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

class AlertSignalTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private OpsAlertSignals signals;

    @BeforeEach
    void attachAppender() {
        signals = new OpsAlertSignals();
        logger = (Logger) LoggerFactory.getLogger(OpsAlertSignals.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        RequestCorrelation.setId("corr-alert-1");
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        RequestCorrelation.clear();
    }

    @Test
    void webhookAndHoldFailuresAreAlertableAndCounted() {
        signals.webhookFailure("RMC-20260929-3989", "processing exploded");
        signals.webhookFailure("RMC-20260929-3989", "processing exploded again");
        signals.holdFailure("RMC-20260929-3989", "release failed");

        assertThat(signals.webhookFailureCount()).isEqualTo(2);
        assertThat(signals.holdFailureCount()).isEqualTo(1);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("ALERT signal=webhook_failure") && msg.contains("count=2"))
                .anyMatch(msg -> msg.contains("ALERT signal=hold_failure") && msg.contains("count=1"));
    }

    @Test
    void webhookControllerRecordsSignalWhenProcessingThrows() {
        MayaPaymentService paymentService = mock(MayaPaymentService.class);
        MayaWebhookController controller = new MayaWebhookController(paymentService, signals);
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "chk-1",
                "chk-1",
                "PAYMENT_SUCCESS",
                "PAYMENT_SUCCESS",
                new BigDecimal("1000.00"),
                null,
                "PHP",
                "RMC-20260929-3989");
        doThrow(new IllegalStateException("db down")).when(paymentService).handleWebhookPayload(payload);

        assertThat(controller.webhook(payload).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signals.webhookFailureCount()).isEqualTo(1);
        verify(paymentService).handleWebhookPayload(payload);
    }

    @Test
    void holdReleaseFailureIsSignaledThenRethrown() {
        InventoryHoldRepository holds = mock(InventoryHoldRepository.class);
        BookingHoldService holdService = new BookingHoldService(
                holds,
                mock(BookingAuditLogRepository.class),
                mock(AvailabilityService.class),
                signals);
        Booking booking = new Booking();
        booking.setId(9L);
        booking.setReference("RMC-20260929-3989");
        doThrow(new IllegalStateException("lock timeout"))
                .when(holds)
                .findByBookingIdAndStatus(9L, HoldStatus.ACTIVE);

        assertThatThrownBy(() -> holdService.releaseActiveHolds(booking))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("lock timeout");
        assertThat(signals.holdFailureCount()).isEqualTo(1);
    }
}
